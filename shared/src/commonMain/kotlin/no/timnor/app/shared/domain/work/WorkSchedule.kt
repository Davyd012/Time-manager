package no.timnor.app.shared.domain.work

import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

data class PlannedWorkDay(
    val date: LocalDate,
    val timeSeconds: Int,
)

/** Existing bulk-entry rule: weekdays only, eight hours Mon–Thu and 5.5 on Friday. */
fun plannedWeekdayWork(dates: List<LocalDate>): List<PlannedWorkDay> =
    dates.mapNotNull { date ->
        when (date.dayOfWeek.isoDayNumber) {
            1, 2, 3, 4 -> PlannedWorkDay(date, 8 * 60 * 60)
            5 -> PlannedWorkDay(date, (5.5 * 60 * 60).toInt())
            else -> null
        }
    }
