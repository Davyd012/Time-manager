package no.timnor.app.shared.features.root.presentation.home

import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarPanelVisibleHeightTest {
    @Test
    fun panelTracksProgressAndAvailableHeight() {
        assertEquals(listOf(0, 200, 400, 600, 800),
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).map { calendarPanelVisibleHeight(it, 800) })
        assertEquals(300, calendarPanelVisibleHeight(0.5f, 600))
        assertEquals(800, calendarPanelVisibleHeight(1.1f, 800))
    }
}
