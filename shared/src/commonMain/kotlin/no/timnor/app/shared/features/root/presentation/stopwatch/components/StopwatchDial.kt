package no.timnor.app.shared.features.root.presentation.stopwatch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import no.timnor.app.shared.domain.time.formatStopwatchTime
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.pause
import no.timnor.app.shared.generated.resources.play
import no.timnor.app.shared.generated.resources.stopwatch_idle_status
import no.timnor.app.shared.generated.resources.stopwatch_paused_status
import no.timnor.app.shared.generated.resources.stopwatch_running_status
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val SECONDS_PER_REVOLUTION = 60

@Composable
internal fun StopwatchDial(elapsedSeconds: Int, isRunning: Boolean, diameter: Dp) {
    val spacing = MaterialTheme.timnorSpacing
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val secondsOnDial = elapsedSeconds % SECONDS_PER_REVOLUTION

    Box(
        modifier = Modifier.width(diameter).heightIn(min = diameter),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(diameter),
            shape = CircleShape,
            color = scheme.surfaceContainerLow,
        ) {}
        CircularProgressIndicator(
            progress = { secondsOnDial.toFloat() / SECONDS_PER_REVOLUTION },
            modifier = Modifier.size(diameter).clearAndSetSemantics {},
            color = scheme.secondary,
            trackColor = scheme.outlineVariant,
            strokeWidth = spacing.small,
            strokeCap = StrokeCap.Round,
            gapSize = spacing.extraSmall,
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            BasicText(
                text = formatStopwatchTime(elapsedSeconds),
                modifier = Modifier.fillMaxWidth(),
                style = typography.displayLarge.copy(
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = typography.labelLarge.fontSize,
                    maxFontSize = typography.displayLarge.fontSize,
                ),
            )
            StopwatchDialStatus(isRunning = isRunning, hasElapsedTime = elapsedSeconds > 0)
        }
    }
}

@Composable
private fun StopwatchDialStatus(isRunning: Boolean, hasElapsedTime: Boolean) {
    val status = when {
        isRunning -> Res.string.stopwatch_running_status
        hasElapsedTime -> Res.string.stopwatch_paused_status
        else -> Res.string.stopwatch_idle_status
    }
    val spacing = MaterialTheme.timnorSpacing
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.small, vertical = spacing.extraSmall),
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isRunning || hasElapsedTime) {
                Icon(
                    painter = painterResource(if (isRunning) Res.drawable.play else Res.drawable.pause),
                    contentDescription = null,
                    modifier = Modifier.size(spacing.medium),
                )
            }
            Text(
                text = stringResource(status),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
