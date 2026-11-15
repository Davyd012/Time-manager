package no.timnor.app.shared.features.root.presentation.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import no.timnor.app.shared.ui.theme.TimnorSpacing
import androidx.compose.ui.unit.Dp

/** Claims vertical movement only when it can move the calendar, leaving the opposite direction to scrolling. */
internal fun Modifier.calendarExpansionGesture(
    enabled: Boolean,
    dragDistancePx: Float,
    currentProgress: () -> Float,
    onDragStart: () -> Unit,
    onProgressChange: suspend (Float) -> Unit,
    onDragStop: (Float) -> Unit,
    collapseStartLimitPx: Float? = null,
): Modifier = composed {
    val currentCollapseStartLimitPx by rememberUpdatedState(collapseStartLimitPx)
    pointerInput(dragDistancePx, enabled) {
    if (!enabled) return@pointerInput
    coroutineScope {
        val touchSlop = viewConfiguration.touchSlop
        val updates = Channel<ExpansionGestureUpdate>(Channel.CONFLATED)
        val updateWorker = launch {
            for (update in updates) {
                when (update) {
                    is ExpansionGestureUpdate.Progress -> onProgressChange(update.value)
                    is ExpansionGestureUpdate.Stop -> {
                        onProgressChange(update.value)
                        onDragStop(update.velocity)
                    }
                }
            }
        }
        try {
            awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var previous = down.position
            var accumulated = Offset.Zero
            var progress = currentProgress()
            var claimed = false
            var completed = false
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        completed = true
                        if (claimed) updates.trySend(
                            ExpansionGestureUpdate.Stop(progress, tracker.calculateVelocity().y),
                        )
                        break
                    }
                    val delta = change.position - previous
                    previous = change.position
                    accumulated += delta
                    tracker.addPosition(change.uptimeMillis, change.position)

                    if (!claimed) {
                        if (accumulated.getDistance() <= touchSlop) continue
                        if (abs(accumulated.y) <= abs(accumulated.x)) return@awaitEachGesture
                        progress = currentProgress()
                        if (!shouldClaimCalendarDrag(progress, accumulated.y)) return@awaitEachGesture
                        val limit = currentCollapseStartLimitPx
                        if (accumulated.y < 0f && limit != null && down.position.y > limit) {
                            return@awaitEachGesture
                        }
                        onDragStart()
                        claimed = true
                        progress = currentProgress()
                        val overSlop = accumulated.y - sign(accumulated.y) * touchSlop
                        progress = (progress + overSlop / dragDistancePx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                        updates.trySend(ExpansionGestureUpdate.Progress(progress))
                    } else {
                        progress = (progress + delta.y / dragDistancePx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                        updates.trySend(ExpansionGestureUpdate.Progress(progress))
                    }
                    change.consume()
                }
            } finally {
                if (claimed && !completed) {
                    updates.trySend(ExpansionGestureUpdate.Stop(progress, tracker.calculateVelocity().y))
                }
            }
            }
        } finally {
            updates.close()
            updateWorker.join()
        }
    }
}
}

private sealed interface ExpansionGestureUpdate {
    data class Progress(val value: Float) : ExpansionGestureUpdate
    data class Stop(val value: Float, val velocity: Float) : ExpansionGestureUpdate
}

internal fun shouldClaimCalendarDrag(progress: Float, verticalDelta: Float): Boolean = when {
    verticalDelta > 0f -> progress < 0.999f
    verticalDelta < 0f -> progress > 0.001f
    else -> false
}

/** Endpoint travel follows the measured grid row count and the same Material spacing tokens as the layout. */
internal fun calendarExpansionTravel(
    monthRows: Int,
    fontScale: Float,
    spacing: TimnorSpacing,
): Dp {
    val scale = fontScale.coerceAtLeast(1f)
    val expanded = (spacing.extraLarge * (7 + 2 * monthRows)) * scale +
        spacing.small * 3 + spacing.extraSmall * 2
    val collapsed = (spacing.extraLarge * 2 + spacing.extraSmall + spacing.extraLarge + spacing.medium +
        spacing.medium * 3) * scale + spacing.extraSmall * 2
    return (expanded - collapsed).coerceAtLeast(spacing.medium)
}
