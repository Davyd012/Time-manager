package no.timnor.app.shared.features.month_view.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

class MonthViewDayCellMotionTest {
    @Test
    fun selectedAndPressedScalesMatchLegacyDayCellPriority() {
        assertEquals(1.04f, monthViewDayCellScale(selected = true, pressed = false))
        assertEquals(0.96f, monthViewDayCellScale(selected = true, pressed = true))
        assertEquals(0.96f, monthViewDayCellScale(selected = false, pressed = true))
        assertEquals(1f, monthViewDayCellScale(selected = false, pressed = false))
    }
}
