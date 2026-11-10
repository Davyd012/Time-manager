package no.timnor.app.shared.domain.calendar

import kotlinx.datetime.LocalDate

/** Aggregates work duration by civil day for one calendar month. */
fun <T> workSecondsByDay(
    entries: List<T>,
    year: Int,
    month: Int,
    dateOf: (T) -> LocalDate,
    secondsOf: (T) -> Int,
): Map<Int, Int> = entries.asSequence()
    .filter { entry ->
        val date = dateOf(entry)
        date.year == year && date.month.ordinal + 1 == month
    }
    .groupBy { dateOf(it).day }
    .mapValues { (_, dayEntries) -> dayEntries.sumOf(secondsOf) }

/** Returns the total duration in decimal hours, matching the calendar summary. */
fun <T> totalWorkHours(entries: Iterable<T>, secondsOf: (T) -> Int): Double =
    entries.sumOf(secondsOf) / 3600.0

/** Formats decimal hours using the Norwegian decimal separator used by the app's resources. */
fun formatCalendarHours(totalSeconds: Long): String {
    val tenths = (totalSeconds.coerceAtLeast(0) * 10 + 1_800) / 3_600
    return "${tenths / 10},${tenths % 10}"
}
