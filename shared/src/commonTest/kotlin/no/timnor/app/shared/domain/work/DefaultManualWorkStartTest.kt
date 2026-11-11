package no.timnor.app.shared.domain.work

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultManualWorkStartTest {
    @Test
    fun todayDefaultsToNowMinusEnteredDuration() {
        val now = LocalDateTime(LocalDate(2026, 11, 10), LocalTime(14, 30))

        assertEquals(
            LocalDateTime(LocalDate(2026, 11, 10), LocalTime(12, 15)),
            defaultManualWorkStart(now.date, 2 * 3_600 + 15 * 60, now),
        )
    }

    @Test
    fun otherDatesDefaultToMidnight() {
        val now = LocalDateTime(LocalDate(2026, 11, 10), LocalTime(14, 30))
        val selectedDate = LocalDate(2026, 11, 9)

        assertEquals(
            LocalDateTime(selectedDate, LocalTime(0, 0)),
            defaultManualWorkStart(selectedDate, 3_600, now),
        )
    }

    @Test
    fun defaultStartCanCrossIntoPreviousDay() {
        val now = LocalDateTime(LocalDate(2026, 11, 10), LocalTime(0, 30))

        assertEquals(
            LocalDateTime(LocalDate(2026, 11, 9), LocalTime(23, 30)),
            defaultManualWorkStart(now.date, 3_600, now),
        )
    }
}
