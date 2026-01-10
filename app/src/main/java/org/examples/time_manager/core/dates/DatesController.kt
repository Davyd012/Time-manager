package org.examples.time_manager.core.dates

import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.WorkDao
import org.examples.time_manager.core.dates.models.DayModel
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

import org.examples.time_manager.App
import org.examples.time_manager.R

class DatesController(private val worksDao: WorkDao) {
    val weekDays: List<String>
        get() = App.context.resources.getStringArray(R.array.week_days_array).toList()

    fun getDatesForCurrentMonth(projects: List<Project>? = null): List<DayModel> {
        val today = LocalDate.now()
        val firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())

        return getDatesForAMonth(firstDayOfMonth, lastDayOfMonth, projects = projects)
    }

    fun getDatesForAMonth(
        firstDayOfMonth: LocalDate,
        lastDayOfMonth: LocalDate?,
        projects: List<Project>? = null
    ): MutableList<DayModel> {
        val dates = mutableListOf<DayModel>()
        var current = firstDayOfMonth

        while (!current.isAfter(lastDayOfMonth)) {
            val (startOfDay, endOfDay) = getBoundariesOfDay(current)
            val hours = if (projects.isNullOrEmpty()) worksDao.getHoursByDay(
                startOfDay,
                endOfDay
            ) else worksDao.getHoursByDayAndProject(
                startOfDay,
                endOfDay,
                ids = projects.map { it.id }
            )
            dates.add(getDateModel(current, hours = hours))
            current = current.plusDays(1)
        }

        return dates
    }


    private fun getDateModel(date: LocalDate, hours: Int = 0) = DayModel(
        day = weekDays.elementAt(date.dayOfWeek.value - 1),
        date = date,
        time = hours
    )

    fun getBoundariesOfDay(current: LocalDate): Pair<Long, Long> {
        val startOfDay =
            current.atStartOfDay(ZoneOffset.UTC).toInstant().epochSecond
        val endOfDay = current.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant()
            .epochSecond
        return Pair(startOfDay, endOfDay)
    }

    fun updateHoursForDay(
        dayModels: List<DayModel>,
        dayToUpdate: Int,
        newHours: Int
    ): List<DayModel> {
        return dayModels.mapIndexed { x, dayModel ->
            if (x == dayToUpdate) {
                dayModel.copy(time = dayModel.time + newHours)
            } else {
                dayModel
            }
        }
    }

}