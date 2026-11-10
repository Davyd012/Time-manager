package no.timnor.app.shared.domain.time

import kotlin.test.Test
import kotlin.test.assertEquals

class StopwatchTimeFormatTest {
    @Test
    fun formatsTotalMinutesAndSeconds() {
        assertEquals("00:00", formatStopwatchTime(0))
        assertEquals("00:45", formatStopwatchTime(45))
        assertEquals("01:05", formatStopwatchTime(65))
        assertEquals("60:00", formatStopwatchTime(3_600))
    }

    @Test
    fun negativeElapsedTimeClampsToZero() {
        assertEquals("00:00", formatStopwatchTime(-1))
    }
}
