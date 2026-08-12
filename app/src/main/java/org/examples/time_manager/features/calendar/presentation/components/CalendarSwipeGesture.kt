package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

internal enum class MonthSwipeDirection {
    Previous,
    Next,
    None,
}

internal fun monthSwipeDirection(
    totalDragX: Float,
    thresholdPx: Float,
): MonthSwipeDirection = when {
    totalDragX <= -thresholdPx -> MonthSwipeDirection.Next
    totalDragX >= thresholdPx -> MonthSwipeDirection.Previous
    else -> MonthSwipeDirection.None
}

internal fun Modifier.calendarMonthSwipeGesture(
    enabled: Boolean,
    thresholdPx: Float,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(enabled, thresholdPx) {
        var totalDragX = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDragX = 0f },
            onHorizontalDrag = { change, dragAmount ->
                totalDragX += dragAmount
                change.consume()
            },
            onDragEnd = {
                when (monthSwipeDirection(totalDragX, thresholdPx)) {
                    MonthSwipeDirection.Previous -> onPreviousMonth()
                    MonthSwipeDirection.Next -> onNextMonth()
                    MonthSwipeDirection.None -> Unit
                }
            },
        )
    }
}
