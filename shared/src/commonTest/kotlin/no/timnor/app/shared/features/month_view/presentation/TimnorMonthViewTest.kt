package no.timnor.app.shared.features.month_view.presentation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.features.month_view.presentation.components.monthViewWorkGroups
import no.timnor.app.shared.features.month_view.presentation.components.monthViewWorksForDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TimnorMonthViewTest {
    @Test
    fun selectedDayWorksAreFilteredAndSortedByStartTime() {
        val selectedDate = LocalDate(2024, 5, 10)
        val early = work(1, selectedDate, LocalTime(8, 0))
        val late = work(2, selectedDate, LocalTime(13, 30))
        val otherProject = work(4, selectedDate, LocalTime(11, 0), projectId = 2)
        val otherDay = work(3, LocalDate(2024, 5, 11), LocalTime(7, 0))

        assertEquals(
            listOf(early, otherProject, late),
            monthViewWorksForDate(listOf(late, otherDay, early, otherProject), selectedDate),
        )
    }

    @Test
    fun timelineGroupsAreOrderedByHourAndKeepChronologicalEntries() {
        val date = LocalDate(2024, 5, 10)
        val laterInHour = work(1, date, LocalTime(9, 45))
        val earlierInHour = work(2, date, LocalTime(9, 5))
        val morning = work(3, date, LocalTime(8, 55))

        assertEquals(
            linkedMapOf(8 to listOf(morning), 9 to listOf(earlierInHour, laterInHour)),
            monthViewWorkGroups(listOf(laterInHour, morning, earlierInHour)),
        )
    }
}

private fun work(id: Int, date: LocalDate, time: LocalTime, projectId: Int = 1) = StoredWork(
    description = "Work $id",
    dateEpochSeconds = utcEpochSecondsFromLocalDateTime(LocalDateTime(date, time)),
    project = projectId,
    task = 0,
    time = 3_600,
    id = id,
)
