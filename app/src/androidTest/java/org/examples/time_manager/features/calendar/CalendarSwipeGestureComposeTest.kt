package org.examples.time_manager.features.calendar

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicInteger
import org.examples.time_manager.features.calendar.presentation.components.calendarMonthSwipeGesture
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CalendarSwipeGestureComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun horizontalSwipesNavigateAndVerticalSwipeIsIgnored() {
        val previousCount = AtomicInteger(0)
        val nextCount = AtomicInteger(0)

        composeRule.setContent {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .testTag("calendar-swipe-test")
                    .calendarMonthSwipeGesture(
                        enabled = true,
                        thresholdPx = 64f,
                        onPreviousMonth = { previousCount.incrementAndGet() },
                        onNextMonth = { nextCount.incrementAndGet() },
                    ),
            )
        }

        composeRule.onNodeWithTag("calendar-swipe-test").performTouchInput {
            swipe(Offset(180f, 120f), Offset(80f, 120f))
        }
        composeRule.onNodeWithTag("calendar-swipe-test").performTouchInput {
            swipe(Offset(80f, 120f), Offset(180f, 120f))
        }
        composeRule.onNodeWithTag("calendar-swipe-test").performTouchInput {
            swipe(Offset(120f, 180f), Offset(120f, 80f))
        }

        assertEquals(1, previousCount.get())
        assertEquals(1, nextCount.get())
    }
}
