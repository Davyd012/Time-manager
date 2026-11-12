package no.timnor.app.shared.domain.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class MonthGridLayoutTest {
    @Test
    fun leapFebruaryHasTwentyNineDaysAndMondayFirstOffset() {
        assertEquals(
            MonthGridLayout(leadingEmptyDays = 3, daysInMonth = 29, trailingEmptyDays = 3),
            monthGridLayout(year = 2024, month = 2),
        )
    }

    @Test
    fun monthStartingOnSundayUsesSixLeadingCells() {
        assertEquals(
            MonthGridLayout(leadingEmptyDays = 6, daysInMonth = 31, trailingEmptyDays = 5),
            monthGridLayout(year = 2021, month = 8),
        )
    }

    @Test
    fun mondayStartingThirtyDayMonthHasTwoRowsOfPadding() {
        assertEquals(
            MonthGridLayout(leadingEmptyDays = 0, daysInMonth = 30, trailingEmptyDays = 5),
            monthGridLayout(year = 2024, month = 4),
        )
    }

    @Test
    fun sameMonthCheckDisablesAdjacentWeekDatesIncludingYearBoundary() {
        val january = LocalDate(2025, 1, 1)

        assertTrue(isSameMonth(LocalDate(2025, 1, 31), january))
        assertFalse(isSameMonth(LocalDate(2024, 12, 31), january))
        assertFalse(isSameMonth(LocalDate(2025, 2, 1), january))
    }

    @Test
    fun november2026UsesThirdRowForNovemberNinthThroughFifteenth() {
        val dates = monthGridDates(2026, 11)
        assertEquals(42, dates.size)
        assertEquals((9..15).map { LocalDate(2026, 11, it) }, dates.subList(14, 21))
        assertEquals(2, monthGridRow(LocalDate(2026, 11, 11), 2026, 11))
        assertEquals((9..15).map { LocalDate(2026, 11, it) }, weekDates(LocalDate(2026, 11, 11)))
    }

    @Test
    fun monthGridDatesIncludeRealAdjacentMonthDates() {
        val dates = monthGridDates(2025, 1)
        assertEquals(LocalDate(2024, 12, 30), dates.first())
        assertEquals(LocalDate(2025, 2, 2), dates.last())
        assertEquals(0, monthGridRow(LocalDate(2025, 1, 1), 2025, 1))
        assertEquals(null, monthGridRow(LocalDate(2024, 12, 31), 2025, 1))
    }

    @Test
    fun adjacentMonthDatesKeepTheirVisibleGridRows() {
        assertEquals(0, monthGridDateRow(LocalDate(2026, 10, 26), 2026, 11))
        assertEquals(5, monthGridDateRow(LocalDate(2026, 12, 6), 2026, 11))
        assertEquals(null, monthGridDateRow(LocalDate(2026, 10, 25), 2026, 11))
    }

    @Test
    fun interpolationClampsProgressAndHasExactEndpoints() {
        assertEquals(10f, interpolateCalendarCoordinate(10f, 30f, 0f))
        assertEquals(20f, interpolateCalendarCoordinate(10f, 30f, 0.5f))
        assertEquals(30f, interpolateCalendarCoordinate(10f, 30f, 1f))
        assertEquals(10f, interpolateCalendarCoordinate(10f, 30f, -1f))
        assertEquals(30f, interpolateCalendarCoordinate(10f, 30f, 2f))
    }
}
