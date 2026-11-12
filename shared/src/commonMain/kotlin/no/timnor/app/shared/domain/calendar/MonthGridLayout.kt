package no.timnor.app.shared.domain.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.isoDayNumber

/** Number of empty cells around a month in a Monday-first calendar grid. */
data class MonthGridLayout(
    val leadingEmptyDays: Int,
    val daysInMonth: Int,
    val trailingEmptyDays: Int,
)

/** Returns whether both civil dates belong to the same calendar month. */
fun isSameMonth(date: LocalDate, month: LocalDate): Boolean =
    date.year == month.year && date.month == month.month

/**
 * Returns the month grid dimensions using ISO weekdays (Monday first).
 * This operates on civil dates and does not convert them to instants or time zones.
 */
fun monthGridLayout(year: Int, month: Int): MonthGridLayout {
    val firstDay = LocalDate(year, month, 1)
    val leadingEmptyDays = firstDay.dayOfWeek.isoDayNumber - 1
    val daysInMonth = firstDay.plus(1, DateTimeUnit.MONTH)
        .minus(1, DateTimeUnit.DAY)
        .day
    val trailingEmptyDays = (7 - (leadingEmptyDays + daysInMonth) % 7) % 7

    return MonthGridLayout(
        leadingEmptyDays = leadingEmptyDays,
        daysInMonth = daysInMonth,
        trailingEmptyDays = trailingEmptyDays,
    )
}

/** All civil dates occupying the Monday-first month grid, including adjacent months. */
fun monthGridDates(year: Int, month: Int): List<LocalDate> {
    val layout = monthGridLayout(year, month)
    val first = LocalDate(year, month, 1).minus(layout.leadingEmptyDays, DateTimeUnit.DAY)
    return List(layout.leadingEmptyDays + layout.daysInMonth + layout.trailingEmptyDays) { offset ->
        first.plus(offset, DateTimeUnit.DAY)
    }
}

/** Zero-based row for a date, or null when the date belongs to another month. */
fun monthGridRow(date: LocalDate, year: Int, month: Int): Int? {
    if (date.year != year || date.month.ordinal + 1 != month) return null
    return (monthGridLayout(year, month).leadingEmptyDays + date.day - 1) / 7
}

/** Zero-based row for any date shown in the Monday-first grid, including adjacent months. */
fun monthGridDateRow(date: LocalDate, year: Int, month: Int): Int? {
    val index = monthGridDates(year, month).indexOf(date)
    return index.takeIf { it >= 0 }?.div(7)
}

/** Monday-first week containing [date]. */
fun weekDates(date: LocalDate): List<LocalDate> {
    val monday = date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    return List(7) { monday.plus(it, DateTimeUnit.DAY) }
}

/** Linear coordinate used by the calendar's measured date-cell layout. */
fun interpolateCalendarCoordinate(start: Float, end: Float, progress: Float): Float =
    start + (end - start) * progress.coerceIn(0f, 1f)
