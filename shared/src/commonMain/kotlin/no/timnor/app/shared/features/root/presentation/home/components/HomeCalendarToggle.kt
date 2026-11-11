package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val label = stringResource(if (expanded) Res.string.calendar_show_week else Res.string.calendar_show_month)
    Surface(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier
            .testTag("home-pull-handle")
            .size(width = MaterialTheme.timnorSpacing.extraLarge * 2, height = MaterialTheme.timnorSpacing.medium * 3)
            .semantics { contentDescription = label },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(
                    width = MaterialTheme.timnorSpacing.medium * 2 + MaterialTheme.timnorSpacing.extraSmall,
                    height = MaterialTheme.timnorSpacing.small / 2 + MaterialTheme.timnorSpacing.extraSmall / 4,
                ),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.52f),
                shape = MaterialTheme.shapes.extraLarge,
            ) {}
        }
    }
}
