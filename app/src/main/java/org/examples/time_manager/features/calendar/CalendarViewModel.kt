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
import org.examples.time_manager.core.dates.DatesController
import org.examples.time_manager.core.dates.models.Today
import org.examples.time_manager.features.calendar.data.CalendarScreenEvents
import org.examples.time_manager.features.calendar.data.CalendarState
import org.examples.time_manager.features.root.domain.ExcelController
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Calendar


class CalendarViewModel() : ViewModel() {
    private val _state = MutableStateFlow(CalendarState())
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
                )
            }

            val projects = projectDao.getAllProjects()
            _state.update { it.copy(projects = projects) }
        }
    }

    private fun getWorksForCertainDate(date: LocalDate): Flow<List<Work>> {
        val (startOfDay, endOfDay) = datesController.getBoundariesOfDay(date)
        val works = worksDao.getAllWorks(startOfDay, endOfDay)
        return works
    }

    fun onEvent(event: CalendarScreenEvents) {
        when (event) {
            is CalendarScreenEvents.UpdateMonth -> viewModelScope.launch(Dispatchers.IO) {
                val resultMonth = state.value.monthDifference + event.month
                val instance = Calendar.getInstance()
                val date = instance.apply {
                    add(Calendar.MONTH, resultMonth)
                }.timeInMillis.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC) }

                _state.update {
                    it.copy(
                        monthDifference = resultMonth,
                        currentDate = LocalDate.of(date.year, date.month, 1)
                    )
                }
                updateProjectExecutions()
            }

            is CalendarScreenEvents.UpdateSelectedProjects -> viewModelScope.launch(Dispatchers.IO) {
                val selectedProjects = state.value.selectedProjects.let {
                    if (it.contains(event.project)) it.minus(event.project) else it.plus(event.project)
                }
                _state.update {
                    it.copy(selectedProjects = selectedProjects)
                }
                updateProjectExecutions()
            }
        }
    }

    private fun updateProjectExecutions() {
        val month = state.value.monthDifference
        val instance = Calendar.getInstance()
        val date = instance.apply {
            add(Calendar.MONTH, month)
        }.timeInMillis.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC) }

        val firstDayOfMonth = LocalDate.of(date.year, date.month, 1)
        val days = datesController.getDatesForAMonth(
            firstDayOfMonth = firstDayOfMonth,
            lastDayOfMonth = firstDayOfMonth.withDayOfMonth(
                firstDayOfMonth.month.length(firstDayOfMonth.isLeapYear)
            ),
            projects = state.value.selectedProjects
        )
        _state.update {
            it.copy(dayPerMonth = days)
        }
    }
}