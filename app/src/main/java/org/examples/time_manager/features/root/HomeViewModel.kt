package org.examples.time_manager.features.root

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import org.examples.time_manager.features.root.data.RootScreenEvents
import org.examples.time_manager.features.root.data.TimerStates
import org.examples.time_manager.features.root.domain.DatesController
import org.examples.time_manager.features.root.domain.ExcelController
import java.time.LocalDate
import java.time.LocalDateTime


@RequiresApi(Build.VERSION_CODES.O)
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
                    counting = stopwatchService?.running ?: false
                )
            }

            val projects = projectDao.getAllProjects()
//            val projects = projectDao.getAllProjects().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
            _state.update { it.copy(projects = projects) }

            runStopwatch()
        }
    }

    private fun getWorksForCertainDate(date: LocalDate): Flow<List<Work>> {
        val (startOfDay, endOfDay) = datesController.getBoundariesOfDay(date)
        val works = worksDao.getAllWorks(startOfDay / 1000, endOfDay / 1000)
        return works
    }

    @OptIn(ExperimentalAnimationApi::class)
    fun onEvent(event: RootScreenEvents) {
        when (event) {
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
            }

            is RootScreenEvents.CreateExcelDocumentEvent -> viewModelScope.launch(Dispatchers.IO) {
                Log.d("RootViewModel", "Launching a creating document activity")
                excelController.createExcelFile(
                    context = event.context,
                    uri = event.uri,
                    dayPerMonth = state.value.dayPerMonth
                )
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