package no.timnor.app.shared.features.root.presentation.newProject.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import no.timnor.app.shared.features.root.domain.TimnorHomeError
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.invalid_project
import no.timnor.app.shared.generated.resources.project_description_optional
import no.timnor.app.shared.generated.resources.project_edit_title
import no.timnor.app.shared.generated.resources.project_editor_title
import no.timnor.app.shared.generated.resources.project_name_hint
import no.timnor.app.shared.generated.resources.project_save
import no.timnor.app.shared.generated.resources.project_save_changes
import no.timnor.app.shared.generated.resources.storage_error
import no.timnor.app.shared.generated.resources.stopwatch_project_locked
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectEditorSheet(
    state: TimnorHomeUiState,
    onProjectNameChange: (String) -> Unit,
    onProjectDescriptionChange: (String) -> Unit,
    onProjectHourlyRateChange: (String?) -> Unit,
    onProjectWeekendRateChange: (String?) -> Unit,
    onSaveProject: ((Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(TextFieldValue(state.projectName)) }
    var description by remember { mutableStateOf(TextFieldValue(state.projectDescription)) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("project-editor"),
        dragHandle = null,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
        ) {
            Text(
                stringResource(
                    if (state.editingProjectId == null) Res.string.project_editor_title else Res.string.project_edit_title,
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    start = MaterialTheme.timnorSpacing.large,
                    end = MaterialTheme.timnorSpacing.large,
                    top = MaterialTheme.timnorSpacing.large,
                ),
            )
            Column(
                modifier = Modifier.weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.timnorSpacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onProjectNameChange(it.text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.project_name_hint)) },
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        onProjectDescriptionChange(it.text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    label = { Text(stringResource(Res.string.project_description_optional)) },
                )
                ProjectHourlyRateField(
                    rate = state.projectHourlyRate,
                    weekendRate = state.projectWeekendRate,
                    isError = state.error == TimnorHomeError.INVALID_HOURLY_RATE,
                    onRateChange = onProjectHourlyRateChange,
                    onWeekendRateChange = onProjectWeekendRateChange,
                )
                state.error?.let { error ->
                    when (error) {
                        TimnorHomeError.INVALID_PROJECT -> Text(
                            stringResource(Res.string.invalid_project),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TimnorHomeError.STORAGE -> Text(
                            stringResource(Res.string.storage_error),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TimnorHomeError.INVALID_WORK, TimnorHomeError.INVALID_HOURLY_RATE -> Unit
                        TimnorHomeError.ACTIVE_STOPWATCH_PROJECT -> Text(
                            stringResource(Res.string.stopwatch_project_locked),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            ProjectEditorActions(
                editing = state.editingProjectId != null,
                onSave = { onSaveProject { saved -> if (saved) onDismiss() } },
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun ProjectEditorActions(editing: Boolean, onSave: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            start = MaterialTheme.timnorSpacing.large,
            end = MaterialTheme.timnorSpacing.large,
            bottom = MaterialTheme.timnorSpacing.small,
        ),
    ) {
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(if (editing) Res.string.project_save_changes else Res.string.project_save))
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.cancel))
        }
    }
}
