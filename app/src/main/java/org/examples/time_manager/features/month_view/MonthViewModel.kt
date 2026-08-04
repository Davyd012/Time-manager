package org.examples.time_manager.features.month_view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.R
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.core.dates.models.Today
import org.examples.time_manager.core.repository.ProjectRepository
import org.examples.time_manager.core.repository.SpreadsheetExporter
import org.examples.time_manager.core.repository.WorkRepository
import org.examples.time_manager.features.month_view.data.MonthViewUiState
import org.examples.time_manager.features.root.data.ModifyWork
import org.examples.time_manager.features.month_view.data.MonthViewIntent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MonthViewModel(
    private val month: LocalDate,
    selectedDay: Int,
    private val workRepository: WorkRepository,
    private val projectRepository: ProjectRepository,
    private val spreadsheetExporter: SpreadsheetExporter,
) : ViewModel() {
    private val monthValue = YearMonth.from(month)
    private val selectedDayFlow = MutableStateFlow(selectedDay.coerceIn(1, monthValue.lengthOfMonth()))
    private val editor = MutableStateFlow(ModifyWork())
    private val message = MutableStateFlow<Int?>(null)
    private val exportProjects = MutableStateFlow<List<Project>>(emptyList())
    private val projects = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val works = selectedDayFlow.flatMapLatest { day ->
        workRepository.observeForDay(monthValue.atDay(day))
    }
    private val days = workRepository.observeForMonth(monthValue).map { entries ->
        val hours = entries.groupBy { it.date.toLocalDate() }
            .mapValues { (_, values) -> values.sumOf(Work::time) }
        (1..monthValue.lengthOfMonth()).map { day ->
            val date = monthValue.atDay(day)
            DayModel(date.dayOfWeek.name, date, hours[date] ?: 0)
        }
    }

    private val baseState = combine(selectedDayFlow, projects, works, days) {
            day, projectList, workList, dayModels ->
        MonthBase(day, projectList, workList, dayModels)
    }

    val state = combine(baseState, editor, message) {
            base, modifyWork, stateMessage ->
        MonthViewUiState(
            isLoading = false,
            messageResId = stateMessage,
            month = monthValue.month.getDisplayName(TextStyle.FULL, Locale.Builder().setLanguage("nb").setRegion("NO").build()),
            dayPerMonth = base.dayModels,
            selectedDay = base.day,
            selectedProject = base.projects.firstOrNull()?.id ?: 1,
            selectedWork = modifyWork,
            projects = base.projects,
            workQueries = base.works,
            today = monthValue.atDay(base.day).toToday(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthViewUiState())

    fun onIntent(event: MonthViewIntent) {
        when (event) {
            is MonthViewIntent.ModifyWork -> viewModelScope.launch(Dispatchers.IO) {
                if (event.delete) workRepository.delete(event.work) else workRepository.upsert(event.work)
            }
            is MonthViewIntent.ModifyWorkState -> editor.value = ModifyWork(event.selected, event.show)
            is MonthViewIntent.SelectDay -> selectedDayFlow.value = event.value.coerceIn(1, monthValue.lengthOfMonth())
            is MonthViewIntent.WriteWork -> {
                if (state.value.projects.isEmpty() || event.hours == 0) {
                    message.value = R.string.create_project_and_register_msg
                } else {
                    viewModelScope.launch(Dispatchers.IO) {
                        workRepository.upsert(Work(event.notes, event.date, event.project, time = event.hours))
                        editor.value = ModifyWork()
                    }
                }
            }
            is MonthViewIntent.WriteRangeWork -> viewModelScope.launch(Dispatchers.IO) {
                event.dates.filter { it.dayOfWeek.value < 6 }.forEach { date ->
                    val seconds = if (date.dayOfWeek.value == 5) (5.5 * 3600).toInt() else 8 * 3600
                    workRepository.upsert(Work(event.notes, date.withHour(7), event.project, time = seconds))
                }
            }
            is MonthViewIntent.NewProject -> viewModelScope.launch(Dispatchers.IO) {
                projectRepository.upsert(
                    event.project?.copy(name = event.name, description = event.description)
                        ?: Project(name = event.name, description = event.description),
                )
            }
            is MonthViewIntent.ModifyProject -> viewModelScope.launch(Dispatchers.IO) {
                if (event.delete) projectRepository.delete(event.project) else projectRepository.upsert(event.project)
            }
            is MonthViewIntent.ModifyExportProjects -> exportProjects.update { selected ->
                if (event.project in selected) selected - event.project else selected + event.project
            }
            is MonthViewIntent.CreateExcelDocument -> viewModelScope.launch(Dispatchers.IO) {
                spreadsheetExporter.export(event.uri, state.value.dayPerMonth)
            }
        }
    }
}

private data class MonthBase(
    val day: Int,
    val projects: List<Project>,
    val works: List<Work>,
    val dayModels: List<DayModel>,
)

private fun LocalDate.toToday() = Today(
    weekDay = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.Builder().setLanguage("nb").setRegion("NO").build()),
    day = dayOfMonth,
    month = month.getDisplayName(TextStyle.FULL, Locale.Builder().setLanguage("nb").setRegion("NO").build()),
    year = year,
)
