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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import no.timnor.app.shared.features.root.domain.TimnorHomeError
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.invalid_project
import no.timnor.app.shared.generated.resources.project_add
import no.timnor.app.shared.generated.resources.project_description_hint
import no.timnor.app.shared.generated.resources.project_name_hint
import no.timnor.app.shared.generated.resources.project_update
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
    onSaveProject: ((Boolean) -> Unit) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding()
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.timnorSpacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
        ) {
            Text(
                stringResource(if (state.editingProjectId == null) Res.string.project_add else Res.string.project_update),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = state.projectName,
                onValueChange = onProjectNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(Res.string.project_name_hint)) },
            )
            OutlinedTextField(
                value = state.projectDescription,
                onValueChange = onProjectDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                label = { Text(stringResource(Res.string.project_description_hint)) },
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
                    TimnorHomeError.INVALID_WORK -> Unit
                    TimnorHomeError.ACTIVE_STOPWATCH_PROJECT -> Text(
                        stringResource(Res.string.stopwatch_project_locked),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            RowActions(
                isEditing = state.editingProjectId != null,
                onSave = {
                    onSaveProject { saved ->
                        if (saved) onDismiss()
                    }
                },
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun RowActions(isEditing: Boolean, onSave: () -> Unit, onCancel: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        Button(onClick = onSave) { Text(stringResource(if (isEditing) Res.string.project_update else Res.string.project_add)) }
        if (isEditing) TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
    }
}
