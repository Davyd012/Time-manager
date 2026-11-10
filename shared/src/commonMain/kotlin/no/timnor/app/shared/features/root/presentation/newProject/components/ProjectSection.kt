package no.timnor.app.shared.features.root.presentation.newProject.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.no_projects
import no.timnor.app.shared.generated.resources.project_add
import no.timnor.app.shared.generated.resources.project_name_hint
import no.timnor.app.shared.generated.resources.projects_title
import no.timnor.app.shared.generated.resources.project_description_hint
import no.timnor.app.shared.generated.resources.project_update
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProjectSection(
    projects: List<StoredProject>,
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    projectDescription: String,
    onProjectDescriptionChange: (String) -> Unit,
    isEditing: Boolean,
    onSaveProject: () -> Unit,
    onCancelProjectEdit: () -> Unit,
    onEditProject: (StoredProject) -> Unit,
    onDeleteProject: (StoredProject) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        Text(stringResource(Res.string.projects_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = projectName,
            onValueChange = onProjectNameChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(Res.string.project_name_hint)) },
        )
        OutlinedTextField(
            value = projectDescription,
            onValueChange = onProjectDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.project_description_hint)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            Button(onClick = onSaveProject, enabled = projectName.isNotBlank()) {
                Text(stringResource(if (isEditing) Res.string.project_update else Res.string.project_add))
            }
            if (isEditing) {
                TextButton(onClick = onCancelProjectEdit) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }
        if (projects.isEmpty()) {
            Text(stringResource(Res.string.no_projects))
        } else {
            projects.forEach { project ->
                ProjectRow(
                    project = project,
                    onEdit = { onEditProject(project) },
                    onDelete = { onDeleteProject(project) },
                )
            }
        }
    }
}
