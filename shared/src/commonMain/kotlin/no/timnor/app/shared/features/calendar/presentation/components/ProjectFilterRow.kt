package no.timnor.app.shared.features.calendar.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import no.timnor.app.shared.ui.theme.timnorSpacing
import no.timnor.app.shared.features.root.presentation.newProject.components.ProjectIndicator
import no.timnor.app.core.repository.SharedWorkSyncIndicator
import no.timnor.app.shared.features.shared_hours.presentation.SharedWorkSyncLabel
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.plannor_link_24
import no.timnor.app.shared.generated.resources.plannor_project_marker_description
import no.timnor.app.shared.generated.resources.plannor_linked_project_label
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Shared, stateless row used by the project filter sheet. */
@Composable
fun ProjectFilterRow(
    projectId: Int,
    projectName: String,
    linkedPlannorProjectName: String? = null,
    syncIndicator: SharedWorkSyncIndicator? = null,
    monthlyHoursLabel: String? = null,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.timnorSpacing.small, vertical = MaterialTheme.timnorSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        ProjectIndicator(projectId = projectId)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = projectName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            linkedPlannorProjectName?.let { linkedName ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.extraSmall),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.plannor_link_24),
                        contentDescription = stringResource(Res.string.plannor_project_marker_description),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(Res.string.plannor_linked_project_label, linkedName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            syncIndicator?.let { SharedWorkSyncLabel(it) }
        }
        monthlyHoursLabel?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onClick() })
    }
}
