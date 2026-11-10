package no.timnor.app.shared.features.month_view.presentation

import no.timnor.app.shared.features.month_view.presentation.components.formatMonthViewDuration
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthViewDurationFormatTest {
    @Test
    fun durationUsesDecimalHoursAndMinutes() {
        assertEquals("1,5h", formatMonthViewDuration(5_400))
        assertEquals("1,5m", formatMonthViewDuration(90))
        assertEquals("30 s", formatMonthViewDuration(30))
    }
}
