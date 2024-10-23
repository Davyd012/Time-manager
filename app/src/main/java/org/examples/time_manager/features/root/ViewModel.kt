package org.examples.time_manager.features.root

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.examples.time_manager.App
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.database.work.WorkDao
import org.examples.time_manager.core.database.getDatabase
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.project.ProjectDao
import org.examples.time_manager.features.root.data.RootScreenEvents
import org.examples.time_manager.features.root.data.TimerStates
import org.examples.time_manager.features.root.domain.DatesController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalUnit
import java.util.Date


@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val _timeCount = MutableStateFlow(0.0)
    val timeCount = _timeCount.asStateFlow()

    private val _snackbarMessage = MutableStateFlow(false)
    val snackbarMessage: StateFlow<Boolean> = _snackbarMessage.asStateFlow()

    private val worksDao: WorkDao = getDatabase(null).workDao()
    private val projectDao: ProjectDao = getDatabase(null).projectDao()
//    private val worksDao: WorkDao = getDatabase(App.getAppContext()).workDao()
//    private val projectDao: ProjectDao = getDatabase(App.getAppContext()).projectDao()

    private val datesController = DatesController(worksDao = worksDao)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("RootViewModel", "init")
            val today = LocalDate.now()
            val days = datesController.getDatesForCurrentMonth()
//            val works = getWorksForCertainDate(today)
            val works = emptyFlow<List<Work>>()
//            val works = flowOf(
//                listOf(
//                    Work(
//                        description = "",
//                        date = Date(),
//                        project = 3,
//                        task = 0,
//                        time = 156,
//                        id = 0
//                    ),
//                    Work(
//                        description = "",
//                        date = datesController.getDate(localDate.atZone(ZoneId.systemDefault())),
//                        project = 3,
//                        task = 0,
//                        time = 156,
//                        id = 0
//                    ),
//                )
//            )

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
                    )
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

    fun onEvent(event: RootScreenEvents) {
        when (event) {
            is RootScreenEvents.NewProjectEvent -> viewModelScope.launch(Dispatchers.IO) {
                val newProject = Project(name = event.name, description = event.description)
                projectDao.upsert(newProject)
            }

            is RootScreenEvents.SelectDayEvent -> viewModelScope.launch(Dispatchers.IO) {
                val works = getWorksForCertainDate(LocalDate.now().withDayOfMonth(event.value))
                _state.update {
                    it.copy(selectedDay = event.value, workQueries = works)
                }
            }

            is RootScreenEvents.UpdateTimerEvent -> when (event.type) {
                TimerStates.StartStopwatch -> viewModelScope.launch(Dispatchers.IO) {
                    Log.d("HomeViewModel", "Starting a stopwatch")
                    val count = state.value.projects.first()

                    if (count.isEmpty()) {
                        _snackbarMessage.value = !snackbarMessage.value
                        return@launch
                    }

                    _state.update {
                        it.copy(counting = true)
                    }
                    while (state.value.counting) {
                        delay(100L)
                        _timeCount.update { it + 0.1 }
                    }
                }

                TimerStates.PauseStopwatch -> _state.update {
                    it.copy(
                        counting = false,
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

                    val time = timeCount.value.toInt()
                    _timeCount.update { 0.0 }
                    val newMonthDays = datesController.updateHoursForDay(
                        state.value.dayPerMonth,
                        state.value.selectedDay - 1,
                        time
                    )
                    _state.update {
                        it.copy(dayPerMonth = newMonthDays)
                    }
                    worksDao.upsert(
                        Work(
                            description = "",
                            date = LocalDateTime.now(),
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

                val dayOfMonth = event.date?.dayOfMonth ?: state.value.selectedDay
//                val date = datesController.getDate(event.date)
                if (event.date == null || (event.date.month == LocalDate.now().month && event.date.year == LocalDate.now().year)) {
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
        }
    }
}