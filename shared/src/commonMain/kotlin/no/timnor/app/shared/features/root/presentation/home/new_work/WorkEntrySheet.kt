package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import no.timnor.app.shared.domain.work.manualWorkDurationSeconds
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.features.root.domain.TimnorHomeError
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.invalid_project
import no.timnor.app.shared.generated.resources.invalid_work
import no.timnor.app.shared.generated.resources.storage_error
import no.timnor.app.shared.generated.resources.stopwatch_project_locked
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkEntrySheet(
    state: TimnorHomeUiState,
    onUpdateWorkDate: (String) -> Unit,
    onUpdateWorkStartTime: (String) -> Unit,
    onUpdateWorkHours: (String) -> Unit,
    onUpdateWorkMinutes: (String) -> Unit,
    onUpdateWorkDescription: (String) -> Unit,
    onSelectWorkProject: (Int) -> Unit,
    onSubmit: () -> Unit,
    onDelete: ((Boolean) -> Unit) -> Unit,
    onSubmitPeriod: (List<LocalDate>) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedProjectId = state.workProjectId?.takeIf { id -> state.projects.any { it.id == id } }
        ?: state.projects.firstOrNull()?.id
    val canSave = selectedProjectId != null &&
        runCatching { LocalDate.parse(state.workDate) }.isSuccess &&
        runCatching { LocalTime.parse(state.workStartTime) }.isSuccess &&
        manualWorkDurationSeconds(state.workHours.toIntOrNull(), state.workMinutes.toIntOrNull()) != null

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
            WorkEntrySection(
                projects = state.projects,
                selectedProjectId = selectedProjectId,
                onProjectSelected = onSelectWorkProject,
                date = state.workDate,
                onDateChange = onUpdateWorkDate,
                startTime = state.workStartTime,
                onStartTimeChange = onUpdateWorkStartTime,
                hours = state.workHours,
                onHoursChange = onUpdateWorkHours,
                minutes = state.workMinutes,
                onMinutesChange = onUpdateWorkMinutes,
                description = state.workDescription,
                onDescriptionChange = onUpdateWorkDescription,
                isEditing = state.editingWorkId != null,
                canSave = canSave,
                onSave = onSubmit,
                onDelete = {
                    onDelete { deleted ->
                        if (deleted) onCancel()
                    }
                },
                onSubmitPeriod = onSubmitPeriod,
                onCancel = onCancel,
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
}
