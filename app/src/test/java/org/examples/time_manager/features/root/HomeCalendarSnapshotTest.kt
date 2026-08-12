package org.examples.time_manager.features.root

import java.time.LocalDateTime
import java.time.YearMonth
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCalendarSnapshotTest {
    @Test
    fun snapshotKeepsDisplayedMonthAndAllCalendarAggregatesInSync() {
        val month = YearMonth.of(2026, 8)
        val selectedProject = Project(id = 1, name = "Selected")
        val filteredWorks = listOf(
            Work(
                project = selectedProject.id,
                date = LocalDateTime.of(2026, 8, 5, 9, 0),
                time = 3600,
            ),
        )
        val allWorks = filteredWorks + listOf(
            Work(
                project = 2,
                date = LocalDateTime.of(2026, 8, 6, 9, 0),
                time = 7200,
            ),
            Work(
                project = 1,
                date = LocalDateTime.of(2026, 9, 1, 9, 0),
                time = 10_800,
            ),
        )

        val snapshot = buildCalendarSnapshot(
            month = month,
            selectedProjects = listOf(selectedProject),
            filteredWorks = filteredWorks,
            allWorks = allWorks,
        )

        assertEquals(month, snapshot.month)
        assertEquals(1.0, snapshot.totalHours, 0.0)
        assertEquals(3600, snapshot.days.first { it.date.dayOfMonth == 5 }.time)
        assertEquals(mapOf(1 to 1.0, 2 to 2.0), snapshot.projectHours)
    }
}
