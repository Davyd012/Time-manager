package no.timnor.app.shared.features.calendar.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectHoursFormatterTest {
    @Test
    fun matchesNorwegianNumberFormatWithAtMostTwoDecimalPlaces() {
        assertEquals("1", formatHours(3_600, ','))
        assertEquals("1,5", formatHours(5_400, ','))
        assertEquals("1,56", formatHours(5_616, ','))
    }

    @Test
    fun roundsTiesToEvenLikeAndroidNumberFormat() {
        assertEquals("0", formatHours(18, ','))
        assertEquals("0,02", formatHours(54, ','))
    }
}
