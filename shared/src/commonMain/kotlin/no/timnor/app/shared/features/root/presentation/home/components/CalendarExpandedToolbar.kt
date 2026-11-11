package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    month: LocalDate,
    expandedContentEnabled: Boolean,
    expansionProgress: Float,
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
    Box(
        modifier = modifier.fillMaxWidth()
            .height(MaterialTheme.timnorSpacing.extraLarge * 2)
            .testTag("calendar-expanded-toolbar")
            .alpha(expansionProgress)
            .then(if (expandedContentEnabled) Modifier else Modifier.clearAndSetSemantics {})
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = { onDragStarted() },
                onDragStopped = { onDragStopped(it) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        onToggleExpanded?.let { close ->
            IconButton(
                onClick = close,
                enabled = expandedContentEnabled,
                modifier = Modifier.align(Alignment.CenterStart).semantics { contentDescription = closeLabel },
            ) {
                Icon(painterResource(Res.drawable.arrow_back), contentDescription = null)
            }
        }
        Text(
            stringResource(Res.string.calendar_title),
            modifier = Modifier.padding(horizontal = MaterialTheme.timnorSpacing.extraLarge * 3),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = onShowFilter,
                enabled = expandedContentEnabled,
                modifier = Modifier.testTag("calendar-filter").semantics { contentDescription = filterLabel },
                shape = MaterialTheme.shapes.medium,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (filterActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (filterActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(painterResource(Res.drawable.filter_list), contentDescription = null)
            }
            FilledTonalIconButton(
                onClick = { onMonthView(LocalDate(month.year, month.month, 1)) },
                enabled = expandedContentEnabled,
                modifier = Modifier.semantics { contentDescription = monthViewLabel },
                shape = MaterialTheme.shapes.medium,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(painterResource(Res.drawable.calendar), contentDescription = null)
            }
        }
    }
}
