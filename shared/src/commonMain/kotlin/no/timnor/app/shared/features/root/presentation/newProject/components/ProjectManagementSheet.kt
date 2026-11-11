package no.timnor.app.shared.features.root.presentation.newProject.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.done
import no.timnor.app.shared.generated.resources.no_projects
import no.timnor.app.shared.generated.resources.project_add
import no.timnor.app.shared.generated.resources.projects_title
import no.timnor.app.shared.generated.resources.storage_error
import no.timnor.app.shared.generated.resources.stopwatch_project_locked
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectManagementSheet(
    state: TimnorHomeUiState,
    onCreateProject: () -> Unit,
    onEditProject: (StoredProject) -> Unit,
    onDeleteProject: (StoredProject) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.timnorSpacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.projects_title), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.done)) }
            }
            when (state.error) {
                no.timnor.app.shared.features.root.domain.TimnorHomeError.STORAGE -> {
                    Text(stringResource(Res.string.storage_error), color = MaterialTheme.colorScheme.error)
                }
                no.timnor.app.shared.features.root.domain.TimnorHomeError.ACTIVE_STOPWATCH_PROJECT -> {
                    Text(stringResource(Res.string.stopwatch_project_locked), color = MaterialTheme.colorScheme.error)
                }
                else -> Unit
            }
            Button(onClick = onCreateProject, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.project_add))
            }
            if (state.projects.isEmpty()) {
                Text(stringResource(Res.string.no_projects), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                ) {
                    items(state.projects, key = StoredProject::id) { project ->
                        ProjectRow(
                            project = project,
                            onEdit = { onEditProject(project) },
                            onDelete = { onDeleteProject(project) },
                        )
                    }
                }
            }
        }
    }
}
