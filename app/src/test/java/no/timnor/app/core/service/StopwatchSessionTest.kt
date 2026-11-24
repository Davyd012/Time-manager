package no.timnor.app.core.service

import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchSessionTest {
    @Test
    fun elapsedTimeIncludesSleepWithoutAnyIntermediateTicks() {
        val running = StopwatchSession().start(nowMillis = 10_000)

        assertEquals(600_250L, running.elapsedMillis(nowMillis = 610_250))
        assertEquals(600_250L, running.pause(nowMillis = 610_250).elapsedMillis(nowMillis = 800_000))
    }

    @Test
    fun resumePreservesFractionsAndExcludesPausedTime() {
        val resumed = StopwatchSession().start(nowMillis = 10_000)
            .pause(nowMillis = 10_750)
            .start(nowMillis = 50_000)

        assertEquals(1_000L, resumed.elapsedMillis(nowMillis = 50_250))
        assertEquals(1_000L, resumed.start(nowMillis = 50_100).elapsedMillis(nowMillis = 50_250))
    }
}
