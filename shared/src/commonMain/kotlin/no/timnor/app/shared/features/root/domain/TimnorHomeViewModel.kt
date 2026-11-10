package no.timnor.app.shared.features.root.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
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
import no.timnor.app.shared.domain.work.plannedWeekdayWork
import no.timnor.app.shared.domain.calendar.monthGridLayout
import no.timnor.app.shared.domain.project.toggleProjectSelection
import no.timnor.app.shared.domain.stopwatch.canChangeStopwatchProject
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform
import no.timnor.app.shared.platform.stopwatch.StopwatchSnapshot

data class TimnorHomeUiState(
    val projects: List<StoredProject> = emptyList(),
    val works: List<StoredWork> = emptyList(),
    val stopwatch: StopwatchSnapshot = StopwatchSnapshot(),
    val selectedProjectId: Int? = null,
    val workProjectId: Int? = null,
    val projectName: String = "",
    val projectDescription: String = "",
    val editingProjectId: Int? = null,
    val workDate: String = currentLocalDate().toString(),
    val workStartTime: String = "07:00",
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

enum class TimnorHomeError { INVALID_PROJECT, INVALID_WORK, STORAGE, ACTIVE_STOPWATCH_PROJECT }

/** Shared Home feature state and intents; platform storage and stopwatch stay behind contracts. */
class TimnorHomeViewModel(
    private val persistence: TimnorPersistence,
    private val stopwatch: StopwatchPlatform,
    initialDate: LocalDate = currentLocalDate(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        TimnorHomeUiState(
            calendarMonth = LocalDate(initialDate.year, initialDate.month.ordinal + 1, 1),
            selectedCalendarDate = initialDate,
        ),
    )
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
                        workProjectId = current.workProjectId
                            ?.takeIf { id -> projects.any { it.id == id } }
                            ?: projects.firstOrNull()?.id,
                        selectedCalendarProjectIds = current.selectedCalendarProjectIds?.let { selected ->
                            val allProjectIds = projects.mapTo(mutableSetOf(), StoredProject::id)
                            if (allProjectIds.isEmpty()) null
                            else selected.intersect(allProjectIds).takeUnless { it == allProjectIds }
                        },
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

    fun saveProject(onResult: (Boolean) -> Unit = {}) =
        saveProject(selectCreatedProject = false, onResult = onResult)

    fun saveProjectAndSelect(onResult: (Boolean) -> Unit = {}) =
        saveProject(selectCreatedProject = true, onResult = onResult)

    private fun saveProject(selectCreatedProject: Boolean, onResult: (Boolean) -> Unit) {
        val form = mutableState.value
        val cleanName = form.projectName.trim()
        if (cleanName.isEmpty()) {
            mutableState.update { it.copy(error = TimnorHomeError.INVALID_PROJECT) }
            onResult(false)
            return
        }
        val project = StoredProject(
            name = cleanName,
            description = form.projectDescription.trim(),
            id = form.editingProjectId ?: 0,
        )
        val existingIds = form.projects.mapTo(mutableSetOf(), StoredProject::id)
        performStorageAction(onResult) {
            persistence.upsertProject(project)
            if (selectCreatedProject && form.editingProjectId == null) {
                persistence.observeProjects().first()
                    .filterNot { it.id in existingIds }
                    .maxByOrNull(StoredProject::id)
                    ?.let { created -> selectProject(created.id) }
            }
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

    fun deleteProject(project: StoredProject, onResult: (Boolean) -> Unit = {}) {
        val timer = stopwatch.state.value
        if (project.id == timer.selectedProject &&
            (timer.isRunning || !canChangeStopwatchProject(timer.elapsedSeconds))
        ) {
            mutableState.update { it.copy(error = TimnorHomeError.ACTIVE_STOPWATCH_PROJECT) }
            onResult(false)
            return
        }
        performStorageAction(onResult) {
            persistence.deleteProject(project)
            val current = mutableState.value
            if (current.editingProjectId == project.id) cancelProjectEdit()
            val replacement = current.projects.firstOrNull { it.id != project.id }?.id
            if (current.selectedProjectId == project.id) {
                mutableState.update { it.copy(selectedProjectId = replacement) }
            }
            if (stopwatch.state.value.selectedProject == project.id) {
                stopwatch.selectProject(replacement ?: 0)
            }
            if (current.workProjectId == project.id) {
                mutableState.update {
                    it.copy(workProjectId = it.projects.firstOrNull { candidate -> candidate.id != project.id }?.id)
                }
            }
        }
    }

    fun updateWorkDate(date: String) {
        mutableState.update { it.copy(workDate = date, error = null) }
    }

    fun updateWorkStartTime(time: String) {
        mutableState.update { it.copy(workStartTime = time, error = null) }
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

    fun beginManualWork() {
        mutableState.update { current ->
            current.copy(
                workProjectId = current.workProjectId?.takeIf { id -> current.projects.any { it.id == id } }
                    ?: current.projects.firstOrNull()?.id,
                workDate = current.selectedCalendarDate.toString(),
                workStartTime = "07:00",
                workHours = "0",
                workMinutes = "0",
                workDescription = "",
                editingWorkId = null,
                error = null,
            )
        }
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

    fun selectCalendarDate(date: LocalDate) {
        mutableState.update {
            it.copy(
                calendarMonth = LocalDate(date.year, date.month.ordinal + 1, 1),
                selectedCalendarDate = date,
            )
        }
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

    fun setCalendarProjectSelection(projectIds: Set<Int>?) {
        val allProjectIds = mutableState.value.projects.mapTo(mutableSetOf(), StoredProject::id)
        val normalized = projectIds?.intersect(allProjectIds)
            ?.takeUnless { it == allProjectIds }
        mutableState.update { it.copy(selectedCalendarProjectIds = normalized) }
    }

    fun selectProject(projectId: Int) {
        if (!canChangeStopwatchProject(mutableState.value.stopwatch.elapsedSeconds)) return
        mutableState.update { it.copy(selectedProjectId = projectId, error = null) }
        stopwatch.selectProject(projectId)
    }

    fun selectWorkProject(projectId: Int) {
        mutableState.update { it.copy(workProjectId = projectId, error = null) }
    }

    fun addManualWork(
        projectId: Int?,
        date: String,
        hours: String,
        minutes: String,
        description: String,
        startTime: String = "07:00",
    ) {
        saveManualWork(null, projectId, date, hours, minutes, description, startTime)
    }

    fun saveManualWork(
        workId: Int?,
        projectId: Int?,
        date: String,
        hours: String,
        minutes: String,
        description: String,
        startTime: String = "07:00",
        onResult: (Boolean) -> Unit = {},
    ) {
        val draft = createManualWork(
            projectId = projectId,
            date = date,
            hours = hours.toIntOrNull(),
            minutes = minutes.toIntOrNull(),
            description = description,
            startTime = startTime,
        )
        val existing = workId?.let { id -> mutableState.value.works.firstOrNull { it.id == id } }
        val secondsRemainder = existing?.time?.rem(60) ?: 0
        val work = draft?.let { entry ->
            val time = entry.time.toLong() + secondsRemainder
            if (time > Int.MAX_VALUE) null else entry.copy(id = workId ?: entry.id, time = time.toInt())
        }
        if (work == null) {
            mutableState.update { it.copy(error = TimnorHomeError.INVALID_WORK) }
            onResult(false)
            return
        }
        performStorageAction(onResult) {
            persistence.upsertWork(work)
            mutableState.update {
                it.copy(workHours = "0", workMinutes = "0", workDescription = "", editingWorkId = null)
            }
        }
    }

    fun submitManualWork(onResult: (Boolean) -> Unit = {}) {
        val form = mutableState.value
        saveManualWork(
            workId = form.editingWorkId,
            projectId = form.workProjectId,
            date = form.workDate,
            hours = form.workHours,
            minutes = form.workMinutes,
            description = form.workDescription,
            startTime = form.workStartTime,
            onResult = onResult,
        )
    }

    fun submitManualWorkPeriod(dates: List<LocalDate>, onResult: (Boolean) -> Unit = {}) {
        val form = mutableState.value
        val projectId = form.workProjectId
            ?.takeIf { id -> form.projects.any { it.id == id } }
        val plannedDays = plannedWeekdayWork(dates)
        if (projectId == null || plannedDays.isEmpty()) {
            mutableState.update { it.copy(error = TimnorHomeError.INVALID_WORK) }
            onResult(false)
            return
        }
        performStorageAction(onResult) {
            persistence.upsertWorks(
                plannedDays.map { day ->
                    StoredWork(
                        id = 0,
                        description = form.workDescription.trim(),
                        dateEpochSeconds = utcEpochSecondsFromLocalDateTime(
                            LocalDateTime(day.date, LocalTime(7, 0)),
                        ),
                        project = projectId,
                        task = 0,
                        time = day.timeSeconds,
                    )
                },
            )
            cancelWorkEdit()
        }
    }

    fun editWork(work: StoredWork) {
        val start = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds)
        mutableState.update {
            it.copy(
                workProjectId = work.project,
                editingWorkId = work.id,
                workDate = start.date.toString(),
                workStartTime = "${start.hour.toString().padStart(2, '0')}:${start.minute.toString().padStart(2, '0')}",
                workHours = (work.time / 3600).toString(),
                workMinutes = (work.time % 3600 / 60).toString(),
                workDescription = work.description,
                error = null,
            )
        }
    }

    fun cancelWorkEdit() {
        mutableState.update {
            it.copy(
                editingWorkId = null,
                workHours = "0",
                workMinutes = "0",
                workStartTime = "07:00",
                workDescription = "",
            )
        }
    }

    fun deleteWork(work: StoredWork, onResult: ((Boolean) -> Unit)? = null) {
        performStorageAction(onResult) {
            persistence.deleteWork(work)
            if (mutableState.value.editingWorkId == work.id) cancelWorkEdit()
        }
    }

    fun startOrPauseStopwatch() {
        if (mutableState.value.stopwatch.isRunning) stopwatch.pause() else stopwatch.start()
    }

    fun cancelStopwatch() {
        stopwatch.cancel()
        mutableState.update { current ->
            if (current.error == TimnorHomeError.ACTIVE_STOPWATCH_PROJECT) current.copy(error = null) else current
        }
    }

    fun saveStopwatch() {
        stopwatch.pause()
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

    private fun performStorageAction(onResult: ((Boolean) -> Unit)? = null, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                mutableState.update { it.copy(error = null) }
                onResult?.invoke(true)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(error = TimnorHomeError.STORAGE) }
                onResult?.invoke(false)
            }
        }
    }
}

private fun currentLocalDate() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
