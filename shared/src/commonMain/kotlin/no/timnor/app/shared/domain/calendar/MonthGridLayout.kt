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
