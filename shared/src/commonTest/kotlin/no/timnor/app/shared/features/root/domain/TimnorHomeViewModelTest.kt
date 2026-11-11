package no.timnor.app.shared.features.root.domain

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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.data.persistence.TimnorDataSnapshot
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform
import no.timnor.app.shared.platform.stopwatch.StopwatchSnapshot
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TimnorHomeViewModelTest {
    @Test
    fun stopwatchControlStartsAndPausesThroughThePlatformBoundary() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val stopwatch = FakeStopwatch()
            val viewModel = TimnorHomeViewModel(FakeTimnorPersistence(), stopwatch)
            runCurrent()

            viewModel.startOrPauseStopwatch()
            runCurrent()
            assertTrue(viewModel.state.value.stopwatch.isRunning)

            viewModel.startOrPauseStopwatch()
            runCurrent()
            assertFalse(viewModel.state.value.stopwatch.isRunning)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun savingStopwatchPersistsElapsedTimeAndProjectBeforeClearingIt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val stopwatch = FakeStopwatch()
            val viewModel = TimnorHomeViewModel(persistence, stopwatch)
            runCurrent()
            val elapsedSeconds = 3_605
            val selectedProject = 7
            stopwatch.setElapsed(elapsedSeconds, selectedProject, isRunning = true)
            runCurrent()
            val nowBeforeSave = Clock.System.now().epochSeconds

            viewModel.saveStopwatch()
            runCurrent()

            val saved = persistence.works.value.single()
            val nowAfterSave = Clock.System.now().epochSeconds
            val expectedStartEpochSeconds = (nowBeforeSave..nowAfterSave).map { now ->
                utcEpochSecondsFromLocalDateTime(
                    Instant.fromEpochSeconds(now - elapsedSeconds)
                        .toLocalDateTime(TimeZone.currentSystemDefault()),
                )
            }
            assertEquals(elapsedSeconds, saved.time)
            assertEquals(selectedProject, saved.project)
            assertTrue(saved.dateEpochSeconds in expectedStartEpochSeconds.min()..expectedStartEpochSeconds.max())
            assertEquals(1, stopwatch.timerSavedCount)
            assertEquals(0, stopwatch.state.value.elapsedSeconds)
            assertFalse(stopwatch.state.value.isRunning)

            viewModel.saveStopwatch()
            runCurrent()

            assertEquals(1, persistence.works.value.size)
            assertEquals(1, stopwatch.timerSavedCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun failedStopwatchSaveKeepsThePausedSessionAvailableForRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence().apply { failWorkWrites = true }
            val stopwatch = FakeStopwatch()
            val viewModel = TimnorHomeViewModel(persistence, stopwatch)
            runCurrent()
            stopwatch.setElapsed(95, 4, isRunning = true)
            runCurrent()

            viewModel.saveStopwatch()
            runCurrent()

            assertTrue(persistence.works.value.isEmpty())
            assertEquals(TimnorHomeError.STORAGE, viewModel.state.value.error)
            assertEquals(95, stopwatch.state.value.elapsedSeconds)
            assertFalse(stopwatch.state.value.isRunning)
            assertEquals(0, stopwatch.timerSavedCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun sharedMonthRouteStartsAtItsSelectedDate() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val selectedDate = kotlinx.datetime.LocalDate(2024, 2, 29)
            val viewModel = TimnorHomeViewModel(FakeTimnorPersistence(), FakeStopwatch(), selectedDate)

            assertEquals(selectedDate, viewModel.state.value.selectedCalendarDate)
            assertEquals(kotlinx.datetime.LocalDate(2024, 2, 1), viewModel.state.value.calendarMonth)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun browsingCalendarMonthsKeepsTheSelectedHomeDate() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val selectedDate = kotlinx.datetime.LocalDate(2026, 11, 10)
            val viewModel = TimnorHomeViewModel(FakeTimnorPersistence(), FakeStopwatch(), selectedDate)

            viewModel.updateCalendarMonth(kotlinx.datetime.LocalDate(2026, 12, 1))

            assertEquals(kotlinx.datetime.LocalDate(2026, 12, 1), viewModel.state.value.calendarMonth)
            assertEquals(selectedDate, viewModel.state.value.selectedCalendarDate)
        } finally {
            Dispatchers.resetMain()
        }
    }

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

            viewModel.setCalendarProjectSelection(setOf(1, 999))
            assertEquals(setOf(1), viewModel.state.value.selectedCalendarProjectIds)
            viewModel.setCalendarProjectSelection(null)
            assertEquals(null, viewModel.state.value.selectedCalendarProjectIds)

            viewModel.toggleCalendarProject(1)
            viewModel.deleteProject(viewModel.state.value.projects.first { it.id == 2 })
            runCurrent()
            assertEquals(emptySet(), viewModel.state.value.selectedCalendarProjectIds)
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
    fun editingWorkDoesNotChangeStopwatchProjectSelection() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val stopwatch = FakeStopwatch()
            val viewModel = TimnorHomeViewModel(persistence, stopwatch)
            runCurrent()
            viewModel.addProject("Stopwatch project")
            runCurrent()
            viewModel.addProject("Work project")
            runCurrent()

            viewModel.selectProject(1)
            viewModel.addManualWork(2, "2024-05-10", "1", "0", "Entry")
            runCurrent()
            viewModel.editWork(persistence.works.value.single())

            assertEquals(1, viewModel.state.value.selectedProjectId)
            assertEquals(2, viewModel.state.value.workProjectId)
            assertEquals(1, stopwatch.state.value.selectedProject)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun creatingProjectFromStopwatchPickerSelectsTheNewProject() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = TimnorHomeViewModel(FakeTimnorPersistence(), FakeStopwatch())
            runCurrent()
            viewModel.addProject("Existing")
            runCurrent()

            viewModel.updateProjectName("New project")
            viewModel.saveProjectAndSelect()
            runCurrent()

            assertEquals(2, viewModel.state.value.selectedProjectId)
            assertEquals(2, viewModel.state.value.stopwatch.selectedProject)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun creatingProjectDuringRecordedStopwatchTimeDoesNotChangeTheActiveProject() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val stopwatch = FakeStopwatch()
            val viewModel = TimnorHomeViewModel(FakeTimnorPersistence(), stopwatch)
            runCurrent()
            viewModel.addProject("Active project")
            runCurrent()
            stopwatch.setElapsed(seconds = 12, projectId = 1)
            runCurrent()

            viewModel.updateProjectName("New project")
            viewModel.saveProjectAndSelect()
            runCurrent()

            assertEquals(listOf("Active project", "New project"), viewModel.state.value.projects.map(StoredProject::name))
            assertEquals(1, viewModel.state.value.selectedProjectId)
            assertEquals(1, viewModel.state.value.stopwatch.selectedProject)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun periodRegistrationCreatesTheExistingWeekdaySchedule() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val viewModel = TimnorHomeViewModel(persistence, FakeStopwatch())
            runCurrent()
            viewModel.addProject("Research")
            runCurrent()
            viewModel.updateWorkDescription("Weekly plan")

            viewModel.submitManualWorkPeriod(
                (1..7).map { kotlinx.datetime.LocalDate(2026, 6, it) },
            )
            runCurrent()

            assertEquals(
                listOf(
                    kotlinx.datetime.LocalDate(2026, 6, 1),
                    kotlinx.datetime.LocalDate(2026, 6, 2),
                    kotlinx.datetime.LocalDate(2026, 6, 3),
                    kotlinx.datetime.LocalDate(2026, 6, 4),
                    kotlinx.datetime.LocalDate(2026, 6, 5),
                ),
                viewModel.state.value.works.map {
                    no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds(it.dateEpochSeconds).date
                },
            )
            assertEquals(
                listOf(8 * 3_600, 8 * 3_600, 8 * 3_600, 8 * 3_600, 5 * 3_600 + 30 * 60),
                viewModel.state.value.works.map(StoredWork::time),
            )
            assertTrue(viewModel.state.value.works.all { it.project == 1 && it.description == "Weekly plan" })
            assertTrue(
                viewModel.state.value.works.all {
                    no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds(it.dateEpochSeconds).hour == 7
                },
            )
            assertEquals(null, viewModel.state.value.editingWorkId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun singleWeekdayPeriodCanBeSubmitted() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val viewModel = TimnorHomeViewModel(persistence, FakeStopwatch())
            runCurrent()
            viewModel.addProject("Research")
            runCurrent()
            var result: Boolean? = null

            viewModel.submitManualWorkPeriod(listOf(kotlinx.datetime.LocalDate(2026, 6, 8))) { result = it }
            runCurrent()

            assertEquals(true, result)
            assertEquals(1, viewModel.state.value.works.size)
            assertEquals(8 * 3_600, viewModel.state.value.works.single().time)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun workEditorReceivesFailureAndKeepsDraftAfterStorageError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val viewModel = TimnorHomeViewModel(persistence, FakeStopwatch())
            runCurrent()
            viewModel.addProject("Research")
            runCurrent()
            viewModel.updateWorkDescription("Keep this draft")
            viewModel.updateWorkHours("1")
            persistence.failWorkWrites = true
            var result: Boolean? = null

            viewModel.submitManualWork { result = it }
            runCurrent()

            assertEquals(false, result)
            assertEquals(TimnorHomeError.STORAGE, viewModel.state.value.error)
            assertEquals("Keep this draft", viewModel.state.value.workDescription)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun failedWorkDeletionReportsFailureAndKeepsTheEntryForRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val viewModel = TimnorHomeViewModel(persistence, FakeStopwatch())
            runCurrent()
            viewModel.addProject("Research")
            runCurrent()
            viewModel.addManualWork(1, "2026-06-08", "2", "0", "Keep this entry")
            runCurrent()
            val work = persistence.works.value.single()
            viewModel.editWork(work)
            persistence.failWorkDeletes = true
            var result: Boolean? = null

            viewModel.deleteWork(work) { result = it }
            runCurrent()

            assertEquals(false, result)
            assertEquals(TimnorHomeError.STORAGE, viewModel.state.value.error)
            assertEquals(work, persistence.works.value.single())
            assertEquals(work.id, viewModel.state.value.editingWorkId)
            assertEquals("Keep this entry", viewModel.state.value.workDescription)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deletingTheStopwatchProjectSelectsAValidReplacement() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val stopwatch = FakeStopwatch()
            val viewModel = TimnorHomeViewModel(persistence, stopwatch)
            runCurrent()
            viewModel.addProject("Timer")
            runCurrent()
            viewModel.addProject("Work")
            runCurrent()
            viewModel.selectWorkProject(2)
            viewModel.selectProject(1)

            viewModel.deleteProject(viewModel.state.value.projects.first { it.id == 1 })
            runCurrent()

            assertEquals(2, stopwatch.state.value.selectedProject)
            assertEquals(2, viewModel.state.value.workProjectId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deletingTheActiveStopwatchProjectIsBlockedUntilItsTimeIsSaved() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val stopwatch = FakeStopwatch()
            val viewModel = TimnorHomeViewModel(persistence, stopwatch)
            runCurrent()
            viewModel.addProject("Active project")
            runCurrent()
            stopwatch.setElapsed(seconds = 12, projectId = 1)
            runCurrent()
            var result: Boolean? = null

            viewModel.deleteProject(viewModel.state.value.projects.single()) { result = it }

            assertEquals(false, result)
            assertEquals(TimnorHomeError.ACTIVE_STOPWATCH_PROJECT, viewModel.state.value.error)
            assertEquals(listOf("Active project"), viewModel.state.value.projects.map(StoredProject::name))
            assertEquals(1, viewModel.state.value.stopwatch.selectedProject)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deletingTheActiveProjectIsBlockedImmediatelyAfterStartingTheStopwatch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = TimnorHomeViewModel(FakeTimnorPersistence(), FakeStopwatch())
            runCurrent()
            viewModel.addProject("Active project")
            runCurrent()
            viewModel.startOrPauseStopwatch()
            runCurrent()
            var result: Boolean? = null

            viewModel.deleteProject(viewModel.state.value.projects.single()) { result = it }

            assertEquals(false, result)
            assertEquals(TimnorHomeError.ACTIVE_STOPWATCH_PROJECT, viewModel.state.value.error)
            assertEquals(listOf("Active project"), viewModel.state.value.projects.map(StoredProject::name))
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
            viewModel.selectCalendarDate(kotlinx.datetime.LocalDate(2024, 5, 1))
            assertEquals(kotlinx.datetime.LocalDate(2024, 5, 1), viewModel.state.value.calendarMonth)
            assertEquals(kotlinx.datetime.LocalDate(2024, 5, 1), viewModel.state.value.selectedCalendarDate)

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
            viewModel.editWork(persistence.works.value.single())
            assertEquals("07:00", viewModel.state.value.workStartTime)
            viewModel.updateWorkDate("2024-03-01")
            viewModel.updateWorkStartTime("09:35")
            viewModel.updateWorkHours("3")
            viewModel.updateWorkMinutes("0")
            viewModel.updateWorkDescription("Updated")
            viewModel.submitManualWork()
            runCurrent()
            assertEquals(1, viewModel.state.value.works.single().id)
            assertEquals("Updated", viewModel.state.value.works.single().description)
            assertEquals(10_805, viewModel.state.value.works.single().time)
            assertEquals(
                utcEpochSecondsFromLocalDateTime(
                    LocalDateTime(kotlinx.datetime.LocalDate(2024, 3, 1), LocalTime(9, 35)),
                ),
                viewModel.state.value.works.single().dateEpochSeconds,
            )

            viewModel.deleteWork(viewModel.state.value.works.single())
            runCurrent()
            assertEquals(emptyList(), viewModel.state.value.works)
            assertEquals(null, viewModel.state.value.editingWorkId)

            viewModel.deleteProject(viewModel.state.value.projects.single())
            runCurrent()
            assertEquals(emptyList(), viewModel.state.value.projects)
            assertEquals(0, stopwatch.state.value.selectedProject)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun newManualWorkKeepsLegacyAutomaticStartTimeUntilUserEditsIt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val selectedDate = kotlinx.datetime.LocalDate(2026, 11, 10)
            val viewModel = TimnorHomeViewModel(
                FakeTimnorPersistence(),
                FakeStopwatch(),
                selectedDate,
            )

            viewModel.beginManualWork()

            assertEquals("00:00", viewModel.state.value.workStartTime)
            assertFalse(viewModel.state.value.workStartTimeManuallySet)
            viewModel.updateWorkStartTime("09:20")
            assertTrue(viewModel.state.value.workStartTimeManuallySet)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FakeTimnorPersistence : TimnorPersistence {
    private val projects = MutableStateFlow(emptyList<StoredProject>())
    val works = MutableStateFlow(emptyList<StoredWork>())
    var failWorkWrites = false
    var failWorkDeletes = false

    override fun observeProjects(): Flow<List<StoredProject>> = projects

    override suspend fun snapshotAll() = TimnorDataSnapshot(projects.value.toList(), works.value.toList())

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
        check(!failWorkWrites)
        val id = work.id.takeIf { it != 0 } ?: ((works.value.maxOfOrNull(StoredWork::id) ?: 0) + 1)
        works.value = works.value.filterNot { it.id == id } + work.copy(id = id)
    }

    override suspend fun upsertWorks(works: List<StoredWork>) {
        check(!failWorkWrites)
        var nextId = this.works.value.maxOfOrNull(StoredWork::id) ?: 0
        val updated = works.fold(this.works.value) { current, work ->
            val id = work.id.takeIf { it != 0 } ?: ++nextId
            current.filterNot { it.id == id } + work.copy(id = id)
        }
        this.works.value = updated
    }

    override suspend fun deleteWork(work: StoredWork) {
        check(!failWorkDeletes)
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
    var timerSavedCount = 0

    override fun start() {
        mutableState.value = mutableState.value.copy(isRunning = true)
    }

    override fun pause() {
        mutableState.value = mutableState.value.copy(isRunning = false)
    }

    override fun cancel() {
        mutableState.value = StopwatchSnapshot(selectedProject = mutableState.value.selectedProject)
    }

    override fun timerSaved() {
        timerSavedCount++
        cancel()
    }

    override fun selectProject(projectId: Int) {
        mutableState.value = mutableState.value.copy(selectedProject = projectId)
    }

    fun setElapsed(seconds: Int, projectId: Int, isRunning: Boolean = false) {
        mutableState.value = StopwatchSnapshot(seconds, isRunning, projectId)
    }
}
