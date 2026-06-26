package org.examples.time_manager.features.month_view

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.examples.time_manager.App
import org.examples.time_manager.core.database.getDatabase
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.project.ProjectDao
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.database.work.WorkDao
import org.examples.time_manager.core.dates.DatesController
import org.examples.time_manager.core.dates.models.Today
import org.examples.time_manager.features.month_view.data.MonthViewState
import org.examples.time_manager.features.root.data.ModifyWork
import org.examples.time_manager.features.root.data.RootScreenEvents
import org.examples.time_manager.features.root.domain.ExcelController
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters


class MonthViewModel(
    private val month: LocalDate,
    selectedDay: Int
) : ViewModel() {
    private val _state = MutableStateFlow(MonthViewState())
    val state = _state.asStateFlow()


    private val _timeCount = MutableStateFlow(0.0)
    val timeCount = _timeCount.asStateFlow()

    private val _snackbarMessage = MutableStateFlow(false)
    val snackbarMessage: StateFlow<Boolean> = _snackbarMessage.asStateFlow()

    //    private val worksDao: WorkDao = getDatabase(null).workDao()
//    private val projectDao: ProjectDao = getDatabase(null).projectDao()
    private val worksDao: WorkDao = getDatabase(App.context).workDao()
    private val projectDao: ProjectDao = getDatabase(App.context).projectDao()

    private val excelController = ExcelController()

    private val datesController = DatesController(worksDao = worksDao)

    private var exportProjects = mutableStateOf(emptyList<Project>())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("MonthViewModel", "init")

            val days = datesController.getDatesForAMonth(
                firstDayOfMonth = month.withDayOfMonth(1),
                lastDayOfMonth = month.with(TemporalAdjusters.lastDayOfMonth()),
                projects = listOf()
            )
            val works = getWorksForCertainDate(month.withDayOfMonth(selectedDay))

            _state.update {
                it.copy(
                    dayPerMonth = days,
                    workQueries = works,
                    selectedDay = selectedDay,
                    today = Today(
                        weekDay = datesController.weekDays.elementAt(
                            month.withDayOfMonth(
                                selectedDay
                            ).dayOfWeek.value - 1
                        ),
                        day = selectedDay,
                        month = month.month.name,
                        year = month.year
                    ),
                    selectedProject = 1,
                )
            }

            val projects = projectDao.getAllProjects()
//            val projects = projectDao.getAllProjects().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
            _state.update { it.copy(projects = projects) }
        }
    }

    private fun getWorksForCertainDate(date: LocalDate): Flow<List<Work>> {
        val (startOfDay, endOfDay) = datesController.getBoundariesOfDay(date)
        val works = worksDao.getAllWorks(startOfDay, endOfDay)
        return works
    }

    @OptIn(ExperimentalAnimationApi::class)
    fun onEvent(event: RootScreenEvents) {
        when (event) {
            is RootScreenEvents.ModifyWorkEvent -> viewModelScope.launch(Dispatchers.IO) {
                if (event.delete) {
                    worksDao.delete(event.work)
                } else worksDao.upsert(event.work)

                val days = datesController.getDatesForAMonth(
                    firstDayOfMonth = month.withDayOfMonth(1),
                    lastDayOfMonth = month.with(TemporalAdjusters.lastDayOfMonth()),
                    projects = exportProjects.value
                )
                _state.update {
                    it.copy(dayPerMonth = days)
                }
            }

            is RootScreenEvents.ModifyWorkStateEvent -> viewModelScope.launch(Dispatchers.IO) {
                _state.update {
                    it.copy(
                        selectedWork = ModifyWork(
                            selectedWork = event.selected,
                            showModal = event.show
                        )
                    )
                }
            }

            is RootScreenEvents.NewProjectEvent -> TODO()

            is RootScreenEvents.SelectDayEvent -> viewModelScope.launch(Dispatchers.IO) {
                Log.d("HomeViewModel", "Selecting another day")
                val works = getWorksForCertainDate(month.withDayOfMonth(event.value))
                _state.update {
                    it.copy(selectedDay = event.value, workQueries = works)
                }
            }

            is RootScreenEvents.UpdateTimerEvent -> TODO()

            is RootScreenEvents.WriteWorkEvent -> viewModelScope.launch(Dispatchers.IO) {
                val count = state.value.projects.first()
                if (count.isEmpty() || event.hours == 0) {
                    _snackbarMessage.value = !snackbarMessage.value
                    return@launch
                }

                val selectedMonth = month.month
                val selectedYear = month.year
                if (event.date.month == selectedMonth && event.date.year == selectedYear) {
                    val newMonthDays = datesController.updateHoursForDay(
                        state.value.dayPerMonth,
                        event.date.dayOfMonth - 1,
                        event.hours
                    )
                    _state.update { it.copy(dayPerMonth = newMonthDays) }
                }

                worksDao.upsert(
                    Work(
                        description = event.notes,
                        date = event.date,
                        project = event.project,
                        task = 0,
                        time = event.hours,
                    )
                )
            }

            is RootScreenEvents.CreateExcelDocumentEvent -> viewModelScope.launch(Dispatchers.IO) {
                Log.d("RootViewModel", "Launching a creating document activity")
                val dayPerMonth = if (event.month != null) {
                    val today =
                        LocalDate.now().withMonth(event.month)
                    val firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
                    val lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())

                    datesController.getDatesForAMonth(
                        firstDayOfMonth,
                        lastDayOfMonth,
                        projects = exportProjects.value
                    )
                } else datesController.getDatesForCurrentMonth(projects = exportProjects.value)

                excelController.createExcelFile(
                    context = event.context,
                    uri = event.uri,
                    dayPerMonth = dayPerMonth
                )
                exportProjects.value = emptyList()
            }

            is RootScreenEvents.ModifyExportProjects -> viewModelScope.launch(Dispatchers.IO) {
                val selected = exportProjects.value.firstOrNull { it.id == event.project.id }
                exportProjects.value =
                    if (selected != null) exportProjects.value.filter { it.id != event.project.id }
                    else exportProjects.value.plus(event.project)
            }

            is RootScreenEvents.WriteRangeWorkEvent -> viewModelScope.launch(Dispatchers.IO) {
                var newInfoForSelectedMonth = state.value.dayPerMonth
                for (day in event.dates) {
                    if (listOf(5, 6).contains(day.dayOfWeek.ordinal)) continue
                    val time = if (day.dayOfWeek.ordinal == 4) (5.5 * 3600).toInt() else 8 * 3600
                    if (day.year == month.year && day.month == month.month) {
                        newInfoForSelectedMonth = datesController.updateHoursForDay(
                            newInfoForSelectedMonth,
                            day.dayOfMonth - 1,
                            time
                        )
                    }
                    worksDao.upsert(
                        Work(
                            description = event.notes,
                            date = day.withHour(7).withMinute(0),
                            project = event.project,
                            task = 0,
                            time = time,
                        )
                    )
                }
                _state.update { it.copy(dayPerMonth = newInfoForSelectedMonth) }
            }

            is RootScreenEvents.ModifyProjectEvent -> TODO()

            is RootScreenEvents.SelectProjectEvent -> TODO()
        }
    }
}
