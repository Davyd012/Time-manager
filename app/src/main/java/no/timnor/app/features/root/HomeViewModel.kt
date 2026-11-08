package no.timnor.app.features.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.timnor.app.core.database.project.Project
import no.timnor.app.R
import no.timnor.app.core.database.work.Work
import no.timnor.app.core.dates.DatesController
import no.timnor.app.core.dates.models.DayModel
import no.timnor.app.core.dates.models.Today
import no.timnor.app.core.repository.ProjectRepository
import no.timnor.app.core.repository.SpreadsheetExporter
import no.timnor.app.core.repository.WorkRepository
import no.timnor.app.features.root.data.HomeUiState
import no.timnor.app.features.root.data.CalendarSnapshot
import no.timnor.app.features.root.data.ModifyWork
import no.timnor.app.features.root.data.HomeIntent
import no.timnor.app.features.root.data.TimerStates
import no.timnor.app.features.calendar.presentation.components.projectHoursById
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform
import no.timnor.app.shared.platform.stopwatch.StopwatchSnapshot
import no.timnor.app.shared.domain.work.plannedWeekdayWork
import no.timnor.app.shared.domain.calendar.workSecondsByDay
import no.timnor.app.shared.domain.calendar.totalWorkHours
import no.timnor.app.shared.domain.project.toggleProjectSelection
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val workRepository: WorkRepository,
    private val projectRepository: ProjectRepository,
    private val stopwatchRepository: StopwatchPlatform,
    private val spreadsheetExporter: SpreadsheetExporter,
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val calendarMonth = MutableStateFlow(YearMonth.now())
    private val selectedCalendarProjects = MutableStateFlow<List<Project>>(emptyList())
    private val calendarIsLoading = MutableStateFlow(false)
    private val editor = MutableStateFlow(ModifyWork())
    private val exportProjects = MutableStateFlow<List<Project>>(emptyList())
    private val message = MutableStateFlow<Int?>(null)

    private val projects = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val works = selectedDate.flatMapLatest(workRepository::observeForDay)
    private val calendarSnapshot = combine(calendarMonth, selectedCalendarProjects) { month, selectedProjects ->
        month to selectedProjects
    }.flatMapLatest { (month, selectedProjects) ->
        combine(
            workRepository.observeForMonth(month, selectedProjects.map(Project::id)),
            workRepository.observeForMonth(month),
        ) { filteredWorks, allWorks ->
            buildCalendarSnapshot(month, selectedProjects, filteredWorks, allWorks)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarSnapshot(calendarMonth.value),
    )

    private val baseState = combine(
        selectedDate,
        selectedCalendarProjects,
        projects,
        works,
    ) { date, selectedProjects, projectList, workList ->
        HomeBase(date, selectedProjects, projectList, workList)
    }

    val state = combine(
        baseState,
        combine(calendarSnapshot, calendarIsLoading) { calendarData, isCalendarLoading ->
            calendarData to isCalendarLoading
        },
        combine(stopwatchRepository.state, editor, message) { stopwatch, modifyWork, stateMessage ->
            HomeTransient(stopwatch, modifyWork, stateMessage)
        },
    ) { base, calendarState, transient ->
        val (calendarData, isCalendarLoading) = calendarState
        HomeUiState(
            isLoading = false,
            isCalendarLoading = isCalendarLoading,
            errorMessage = null,
            messageResId = transient.message,
            counting = transient.stopwatch.isRunning,
            elapsedSeconds = transient.stopwatch.elapsedSeconds,
            dayPerMonth = calendarData.days,
            selectedDay = base.date.dayOfMonth,
            selectedProject = transient.stopwatch.selectedProject,
            selectedWork = transient.modifyWork,
            projects = base.projects,
            workQueries = base.works,
            today = base.date.toToday(),
            calendarMonth = calendarData.month,
            calendarDays = calendarData.days,
            calendarTotalHours = calendarData.totalHours,
            calendarSelectedProjects = calendarData.selectedProjects,
            calendarProjectHours = calendarData.projectHours,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onIntent(event: HomeIntent) {
        when (event) {
            is HomeIntent.NewProject -> viewModelScope.launch(Dispatchers.IO) {
                projectRepository.upsert(
                    event.project?.copy(name = event.name, description = event.description)
                        ?: Project(name = event.name, description = event.description),
                )
            }
            is HomeIntent.SelectDay -> {
                val day = event.value.coerceIn(1, selectedDate.value.lengthOfMonth())
                selectedDate.value = selectedDate.value.withDayOfMonth(day)
            }
            is HomeIntent.WriteWork -> writeWork(event.project, event.hours, event.date, event.notes)
            is HomeIntent.WriteRangeWork -> writeRange(event)
            is HomeIntent.ModifyWork -> viewModelScope.launch(Dispatchers.IO) {
                if (event.delete) workRepository.delete(event.work) else workRepository.upsert(event.work)
            }
            is HomeIntent.ModifyWorkState -> editor.value = ModifyWork(event.selected, event.show)
            is HomeIntent.ModifyExportProjects -> exportProjects.update { selected ->
                if (event.project in selected) selected - event.project else selected + event.project
            }
            is HomeIntent.CreateExcelDocument -> viewModelScope.launch(Dispatchers.IO) {
                val month = event.month?.let { YearMonth.of(LocalDate.now().year, it + 1) }
                    ?: state.value.calendarMonth
                val selectedIds = exportProjects.value.map(Project::id)
                val monthWorks = workRepository.observeForMonth(month, selectedIds).first()
                spreadsheetExporter.export(
                    event.uri,
                    month.toDayModels(monthWorks),
                )
                exportProjects.value = emptyList()
            }
            is HomeIntent.ModifyProject -> viewModelScope.launch(Dispatchers.IO) {
                if (event.delete) projectRepository.delete(event.project)
                else projectRepository.upsert(event.project)
            }
            is HomeIntent.ChangeCalendarMonth -> requestCalendarMonth(event.month)
            is HomeIntent.ToggleCalendarProject -> {
                val allProjectIds = projects.value.mapTo(mutableSetOf(), Project::id)
                val selectedIds = selectedCalendarProjects.value
                    .mapTo(mutableSetOf(), Project::id)
                    .takeUnless { it.isEmpty() }
                val nextSelection = toggleProjectSelection(
                    selectedIds = selectedIds,
                    projectId = event.project.id,
                    allProjectIds = allProjectIds,
                )
                selectedCalendarProjects.value = nextSelection
                    ?.let { ids -> projects.value.filter { it.id in ids } }
                    .orEmpty()
            }
            is HomeIntent.SetCalendarProjects -> selectedCalendarProjects.value = event.projects
            HomeIntent.ClearCalendarProjects -> selectedCalendarProjects.value = emptyList()
        }
    }

    fun onBackupRestored() {
        selectedCalendarProjects.value = emptyList()
        editor.value = ModifyWork()
        viewModelScope.launch(Dispatchers.IO) {
            val currentProjects = projectRepository.observeAll().first()
            val selectedId = stopwatchRepository.state.value.selectedProject
            if (currentProjects.none { it.id == selectedId }) {
                stopwatchRepository.selectProject(currentProjects.firstOrNull()?.id ?: 0)
            }
        }
    }

    private fun requestCalendarMonth(month: YearMonth) {
        if (calendarIsLoading.value || month == calendarMonth.value) return

        calendarIsLoading.value = true
        calendarMonth.value = month
        viewModelScope.launch {
            calendarSnapshot.filter { snapshot -> snapshot.month == month }.first()
            calendarIsLoading.value = false
        }
    }

    private fun saveStopwatch() {
        val snapshot = stopwatchRepository.state.value
        if (snapshot.elapsedSeconds == 0) return
        stopwatchRepository.pause()
        writeWork(
            project = snapshot.selectedProject,
            hours = snapshot.elapsedSeconds,
            date = LocalDateTime.now().minusSeconds(snapshot.elapsedSeconds.toLong()),
            notes = "",
            onSaved = {
                stopwatchRepository.cancel()
                stopwatchRepository.timerSaved()
            },
        )
    }

    private fun writeWork(
        project: Int,
        hours: Int,
        date: LocalDateTime,
        notes: String,
        onSaved: (() -> Unit)? = null,
    ) {
        if (state.value.projects.isEmpty() || hours == 0) {
            showMessage(R.string.create_project_and_register_msg)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            workRepository.upsert(Work(description = notes, date = date, project = project, time = hours))
            onSaved?.invoke()
            editor.value = ModifyWork()
        }
    }

    private fun writeRange(event: HomeIntent.WriteRangeWork) {
        viewModelScope.launch(Dispatchers.IO) {
            plannedWeekdayWork(event.dates.map { it.toLocalDate().toKotlinLocalDate() }).forEach { plannedDay ->
                workRepository.upsert(
                    Work(
                        description = event.notes,
                        date = plannedDay.date.toJavaLocalDate().atTime(7, 0),
                        project = event.project,
                        time = plannedDay.timeSeconds,
                    ),
                )
            }
        }
    }

    private fun showMessage(message: Int) {
        this.message.value = message
    }
}

private data class HomeBase(
    val date: LocalDate,
    val selectedProjects: List<Project>,
    val projects: List<Project>,
    val works: List<Work>,
)

private data class HomeTransient(
    val stopwatch: StopwatchSnapshot,
    val modifyWork: ModifyWork,
    val message: Int?,
)

private fun YearMonth.toDayModels(works: List<Work>): List<DayModel> {
    val secondsByDay = workSecondsByDay(
        entries = works,
        year = year,
        month = monthValue,
        dateOf = { it.date.toLocalDate().toKotlinLocalDate() },
        secondsOf = Work::time,
    )
    return (1..lengthOfMonth()).map { day ->
        val date = atDay(day)
        DayModel(date.dayOfWeek.name, date, secondsByDay[day] ?: 0)
    }
}

internal fun buildCalendarSnapshot(
    month: YearMonth,
    selectedProjects: List<Project>,
    filteredWorks: List<Work>,
    allWorks: List<Work>,
): CalendarSnapshot {
    val monthRange = month.atDay(1)..month.atEndOfMonth()
    val monthFilteredWorks = filteredWorks.filter { it.date.toLocalDate() in monthRange }
    val monthWorks = allWorks.filter { it.date.toLocalDate() in monthRange }
    return CalendarSnapshot(
        month = month,
        selectedProjects = selectedProjects,
        days = month.toDayModels(monthFilteredWorks),
        totalHours = totalWorkHours(monthFilteredWorks, Work::time),
        projectHours = projectHoursById(monthWorks),
    )
}

private fun LocalDate.toToday() = Today(
    weekDay = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.Builder().setLanguage("nb").setRegion("NO").build()),
    day = dayOfMonth,
    month = month.getDisplayName(TextStyle.FULL, Locale.Builder().setLanguage("nb").setRegion("NO").build()),
    year = year,
)
