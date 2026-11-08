package no.timnor.app.shared.domain.project

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectFilterTest {
    private data class Project(val id: Int, val name: String)
    private data class Work(val projectId: Int, val seconds: Int)

    @Test
    fun searchTrimsQueryAndMatchesNameWithoutChangingIdentity() {
        val projects = listOf(
            Project(1, "Bilservice"),
            Project(2, "Nettside redesign"),
            Project(3, "Mobilapp"),
        )

        assertEquals(
            listOf(projects[1]),
            filterProjects(projects, "  REDESIGN ", Project::name),
        )
        assertEquals(projects, filterProjects(projects, "  ", Project::name))
    }

    @Test
    fun workHoursAreSummedByProject() {
        val works = listOf(
            Work(1, 3600),
            Work(1, 1800),
            Work(2, 7200),
        )

        assertEquals(
            mapOf(1 to 1.5, 2 to 2.0),
            projectHoursById(works, Work::projectId, Work::seconds),
        )
    }
}
