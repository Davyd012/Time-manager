package no.timnor.app.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.work.manualWorkDurationSeconds
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.app_title
import no.timnor.app.shared.generated.resources.invalid_work
import no.timnor.app.shared.generated.resources.invalid_project
import no.timnor.app.shared.generated.resources.no_work
import no.timnor.app.shared.generated.resources.recent_work_title
import no.timnor.app.shared.generated.resources.storage_error
import no.timnor.app.shared.ui.components.CalendarSection
import no.timnor.app.shared.ui.components.ProjectSection
import no.timnor.app.shared.ui.components.RecentWorkRow
import no.timnor.app.shared.ui.components.StopwatchSection
import no.timnor.app.shared.ui.components.WorkEntrySection
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

/** Shared Home screen composition; feature actions and persisted state live in its ViewModel. */
@Composable
fun TimnorApp(viewModel: TimnorHomeViewModel) {
    val state by viewModel.state.collectAsState()
    val projects = state.projects
    val works = state.works
    val stopwatch = state.stopwatch

    var projectMenuExpanded by remember { mutableStateOf(false) }
    val activeProjectId = state.selectedProjectId?.takeIf { selected -> projects.any { it.id == selected } }
        ?: projects.firstOrNull()?.id

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.timnorSpacing.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
    ) {
        item {
            Text(stringResource(Res.string.app_title), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            ProjectSection(
                projects = projects,
                projectName = state.projectName,
                onProjectNameChange = viewModel::updateProjectName,
                projectDescription = state.projectDescription,
                onProjectDescriptionChange = viewModel::updateProjectDescription,
                isEditing = state.editingProjectId != null,
                onSaveProject = viewModel::saveProject,
                onCancelProjectEdit = viewModel::cancelProjectEdit,
                onEditProject = viewModel::editProject,
                onDeleteProject = viewModel::deleteProject,
            )
        }
        item { HorizontalDivider() }
        item {
            StopwatchSection(
                projects = projects,
                elapsedSeconds = stopwatch.elapsedSeconds,
                isRunning = stopwatch.isRunning,
                selectedProjectId = stopwatch.selectedProject,
                onSelectProject = viewModel::selectProject,
                onStartPause = viewModel::startOrPauseStopwatch,
                onCancel = viewModel::cancelStopwatch,
                onSave = viewModel::saveStopwatch,
            )
        }
        item { HorizontalDivider() }
        item {
            WorkEntrySection(
                projects = projects,
                selectedProjectId = activeProjectId,
                projectMenuExpanded = projectMenuExpanded,
                onProjectMenuExpandedChange = { projectMenuExpanded = it },
                onProjectSelected = { projectId ->
                    viewModel.selectProject(projectId)
                },
                date = state.workDate,
                onDateChange = viewModel::updateWorkDate,
                hours = state.workHours,
                onHoursChange = viewModel::updateWorkHours,
                minutes = state.workMinutes,
                onMinutesChange = viewModel::updateWorkMinutes,
                description = state.workDescription,
                onDescriptionChange = viewModel::updateWorkDescription,
                isEditing = state.editingWorkId != null,
                canSave = activeProjectId != null &&
                    state.workHours.toIntOrNull() != null && state.workMinutes.toIntOrNull() != null &&
                    runCatching { LocalDate.parse(state.workDate) }.isSuccess &&
                    manualWorkDurationSeconds(state.workHours.toIntOrNull(), state.workMinutes.toIntOrNull()) != null,
                onSave = viewModel::submitManualWork,
                onCancel = viewModel::cancelWorkEdit,
            )
        }
        state.error?.let { error ->
            item {
                Text(
                    text = stringResource(
                        when (error) {
                            TimnorHomeError.INVALID_PROJECT -> Res.string.invalid_project
                            TimnorHomeError.INVALID_WORK -> Res.string.invalid_work
                            TimnorHomeError.STORAGE -> Res.string.storage_error
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item { HorizontalDivider() }
        item {
            CalendarSection(
                month = state.calendarMonth,
                selectedDate = state.selectedCalendarDate,
                projects = projects,
                selectedProjectIds = state.selectedCalendarProjectIds,
                works = works,
                onMonthChange = viewModel::updateCalendarMonth,
                onSelectDate = viewModel::selectCalendarDate,
                onToggleProject = viewModel::toggleCalendarProject,
                onEditWork = viewModel::editWork,
                onDeleteWork = viewModel::deleteWork,
            )
        }
        item {
            Text(stringResource(Res.string.recent_work_title), style = MaterialTheme.typography.titleLarge)
        }
        if (works.isEmpty()) {
            item { Text(stringResource(Res.string.no_work)) }
        } else {
            items(works.sortedByDescending(StoredWork::dateEpochSeconds).take(30), key = StoredWork::id) { work ->
                RecentWorkRow(
                    work = work,
                    projectName = projects.firstOrNull { it.id == work.project }?.name.orEmpty(),
                    onEdit = { viewModel.editWork(work) },
                    onDelete = { viewModel.deleteWork(work) },
                )
            }
        }
    }
}
