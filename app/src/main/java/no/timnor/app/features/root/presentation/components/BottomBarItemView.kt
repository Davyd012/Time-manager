package no.timnor.app.features.root.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun BottomBarItemView(
    item: BottomBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    val contentColor = when {
        selected && colors.surface.luminance() < 0.5f -> colors.primaryContainer
        selected -> colors.primary
        else -> colors.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(
                horizontal = 4.dp,
                vertical = 6.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) item.activeIcon else item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            textAlign = TextAlign.Center,

            // To linjer gjør løsningen mer robust for større tekst.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
