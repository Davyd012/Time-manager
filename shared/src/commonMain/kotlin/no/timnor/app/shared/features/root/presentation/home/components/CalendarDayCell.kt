package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.domain.calendar.formatDayHours
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import no.timnor.app.shared.ui.theme.timnorSpacing

@Composable
internal fun CalendarDayCell(
    date: LocalDate,
    dayLabel: String? = null,
    seconds: Int,
    selected: Boolean,
    showHours: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val contentColorTarget = when {
        selected -> colors.onPrimaryContainer
        enabled -> colors.onSurface
        else -> colors.onSurfaceVariant
    }
    val containerColorTarget = when {
        selected -> colors.primaryContainer
        enabled -> colors.surfaceContainerLow
        else -> colors.surfaceContainerHighest
    }
    val contentColor by animateColorAsState(
        targetValue = contentColorTarget,
        animationSpec = if (animationsEnabled) tween(160) else snap(),
        label = "calendarDayContentColor",
    )
    val containerColor by animateColorAsState(
        targetValue = containerColorTarget,
        animationSpec = if (animationsEnabled) tween(160) else snap(),
        label = "calendarDayContainerColor",
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = if (animationsEnabled) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            snap()
        },
        label = "calendarDaySelectionScale",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.scale(scale).semantics { this.selected = selected },
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
    ) {
        BoxWithConstraints {
            val compact = maxWidth < 48.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.15f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MaterialTheme.timnorSpacing.extraLarge * if (compact) 2.25f else 2.5f)
                    .padding(horizontal = MaterialTheme.timnorSpacing.extraSmall, vertical = MaterialTheme.timnorSpacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                dayLabel?.let {
                    Text(
                        if (compact) it.take(2) else it.take(3),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
                Text(date.day.toString(), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                val hoursText = formatDayHours(seconds.toLong())
                if (showHours && (!compact || hoursText != "0")) {
                    Text(
                        hoursText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(Modifier.height(MaterialTheme.timnorSpacing.small))
                }
            }
        }
    }
}
