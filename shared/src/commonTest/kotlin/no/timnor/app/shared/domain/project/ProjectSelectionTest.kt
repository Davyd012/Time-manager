package no.timnor.app.shared.domain.project

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectSelectionTest {
    private val allProjectIds = setOf(1, 2, 3)

    @Test
    fun togglingOneProjectFromAllCreatesAnExplicitSelection() {
        assertEquals(
            setOf(2, 3),
            toggleProjectSelection(
                selectedIds = null,
                projectId = 1,
                allProjectIds = allProjectIds,
            ),
        )
    }

    @Test
    fun togglingBackToAllProjectsRestoresImplicitSelection() {
        assertEquals(
            null,
            toggleProjectSelection(
                selectedIds = setOf(2, 3),
                projectId = 1,
                allProjectIds = allProjectIds,
            ),
        )
    }

    @Test
    fun togglingTheLastSelectedProjectLeavesAnExplicitEmptySelection() {
        assertEquals(
            emptySet(),
            toggleProjectSelection(
                selectedIds = setOf(1),
                projectId = 1,
                allProjectIds = allProjectIds,
            ),
        )
    }

    @Test
    fun selectingAllVisibleProjectsWithSearchActivePreservesHiddenSelection() {
        assertEquals(
            setOf(1, 3),
            toggleVisibleProjectSelection(
                selectedIds = emptySet(),
                visibleProjectIds = setOf(1, 3),
                allProjectIds = allProjectIds,
            ),
        )
    }

    @Test
    fun deselectingAllProjectsCanLeaveNoneSelected() {
        assertEquals(
            emptySet(),
            toggleVisibleProjectSelection(
                selectedIds = null,
                visibleProjectIds = allProjectIds,
                allProjectIds = allProjectIds,
            ),
        )
    }

    @Test
    fun togglingAFilteredSubsetChangesOnlyThoseProjects() {
        assertEquals(
            setOf(2),
            toggleVisibleProjectSelection(
                selectedIds = null,
                visibleProjectIds = setOf(1, 3),
                allProjectIds = allProjectIds,
            ),
        )
    }

    @Test
    fun togglingNoVisibleProjectsLeavesSelectionUnchanged() {
        assertEquals(
            setOf(2),
            toggleVisibleProjectSelection(
                selectedIds = setOf(2),
                visibleProjectIds = emptySet(),
                allProjectIds = allProjectIds,
            ),
        )
    }
}
