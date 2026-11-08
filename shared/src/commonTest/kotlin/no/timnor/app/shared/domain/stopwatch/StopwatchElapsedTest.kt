package no.timnor.app.shared.domain.stopwatch

import kotlin.test.Test
import kotlin.test.assertEquals

class StopwatchElapsedTest {
    @Test
    fun derivesElapsedTimeFromPersistedStartInstantAfterResume() {
        assertEquals(135, stopwatchElapsedSeconds(45, startedAtEpochSeconds = 1_000, nowEpochSeconds = 1_090))
    }

    @Test
    fun keepsPausedTimeAndClampsInvalidOrUnrepresentableValues() {
        assertEquals(120, stopwatchElapsedSeconds(120, startedAtEpochSeconds = null, nowEpochSeconds = 10_000))
        assertEquals(30, stopwatchElapsedSeconds(30, startedAtEpochSeconds = 2_000, nowEpochSeconds = 1_900))
        assertEquals(Int.MAX_VALUE, stopwatchElapsedSeconds(Int.MAX_VALUE.toLong(), 0, Long.MAX_VALUE))
    }
}
