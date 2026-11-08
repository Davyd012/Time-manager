package no.timnor.app.shared.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.time.Clock
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.domain.work.createManualWork
import no.timnor.app.shared.domain.calendar.monthGridLayout
import no.timnor.app.shared.domain.project.toggleProjectSelection
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform
import no.timnor.app.shared.platform.stopwatch.StopwatchSnapshot

data class TimnorHomeUiState(
    val projects: List<StoredProject> = emptyList(),
    val works: List<StoredWork> = emptyList(),
    val stopwatch: StopwatchSnapshot = StopwatchSnapshot(),
    val selectedProjectId: Int? = null,
    val projectName: String = "",
    val projectDescription: String = "",
    val editingProjectId: Int? = null,
    val workDate: String = currentLocalDate().toString(),
    val workHours: String = "0",
    val workMinutes: String = "0",
    val workDescription: String = "",
    val editingWorkId: Int? = null,
    val calendarMonth: LocalDate = currentLocalDate().let { LocalDate(it.year, it.month.ordinal + 1, 1) },
    val selectedCalendarDate: LocalDate = currentLocalDate(),
    /** Null selects every current project, matching the Android calendar filter semantics. */
    val selectedCalendarProjectIds: Set<Int>? = null,
    val error: TimnorHomeError? = null,
)

enum class TimnorHomeError { INVALID_PROJECT, INVALID_WORK, STORAGE }

/** Shared Home feature state and intents; platform storage and stopwatch stay behind contracts. */
class TimnorHomeViewModel(
    private val persistence: TimnorPersistence,
    private val stopwatch: StopwatchPlatform,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TimnorHomeUiState())
    val state: StateFlow<TimnorHomeUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            persistence.observeProjects().collect { projects ->
                mutableState.update { current ->
                    current.copy(
                        projects = projects,
                        selectedProjectId = current.selectedProjectId
                            ?.takeIf { id -> projects.any { it.id == id } }
                            ?: projects.firstOrNull()?.id,
                        selectedCalendarProjectIds = current.selectedCalendarProjectIds
                            ?.intersect(projects.mapTo(mutableSetOf(), StoredProject::id))
                            ?.takeUnless { it.isEmpty() || it.size == projects.size },
                    )
                }
            }
        }
        viewModelScope.launch {
            persistence.observeWorks(Long.MIN_VALUE, Long.MAX_VALUE).collect { works ->
                mutableState.update { it.copy(works = works) }
            }
        }
        viewModelScope.launch {
            stopwatch.state.collect { timer -> mutableState.update { it.copy(stopwatch = timer) } }
        }
    }

    fun addProject(name: String) {
        updateProjectName(name)
        saveProject()
    }

    fun saveProject() {
        val form = mutableState.value
        val cleanName = form.projectName.trim()
        if (cleanName.isEmpty()) {
            mutableState.update { it.copy(error = TimnorHomeError.INVALID_PROJECT) }
            return
        }
        val project = StoredProject(
            name = cleanName,
            description = form.projectDescription.trim(),
            id = form.editingProjectId ?: 0,
        )
        performStorageAction {
            persistence.upsertProject(project)
            cancelProjectEdit()
        }
    }

    fun updateProjectName(name: String) {
        mutableState.update { it.copy(projectName = name, error = null) }
    }

    fun updateProjectDescription(description: String) {
        mutableState.update { it.copy(projectDescription = description, error = null) }
    }

    fun editProject(project: StoredProject) {
        mutableState.update {
            it.copy(
                projectName = project.name,
                projectDescription = project.description,
                editingProjectId = project.id,
                error = null,
            )
        }
    }

    fun cancelProjectEdit() {
        mutableState.update {
            it.copy(projectName = "", projectDescription = "", editingProjectId = null)
        }
    }

    fun deleteProject(project: StoredProject) {
        performStorageAction {
            persistence.deleteProject(project)
            val current = mutableState.value
            if (current.editingProjectId == project.id) cancelProjectEdit()
            if (current.selectedProjectId == project.id) {
                val replacement = current.projects.firstOrNull { it.id != project.id }?.id
                mutableState.update { it.copy(selectedProjectId = replacement) }
                stopwatch.selectProject(replacement ?: 0)
            }
        }
    }

    fun updateWorkDate(date: String) {
        mutableState.update { it.copy(workDate = date, error = null) }
    }

    fun updateWorkHours(hours: String) {
        mutableState.update { it.copy(workHours = hours, error = null) }
    }

    fun updateWorkMinutes(minutes: String) {
        mutableState.update { it.copy(workMinutes = minutes, error = null) }
    }

    fun updateWorkDescription(description: String) {
        mutableState.update { it.copy(workDescription = description, error = null) }
    }

    fun updateCalendarMonth(month: LocalDate) {
        mutableState.update { current ->
            val firstOfMonth = LocalDate(month.year, month.month.ordinal + 1, 1)
            val day = current.selectedCalendarDate.day.coerceAtMost(monthGridLayout(month.year, month.month.ordinal + 1).daysInMonth)
            current.copy(
                calendarMonth = firstOfMonth,
                selectedCalendarDate = LocalDate(month.year, month.month.ordinal + 1, day),
            )
        }
    }

    fun selectCalendarDate(day: Int) {
        val current = mutableState.value
        val month = current.calendarMonth
        val dayInMonth = day.coerceIn(1, monthGridLayout(month.year, month.month.ordinal + 1).daysInMonth)
        mutableState.update { it.copy(selectedCalendarDate = LocalDate(month.year, month.month.ordinal + 1, dayInMonth)) }
    }

    fun toggleCalendarProject(projectId: Int) {
        mutableState.update { current ->
            current.copy(
                selectedCalendarProjectIds = toggleProjectSelection(
                    selectedIds = current.selectedCalendarProjectIds,
                    projectId = projectId,
                    allProjectIds = current.projects.mapTo(mutableSetOf(), StoredProject::id),
                ),
            )
        }
    }

    fun selectProject(projectId: Int) {
        mutableState.update { it.copy(selectedProjectId = projectId, error = null) }
        stopwatch.selectProject(projectId)
    }

    fun addManualWork(projectId: Int?, date: String, hours: String, minutes: String, description: String) {
        saveManualWork(null, projectId, date, hours, minutes, description)
    }

    fun saveManualWork(
        workId: Int?,
        projectId: Int?,
        date: String,
        hours: String,
        minutes: String,
        description: String,
    ) {
        val draft = createManualWork(
            projectId = projectId,
            date = date,
            hours = hours.toIntOrNull(),
            minutes = minutes.toIntOrNull(),
            description = description,
        )
        val existing = workId?.let { id -> mutableState.value.works.firstOrNull { it.id == id } }
        val secondsRemainder = existing?.time?.rem(60) ?: 0
        val work = draft?.let { entry ->
            val time = entry.time.toLong() + secondsRemainder
            if (time > Int.MAX_VALUE) null else entry.copy(id = workId ?: entry.id, time = time.toInt())
        }
        if (work == null) {
            mutableState.update { it.copy(error = TimnorHomeError.INVALID_WORK) }
            return
        }
        performStorageAction {
            persistence.upsertWork(work)
            mutableState.update {
                it.copy(workHours = "0", workMinutes = "0", workDescription = "", editingWorkId = null)
            }
        }
    }

    fun submitManualWork() {
        val form = mutableState.value
        saveManualWork(
            workId = form.editingWorkId,
            projectId = form.selectedProjectId,
            date = form.workDate,
            hours = form.workHours,
            minutes = form.workMinutes,
            description = form.workDescription,
        )
    }

    fun editWork(work: StoredWork) {
        val date = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date
        mutableState.update {
            it.copy(
                selectedProjectId = work.project,
                editingWorkId = work.id,
                workDate = date.toString(),
                workHours = (work.time / 3600).toString(),
                workMinutes = (work.time % 3600 / 60).toString(),
                workDescription = work.description,
                error = null,
            )
        }
    }

    fun cancelWorkEdit() {
        mutableState.update { it.copy(editingWorkId = null, workHours = "0", workMinutes = "0", workDescription = "") }
    }

    fun deleteWork(work: StoredWork) {
        performStorageAction {
            persistence.deleteWork(work)
            if (mutableState.value.editingWorkId == work.id) cancelWorkEdit()
        }
    }

    fun startOrPauseStopwatch() {
        if (mutableState.value.stopwatch.isRunning) stopwatch.pause() else stopwatch.start()
    }

    fun cancelStopwatch() = stopwatch.cancel()

    fun saveStopwatch() {
        val timer = mutableState.value.stopwatch
        if (timer.elapsedSeconds <= 0) return
        val projectId = timer.selectedProject
        val localStart = Instant.fromEpochSeconds(Clock.System.now().epochSeconds - timer.elapsedSeconds)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val work = StoredWork(
            id = 0,
            description = "",
            dateEpochSeconds = utcEpochSecondsFromLocalDateTime(localStart),
            project = projectId,
            task = 0,
            time = timer.elapsedSeconds,
        )
        performStorageAction {
            persistence.upsertWork(work)
            stopwatch.timerSaved()
        }
    }

    fun clearError() {
        mutableState.update { it.copy(error = null) }
    }

    private fun performStorageAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                mutableState.update { it.copy(error = null) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(error = TimnorHomeError.STORAGE) }
            }
        }
    }
}

private fun currentLocalDate() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
