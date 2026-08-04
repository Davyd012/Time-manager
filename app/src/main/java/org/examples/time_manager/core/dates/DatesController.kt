package org.examples.time_manager.core.dates

import org.examples.time_manager.core.dates.models.DayModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/** Pure calendar calculations. Data access stays in repositories and ViewModels. */
class DatesController {
    fun getDatesForMonth(
        month: YearMonth,
        hoursByDay: Map<LocalDate, Int> = emptyMap(),
    ): List<DayModel> = month.range().map { date ->
        DayModel(
            day = date.dayOfWeek.name,
            date = date,
            time = hoursByDay[date] ?: 0,
        )
    }

    fun getBoundariesOfDay(date: LocalDate): Pair<Long, Long> =
        date.atStartOfDay(ZoneOffset.UTC).toEpochSecond() to
            date.atTime(23, 59, 59).toEpochSecond(ZoneOffset.UTC)

    fun updateHoursForDay(
        dayModels: List<DayModel>,
        dayToUpdate: Int,
        newHours: Int,
    ): List<DayModel> = dayModels.mapIndexed { index, dayModel ->
        if (index == dayToUpdate) dayModel.copy(time = dayModel.time + newHours) else dayModel
    }
}

private fun YearMonth.range(): List<LocalDate> =
    (1..lengthOfMonth()).map(::atDay)
