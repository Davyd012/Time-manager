package no.timnor.app.shared.domain.work

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkScheduleTest {
    @Test
    fun plansWeekdaysWithExistingDurationsAndSkipsWeekend() {
        val dates = (1..7).map { LocalDate(2026, 6, it) }

        assertEquals(
            listOf(
                PlannedWorkDay(LocalDate(2026, 6, 1), 8 * 60 * 60),
                PlannedWorkDay(LocalDate(2026, 6, 2), 8 * 60 * 60),
                PlannedWorkDay(LocalDate(2026, 6, 3), 8 * 60 * 60),
                PlannedWorkDay(LocalDate(2026, 6, 4), 8 * 60 * 60),
                PlannedWorkDay(LocalDate(2026, 6, 5), 5 * 60 * 60 + 30 * 60),
            ),
            plannedWeekdayWork(dates),
        )
    }
}
