package no.timnor.app.shared.features.root.presentation.home.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmptyListExpansionTargetTest {
    @Test
    fun slowDragUsesCappedDistanceFromStartingAnchor() {
        fun target(progress: Float, expanded: Boolean) = emptyListExpansionTarget(
            progress, expanded, velocity = 0f, velocityThreshold = 500f,
            distance = 800f, positionalThreshold = 56f,
        )
        assertFalse(target(0.06f, false))
        assertTrue(target(0.08f, false))
        assertTrue(target(0.94f, true))
        assertFalse(target(0.92f, true))
    }

    @Test
    fun flingDirectionOverridesDistance() {
        assertTrue(emptyListExpansionTarget(0.01f, false, 600f, 500f, 800f, 56f))
        assertFalse(emptyListExpansionTarget(0.99f, true, -600f, 500f, 800f, 56f))
    }
}
