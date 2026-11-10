package com.pantri.app

import com.pantri.app.data.BackupData
import com.pantri.app.data.BackupException
import com.pantri.app.data.GroceryList
import com.pantri.app.data.Item
import com.pantri.app.data.Product
import com.pantri.app.data.Store
import com.pantri.app.data.StorePrice
import com.pantri.app.data.parseBackup
import com.pantri.app.data.toJson
import com.pantri.app.domain.isBackupDue
import com.pantri.app.domain.rankOffers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class StoresBackupTest {
    private fun price(store: Long, centavos: Long, qty: Double, unit: String, at: Long = 1) =
        StorePrice(storeId = store, productKey = "rice", priceCentavos = centavos, quantity = qty, unit = unit, at = at)

    private val names = mapOf(1L to "OSave", 2L to "Savemore", 3L to "Market")

    @Test fun differentPackSizesAreComparedPerKg() {
        // 500 g at 80 pesos is 160 per kg; 1 kg at 140 pesos is cheaper.
        val ranked = rankOffers(listOf(price(1, 8000, 500.0, "g"), price(2, 14000, 1.0, "kg"))) { names[it]!! }
        assertEquals("Savemore", ranked[0].storeName)
        assertTrue(ranked[0].best)
        assertFalse(ranked[1].best)
        assertEquals(2000.0, ranked[1].extraPerUnit, 0.001)
    }

    @Test fun otherUnitsAreListedButNotRanked() {
        val ranked = rankOffers(listOf(price(1, 8000, 1.0, "kg"), price(2, 9000, 1.0, "kg"), price(3, 500, 1.0, "pack"))) { names[it]!! }
        assertEquals(listOf("OSave", "Savemore", "Market"), ranked.map { it.storeName })
        assertNull(ranked[2].perUnit)
        assertFalse(ranked[2].best)
    }

    @Test fun noUnitMeansPerEach() {
        val ranked = rankOffers(listOf(price(1, 3000, 2.0, ""), price(2, 1400, 1.0, ""))) { names[it]!! }
        assertEquals("Savemore", ranked[0].storeName) // 14 each beats 15 each
    }

    @Test fun backupDaySchedule() {
        val today = LocalDate.of(2026, 11, 11) // a Wednesday
        assertFalse(isBackupDue(today, null, 0, 0))
        assertTrue(isBackupDue(today, null, 3, 0))
        assertFalse(isBackupDue(today, today.minusDays(2), 3, 0))
        assertTrue(isBackupDue(today, today.minusDays(3), 3, 0))
        // weekly on Monday (1): due once the most recent Monday (Nov 9) has passed without a backup
        assertTrue(isBackupDue(today, today.minusDays(5), 0, 1))
        assertFalse(isBackupDue(today, LocalDate.of(2026, 11, 9), 0, 1))
        // weekly on Friday (5): the latest Friday was Nov 6, so a backup on Nov 7 is current
        assertFalse(isBackupDue(today, LocalDate.of(2026, 11, 7), 0, 5))
        assertTrue(isBackupDue(today, LocalDate.of(2026, 11, 5), 0, 5))
    }

    @Test fun backupRoundTrip() {
        val data = BackupData(
            createdAt = 123, darkMode = true, haptics = false, defaultBudget = 600_000,
            lists = listOf(GroceryList(1, "Weekly \"shop\"\nline", false, 5, 2)),
            items = listOf(Item(1, 1, "Mangga", 1.5, "kg", "Produce", "ripe", 9900, true, 77, 3, 6)),
            products = listOf(Product("mangga", "Mangga", "Produce", "kg", 6600, 2, true, 9)),
            stores = listOf(Store(2, "OSave", 4)),
            storePrices = listOf(StorePrice(1, "mangga", 2, 9900, 1.5, "kg", 8)),
        )
        val back = parseBackup(data.toJson())
        assertEquals(data, back)
    }

    @Test fun oldBackupWithoutStoresStillReads() {
        val back = parseBackup("""{"app":"Pantri","format":1,"createdAt":5,"lists":[]}""")
        assertTrue(back.stores.isEmpty())
    }

    @Test fun badFilesAreRefusedWithAMessage() {
        for (text in listOf("not json", "{}", """{"app":"Other","format":1,"createdAt":1}""", """{"app":"Pantri","format":99,"createdAt":1}""", """{"app":"Pantri","format":1,"createdAt":1,"items":[{"id":1}]}""")) {
            try { parseBackup(text); fail("accepted: $text") } catch (e: BackupException) { assertTrue(e.message!!.isNotBlank()) }
        }
    }
}
