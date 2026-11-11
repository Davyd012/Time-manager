package no.timnor.app.shared.features.root.presentation.home.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeCalendarToggleTest {
    @Test
    fun expansionDragUsesFastFlingDirectionBeforeProgressThreshold() {
        assertTrue(
            calendarExpansionTarget(
                progress = 0.2f,
                velocityPxPerSecond = -600f,
                velocityThresholdPxPerSecond = 500f,
            ),
        )
        assertFalse(
            calendarExpansionTarget(
                progress = 0.8f,
                velocityPxPerSecond = 600f,
                velocityThresholdPxPerSecond = 500f,
            ),
        )
    }

    @Test
    fun expansionDragUsesProgressThresholdWhenFlingIsSlow() {
        assertFalse(
            calendarExpansionTarget(
                progress = 0.49f,
                velocityPxPerSecond = -100f,
                velocityThresholdPxPerSecond = 500f,
            ),
        )
        assertTrue(
            calendarExpansionTarget(
                progress = 0.5f,
                velocityPxPerSecond = 100f,
                velocityThresholdPxPerSecond = 500f,
            ),
        )
    }
}
