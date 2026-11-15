package no.timnor.app.shared.navigation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.clock_outline
import no.timnor.app.shared.generated.resources.home_outline
import no.timnor.app.shared.generated.resources.home_tab
import no.timnor.app.shared.generated.resources.stopwatch_title
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Shared tab bar matching the Android reference geometry and content treatment. */
@Composable
fun SharedNavigationBar(
    selectedTab: SharedAppTab,
    onSelectTab: (SharedAppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val items = listOf(
        NavigationItem(SharedAppTab.Home, stringResource(Res.string.home_tab), Res.drawable.home_outline),
        NavigationItem(SharedAppTab.Stopwatch, stringResource(Res.string.stopwatch_title), Res.drawable.clock_outline),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(min = 72.dp)
                .padding(
                    horizontal = MaterialTheme.timnorSpacing.small,
                    vertical = MaterialTheme.timnorSpacing.extraSmall,
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = selectedTab == item.destination
                NavigationBarItem(
                    item = item,
                    selected = selected,
                    onClick = { onSelectTab(item.destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavigationBarItem(
    item: NavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.timnorSpacing
    val contentColor = when {
        selected && colors.surface.luminance() < 0.5f -> colors.primaryContainer
        selected -> colors.primary
        else -> colors.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = spacing.extraSmall, vertical = spacing.extraSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(spacing.extraSmall))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class NavigationItem(
    val destination: SharedAppTab,
    val label: String,
    val icon: DrawableResource,
)
