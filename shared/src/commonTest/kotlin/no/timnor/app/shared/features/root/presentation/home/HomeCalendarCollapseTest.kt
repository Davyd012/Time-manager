package no.timnor.app.shared.features.root.presentation.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.unit.dp
import no.timnor.app.shared.ui.theme.TimnorSpacing

class HomeCalendarCollapseTest {
    @Test
    fun gestureArbitrationLeavesOppositeAnchorDirectionForScrolling() {
        assertFalse(shouldClaimCalendarDrag(progress = 0f, verticalDelta = -20f))
        assertTrue(shouldClaimCalendarDrag(progress = 0f, verticalDelta = 20f))
        assertTrue(shouldClaimCalendarDrag(progress = 1f, verticalDelta = -20f))
        assertFalse(shouldClaimCalendarDrag(progress = 1f, verticalDelta = 20f))
    }

    @Test
    fun eitherDirectionClaimsWhileCalendarIsBetweenAnchors() {
        assertTrue(shouldClaimCalendarDrag(progress = 0.24f, verticalDelta = -1f))
        assertTrue(shouldClaimCalendarDrag(progress = 0.72f, verticalDelta = 1f))
        assertFalse(shouldClaimCalendarDrag(progress = 0.5f, verticalDelta = 0f))
    }

    @Test
    fun expansionTravelUsesDisplayedRowsAndFontScale() {
        assertEquals(468.dp, calendarExpansionTravel(6, 1f, TimnorSpacing()))
        assertEquals(340.dp, calendarExpansionTravel(4, 1f, TimnorSpacing()))
        assertTrue(calendarExpansionTravel(6, 1.5f, TimnorSpacing()) > 468.dp)
    }
}
