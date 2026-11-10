package com.pantri.app.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val whole = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
private val cents = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

/** Pesos from centavos: "₱1,430", or "₱1,430.50" when there are centavos. */
fun pesos(centavos: Long): String {
    val neg = centavos < 0
    val a = kotlin.math.abs(centavos)
    val s = if (a % 100 == 0L) whole.format(a / 100) else cents.format(a / 100.0)
    return (if (neg) "-₱" else "₱") + s
}

/** Parses "65", "65.5" or "1,200.25" into centavos. Returns null for anything else, and for negatives. */
fun parsePesos(text: String): Long? {
    val t = text.trim().replace(",", "").removePrefix("₱")
    if (t.isEmpty()) return null
    val d = t.toBigDecimalOrNull() ?: return null
    if (d.signum() < 0 || d > java.math.BigDecimal("999999999")) return null
    return d.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
}

/** "1", "0.5", "2.25": no trailing zeros. */
fun qty(q: Double): String = if (q % 1.0 == 0.0) q.toLong().toString() else "%.2f".format(Locale.US, q).trimEnd('0').trimEnd('.')

/** Parses a quantity; null when empty, zero or not a number. */
fun parseQty(text: String): Double? = text.trim().toDoubleOrNull()?.takeIf { it > 0 && it < 1_000_000 }
