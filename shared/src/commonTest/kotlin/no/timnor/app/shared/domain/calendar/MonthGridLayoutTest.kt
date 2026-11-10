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
}
