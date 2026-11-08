package no.timnor.app.features.calendar

import java.time.LocalDateTime
import no.timnor.app.core.database.project.Project
import no.timnor.app.core.database.work.Work
import no.timnor.app.features.calendar.presentation.components.filterProjects
import no.timnor.app.features.calendar.presentation.components.projectHoursById
import no.timnor.app.features.calendar.presentation.components.toggleProjectSelection
import no.timnor.app.features.calendar.presentation.components.toggleVisibleProjectSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectFilterLogicTest {
    private val projects = listOf(
        Project(name = "Bilservice", id = 1),
        Project(name = "Nettside redesign", id = 2),
        Project(name = "Mobilapp", id = 3),
    )

    @Test
    fun searchFiltersByNameWithoutChangingProjectIdentity() {
        assertEquals(
            listOf(projects[1]),
            filterProjects(projects, "REDESIGN"),
        )
    }

    @Test
    fun togglingOneProjectFromAllCreatesAnExplicitSelection() {
        assertEquals(
            setOf(2, 3),
            toggleProjectSelection(
                selectedIds = null,
                projectId = 1,
                allProjectIds = projects.map(Project::id).toSet(),
            ),
        )
    }

    @Test
    fun selectingAllVisibleProjectsPreservesShowAllSemanticsWhenCleared() {
        assertEquals(
            null,
            toggleVisibleProjectSelection(
                selectedIds = null,
                visibleProjectIds = projects.map(Project::id).toSet(),
                allProjectIds = projects.map(Project::id).toSet(),
            ),
        )
    }

    @Test
    fun projectHoursAreAggregatedByProject() {
        val works = listOf(
            Work(date = LocalDateTime.of(2026, 8, 1, 9, 0), project = 1, time = 3600),
            Work(date = LocalDateTime.of(2026, 8, 2, 9, 0), project = 1, time = 1800),
            Work(date = LocalDateTime.of(2026, 8, 2, 12, 0), project = 2, time = 7200),
        )

        assertEquals(mapOf(1 to 1.5, 2 to 2.0), projectHoursById(works))
    }
}
