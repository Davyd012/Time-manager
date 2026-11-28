package no.timnor.app.shared.features.root.domain

import no.timnor.app.shared.domain.project.formatPayAmount
import no.timnor.app.shared.domain.project.hourlyRateFor
import no.timnor.app.shared.domain.project.parseHourlyRateOre
import no.timnor.app.shared.domain.project.withPayByWorkDate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.time.Clock
import no.timnor.app.shared.data.persistence.CalendarProjectFilterStore
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.domain.work.createManualWork
import no.timnor.app.shared.domain.work.defaultManualWorkStart
import no.timnor.app.shared.domain.work.durationBetweenWorkTimes
import no.timnor.app.shared.domain.work.manualWorkDurationSeconds
import no.timnor.app.shared.domain.work.plannedWeekdayWork
import no.timnor.app.shared.domain.calendar.monthGridLayout
import no.timnor.app.shared.domain.project.toggleProjectSelection
import no.timnor.app.shared.domain.stopwatch.canChangeStopwatchProject
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform
import no.timnor.app.shared.platform.stopwatch.StopwatchSnapshot
import no.timnor.app.shared.features.shared_hours.domain.PlannorTaskOption
import no.timnor.app.core.repository.SharedWorkSyncIndicator

data class TimnorHomeUiState(
    val projects: List<StoredProject> = emptyList(),
    val linkedPlannorProjectNames: Map<Int, String> = emptyMap(),
    val sharedWorkIds: Set<Int> = emptySet(),
    val sharedWorkIndicators: Map<Int, SharedWorkSyncIndicator> = emptyMap(),
    val linkedPlannorTasks: List<PlannorTaskOption> = emptyList(),
    val selectedPlannorTaskId: Int? = null,
    val stopwatchPlannorTasks: List<PlannorTaskOption> = emptyList(),
    val selectedStopwatchPlannorTaskId: Int? = null,
    val works: List<StoredWork> = emptyList(),
    val stopwatch: StopwatchSnapshot = StopwatchSnapshot(),
    val selectedProjectId: Int? = null,
    val workProjectId: Int? = null,
    val projectName: String = "",
    val projectDescription: String = "",
    val projectHourlyRate: String? = null,
    val projectWeekendRate: String? = null,
    val editingProjectId: Int? = null,
    val workDate: String = currentLocalDate().toString(),
    val workStartTime: String = "00:00",
    val workStartTimeManuallySet: Boolean = false,
    val workEndTime: LocalTime? = null,
    val workHours: String = "0",
    val workMinutes: String = "0",
    val workDescription: String = "",
    val editingWorkId: Int? = null,
    val calendarMonth: LocalDate = currentLocalDate().let { LocalDate(it.year, it.month.ordinal + 1, 1) },
    val selectedCalendarDate: LocalDate = currentLocalDate(),
    /** Null selects every current project, matching the Android calendar filter semantics. */
    val selectedCalendarProjectIds: Set<Int>? = null,
    val error: TimnorHomeError? = null,
) {
    val sharedWorkIndicatorsByProject: Map<Int, SharedWorkSyncIndicator>
        get() {
            val projectsByWorkId = works.associate { it.id to it.project }
            return sharedWorkIndicators.mapNotNull { (workId, indicator) ->
                projectsByWorkId[workId]?.let { it to indicator }
            }.groupBy({ it.first }, { it.second })
                .mapValues { (_, indicators) -> indicators.maxBy(SharedWorkSyncIndicator::ordinal) }
        }

    val editingWorkSyncIndicator: SharedWorkSyncIndicator?
        get() = editingWorkId?.let(sharedWorkIndicators::get)
}

enum class TimnorHomeError { INVALID_PROJECT, INVALID_HOURLY_RATE, INVALID_WORK, STORAGE, ACTIVE_STOPWATCH_PROJECT }

/** Shared Home feature state and intents; platform storage and stopwatch stay behind contracts. */
class TimnorHomeViewModel(
    private val persistence: TimnorPersistence,
    private val stopwatch: StopwatchPlatform,
    initialDate: LocalDate = currentLocalDate(),
    private val loadLinkedPlannorProjects: suspend () -> Map<Int, String> = { emptyMap() },
    private val observeSharedWorkIds: () -> Flow<Set<Int>> = { flowOf(emptySet()) },
    private val observeSharedWorkIndicators: () -> Flow<Map<Int, SharedWorkSyncIndicator>> = { flowOf(emptyMap()) },
    private val loadLinkedPlannorTasks: suspend (Int) -> List<PlannorTaskOption> = { emptyList() },
    private val onSharedWorkChanged: () -> Unit = {},
    private val calendarProjectFilterStore: CalendarProjectFilterStore? = null,
    private val clock: Clock = Clock.System,
    private val stopwatchTimeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        TimnorHomeUiState(
            calendarMonth = LocalDate(initialDate.year, initialDate.month.ordinal + 1, 1),
            selectedCalendarDate = initialDate,
            selectedCalendarProjectIds = calendarProjectFilterStore?.load(),
        ),
    )
    val state: StateFlow<TimnorHomeUiState> = mutableState.asStateFlow()
    private var stopwatchSaveInProgress = false

    init {
        viewModelScope.launch {
            observeSharedWorkIds().collect { ids -> mutableState.update { it.copy(sharedWorkIds = ids) } }
        }
        viewModelScope.launch {
            observeSharedWorkIndicators().collect { indicators ->
                mutableState.update { it.copy(sharedWorkIndicators = indicators) }
            }
        }
        refreshLinkedProjectNames()
        viewModelScope.launch {
            persistence.observeProjects().collect { projects ->
                val previousCalendarSelection = mutableState.value.selectedCalendarProjectIds
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
                val calendarSelection = mutableState.value.selectedCalendarProjectIds
                if (calendarSelection != previousCalendarSelection) calendarProjectFilterStore?.save(calendarSelection)
            }
        }
        viewModelScope.launch {
            persistence.observeWorks(Long.MIN_VALUE, Long.MAX_VALUE).collect { works ->
                mutableState.update { it.copy(works = works) }
            }
        }
        viewModelScope.launch {
            stopwatch.state.collect { timer ->
                val selectedProjectChanged = timer.selectedProject != mutableState.value.stopwatch.selectedProject
                mutableState.update { it.copy(stopwatch = timer) }
                if (selectedProjectChanged) loadTasksForStopwatchProject(timer.selectedProject)
            }
        }
    }

    fun refreshLinkedProjectNames() {
        viewModelScope.launch {
            val linked = runCatching { loadLinkedPlannorProjects() }.getOrDefault(emptyMap())
            mutableState.update { it.copy(linkedPlannorProjectNames = linked) }
        }
    }

    fun addProject(name: String) {
        updateProjectName(name)
        saveProject()
    }

    fun saveProject(onResult: (Boolean) -> Unit = {}) =
        saveProject(selection = null, onResult = onResult)

    fun saveProjectAndSelect(onResult: (Boolean) -> Unit = {}) =
        saveProject(selection = ::selectProject, onResult = onResult)

    fun saveProjectForWork(onResult: (Boolean) -> Unit = {}) =
        saveProject(selection = ::selectWorkProject, onResult = onResult)

    private fun saveProject(selection: ((Int) -> Unit)?, onResult: (Boolean) -> Unit) {
        val form = mutableState.value
        val cleanName = form.projectName.trim()
        if (cleanName.isEmpty()) {
            mutableState.update { it.copy(error = TimnorHomeError.INVALID_PROJECT) }
            onResult(false)
            return
        }
        val hourlyRateOre = form.projectHourlyRate?.let(::parseHourlyRateOre)
        val weekendRate = form.projectWeekendRate.takeIf { form.projectHourlyRate != null }
        val weekendHourlyRateOre = weekendRate?.let(::parseHourlyRateOre)
        if ((form.projectHourlyRate != null && hourlyRateOre == null) ||
            (weekendRate != null && weekendHourlyRateOre == null)
        ) {
            mutableState.update { it.copy(error = TimnorHomeError.INVALID_HOURLY_RATE) }
            onResult(false)
            return
        }
        val project = StoredProject(
            name = cleanName,
            hourlyRateOre = hourlyRateOre,
            weekendHourlyRateOre = weekendHourlyRateOre,
            description = form.projectDescription.trim(),
            id = form.editingProjectId ?: 0,
        )
        performStorageAction(onResult) {
            if (selection != null && form.editingProjectId == null) {
                selection(persistence.createProject(project).id)
            } else {
                persistence.upsertProject(project)
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

    fun updateProjectHourlyRate(rate: String?) {
        mutableState.update {
            it.copy(projectHourlyRate = rate, projectWeekendRate = it.projectWeekendRate.takeIf { rate != null }, error = null)
        }
    }

    fun updateProjectWeekendRate(rate: String?) {
        mutableState.update { it.copy(projectWeekendRate = rate, error = null) }
    }

    fun editProject(project: StoredProject) {
        mutableState.update {
            it.copy(
                projectName = project.name,
                projectDescription = project.description,
                projectHourlyRate = project.hourlyRateOre?.let { formatPayAmount(it.toLong(), ".") },
                projectWeekendRate = project.weekendHourlyRateOre?.let { formatPayAmount(it.toLong(), ".") },
                editingProjectId = project.id,
                error = null,
            )
        }
    }

    fun cancelProjectEdit() {
        mutableState.update {
            it.copy(
                projectName = "",
                projectDescription = "",
                projectHourlyRate = null,
                projectWeekendRate = null,
                editingProjectId = null,
                error = it.error.takeUnless { error ->
                    error == TimnorHomeError.INVALID_PROJECT || error == TimnorHomeError.INVALID_HOURLY_RATE
                },
            )
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
        mutableState.update { current ->
            val duration = current.workEndTime?.let { durationBetweenWorkTimes(time, it.toString()) }
            current.copy(
                workStartTime = time,
                workStartTimeManuallySet = true,
                workHours = duration?.let { (it / 60).toString() } ?: current.workHours,
                workMinutes = duration?.let { (it % 60).toString() } ?: current.workMinutes,
                error = null,
            )
        }
    }

    fun updateWorkEndTime(hour: Int, minute: Int) {
        val endTime = LocalTime(hour, minute)
        mutableState.update { current ->
            val duration = durationBetweenWorkTimes(current.workStartTime, endTime.toString()) ?: return@update current
            current.copy(
                workStartTimeManuallySet = true,
                workEndTime = endTime,
                workHours = (duration / 60).toString(),
                workMinutes = (duration % 60).toString(),
                error = null,
            )
        }
    }

    fun updateWorkDuration(hours: Int, minutes: Int) {
        mutableState.update {
            it.copy(workHours = hours.toString(), workMinutes = minutes.toString(), workEndTime = null, error = null)
        }
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
                workStartTime = "00:00",
                workStartTimeManuallySet = false,
                workEndTime = null,
                workHours = "0",
                workMinutes = "0",
                workDescription = "",
                editingWorkId = null,
                linkedPlannorTasks = emptyList(),
                selectedPlannorTaskId = null,
                error = null,
            )
        }
        mutableState.value.workProjectId?.let(::loadTasksForLocalProject)
    }

    fun updateCalendarMonth(month: LocalDate) {
        mutableState.update { current ->
            val firstOfMonth = LocalDate(month.year, month.month.ordinal + 1, 1)
            current.copy(calendarMonth = firstOfMonth)
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
        val current = mutableState.value
        setCalendarProjectSelection(
            toggleProjectSelection(
                selectedIds = current.selectedCalendarProjectIds,
                projectId = projectId,
                allProjectIds = current.projects.mapTo(mutableSetOf(), StoredProject::id),
            ),
        )
    }

    fun setCalendarProjectSelection(projectIds: Set<Int>?) {
        val allProjectIds = mutableState.value.projects.mapTo(mutableSetOf(), StoredProject::id)
        val normalized = projectIds?.intersect(allProjectIds)
            ?.takeUnless { it == allProjectIds }
        mutableState.update { it.copy(selectedCalendarProjectIds = normalized) }
        calendarProjectFilterStore?.save(normalized)
    }

    fun selectProject(projectId: Int) {
        if (!canChangeStopwatchProject(mutableState.value.stopwatch.elapsedSeconds)) return
        mutableState.update {
            it.copy(
                selectedProjectId = projectId,
                stopwatchPlannorTasks = emptyList(),
                selectedStopwatchPlannorTaskId = null,
                error = null,
            )
        }
        stopwatch.selectProject(projectId)
        loadTasksForStopwatchProject(projectId)
    }

    fun selectStopwatchPlannorTask(taskId: Int?) {
        val current = mutableState.value
        if (taskId != null && current.stopwatchPlannorTasks.none { it.taskId == taskId }) return
        mutableState.update { it.copy(selectedStopwatchPlannorTaskId = taskId) }
    }

    fun selectWorkProject(projectId: Int) {
        mutableState.update {
            it.copy(
                workProjectId = projectId,
                linkedPlannorTasks = emptyList(),
                selectedPlannorTaskId = null,
                error = null,
            )
        }
        loadTasksForLocalProject(projectId)
    }

    fun selectPlannorTask(taskId: Int?) {
        val current = mutableState.value
        if (taskId != null && current.linkedPlannorTasks.none { it.taskId == taskId }) return
        mutableState.update { it.copy(selectedPlannorTaskId = taskId) }
    }

    fun addManualWork(
        projectId: Int?,
        date: String,
        hours: String,
        minutes: String,
        description: String,
        startTime: String = "07:00",
    ) {
        saveManualWork(null, projectId, date, hours, minutes, description, startTime, taskId = null)
    }

    fun saveManualWork(
        workId: Int?,
        projectId: Int?,
        date: String,
        hours: String,
        minutes: String,
        description: String,
        startTime: String = "07:00",
        taskId: Int? = null,
        endTime: LocalTime? = null,
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
        val secondsRemainder = if (endTime == null) existing?.time?.rem(60) ?: 0 else 0
        val work = draft?.let { entry ->
            val time = entry.time.toLong() + secondsRemainder
            if (time > Int.MAX_VALUE) null else entry.copy(
                id = workId ?: entry.id,
                time = time.toInt(),
                enteredEndMinute = endTime?.let { it.hour * 60 + it.minute },
                task = taskId ?: 0,
                appliedHourlyRateOre = existing?.takeIf { it.project == entry.project }?.appliedHourlyRateOre,
            )
        }
        if (work == null) {
            mutableState.update { it.copy(error = TimnorHomeError.INVALID_WORK) }
            onResult(false)
            return
        }
        val project = mutableState.value.projects.firstOrNull { it.id == work.project }
        performStorageAction(onResult) {
            if (workId == null) persistNewWork(work.withPayByWorkDate(project))
            else persistence.upsertWork(work)
            mutableState.update {
                it.copy(
                    workStartTime = "00:00",
                    workStartTimeManuallySet = false,
                    workEndTime = null,
                    workHours = "0",
                    workMinutes = "0",
                    workDescription = "",
                    editingWorkId = null,
                )
            }
        }
    }

    fun submitManualWork(onResult: (Boolean) -> Unit = {}) {
        val form = mutableState.value
        val startTime = if (form.workStartTimeManuallySet) {
            form.workStartTime
        } else {
            val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val selectedDate = runCatching { LocalDate.parse(form.workDate) }.getOrNull()
            val duration = manualWorkDurationSeconds(
                form.workHours.toIntOrNull(),
                form.workMinutes.toIntOrNull(),
            ) ?: 0
            if (selectedDate == null) form.workStartTime
            else defaultManualWorkStart(selectedDate, duration, currentDateTime).let {
                "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
            }
        }
        saveManualWork(
            workId = form.editingWorkId,
            projectId = form.workProjectId,
            date = form.workDate,
            hours = form.workHours,
            minutes = form.workMinutes,
            description = form.workDescription,
            startTime = startTime,
            taskId = form.selectedPlannorTaskId,
            endTime = form.workEndTime,
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
        val project = form.projects.first { it.id == projectId }
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
                        appliedHourlyRateOre = project.hourlyRateFor(day.date),
                    )
                },
            )
            cancelWorkEdit()
        }
    }

    fun editWork(work: StoredWork) {
        val start = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds)
        val endTime = work.enteredEndMinute?.takeIf { it in 0 until 24 * 60 }
            ?.let { LocalTime(it / 60, it % 60) }
            ?.takeIf { durationBetweenWorkTimes(start.time.toString(), it.toString()) == work.time / 60 }
        mutableState.update {
            it.copy(
                workProjectId = work.project,
                editingWorkId = work.id,
                workDate = start.date.toString(),
                workStartTime = "${start.hour.toString().padStart(2, '0')}:${start.minute.toString().padStart(2, '0')}",
                workStartTimeManuallySet = true,
                workEndTime = endTime,
                workHours = (work.time / 3600).toString(),
                workMinutes = (work.time % 3600 / 60).toString(),
                workDescription = work.description,
                linkedPlannorTasks = emptyList(),
                selectedPlannorTaskId = work.task.takeIf { it > 0 },
                error = null,
            )
        }
        loadTasksForLocalProject(work.project, work.task.takeIf { it > 0 })
    }

    fun cancelWorkEdit() {
        mutableState.update {
            it.copy(
                editingWorkId = null,
                workHours = "0",
                workMinutes = "0",
                workStartTime = "00:00",
                workStartTimeManuallySet = false,
                workEndTime = null,
                workDescription = "",
                linkedPlannorTasks = emptyList(),
                selectedPlannorTaskId = null,
            )
        }
    }

    fun deleteWork(work: StoredWork, onResult: ((Boolean) -> Unit)? = null, sharedDeletionConfirmed: Boolean = false) {
        performStorageAction(onResult) {
            persistence.deleteWork(work, sharedDeletionConfirmed)
            if (mutableState.value.editingWorkId == work.id) cancelWorkEdit()
        }
    }

    fun startOrPauseStopwatch() {
        if (stopwatchSaveInProgress) return
        if (stopwatch.state.value.isRunning) stopwatch.pause() else stopwatch.start()
    }

    fun cancelStopwatch() {
        if (stopwatchSaveInProgress) return
        stopwatch.cancel()
        mutableState.update { current ->
            if (current.error == TimnorHomeError.ACTIVE_STOPWATCH_PROJECT) current.copy(error = null) else current
        }
    }

    fun saveStopwatch() {
        if (stopwatchSaveInProgress) return
        stopwatchSaveInProgress = true
        val task = mutableState.value.selectedStopwatchPlannorTaskId ?: 0
        viewModelScope.launch {
            try {
                stopwatch.withPausedTimerForSave { timer ->
                    if (timer.elapsedSeconds <= 0) return@withPausedTimerForSave
                    val end = timer.pausedAtEpochSeconds ?: clock.now().epochSeconds
                    val start = Instant.fromEpochSeconds(end - timer.elapsedSeconds)
                    val zone = stopwatchTimeZone()
                    val project = mutableState.value.projects.firstOrNull { it.id == timer.selectedProject }
                    val work = StoredWork(
                        id = 0,
                        description = "",
                        dateEpochSeconds = utcEpochSecondsFromLocalDateTime(start.toLocalDateTime(zone)),
                        project = timer.selectedProject,
                        task = task,
                        time = timer.elapsedSeconds,
                    )
                    persistNewWork(work.withPayByWorkDate(project, start, zone))
                    stopwatch.timerSaved()
                    onSharedWorkChanged()
                    mutableState.update { it.copy(error = null) }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(error = TimnorHomeError.STORAGE) }
            } finally {
                stopwatchSaveInProgress = false
            }
        }
    }

    fun clearError() {
        mutableState.update { it.copy(error = null) }
    }

    private suspend fun persistNewWork(works: List<StoredWork>) {
        if (works.size == 1) persistence.upsertWork(works.single())
        else persistence.upsertWorks(works)
    }

    private fun performStorageAction(onResult: ((Boolean) -> Unit)? = null, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                onSharedWorkChanged()
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

    private fun loadTasksForLocalProject(localProjectId: Int, selectedTaskId: Int? = null) {
        viewModelScope.launch {
            val tasks = runCatching { loadLinkedPlannorTasks(localProjectId) }.getOrDefault(emptyList())
            mutableState.update { current ->
                if (current.workProjectId != localProjectId) current
                else current.copy(
                    linkedPlannorTasks = tasks,
                    selectedPlannorTaskId = (selectedTaskId ?: current.selectedPlannorTaskId)
                        ?.takeIf { taskId -> tasks.any { it.taskId == taskId } },
                )
            }
        }
    }

    private fun loadTasksForStopwatchProject(localProjectId: Int) {
        viewModelScope.launch {
            val tasks = runCatching { loadLinkedPlannorTasks(localProjectId) }.getOrDefault(emptyList())
            mutableState.update { current ->
                if (current.selectedProjectId != localProjectId) current
                else current.copy(
                    stopwatchPlannorTasks = tasks,
                    selectedStopwatchPlannorTaskId = current.selectedStopwatchPlannorTaskId
                        ?.takeIf { taskId -> tasks.any { it.taskId == taskId } },
                )
            }
        }
    }
}

private fun currentLocalDate() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
