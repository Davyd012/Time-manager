package no.timnor.app.shared.features.root.presentation.stopwatch.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StopwatchControls(
    isRunning: Boolean,
    hasElapsedTime: Boolean,
    onStartPause: () -> Unit,
    onFinish: () -> Unit,
) {
    val spacing = MaterialTheme.timnorSpacing
    val scheme = MaterialTheme.colorScheme
    val timerLabel = stringResource(
        when {
            isRunning -> Res.string.stopwatch_pause
            hasElapsedTime -> Res.string.stopwatch_resume
            else -> Res.string.stopwatch_start
        },
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            FilledIconButton(
                onClick = onStartPause,
                modifier = Modifier.size(spacing.extraLarge * 2 + spacing.medium),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isRunning) scheme.inverseSurface else scheme.primaryContainer,
                    contentColor = if (isRunning) scheme.inverseOnSurface else scheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    painter = painterResource(if (isRunning) Res.drawable.pause else Res.drawable.play),
                    contentDescription = timerLabel,
                    modifier = Modifier.size(spacing.extraLarge),
                )
            }
            Text(
                text = timerLabel,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        if (hasElapsedTime) {
            FinishStopwatchButton(isRunning = isRunning, onClick = onFinish)
        }
    }
}

@Composable
private fun FinishStopwatchButton(isRunning: Boolean, onClick: () -> Unit) {
    val spacing = MaterialTheme.timnorSpacing
    val scheme = MaterialTheme.colorScheme
    val modifier = Modifier.fillMaxWidth()
        .heightIn(min = spacing.extraLarge + spacing.large)
    val shape = MaterialTheme.shapes.extraLarge
    val content: @Composable RowScope.() -> Unit = {
        Icon(painterResource(Res.drawable.done), contentDescription = null)
        Spacer(Modifier.width(spacing.small))
        Text(
            text = stringResource(Res.string.stopwatch_finish),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }

    if (isRunning) {
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
                containerColor = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
            ),
            content = content,
        )
    }
}
