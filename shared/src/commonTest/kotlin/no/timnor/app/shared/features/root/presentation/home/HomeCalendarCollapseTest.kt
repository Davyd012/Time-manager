package no.timnor.app.shared.features.root.presentation.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeCalendarCollapseTest {
    @Test
    fun collapseUsesLiveDragProgressWhenGestureIsInFlight() {
        assertEquals(
            0.24f,
            homeCalendarCollapseStartProgress(
                isDragging = true,
                dragProgress = 0.24f,
                animationProgress = 0f,
            ),
        )
    }

    @Test
    fun collapseUsesAnimationProgressWhenNoGestureIsInFlight() {
        assertEquals(
            0.72f,
            homeCalendarCollapseStartProgress(
                isDragging = false,
                dragProgress = 0.24f,
                animationProgress = 0.72f,
            ),
        )
    }
}
