package org.examples.time_manager.features.calendar

import org.examples.time_manager.features.calendar.presentation.components.MonthSwipeDirection
import org.examples.time_manager.features.calendar.presentation.components.monthSwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarSwipeGestureTest {
    @Test
    fun leftSwipeAdvancesOneMonth() {
        assertEquals(MonthSwipeDirection.Next, monthSwipeDirection(-80f, 64f))
    }

    @Test
    fun rightSwipeReturnsOneMonth() {
        assertEquals(MonthSwipeDirection.Previous, monthSwipeDirection(80f, 64f))
    }

    @Test
    fun shortHorizontalDragDoesNotNavigate() {
        assertEquals(MonthSwipeDirection.None, monthSwipeDirection(63f, 64f))
    }
}
