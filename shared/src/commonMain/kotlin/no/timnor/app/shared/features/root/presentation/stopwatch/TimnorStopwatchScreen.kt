package no.timnor.app.shared.features.root.presentation.stopwatch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.newProject.components.ProjectEditorSheet
import no.timnor.app.shared.features.root.presentation.stopwatch.components.StopwatchSection
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.icon_add
import no.timnor.app.shared.generated.resources.project_add
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimnorStopwatchScreen(
    state: TimnorHomeUiState,
    viewModel: TimnorHomeViewModel,
    modifier: Modifier = Modifier,
) {
    var showProjectEditor by rememberSaveable { mutableStateOf(false) }

    fun createProject() {
        viewModel.cancelProjectEdit()
        showProjectEditor = true
    }

    fun editProject(project: no.timnor.app.shared.data.persistence.StoredProject) {
        viewModel.editProject(project)
        showProjectEditor = true
    }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            StopwatchSection(
            projects = state.projects,
            elapsedSeconds = state.stopwatch.elapsedSeconds,
            isRunning = state.stopwatch.isRunning,
            selectedProjectId = state.stopwatch.selectedProject,
            linkedPlannorProjectNames = state.linkedPlannorProjectNames,
            sharedProjectIndicators = state.sharedWorkIndicatorsByProject,
            plannorTasks = state.stopwatchPlannorTasks,
            selectedPlannorTaskId = state.selectedStopwatchPlannorTaskId,
            error = state.error,
            onSelectProject = viewModel::selectProject,
            onSelectPlannorTask = viewModel::selectStopwatchPlannorTask,
            onStartPause = viewModel::startOrPauseStopwatch,
            onSave = viewModel::saveStopwatch,
            onCreateProject = ::createProject,
            onEditProject = ::editProject,
            onDeleteProject = { project, onResult -> viewModel.deleteProject(project, onResult) },
            modifier = Modifier.fillMaxSize().padding(horizontal = MaterialTheme.timnorSpacing.medium),
        )
            FloatingActionButton(
            onClick = ::createProject,
            modifier = Modifier.align(Alignment.BottomEnd).padding(MaterialTheme.timnorSpacing.medium),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
            Icon(
                painter = painterResource(Res.drawable.icon_add),
                contentDescription = stringResource(Res.string.project_add),
            )
            }
        }
    }
    if (showProjectEditor) {
        ProjectEditorSheet(
            state = state,
            onProjectNameChange = viewModel::updateProjectName,
            onProjectDescriptionChange = viewModel::updateProjectDescription,
            onProjectHourlyRateChange = viewModel::updateProjectHourlyRate,
            onProjectWeekendRateChange = viewModel::updateProjectWeekendRate,
            onSaveProject = viewModel::saveProjectAndSelect,
            onDismiss = {
                showProjectEditor = false
                viewModel.cancelProjectEdit()
            },
        )
    }
}
