package no.timnor.app.shared.domain.calendar

import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarDayHoursFormatTest {
    @Test
    fun matchesLegacyNorwegianMonthCellPrecision() {
        assertEquals("0", formatCalendarDayHours(0))
        assertEquals("0,01", formatCalendarDayHours(30))
        assertEquals("0,03", formatCalendarDayHours(90))
        assertEquals("1,5", formatCalendarDayHours(5_400))
        assertEquals("1", formatCalendarDayHours(3_600))
        assertEquals("2", formatCalendarDayHours(7_200))
    }
}
