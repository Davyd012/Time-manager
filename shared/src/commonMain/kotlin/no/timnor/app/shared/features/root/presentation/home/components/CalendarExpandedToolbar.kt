package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.arrow_back
import no.timnor.app.shared.generated.resources.calendar
import no.timnor.app.shared.generated.resources.calendar_title
import no.timnor.app.shared.generated.resources.filter_list
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarExpandedToolbar(
    selectedDate: LocalDate,
    expandedContentEnabled: Boolean,
    expansionProgress: Float,
    hasProjects: Boolean,
    filterActive: Boolean,
    closeLabel: String,
    monthViewLabel: String,
    filterLabel: String,
    onToggleExpanded: (() -> Unit)?,
    onMonthView: (LocalDate) -> Unit,
    onShowFilter: () -> Unit,
    dragState: DraggableState,
    onDragStarted: () -> Unit,
    onDragStopped: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .alpha(expansionProgress)
            .then(if (expandedContentEnabled) Modifier else Modifier.clearAndSetSemantics {})
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = { onDragStarted() },
                onDragStopped = { velocity -> onDragStopped(velocity) },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        onToggleExpanded?.let { close ->
            IconButton(
                onClick = close,
                enabled = expandedContentEnabled,
                modifier = Modifier.semantics { contentDescription = closeLabel },
            ) {
                Icon(painterResource(Res.drawable.arrow_back), contentDescription = null)
            }
        }
        Text(
            stringResource(Res.string.calendar_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
        )
        IconButton(
            onClick = { onMonthView(selectedDate) },
            enabled = expandedContentEnabled,
            modifier = Modifier.semantics { contentDescription = monthViewLabel },
        ) {
            Icon(painterResource(Res.drawable.calendar), contentDescription = null)
        }
        if (hasProjects) {
            if (filterActive) {
                FilledTonalIconButton(
                    onClick = onShowFilter,
                    enabled = expandedContentEnabled,
                    modifier = Modifier.semantics { contentDescription = filterLabel },
                ) {
                    Icon(painterResource(Res.drawable.filter_list), contentDescription = null)
                }
            } else {
                IconButton(
                    onClick = onShowFilter,
                    enabled = expandedContentEnabled,
                    modifier = Modifier.semantics { contentDescription = filterLabel },
                ) {
                    Icon(painterResource(Res.drawable.filter_list), contentDescription = null)
                }
            }
        }
    }
}
