package no.timnor.app.shared.features.calendar.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectFilterHoursFormatTest {
    @Test
    fun hoursUseTheSuppliedHostLocaleDecimalSeparator() {
        assertEquals("1,25", formatHours(4_500, ','))
        assertEquals("1.25", formatHours(4_500, '.'))
    }

    @Test
    fun wholeAndSingleDecimalHoursKeepCompactLegacyFormatting() {
        assertEquals("2", formatHours(7_200, '.'))
        assertEquals("0.5", formatHours(1_800, '.'))
    }
}
