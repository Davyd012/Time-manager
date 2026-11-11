package no.timnor.app.shared.features.root.presentation.home.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeDailyWorkListInteractionTest {
    @Test
    fun listRemainsInteractiveUntilCalendarExpansionPassesLegacyThreshold() {
        assertFalse(shouldBlockHomeWorkListInput(0f))
        assertFalse(shouldBlockHomeWorkListInput(0.25f))
        assertFalse(shouldBlockHomeWorkListInput(0.8f))
        assertTrue(shouldBlockHomeWorkListInput(0.801f))
        assertTrue(shouldBlockHomeWorkListInput(1f))
    }
}
