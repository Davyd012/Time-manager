package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.ui.Modifier
import kotlin.math.abs

internal fun Modifier.calendarExpansionDrag(
    state: DraggableState,
    onStarted: () -> Unit,
    onStopped: (Float) -> Unit,
    enabled: Boolean = true,
): Modifier = draggable(
    state = state,
    orientation = Orientation.Vertical,
    enabled = enabled,
    onDragStarted = { onStarted() },
    onDragStopped = { onStopped(it) },
)

internal fun emptyListExpansionTarget(
    progress: Float,
    startExpanded: Boolean,
    velocity: Float,
    velocityThreshold: Float,
    distance: Float,
    positionalThreshold: Float,
): Boolean {
    val threshold = maxOf(0.3f, minOf(0.4f, positionalThreshold / distance.coerceAtLeast(1f)))
    return when {
        velocity <= -abs(velocityThreshold) -> true
        velocity >= abs(velocityThreshold) -> false
        startExpanded -> progress > 1f - threshold
        else -> progress >= threshold
    }
}
