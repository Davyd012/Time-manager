package no.timnor.app.shared.features.root.domain

import no.timnor.app.shared.data.persistence.CalendarProjectFilterStore
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
import no.timnor.app.shared.domain.project.workPayOre
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform
import no.timnor.app.shared.platform.stopwatch.StopwatchSnapshot
import no.timnor.app.shared.features.shared_hours.domain.PlannorTaskOption
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TimnorHomeViewModelTest {
    @Test
    fun projectRateDraftValidatesEnabledRatesAndCancelKeepsSavedRates() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val project = StoredProject("", "Project", 1, hourlyRateOre = 18_000, weekendHourlyRateOre = 22_000)
            persistence.upsertProject(project)
            val viewModel = TimnorHomeViewModel(persistence, FakeStopwatch())
            runCurrent()
            viewModel.editProject(project)

            for (invalid in listOf("", "-1", "220.555", "abc")) {
                viewModel.updateProjectWeekendRate(invalid)
                viewModel.saveProject()
                assertEquals(TimnorHomeError.INVALID_HOURLY_RATE, viewModel.state.value.error)
            }
            viewModel.updateProjectHourlyRate("")
            viewModel.updateProjectWeekendRate("0")
            viewModel.saveProject()
            assertEquals(TimnorHomeError.INVALID_HOURLY_RATE, viewModel.state.value.error)
            viewModel.cancelProjectEdit()
            assertEquals(project, persistence.snapshotAll().projects.single())

            viewModel.editProject(project)
            viewModel.updateProjectName("  ")
            viewModel.saveProject()
            assertEquals(TimnorHomeError.INVALID_PROJECT, viewModel.state.value.error)
            viewModel.updateProjectName(project.name)
            viewModel.updateProjectHourlyRate("180,50")
            viewModel.updateProjectWeekendRate("0")
            viewModel.saveProject()
            runCurrent()
            val saved = persistence.snapshotAll().projects.single()
            assertEquals(project.copy(hourlyRateOre = 18_050, weekendHourlyRateOre = 0), saved)
            viewModel.editProject(saved)
            assertEquals("180.50", viewModel.state.value.projectHourlyRate)
            assertEquals("0", viewModel.state.value.projectWeekendRate)
            viewModel.updateProjectWeekendRate(null)
            viewModel.saveProject()
            runCurrent()
            assertEquals(saved.copy(weekendHourlyRateOre = null), persistence.snapshotAll().projects.single())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun manualWorkReopensWithEnteredEndTimeAndClearsItWhenDurationIsSet() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            persistence.upsertProject(StoredProject("", "Project", 1))
            val viewModel = TimnorHomeViewModel(persistence, FakeStopwatch())
            runCurrent()
            viewModel.beginManualWork()
            viewModel.updateWorkStartTime("16:00")
            viewModel.updateWorkEndTime(19, 45)
            viewModel.submitManualWork()
            runCurrent()

            val saved = persistence.works.value.single()
            assertEquals(13_500, saved.time)
            viewModel.editWork(saved)
            assertEquals("19:45", viewModel.state.value.workEndTime.toString())
            viewModel.updateWorkStartTime("17:15")
            assertEquals("2", viewModel.state.value.workHours)
            assertEquals("30", viewModel.state.value.workMinutes)
            viewModel.updateWorkDuration(2, 0)
            viewModel.submitManualWork()
            runCurrent()

            val edited = persistence.works.value.single()
            assertEquals(7_200, edited.time)
            viewModel.editWork(edited)
            assertNull(viewModel.state.value.workEndTime)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun manualWorkSplitsFridayNightAndKeepsItsPayAfterRateChanges() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            persistence.upsertProject(
                StoredProject("", "Project", 1, hourlyRateOre = 18_000, weekendHourlyRateOre = 22_000),
            )
            val viewModel = TimnorHomeViewModel(persistence, FakeStopwatch())
            runCurrent()
            viewModel.beginManualWork()
            viewModel.updateWorkDate("2026-11-13")
            viewModel.updateWorkDescription("Shift")
            viewModel.updateWorkStartTime("23:00")
            viewModel.updateWorkEndTime(1, 0)
            viewModel.submitManualWork()
            runCurrent()
            val original = persistence.works.value
            assertEquals(listOf(18_000, 22_000), original.map { it.appliedHourlyRateOre })
            assertEquals(listOf(3600, 3600), original.map { it.time })
            assertEquals(listOf(0, 60), original.map { it.enteredEndMinute })
            assertEquals(listOf(18_000L, 22_000L), original.map { workPayOre(it, null) })
            assertEquals(
                utcEpochSecondsFromLocalDateTime(LocalDateTime.parse("2026-11-14T00:00")),
                original.last().dateEpochSeconds,
            )

            viewModel.editProject(viewModel.state.value.projects.single())
            viewModel.updateProjectHourlyRate("200")
            viewModel.updateProjectWeekendRate(null)
            viewModel.saveProject()
            runCurrent()
            viewModel.addManualWork(1, "2026-11-15", "1", "0", "New shift", "12:00")
            runCurrent()
            assertEquals(original, persistence.works.value.take(2))
            assertEquals(20_000, persistence.works.value.last().appliedHourlyRateOre)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun stopwatchUsesLocalMidnightAndKeepsPauseTimeOnRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            persistence.upsertProject(
                StoredProject("", "Project", 1, hourlyRateOre = 18_000, weekendHourlyRateOre = 22_000),
            )
            val stopwatch = FakeStopwatch()
            val viewModel = TimnorHomeViewModel(
                persistence,
                stopwatch,
                clock = object : Clock {
                    override fun now() = Instant.parse("2026-11-16T12:00:00Z")
                },
                stopwatchTimeZone = { TimeZone.of("Europe/Oslo") },
            )
            runCurrent()
            stopwatch.setElapsed(
                seconds = 7200,
                projectId = 1,
                pausedAtEpochSeconds = Instant.parse("2026-11-16T00:00:00Z").epochSeconds,
            )
            persistence.failWorkWrites = true
            viewModel.saveStopwatch()
            runCurrent()
            assertTrue(persistence.works.value.isEmpty())
            assertEquals(0, stopwatch.timerSavedCount)

            persistence.failWorkWrites = false
            viewModel.saveStopwatch()
            runCurrent()
            assertEquals(listOf(22_000, 18_000), persistence.works.value.map { it.appliedHourlyRateOre })
            assertEquals(listOf(3600, 3600), persistence.works.value.map { it.time })
            assertEquals(1, stopwatch.timerSavedCount)
        } finally {
            Dispatchers.resetMain()
        }
    }
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
    fun calendarFilterSurvivesViewModelRecreationAndCanBeCleared() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            var savedFilter: String? = null
            val store = CalendarProjectFilterStore(read = { savedFilter }, write = { savedFilter = it })
            fun createViewModel() = TimnorHomeViewModel(
                persistence,
                FakeStopwatch(),
                calendarProjectFilterStore = store,
            )
            val first = createViewModel()
            first.addProject("Research")
            runCurrent()
            first.addProject("Planning")
            runCurrent()
            first.setCalendarProjectSelection(setOf(2))

            val reopened = createViewModel()
            runCurrent()
            assertEquals(setOf(2), reopened.state.value.selectedCalendarProjectIds)

            reopened.setCalendarProjectSelection(null)
            val reopenedAfterClear = createViewModel()
            runCurrent()
            assertEquals(null, reopenedAfterClear.state.value.selectedCalendarProjectIds)
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
    fun stopwatchCanOptionallyAttributeLinkedWorkToAPlannorTask() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val stopwatch = FakeStopwatch()
            val viewModel = TimnorHomeViewModel(
                persistence = persistence,
                stopwatch = stopwatch,
                loadLinkedPlannorTasks = { localProjectId ->
                    if (localProjectId == 1) listOf(PlannorTaskOption(21, 42, "Review plans")) else emptyList()
                },
            )
            runCurrent()
            viewModel.addProject("Linked project")
            runCurrent()
            viewModel.selectProject(1)
            runCurrent()
            assertEquals(42, viewModel.state.value.stopwatchPlannorTasks.single().taskId)
            viewModel.selectStopwatchPlannorTask(42)
            stopwatch.setElapsed(900, projectId = 1)
            runCurrent()

            viewModel.saveStopwatch()
            runCurrent()

            assertEquals(42, persistence.works.value.single().task)
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
    fun creatingProjectForWorkSelectsItAndPreservesTheEntry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = TimnorHomeViewModel(FakeTimnorPersistence(), FakeStopwatch())
            runCurrent()
            viewModel.addProject("Existing")
            runCurrent()
            viewModel.updateWorkDescription("Pending work")
            viewModel.updateProjectName("New project")

            viewModel.saveProjectForWork()
            runCurrent()

            assertEquals(2, viewModel.state.value.workProjectId)
            assertEquals("Pending work", viewModel.state.value.workDescription)
            assertEquals(1, viewModel.state.value.stopwatch.selectedProject)
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
            viewModel.updateWorkDuration(1, 0)
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

            viewModel.deleteWork(work, onResult = { result = it })
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
            viewModel.updateWorkDuration(3, 0)
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

    @Test
    fun manualWorkCanSelectAndRetainAnOptionalLinkedTask() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = FakeTimnorPersistence()
            val viewModel = TimnorHomeViewModel(
                persistence = persistence,
                stopwatch = FakeStopwatch(),
                loadLinkedPlannorTasks = { localProjectId ->
                    if (localProjectId == 1) listOf(PlannorTaskOption(7, 91, "Prepare plan")) else emptyList()
                },
            )
            runCurrent()
            viewModel.addProject("Linked project")
            runCurrent()
            viewModel.beginManualWork()
            runCurrent()
            assertEquals(listOf(91), viewModel.state.value.linkedPlannorTasks.map { it.taskId })

            viewModel.selectPlannorTask(91)
            viewModel.updateWorkDuration(1, 0)
            viewModel.submitManualWork()
            runCurrent()
            assertEquals(91, persistence.works.value.single().task)

            viewModel.editWork(persistence.works.value.single())
            runCurrent()
            assertEquals(91, viewModel.state.value.selectedPlannorTaskId)
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

    fun setElapsed(seconds: Int, projectId: Int, isRunning: Boolean = false, pausedAtEpochSeconds: Long? = null) {
        mutableState.value = StopwatchSnapshot(seconds, isRunning, projectId, pausedAtEpochSeconds)
    }
}
