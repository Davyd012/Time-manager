package no.timnor.app.shared.features.root.presentation.newProject.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.core.repository.SharedWorkSyncIndicator
import no.timnor.app.shared.features.shared_hours.presentation.SharedWorkSyncLabel
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.plannor_linked_project_label
import no.timnor.app.shared.generated.resources.plannor_linked_project_unavailable
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProjectSelector(
    projects: List<StoredProject>,
    linkedPlannorProjectNames: Map<Int, String> = emptyMap(),
    sharedProjectIndicators: Map<Int, SharedWorkSyncIndicator> = emptyMap(),
    selectedProjectId: Int?,
    onProjectSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val animationsEnabled = rememberSystemAnimationsEnabled()
    Row(
        modifier = modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = MaterialTheme.timnorSpacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        projects.forEach { project ->
            val isSelected = project.id == selectedProjectId
            val containerColor = animateColorAsState(
                targetValue = if (isSelected) colors.primaryContainer else colors.primary,
                animationSpec = if (animationsEnabled) tween() else snap(),
                label = "projectSelectorContainer",
            )
            val contentColor = animateColorAsState(
                targetValue = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                animationSpec = if (animationsEnabled) tween() else snap(),
                label = "projectSelectorContent",
            )
            Surface(
                onClick = { onProjectSelected(project.id) },
                modifier = Modifier
                    .semantics { selected = isSelected }
                    .testTag("project-selector-${project.id}"),
                shape = MaterialTheme.shapes.small,
                color = containerColor.value,
                contentColor = contentColor.value,
                border = if (isSelected) null else BorderStroke(1.dp, colors.outlineVariant),
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .widthIn(max = MaterialTheme.timnorSpacing.extraLarge * 6)
                        .padding(
                            horizontal = MaterialTheme.timnorSpacing.medium + MaterialTheme.timnorSpacing.extraSmall / 2,
                            vertical = MaterialTheme.timnorSpacing.small + MaterialTheme.timnorSpacing.extraSmall,
                        ),
                ) {
                    Text(
                        project.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ProjectHourlyRateLabel(project.hourlyRateOre)
                    linkedPlannorProjectNames[project.id]?.let { linkedName ->
                        Text(
                            if (linkedName.isBlank()) stringResource(Res.string.plannor_linked_project_unavailable)
                            else stringResource(Res.string.plannor_linked_project_label, linkedName),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    sharedProjectIndicators[project.id]?.let { indicator ->
                        SharedWorkSyncLabel(indicator)
                    }
                }
            }
        }
    }
}
