package no.timnor.app.shared.features.root.presentation.stopwatch.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.done
import no.timnor.app.shared.generated.resources.pause
import no.timnor.app.shared.generated.resources.play
import no.timnor.app.shared.generated.resources.stopwatch_finish
import no.timnor.app.shared.generated.resources.stopwatch_pause
import no.timnor.app.shared.generated.resources.stopwatch_resume
import no.timnor.app.shared.generated.resources.stopwatch_start
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StopwatchControls(
    isRunning: Boolean,
    hasElapsedTime: Boolean,
    onStartPause: () -> Unit,
    onFinish: () -> Unit,
) {
    val timerLabel = stringResource(
        when {
            isRunning -> Res.string.stopwatch_pause
            hasElapsedTime -> Res.string.stopwatch_resume
            else -> Res.string.stopwatch_start
        },
    )
    val timerIcon = if (isRunning) Res.drawable.pause else Res.drawable.play
    val isPaused = !isRunning && hasElapsedTime

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        StopwatchAction(
            label = timerLabel,
            icon = timerIcon,
            outlined = isPaused,
            inverse = isRunning,
            onClick = onStartPause,
        )
        if (hasElapsedTime) {
            StopwatchAction(
                label = stringResource(Res.string.stopwatch_finish),
                icon = Res.drawable.done,
                outlined = isRunning,
                onClick = onFinish,
            )
        }
    }
}

@Composable
private fun StopwatchAction(
    label: String,
    icon: DrawableResource,
    outlined: Boolean,
    onClick: () -> Unit,
    inverse: Boolean = false,
) {
    val spacing = MaterialTheme.timnorSpacing
    val scheme = MaterialTheme.colorScheme
    val modifier = Modifier.fillMaxWidth()
        .defaultMinSize(minHeight = spacing.extraLarge + spacing.large)
    val shape = MaterialTheme.shapes.large
    val content: @Composable RowScope.() -> Unit = {
        Icon(painterResource(icon), contentDescription = null)
        Spacer(Modifier.width(spacing.small))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }

    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            border = BorderStroke(1.dp, scheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
            ),
            content = content,
        )
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (inverse) scheme.inverseSurface else scheme.primaryContainer,
                contentColor = if (inverse) scheme.inverseOnSurface else scheme.onPrimaryContainer,
            ),
            content = content,
        )
    }
}
