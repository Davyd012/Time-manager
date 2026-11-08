package no.timnor.app.shared.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform
import no.timnor.app.shared.platform.stopwatch.StopwatchSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TimnorHomeViewModelTest {
    @Test
    fun calendarProjectFilterUsesImplicitAllAndTracksDeletedProjects() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val viewModel = TimnorHomeViewModel(persistence, FakeStopwatch())
            viewModel.addProject("Research")
            runCurrent()
            viewModel.addProject("Planning")
            runCurrent()

            viewModel.toggleCalendarProject(1)
            assertEquals(setOf(2), viewModel.state.value.selectedCalendarProjectIds)
            viewModel.toggleCalendarProject(1)
            assertEquals(null, viewModel.state.value.selectedCalendarProjectIds)

            viewModel.toggleCalendarProject(1)
            viewModel.deleteProject(viewModel.state.value.projects.first { it.id == 2 })
            runCurrent()
            assertEquals(null, viewModel.state.value.selectedCalendarProjectIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun savingStopwatchPersistsElapsedSecondsForSelectedProject() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val stopwatch = FakeStopwatch()
            val viewModel = TimnorHomeViewModel(persistence, stopwatch)
            runCurrent()
            viewModel.addProject("Research")
            runCurrent()
            stopwatch.setElapsed(3_661, projectId = 1)
            runCurrent()

            viewModel.saveStopwatch()
            runCurrent()

            val saved = viewModel.state.value.works.single()
            assertEquals("", saved.description)
            assertEquals(1, saved.project)
            assertEquals(3_661, saved.time)
            assertTrue(saved.dateEpochSeconds > 0)
            assertEquals(0, viewModel.state.value.stopwatch.elapsedSeconds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun addProjectAndManualWorkUpdateObservedState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val stopwatch = FakeStopwatch()
            val viewModel = TimnorHomeViewModel(persistence, stopwatch)
            runCurrent()

            viewModel.updateCalendarMonth(kotlinx.datetime.LocalDate(2024, 2, 1))
            viewModel.selectCalendarDate(29)
            assertEquals(kotlinx.datetime.LocalDate(2024, 2, 29), viewModel.state.value.selectedCalendarDate)
            viewModel.updateCalendarMonth(kotlinx.datetime.LocalDate(2024, 4, 1))
            viewModel.selectCalendarDate(31)
            assertEquals(kotlinx.datetime.LocalDate(2024, 4, 30), viewModel.state.value.selectedCalendarDate)

            viewModel.addProject("  Research  ")
            runCurrent()
            viewModel.editProject(viewModel.state.value.projects.single())
            viewModel.updateProjectName("Research plan")
            viewModel.updateProjectDescription("Quarterly work")
            viewModel.saveProject()
            runCurrent()
            viewModel.addManualWork(1, "2024-02-29", "2", "15", "  Planning  ")
            runCurrent()

            assertEquals(listOf(StoredProject("Quarterly work", "Research plan", 1)), viewModel.state.value.projects)
            assertEquals(
                listOf(
                    StoredWork(
                        description = "Planning",
                        dateEpochSeconds = utcEpochSecondsFromLocalDateTime(
                            LocalDateTime(kotlinx.datetime.LocalDate(2024, 2, 29), LocalTime(7, 0)),
                        ),
                        project = 1,
                        task = 0,
                        time = 8_100,
                        id = 1,
                    ),
                ),
                viewModel.state.value.works,
            )
            assertEquals(null, viewModel.state.value.error)

            persistence.upsertWork(persistence.works.value.single().copy(time = 10_805))
            runCurrent()
            viewModel.saveManualWork(1, 1, "2024-03-01", "3", "0", "Updated")
            runCurrent()
            assertEquals(1, viewModel.state.value.works.single().id)
            assertEquals("Updated", viewModel.state.value.works.single().description)
            assertEquals(10_805, viewModel.state.value.works.single().time)

            viewModel.deleteWork(viewModel.state.value.works.single())
            runCurrent()
            assertEquals(emptyList(), viewModel.state.value.works)

            viewModel.deleteProject(viewModel.state.value.projects.single())
            runCurrent()
            assertEquals(emptyList(), viewModel.state.value.projects)
            assertEquals(0, stopwatch.state.value.selectedProject)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FakeTimnorPersistence : TimnorPersistence {
    private val projects = MutableStateFlow(emptyList<StoredProject>())
    val works = MutableStateFlow(emptyList<StoredWork>())

    override fun observeProjects(): Flow<List<StoredProject>> = projects

    override fun observeWorks(
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        projectIds: List<Int>,
    ): Flow<List<StoredWork>> = works

    override suspend fun hoursForRange(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>) =
        works.value.filter { it.dateEpochSeconds in startEpochSeconds..endEpochSeconds }
            .filter { projectIds.isEmpty() || it.project in projectIds }
            .sumOf(StoredWork::time)

    override suspend fun upsertProject(project: StoredProject) {
        val id = project.id.takeIf { it != 0 } ?: ((projects.value.maxOfOrNull(StoredProject::id) ?: 0) + 1)
        projects.value = projects.value.filterNot { it.id == id } + project.copy(id = id)
    }

    override suspend fun deleteProject(project: StoredProject) {
        projects.value = projects.value.filterNot { it.id == project.id }
    }

    override suspend fun upsertWork(work: StoredWork) {
        val id = work.id.takeIf { it != 0 } ?: ((works.value.maxOfOrNull(StoredWork::id) ?: 0) + 1)
        works.value = works.value.filterNot { it.id == id } + work.copy(id = id)
    }

    override suspend fun deleteWork(work: StoredWork) {
        works.value = works.value.filterNot { it.id == work.id }
    }

    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) {
        this.projects.value = projects
        this.works.value = works
    }
}

private class FakeStopwatch : StopwatchPlatform {
    private val mutableState = MutableStateFlow(StopwatchSnapshot())
    override val state = mutableState

    override fun start() {
        mutableState.value = mutableState.value.copy(isRunning = true)
    }

    override fun pause() {
        mutableState.value = mutableState.value.copy(isRunning = false)
    }

    override fun cancel() {
        mutableState.value = StopwatchSnapshot(selectedProject = mutableState.value.selectedProject)
    }

    override fun timerSaved() = cancel()

    override fun selectProject(projectId: Int) {
        mutableState.value = mutableState.value.copy(selectedProject = projectId)
    }

    fun setElapsed(seconds: Int, projectId: Int) {
        mutableState.value = StopwatchSnapshot(seconds, isRunning = false, selectedProject = projectId)
    }
}
