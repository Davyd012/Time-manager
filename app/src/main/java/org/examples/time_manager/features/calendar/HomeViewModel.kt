package org.examples.time_manager.features.calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.examples.time_manager.App
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.database.work.WorkDao
import org.examples.time_manager.core.database.getDatabase
import org.examples.time_manager.core.database.project.ProjectDao
import org.examples.time_manager.features.root.data.HomeState
import org.examples.time_manager.features.root.data.RootScreenEvents
import org.examples.time_manager.features.root.data.Today
import org.examples.time_manager.features.root.domain.DatesController
import org.examples.time_manager.features.root.domain.ExcelController
import java.time.LocalDate


class CalendarViewModel() : ViewModel() {
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
            Log.d("CalendarViewModel", "init")

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

    fun onEvent(event: RootScreenEvents) {
    }

    private suspend fun runStopwatch() {
        while (state.value.counting) {
            delay(100L)
            _timeCount.update { it + 0.1 }
        }
    }
}