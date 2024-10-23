package org.examples.time_manager.features.root.domain

import android.os.Build
import androidx.annotation.RequiresApi
import org.examples.time_manager.core.database.work.WorkDao
import org.examples.time_manager.features.root.DayModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Date

class DatesController(private val worksDao: WorkDao) {
    private val months = listOf(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    )
    val weekDays =
        listOf("Mandag", "Tirsdag", "Onsdag", "Torsdag", "Fredag", "Lørdag", "Søndag")

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDatesForCurrentMonth(): List<DayModel> {
        val today = LocalDate.now()
        val firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
        val lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())

        val dates = mutableListOf<DayModel>()
        var current = firstDayOfMonth

        while (!current.isAfter(lastDayOfMonth)) {
            val (startOfDay, endOfDay) = getBoundariesOfDay(current)
            dates.add(
                getDateModel(current, hours = worksDao.getHoursByDay(startOfDay, endOfDay))
            )
            current = current.plusDays(1)
        }

        return dates
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDateModel(date: LocalDate, hours: Int = 0) = DayModel(
        weekDays.elementAt(date.dayOfWeek.value - 1),
        date.dayOfMonth,
        hours
    )

    @RequiresApi(Build.VERSION_CODES.O)
    fun getBoundariesOfDay(current: LocalDate): Pair<Long, Long> {
        val startOfDay =
            current.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = current.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            .toEpochMilli()
        return Pair(startOfDay, endOfDay)
    }

    fun updateHoursForDay(
        dayModels: List<DayModel>,
        dayToUpdate: Int,
        newHours: Int
    ): List<DayModel> {
        return dayModels.mapIndexed { x, dayModel ->
            if (x == dayToUpdate) {
                dayModel.copy(hours = dayModel.hours + newHours)
            } else {
                dayModel
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDate(localDate: ZonedDateTime?): Date {
//        val zonedDateTime = localDate.atStartOfDay(ZoneId.systemDefault())
        // Convert ZonedDateTime to Instant, then to Date
        if (localDate != null) {
            return Date.from(localDate.toInstant())
        }
        return Date()
    }
}