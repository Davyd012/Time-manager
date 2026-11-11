package no.timnor.app.shared.domain.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class WorkCalendarAggregationTest {
    @Test
    fun compactDayHoursMatchAndroidLabels() {
        assertEquals("0", formatDayHours(0))
        assertEquals("1", formatDayHours(3_600))
        assertEquals("1.5", formatDayHours(5_400))
    }

    @Test
    fun sumsEntriesByDayAndExcludesAdjacentMonths() {
        val entries = listOf(
            Entry(LocalDate(2024, 2, 1), 3_600),
            Entry(LocalDate(2024, 2, 1), 1_800),
            Entry(LocalDate(2024, 2, 29), 7_200),
            Entry(LocalDate(2024, 3, 1), 9_000),
        )

        assertEquals(mapOf(1 to 5_400, 29 to 7_200), workSecondsByDay(entries, 2024, 2, Entry::date, Entry::seconds))
        assertEquals(3.5, totalWorkHours(entries.take(3), Entry::seconds))
    }

    private data class Entry(val date: LocalDate, val seconds: Int)
}
