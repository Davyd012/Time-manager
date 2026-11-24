package no.timnor.app.features.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.timnor.app.core.database.project.Project
import no.timnor.app.R
import no.timnor.app.core.database.work.Work
import no.timnor.app.core.repository.ProjectRepository
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform
import no.timnor.app.core.repository.WorkRepository
import no.timnor.app.features.root.data.StopwatchIntent
import no.timnor.app.features.root.data.StopwatchUiState
import no.timnor.app.features.root.data.TimerStates
import java.time.LocalDateTime

class StopwatchViewModel(
    private val projectRepository: ProjectRepository,
    private val workRepository: WorkRepository,
    private val stopwatchRepository: StopwatchPlatform,
) : ViewModel() {
    private val message = MutableStateFlow<Int?>(null)
    private val projects = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state = combine(projects, stopwatchRepository.state, message) { projectList, stopwatch, stateMessage ->
        StopwatchUiState(
            isLoading = false,
            messageResId = stateMessage,
            elapsedSeconds = stopwatch.elapsedSeconds,
            isRunning = stopwatch.isRunning,
            selectedProject = stopwatch.selectedProject,
            projects = projectList,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StopwatchUiState())

    fun onIntent(event: StopwatchIntent) {
        when (event) {
            is StopwatchIntent.NewProject -> viewModelScope.launch(Dispatchers.IO) {
                projectRepository.upsert(
                    event.project?.copy(name = event.name, description = event.description)
                        ?: Project(name = event.name, description = event.description),
                )
            }
            is StopwatchIntent.ModifyProject -> viewModelScope.launch(Dispatchers.IO) {
                if (event.delete) projectRepository.delete(event.project) else projectRepository.upsert(event.project)
            }
            is StopwatchIntent.SelectProject -> stopwatchRepository.selectProject(event.value)
            is StopwatchIntent.UpdateTimer -> when (event.type) {
                TimerStates.StartStopwatch -> if (state.value.projects.isEmpty()) {
                    message.value = R.string.create_project_first_msg
                } else stopwatchRepository.start()
                TimerStates.PauseStopwatch -> stopwatchRepository.pause()
                TimerStates.SaveResult -> saveResult()
            }
        }
    }

    private fun saveResult() {
        viewModelScope.launch {
            stopwatchRepository.withPausedTimerForSave { snapshot ->
                if (snapshot.elapsedSeconds == 0) return@withPausedTimerForSave
                withContext(Dispatchers.IO) {
                    workRepository.upsert(
                        Work(
                            date = LocalDateTime.now().minusSeconds(snapshot.elapsedSeconds.toLong()),
                            project = snapshot.selectedProject,
                            time = snapshot.elapsedSeconds,
                        ),
                    )
                }
                stopwatchRepository.timerSaved()
            }
        }
    }
}
