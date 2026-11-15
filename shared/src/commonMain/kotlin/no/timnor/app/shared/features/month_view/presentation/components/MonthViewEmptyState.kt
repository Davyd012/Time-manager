package no.timnor.app.shared.features.month_view.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.month_add_hours
import no.timnor.app.shared.generated.resources.month_empty_description
import no.timnor.app.shared.generated.resources.month_empty_title
import no.timnor.app.shared.generated.resources.icon_add
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun MonthViewEmptyState(
    onAddWork: () -> Unit,
    modifier: Modifier = Modifier,
    loopAnimation: Boolean = false,
) {
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(Res.readBytes("files/empty.json").decodeToString())
    }
    val progress = if (animationsEnabled) {
        animateLottieCompositionAsState(composition, iterations = if (loopAnimation) Int.MAX_VALUE else 1).value
    } else {
        0f
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val illustrationSize = minOf(
            MaterialTheme.timnorSpacing.extraLarge * 8,
            maxHeight * 0.5f,
            (maxWidth - MaterialTheme.timnorSpacing.large * 2).coerceAtLeast(0.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.timnorSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
        ) {
            Image(
                painter = rememberLottiePainter(composition = composition, progress = { progress }),
                contentDescription = null,
                modifier = Modifier
                    .size(illustrationSize)
                    .testTag("home-empty-state-illustration"),
            )
            Text(
                stringResource(Res.string.month_empty_title),
                modifier = Modifier.testTag("home-empty-state-title"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.month_empty_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onAddWork,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MaterialTheme.timnorSpacing.extraLarge * 2)
                    .testTag("home-add-hours-action"),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Icon(painterResource(Res.drawable.icon_add), contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.timnorSpacing.small))
                Text(stringResource(Res.string.month_add_hours), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
