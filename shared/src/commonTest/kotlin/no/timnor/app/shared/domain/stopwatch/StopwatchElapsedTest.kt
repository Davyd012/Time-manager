package no.timnor.app.shared.domain.stopwatch

import kotlin.test.Test
import kotlin.test.assertEquals

class StopwatchElapsedTest {
    @Test
    fun projectSelectionLocksOnceElapsedTimeHasBeenRecorded() {
        assertEquals(true, canChangeStopwatchProject(0))
        assertEquals(false, canChangeStopwatchProject(1))
        assertEquals(false, canChangeStopwatchProject(3_600))
    }

    @Test
    fun derivesElapsedTimeFromPersistedStartInstantAfterResume() {
        assertEquals(135_500L, stopwatchElapsedMillis(45_500, startedAtMillis = 1_000_000, nowMillis = 1_090_000))
    }

    @Test
    fun keepsPausedTimeAndClampsInvalidOrUnrepresentableValues() {
        assertEquals(120L, stopwatchElapsedMillis(120, startedAtMillis = null, nowMillis = 10_000))
        assertEquals(30L, stopwatchElapsedMillis(30, startedAtMillis = 2_000, nowMillis = 1_900))
        assertEquals(MAX_STOPWATCH_MILLIS, stopwatchElapsedMillis(MAX_STOPWATCH_MILLIS, 0, Long.MAX_VALUE))
    }
}
