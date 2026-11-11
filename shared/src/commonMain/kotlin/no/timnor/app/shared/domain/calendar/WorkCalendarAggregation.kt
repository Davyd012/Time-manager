package no.timnor.app.shared.domain.calendar

import kotlinx.datetime.LocalDate
import kotlin.math.absoluteValue

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

/** Formats a calendar cell's hour value to match the legacy Norwegian month grid. */
fun formatCalendarDayHours(totalSeconds: Long): String {
    if (totalSeconds == 0L) return "0"

    val absoluteSeconds = totalSeconds.absoluteValue
    val hundredths = (absoluteSeconds * 100 + 1_800) / 3_600
    val wholeHours = hundredths / 100
    val hundredthRemainder = hundredths % 100
    val fraction = when {
        hundredthRemainder == 0L -> ""
        hundredthRemainder % 10L == 0L -> ",${hundredthRemainder / 10L}"
        else -> ",${hundredthRemainder.toString().padStart(2, '0')}"
    }
    val sign = if (totalSeconds < 0L) "-" else ""
    return "$sign$wholeHours$fraction"
}

/** Matches the compact hour labels used by the Android day strips. */
fun formatDayHours(totalSeconds: Long): String {
    val tenths = (totalSeconds.coerceAtLeast(0) * 10 + 1_800) / 3_600
    return if (tenths % 10 == 0L) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
}
