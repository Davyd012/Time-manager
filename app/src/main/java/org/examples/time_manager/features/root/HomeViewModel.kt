package org.examples.time_manager.features.root

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.examples.time_manager.App
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.database.work.WorkDao
import org.examples.time_manager.core.database.getDatabase
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.project.ProjectDao
import org.examples.time_manager.core.service.ServiceHelper
import org.examples.time_manager.core.service.StopwatchService
import org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_CANCEL
import org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_START
import org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_STOP
import org.examples.time_manager.features.root.data.HomeState
import org.examples.time_manager.features.root.data.ModifyWork
import org.examples.time_manager.features.root.data.RootScreenEvents
import org.examples.time_manager.features.root.data.TimerStates
import org.examples.time_manager.features.root.data.Today
import org.examples.time_manager.features.root.domain.DatesController
import org.examples.time_manager.features.root.domain.ExcelController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters


class HomeViewModel(
    private val stopwatchService: StopwatchService?,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("RootViewModel", "init")
            Log.d("RootViewModel", "init ${stopwatchService == null}")

            _timeCount.value = stopwatchService?.seconds?.intValue?.toDouble() ?: 0.0

            val today = LocalDate.now()
            val days = datesController.getDatesForCurrentMonth()
            val works = getWorksForCertainDate(today)

            _state.update {
                it.copy(
                    dayPerMonth = days,
                    workQueries = works,
                    selectedDay = today.dayOfMonth,
                    today = Today(
                        weekDay = datesController.weekDays.elementAt(today.dayOfWeek.value - 1),
                        day = today.dayOfMonth,
                        month = today.month.name,
                        year = today.year
                    ),
                    selectedProject = stopwatchService?.project ?: 1,
                    counting = stopwatchService?.running ?: false
                )
            }

            val projects = projectDao.getAllProjects()
//            val projects = projectDao.getAllProjects().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
            _state.update { it.copy(projects = projects) }

            runStopwatch()
            Log.d("HomeViewModel", "Started a stopwatch from the init")
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

                val days = datesController.getDatesForCurrentMonth()
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

            is RootScreenEvents.NewProjectEvent -> viewModelScope.launch(Dispatchers.IO) {
                Log.d("HomeViewModel", "New project")
                val newProject = Project(name = event.name, description = event.description)
                projectDao.upsert(newProject)
            }

            is RootScreenEvents.SelectDayEvent -> viewModelScope.launch(Dispatchers.IO) {
                Log.d("HomeViewModel", "Selecting another day")
                val works = getWorksForCertainDate(LocalDate.now().withDayOfMonth(event.value))
                _state.update {
                    it.copy(selectedDay = event.value, workQueries = works)
                }
            }

            is RootScreenEvents.UpdateTimerEvent -> when (event.type) {
                TimerStates.StartStopwatch -> viewModelScope.launch(Dispatchers.IO) {
                    Log.d("HomeViewModel", "Starting a stopwatch")
                    val count = state.value.projects.firstOrNull()?.isEmpty() ?: false

                    if (count) {
                        Log.d("HomeViewModel", "Starting a service11111")
                        _snackbarMessage.value = !snackbarMessage.value
                        return@launch
                    }

                    Log.d("HomeViewModel", "Starting a service")
                    ServiceHelper.triggerForegroundService(
                        context = App.context, action = ACTION_SERVICE_START
                    )

                    _state.update {
                        it.copy(counting = true)
                    }
                    runStopwatch()
                }

                TimerStates.PauseStopwatch -> viewModelScope.launch(Dispatchers.IO) {
                    _state.update {
                        it.copy(
                            counting = false,
                        )
                    }

                    ServiceHelper.triggerForegroundService(
                        context = App.context, action = ACTION_SERVICE_STOP
                    )
                }

                TimerStates.SaveResult -> viewModelScope.launch(Dispatchers.IO) {
                    Log.d("HomeViewModel", "Saving result")
                    val count = state.value.projects.first()
                    if (count.isEmpty()) {
                        _snackbarMessage.value = !snackbarMessage.value
                        return@launch
                    }
                    if (timeCount.value == 0.0) return@launch
                    ServiceHelper.triggerForegroundService(
                        context = App.context, action = ACTION_SERVICE_CANCEL
                    )

                    val time = timeCount.value.toInt()
                    _timeCount.update { 0.0 }
                    val newMonthDays = datesController.updateHoursForDay(
                        state.value.dayPerMonth,
                        state.value.selectedDay - 1,
                        time
                    )
                    _state.update {
                        it.copy(dayPerMonth = newMonthDays, counting = false)
                    }
                    val date = LocalDateTime.now().minusSeconds(time.toLong())

                    worksDao.upsert(
                        Work(
                            description = "",
                            date = date,
                            project = state.value.selectedProject,
                            task = 0,
                            time = time,
                        )
                    )
                }
            }

            is RootScreenEvents.WriteWorkEvent -> viewModelScope.launch(Dispatchers.IO) {
                Log.d("HomeViewModel", "Saving result ${event.project}")
                val count = state.value.projects.first()
                if (count.isEmpty()) {
                    _snackbarMessage.value = !snackbarMessage.value
                    return@launch
                }
                if (event.hours == 0) {
                    _snackbarMessage.value = !snackbarMessage.value
                    return@launch
                }

                val dayOfMonth = event.date.dayOfMonth
//                val date = datesController.getDate(event.date)
                if (event.date.month == LocalDate.now().month && event.date.year == LocalDate.now().year) {
                    val newMonthDays = datesController.updateHoursForDay(
                        state.value.dayPerMonth,
                        dayOfMonth - 1,
                        event.hours
                    )
                    _state.update {
                        it.copy(dayPerMonth = newMonthDays)
                    }
                }
                worksDao.upsert(
                    Work(
                        description = "",
                        date = event.date,
                        project = event.project,
                        task = 0,
                        time = event.hours,
                    )
                )
            }

            is RootScreenEvents.SelectProjectEvent -> viewModelScope.launch(Dispatchers.IO) {
                _state.update { it.copy(selectedProject = event.value) }
                if (stopwatchService != null) {
                    stopwatchService.project = event.value
                }
            }

            is RootScreenEvents.CreateExcelDocumentEvent -> viewModelScope.launch(Dispatchers.IO) {
                Log.d("RootViewModel", "Launching a creating document activity")
                var dayPerMonth = state.value.dayPerMonth
                if (event.month != null) {
                    val today =
                        LocalDate.now().withMonth(event.month)
                    val firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
                    val lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())

                    dayPerMonth = datesController.getDatesForAMonth(firstDayOfMonth, lastDayOfMonth)
                }

                excelController.createExcelFile(
                    context = event.context,
                    uri = event.uri,
                    dayPerMonth = dayPerMonth
                )
            }

            is RootScreenEvents.WriteRangeWorkEvent -> viewModelScope.launch(Dispatchers.IO) {
                Log.d("RootViewModel", "Launching a write range event")

                val date = LocalDateTime.now()

                var newInfoForCurrentMonth = state.value.dayPerMonth
                for (day in event.dates) {
                    if (listOf(5, 6).contains(day.dayOfWeek.ordinal)) continue
                    val time = if (day.dayOfWeek.ordinal == 4) (5.5 * 3600).toInt() else 8 * 3600
                    if (day.year == date.year && day.month == date.month) {
                        newInfoForCurrentMonth = datesController.updateHoursForDay(
                            newInfoForCurrentMonth,
                            day.dayOfMonth - 1,
                            time
                        )
                    }
                    worksDao.upsert(
                        Work(
                            description = "",
                            date = day.withHour(event.started.hour)
                                .withMinute(event.started.minute),
                            project = event.project,
                            task = 0,
                            time = time,
                        )
                    )
                }

                Log.d("RootViewModel", "Done inserting new work events")
                _state.update {
                    it.copy(dayPerMonth = newInfoForCurrentMonth)
                }
                Log.d("RootViewModel", "Done updating the state")
            }
        }
    }

    private suspend fun runStopwatch() {
        while (state.value.counting) {
            delay(100L)
            _timeCount.update { it + 0.1 }
        }
    }
}