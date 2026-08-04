package org.examples.time_manager.features.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.R
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.dates.DatesController
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.core.dates.models.Today
import org.examples.time_manager.core.repository.ProjectRepository
import org.examples.time_manager.core.repository.SpreadsheetExporter
import org.examples.time_manager.core.repository.StopwatchRepository
import org.examples.time_manager.core.repository.WorkRepository
import org.examples.time_manager.features.root.data.HomeUiState
import org.examples.time_manager.features.root.data.ModifyWork
import org.examples.time_manager.features.root.data.HomeIntent
import org.examples.time_manager.features.root.data.TimerStates
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val workRepository: WorkRepository,
    private val projectRepository: ProjectRepository,
    private val stopwatchRepository: StopwatchRepository,
    private val spreadsheetExporter: SpreadsheetExporter,
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val calendarMonth = MutableStateFlow(YearMonth.now())
    private val selectedCalendarProjects = MutableStateFlow<List<Project>>(emptyList())
    private val editor = MutableStateFlow(ModifyWork())
    private val exportProjects = MutableStateFlow<List<Project>>(emptyList())
    private val message = MutableStateFlow<Int?>(null)

    private val projects = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val works = selectedDate.flatMapLatest(workRepository::observeForDay)
    private val calendarWorks = combine(calendarMonth, selectedCalendarProjects) { month, selected ->
        month to selected.map(Project::id)
    }.flatMapLatest { (month, ids) -> workRepository.observeForMonth(month, ids) }
    private val calendarDays = combine(calendarMonth, calendarWorks) { month, works ->
        month.toDayModels(works)
    }

    private val baseState = combine(
        selectedDate,
        calendarMonth,
        selectedCalendarProjects,
        projects,
        works,
    ) { date, month, selectedProjects, projectList, workList ->
        HomeBase(date, month, selectedProjects, projectList, workList)
    }

    val state = combine(
        baseState,
        calendarDays,
        stopwatchRepository.state,
        editor,
        message,
    ) { base, days, stopwatch, modifyWork, stateMessage ->
        HomeUiState(
            isLoading = false,
            messageResId = stateMessage,
            counting = stopwatch.isRunning,
            elapsedSeconds = stopwatch.elapsedSeconds,
            dayPerMonth = days,
            selectedDay = base.date.dayOfMonth,
            selectedProject = stopwatch.selectedProject,
            selectedWork = modifyWork,
            projects = base.projects,
            workQueries = base.works,
            today = base.date.toToday(),
            calendarMonth = base.month,
            calendarDays = days,
            calendarSelectedProjects = base.selectedProjects,
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
            is HomeIntent.ChangeCalendarMonth -> calendarMonth.value = event.month
            is HomeIntent.ToggleCalendarProject -> selectedCalendarProjects.update { selected ->
                if (event.project in selected) selected - event.project else selected + event.project
            }
            HomeIntent.ClearCalendarProjects -> selectedCalendarProjects.value = emptyList()
        }
    }

    private fun saveStopwatch() {
        val snapshot = stopwatchRepository.state.value
        if (snapshot.elapsedSeconds == 0) return
        stopwatchRepository.cancel()
        writeWork(
            project = snapshot.selectedProject,
            hours = snapshot.elapsedSeconds,
            date = LocalDateTime.now().minusSeconds(snapshot.elapsedSeconds.toLong()),
            notes = "",
        )
    }

    private fun writeWork(project: Int, hours: Int, date: LocalDateTime, notes: String) {
        if (state.value.projects.isEmpty() || hours == 0) {
            showMessage(R.string.create_project_and_register_msg)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            workRepository.upsert(Work(description = notes, date = date, project = project, time = hours))
            editor.value = ModifyWork()
        }
    }

    private fun writeRange(event: HomeIntent.WriteRangeWork) {
        viewModelScope.launch(Dispatchers.IO) {
            event.dates.filter { it.dayOfWeek.value < 6 }.forEach { date ->
                val seconds = if (date.dayOfWeek.value == 5) (5.5 * 3600).toInt() else 8 * 3600
                workRepository.upsert(
                    Work(description = event.notes, date = date.withHour(7), project = event.project, time = seconds),
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
    val month: YearMonth,
    val selectedProjects: List<Project>,
    val projects: List<Project>,
    val works: List<Work>,
)

private fun YearMonth.toDayModels(works: List<Work>): List<DayModel> {
    val hours = works.groupBy { it.date.toLocalDate() }.mapValues { (_, entries) -> entries.sumOf { it.time } }
    return (1..lengthOfMonth()).map { day ->
        val date = atDay(day)
        DayModel(date.dayOfWeek.name, date, hours[date] ?: 0)
    }
}

private fun LocalDate.toToday() = Today(
    weekDay = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.Builder().setLanguage("nb").setRegion("NO").build()),
    day = dayOfMonth,
    month = month.getDisplayName(TextStyle.FULL, Locale.Builder().setLanguage("nb").setRegion("NO").build()),
    year = year,
)
