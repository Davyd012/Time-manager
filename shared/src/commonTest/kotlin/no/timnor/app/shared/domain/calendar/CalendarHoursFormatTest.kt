package no.timnor.app.shared.domain.calendar

import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarHoursFormatTest {
    @Test
    fun formatsDecimalHoursWithNorwegianSeparator() {
        assertEquals("0,0", formatCalendarHours(0))
        assertEquals("1,5", formatCalendarHours(5_400))
        assertEquals("25,0", formatCalendarHours(90_000))
    }
}
