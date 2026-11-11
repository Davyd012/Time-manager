package no.timnor.app.shared.features.month_view.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkTimelineDurationLabelTest {
    @Test
    fun durationLabelsRespectTheProvidedHostDecimalSeparator() {
        assertNull(workTimelineDurationLabel(seconds = 8_280, showDuration = false))
        assertEquals("2,3h", workTimelineDurationLabel(seconds = 8_280, showDuration = true))
        assertEquals("2.3h", workTimelineDurationLabel(seconds = 8_280, showDuration = true, decimalSeparator = '.'))
    }
}
