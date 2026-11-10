package no.timnor.app.shared.features.root.presentation.home.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarExpansionTargetTest {
    @Test
    fun settlesByProgressWhenVelocityIsBelowThreshold() {
        assertFalse(
            calendarExpansionTarget(
                progress = 0.49f,
                velocityPxPerSecond = 0f,
                velocityThresholdPxPerSecond = 1_000f,
            ),
        )
        assertTrue(
            calendarExpansionTarget(
                progress = 0.5f,
                velocityPxPerSecond = 0f,
                velocityThresholdPxPerSecond = 1_000f,
            ),
        )
    }

    @Test
    fun velocitySettlesInItsDirectionBeforeProgressThreshold() {
        assertTrue(
            calendarExpansionTarget(
                progress = 0.1f,
                velocityPxPerSecond = -1_000f,
                velocityThresholdPxPerSecond = 1_000f,
            ),
        )
        assertFalse(
            calendarExpansionTarget(
                progress = 0.9f,
                velocityPxPerSecond = 1_000f,
                velocityThresholdPxPerSecond = 1_000f,
            ),
        )
    }
}
