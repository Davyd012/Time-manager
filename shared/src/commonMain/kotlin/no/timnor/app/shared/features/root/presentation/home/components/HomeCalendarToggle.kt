package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.abs
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.calendar_show_month
import no.timnor.app.shared.generated.resources.calendar_show_week
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeCalendarToggle(
    expanded: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val threshold = MaterialTheme.timnorSpacing.extraLarge
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    val label = stringResource(if (expanded) Res.string.calendar_show_week else Res.string.calendar_show_month)
    Surface(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier
            .size(width = MaterialTheme.timnorSpacing.extraLarge * 2, height = MaterialTheme.timnorSpacing.medium * 3)
            .semantics { contentDescription = label }
            .pointerInput(expanded, thresholdPx, enabled) {
                if (enabled) {
                    var dragDistance = 0f
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            dragDistance += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            if (shouldToggleExpandedCalendar(expanded, dragDistance, thresholdPx)) onToggle()
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f },
                    )
                }
            },
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(
                    width = MaterialTheme.timnorSpacing.medium * 2,
                    height = MaterialTheme.timnorSpacing.small / 2,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.extraLarge,
            ) {}
        }
    }
}

internal fun shouldToggleExpandedCalendar(
    expanded: Boolean,
    dragDistancePx: Float,
    thresholdPx: Float,
): Boolean {
    val crossedThreshold = abs(dragDistancePx) >= thresholdPx
    return crossedThreshold && if (expanded) dragDistancePx < 0f else dragDistancePx > 0f
}
