package com.pantri.app.domain

import com.pantri.app.data.StorePrice
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** What one pack price turns into per unit, so 500 g at ₱80 and 1 kg at ₱140 can be compared. */
data class Unitised(val group: String, val perUnitCentavos: Double)

/** "kg" and "g" share a group (priced per kg), "l" and "ml" too. Any other unit, or none, only matches itself. */
fun unitised(priceCentavos: Long, quantity: Double, unit: String): Unitised? {
    if (quantity <= 0) return null
    val (group, factor) = when (normUnit(unit)) {
        "kg" -> "kg" to 1.0
        "g" -> "kg" to 0.001
        "l" -> "l" to 1.0
        "ml" -> "l" to 0.001
        else -> normUnit(unit) to 1.0
    }
    return Unitised(group, priceCentavos / (quantity * factor))
}

/** "per kg", "per L", "each" or "per bunch". */
fun perLabel(group: String) = when (group) {
    "kg" -> "per kg"
    "l" -> "per L"
    "", "pc" -> "each"
    else -> "per $group"
}

data class Offer(
    val price: StorePrice,
    val storeName: String,
    /** Centavos per unit in the comparison group; null when this price uses a unit that cannot be compared with the others. */
    val perUnit: Double?,
    val group: String,
    val best: Boolean,
    /** Centavos per unit above the cheapest; 0 for the cheapest. */
    val extraPerUnit: Double,
)

/**
 * Ranks [prices] (all for one product) cheapest first. Prices are compared in the unit most stores share (newest wins a tie);
 * prices in another unit are listed last and marked not comparable.
 */
fun rankOffers(prices: List<StorePrice>, nameOf: (Long) -> String): List<Offer> {
    val withUnit = prices.map { it to unitised(it.priceCentavos, it.quantity, it.unit) }
    val group = withUnit.mapNotNull { it.second?.group }.groupingBy { it }.eachCount().let { counts ->
        counts.maxWithOrNull(compareBy({ it.value }, { g -> withUnit.filter { it.second?.group == g.key }.maxOf { it.first.at } }))?.key
    }
    val comparable = withUnit.filter { it.second != null && it.second!!.group == group }.sortedBy { it.second!!.perUnitCentavos }
    val cheapest = comparable.firstOrNull()?.second?.perUnitCentavos
    val others = withUnit.filter { it !in comparable }.sortedBy { nameOf(it.first.storeId).lowercase() }
    return comparable.map { (p, u) -> Offer(p, nameOf(p.storeId), u!!.perUnitCentavos, u.group, u.perUnitCentavos <= cheapest!! + 1e-9, u.perUnitCentavos - cheapest!!) } +
        others.map { (p, u) -> Offer(p, nameOf(p.storeId), null, u?.group ?: "", false, 0.0) }
}

/** Is an automatic backup due? [weekday] 1 to 7 (Monday to Sunday) means weekly on that day; otherwise every [days] days; both 0 means off. */
fun isBackupDue(today: LocalDate, last: LocalDate?, days: Int, weekday: Int): Boolean {
    // A last backup dated in the future means the phone's clock was set back; back up again rather than wait for it.
    if (weekday in 1..7) {
        val latest = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(weekday)))
        return last == null || last.isAfter(today) || last.isBefore(latest)
    }
    if (days <= 0) return false
    return last == null || last.isAfter(today) || ChronoUnit.DAYS.between(last, today) >= days
}
