package com.pantri.app.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * A budget period. Pay comes twice a month, so each month has two: the 1st to the 15th,
 * and the 16th to the last day of the month (shortcut: month end instead of a fixed 30th, so February works).
 */
data class Cutoff(val year: Int, val month: Int, val second: Boolean) {
    val key: String get() = "%04d-%02d-%s".format(year, month, if (second) "B" else "A")
    val start: LocalDate get() = LocalDate.of(year, month, if (second) 16 else 1)
    val end: LocalDate get() = if (second) YearMonth.of(year, month).atEndOfMonth() else LocalDate.of(year, month, 15)

    fun startMillis(zone: ZoneId = ZoneId.systemDefault()): Long = start.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endMillis(zone: ZoneId = ZoneId.systemDefault()): Long = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    fun previous(): Cutoff =
        if (second) Cutoff(year, month, false)
        else YearMonth.of(year, month).minusMonths(1).let { Cutoff(it.year, it.monthValue, true) }

    /** Days left including today's end; 0 means the cutoff is over. */
    fun daysLeft(today: LocalDate): Int = (ChronoUnit.DAYS.between(today, end).toInt() + 1).coerceAtLeast(0)

    /** "Nov 1 to 15" */
    fun label(): String {
        val m = start.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        return "$m ${start.dayOfMonth} to ${end.dayOfMonth}"
    }

    companion object {
        fun of(date: LocalDate) = Cutoff(date.year, date.monthValue, date.dayOfMonth > 15)
    }
}
