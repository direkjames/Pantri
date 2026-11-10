package com.pantri.app

import com.pantri.app.data.HistoryRow
import com.pantri.app.data.Item
import com.pantri.app.data.streakOf
import com.pantri.app.domain.Cutoff
import com.pantri.app.domain.Incoming
import com.pantri.app.domain.mergeIngredients
import com.pantri.app.domain.parsePesos
import com.pantri.app.domain.pesos
import com.pantri.app.domain.qty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LogicTest {
    @Test fun cutoffHalves() {
        assertEquals(Cutoff(2026, 11, false), Cutoff.of(LocalDate.of(2026, 11, 10)))
        assertEquals(Cutoff(2026, 11, false), Cutoff.of(LocalDate.of(2026, 11, 15)))
        assertEquals(Cutoff(2026, 11, true), Cutoff.of(LocalDate.of(2026, 11, 16)))
        assertEquals(LocalDate.of(2026, 11, 30), Cutoff(2026, 11, true).end)
        assertEquals(LocalDate.of(2028, 2, 29), Cutoff(2028, 2, true).end)
        assertEquals(LocalDate.of(2027, 2, 28), Cutoff(2027, 2, true).end)
    }

    @Test fun cutoffPrevious() {
        assertEquals(Cutoff(2026, 11, false), Cutoff(2026, 11, true).previous())
        assertEquals(Cutoff(2025, 12, true), Cutoff(2026, 1, false).previous())
        assertEquals("Nov 1 to 15", Cutoff(2026, 11, false).label())
        assertEquals(5, Cutoff(2026, 11, false).daysLeft(LocalDate.of(2026, 11, 11)))
        assertEquals(0, Cutoff(2026, 11, false).daysLeft(LocalDate.of(2026, 11, 16)))
    }

    @Test fun moneyFormatting() {
        assertEquals("₱1,430", pesos(143_000))
        assertEquals("₱65.50", pesos(6_550))
        assertEquals("-₱410", pesos(-41_000))
        assertEquals(6_550L, parsePesos("65.5"))
        assertEquals(120_025L, parsePesos("₱1,200.25"))
        assertNull(parsePesos("abc"))
        assertNull(parsePesos("-5"))
        assertEquals("0.5", qty(0.5))
        assertEquals("2", qty(2.0))
    }

    private fun item(name: String, q: Double, unit: String, price: Long = 0, checked: Boolean = false) =
        Item(id = 1, listId = 1, name = name, quantity = q, unit = unit, priceCentavos = price, checked = checked, createdAt = 0)

    @Test fun mergeAddsSameUnitOnly() {
        val existing = listOf(item("Onion", 2.0, "pc", 4_000), item("Rice", 1.0, "kg", 5_000))
        val plan = mergeIngredients(
            existing,
            listOf(Incoming("onion", 1.0, "pcs", "Produce"), Incoming("Onion", 1.0, "pc", "Produce"), Incoming("Rice", 500.0, "g", "Pantry")),
        )
        assertEquals(1, plan.updates.size)
        assertEquals(4.0, plan.updates[0].quantity, 0.0)
        assertEquals(8_000L, plan.updates[0].priceCentavos)
        assertEquals(listOf("Rice"), plan.inserts.map { it.name })
    }

    @Test fun mergeIgnoresCheckedItems() {
        val plan = mergeIngredients(listOf(item("Milk", 1.0, "l", 11_000, checked = true)), listOf(Incoming("Milk", 1.0, "L", "Dairy")))
        assertEquals(0, plan.updates.size)
        assertEquals(1, plan.inserts.size)
    }

    @Test fun streakStopsAtOverspendOrGap() {
        val now = Cutoff(2026, 11, false)
        val p1 = now.previous()
        val p2 = p1.previous()
        val p3 = p2.previous()
        val rows = listOf(HistoryRow(p1, 600_000, 500_000), HistoryRow(p2, 600_000, 590_000), HistoryRow(p3, 600_000, 650_000))
        assertEquals(2, streakOf(rows, p1))
        assertEquals(0, streakOf(rows.drop(1), p1))
    }
}
