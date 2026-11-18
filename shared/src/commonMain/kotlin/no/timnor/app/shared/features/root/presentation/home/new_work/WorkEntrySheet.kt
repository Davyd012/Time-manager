package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import no.timnor.app.shared.domain.work.manualWorkDurationSeconds
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.features.root.domain.TimnorHomeError
import no.timnor.app.shared.features.root.presentation.newProject.components.ProjectEditorSheet
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.invalid_project
import no.timnor.app.shared.generated.resources.invalid_work
import no.timnor.app.shared.generated.resources.storage_error
import no.timnor.app.shared.generated.resources.stopwatch_project_locked
import no.timnor.app.shared.generated.resources.delete_work_title
import no.timnor.app.shared.generated.resources.delete_shared_work_confirmation
import no.timnor.app.shared.generated.resources.delete
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkEntrySheet(
    state: TimnorHomeUiState,
    linkedPlannorProjectNames: Map<Int, String> = emptyMap(),
    onUpdateWorkDate: (String) -> Unit,
    onUpdateWorkStartTime: (String) -> Unit,
    onUpdateWorkHours: (String) -> Unit,
    onUpdateWorkMinutes: (String) -> Unit,
    onUpdateWorkDescription: (String) -> Unit,
    onSelectWorkProject: (Int) -> Unit,
    onBeginProject: () -> Unit,
    onProjectNameChange: (String) -> Unit,
    onProjectDescriptionChange: (String) -> Unit,
    onSaveProject: ((Boolean) -> Unit) -> Unit,
    onCancelProject: () -> Unit,
    onSelectPlannorTask: (Int?) -> Unit,
    onSubmit: () -> Unit,
    onDelete: (Boolean, (Boolean) -> Unit) -> Unit,
    onSubmitPeriod: (List<LocalDate>) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    showDragHandle: Boolean,
) {
    var confirmSharedDeletion by remember(state.editingWorkId) { mutableStateOf(false) }
    var showProjectEditor by remember { mutableStateOf(false) }
    var showDurationPicker by rememberSaveable { mutableStateOf(false) }
    fun delete(confirmed: Boolean) {
        onCancel()
        onDelete(confirmed) { _ -> }
    }
    val selectedProjectId = state.workProjectId?.takeIf { id -> state.projects.any { it.id == id } }
        ?: state.projects.firstOrNull()?.id
    val canSave = selectedProjectId != null &&
        runCatching { LocalDate.parse(state.workDate) }.isSuccess &&
        runCatching { LocalTime.parse(state.workStartTime) }.isSuccess &&
        manualWorkDurationSeconds(state.workHours.toIntOrNull(), state.workMinutes.toIntOrNull()) != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = if (showDragHandle) {
            { BottomSheetDefaults.DragHandle(modifier = Modifier.testTag(WORK_ENTRY_DRAG_HANDLE_TAG)) }
        } else {
            null
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.timnorSpacing.medium, vertical = MaterialTheme.timnorSpacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
        ) {
            WorkEntrySection(
                projects = state.projects,
                linkedPlannorProjectNames = linkedPlannorProjectNames,
                sharedProjectIndicators = state.sharedWorkIndicatorsByProject,
                editingSyncIndicator = state.editingWorkSyncIndicator,
                selectedProjectId = selectedProjectId,
                onProjectSelected = onSelectWorkProject,
                onCreateProject = { onBeginProject(); showProjectEditor = true },
                taskOptions = state.linkedPlannorTasks,
                selectedTaskId = state.selectedPlannorTaskId,
                onTaskSelected = onSelectPlannorTask,
                date = state.workDate,
                onDateChange = onUpdateWorkDate,
                startTime = state.workStartTime,
                onStartTimeChange = onUpdateWorkStartTime,
                hours = state.workHours,
                minutes = state.workMinutes,
                description = state.workDescription,
                onDescriptionChange = onUpdateWorkDescription,
                isEditing = state.editingWorkId != null,
                canSave = canSave,
                onSave = onSubmit,
                onDelete = {
                    if (state.editingWorkId?.let { it in state.sharedWorkIds } == true) confirmSharedDeletion = true
                    else delete(false)
                },
                onSubmitPeriod = onSubmitPeriod,
                onOpenDurationPicker = { showDurationPicker = true },
            )
            state.error?.let { error ->
                Text(
                    text = stringResource(
                        when (error) {
                            TimnorHomeError.INVALID_PROJECT -> Res.string.invalid_project
                            TimnorHomeError.INVALID_WORK -> Res.string.invalid_work
                            TimnorHomeError.STORAGE -> Res.string.storage_error
                            TimnorHomeError.ACTIVE_STOPWATCH_PROJECT -> Res.string.stopwatch_project_locked
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    if (showDurationPicker) {
        ModalBottomSheet(
            onDismissRequest = { showDurationPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            sheetGesturesEnabled = false,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null,
        ) {
            WorkDurationDialog(
                initialHours = state.workHours,
                initialMinutes = state.workMinutes,
                onDismiss = { showDurationPicker = false },
                onConfirm = { hours, minutes ->
                    onUpdateWorkHours(hours.toString())
                    onUpdateWorkMinutes(minutes.toString())
                    showDurationPicker = false
                },
            )
        }
    }
    if (showProjectEditor) {
        ProjectEditorSheet(
            state = state,
            onProjectNameChange = onProjectNameChange,
            onProjectDescriptionChange = onProjectDescriptionChange,
            onSaveProject = onSaveProject,
            onDismiss = { showProjectEditor = false; onCancelProject() },
        )
    }
    if (confirmSharedDeletion) {
        AlertDialog(
            onDismissRequest = { confirmSharedDeletion = false },
            title = { Text(stringResource(Res.string.delete_work_title)) },
            text = { Text(stringResource(Res.string.delete_shared_work_confirmation)) },
            confirmButton = {
                TextButton(onClick = { confirmSharedDeletion = false; delete(true) }) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSharedDeletion = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

internal const val WORK_ENTRY_DRAG_HANDLE_TAG = "work-entry-sheet-drag-handle"
