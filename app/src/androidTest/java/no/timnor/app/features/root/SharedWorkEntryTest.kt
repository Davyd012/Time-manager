package no.timnor.app.features.root

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import androidx.compose.ui.unit.dp
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorDataSnapshot
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.home.TimnorHomeScreen
import no.timnor.app.shared.ui.theme.TimnorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SharedWorkEntryTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sharedWorkDeletionRequiresConfirmation() {
        val date = LocalDate(2026, 11, 10)
        val persistence = WorkPersistence(date)
        val viewModel = TimnorHomeViewModel(persistence, IdleStopwatch(), date,
            observeSharedWorkIds = { flowOf(setOf(1)) })
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() && 1 in viewModel.state.value.sharedWorkIds }
        composeRule.onNodeWithTag("work-entry-1").performClick()
        composeRule.onNodeWithTag("delete-work").performScrollTo().performClick()
        composeRule.onNodeWithText("Slette registrering?").assertIsDisplayed()
        composeRule.onNodeWithText("Avbryt").performClick()
        assertEquals(1, viewModel.state.value.works.size)
        composeRule.onNodeWithTag("delete-work").performScrollTo().performClick()
        composeRule.onNodeWithText("Slett").performClick()
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isEmpty() }
        assertEquals(true, persistence.sharedDeletionConfirmed)
    }

    @Test
    fun existingWorkOpensEditorAndCanBeDeleted() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(WorkPersistence(date), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }
        composeRule.onNodeWithTag("work-entry-1").performClick()
        composeRule.onNodeWithText("Registrer timer").assertIsDisplayed()
        composeRule.onNodeWithTag("work-entry-sheet-drag-handle", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("Avbryt").assertCountEquals(0)
        composeRule.onNodeWithTag("delete-work").performScrollTo()
        composeRule.onNodeWithContentDescription("Fjern", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("delete-work")
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
        composeRule.onNodeWithTag("delete-work").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isEmpty() }
        composeRule.onNodeWithTag("delete-work").assertDoesNotExist()
    }

    @Test
    fun homeWorkRowShowsLegacyProjectMarker() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(WorkPersistence(date), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }

        composeRule.onNodeWithTag("work-entry-project-marker-1", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun homeEmptyStateOpensEditorWithoutProjectsLikeLegacy() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }

        composeRule.onNodeWithText("Legg til timer").performClick()

        composeRule.onNodeWithText("Registrer timer").assertIsDisplayed()
    }

    @Test
    fun projectChoicesMatchLegacyChipHeightAndExposeSelection() {
        val date = LocalDate(2026, 11, 10)
        val projects = listOf(
            StoredProject(description = "", name = "Test", id = 1),
            StoredProject(description = "", name = "Second", id = 2),
        )
        val viewModel = TimnorHomeViewModel(
            WorkPersistence(date, projects = projects),
            IdleStopwatch(),
            date,
        )
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }
        composeRule.onNodeWithTag("work-entry-1").performClick()

        composeRule.onNodeWithTag("project-selector-1")
            .assertHeightIsEqualTo(48.dp)
            .assertIsSelected()
        composeRule.onNodeWithTag("project-selector-2")
            .assertHeightIsEqualTo(48.dp)
            .assertIsNotSelected()
            .performClick()
        composeRule.waitUntil(5_000) { viewModel.state.value.workProjectId == 2 }
        composeRule.onNodeWithTag("project-selector-2").assertIsSelected()
    }

    @Test
    fun homeWorkRowShowsLegacyDurationLabel() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(WorkPersistence(date), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }

        composeRule.onNodeWithTag("work-entry-duration-1", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun spreadsheetDialogForwardsSelectedYearMonthAndProjectSubset() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(WorkPersistence(date), IdleStopwatch(), date)
        var exported: Triple<Int, Int, Set<Int>?>? = null
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(
                    state = state,
                    viewModel = viewModel,
                    homeReselectionCount = 0,
                    onMonthView = {},
                    onExportSpreadsheet = { year, month, projects ->
                        exported = Triple(year, month, projects)
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Eksporter Excel").performClick()
        composeRule.onNodeWithTag("month-export-header-icon").assertIsDisplayed()
        val expectedYear = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).year - 1
        composeRule.onNodeWithTag("month-export-previous-year").performClick()
        composeRule.onNodeWithTag("month-export-option-0").performClick()
        composeRule.onNodeWithTag("month-export-project-1").performClick()
        composeRule.onNodeWithText("Eksporter").performClick()

        assertEquals(Triple(expectedYear, 1, setOf(1)), exported)
        composeRule.onNodeWithTag("month-export-year").assertDoesNotExist()
    }

    @Test
    fun failedDeleteClosesEditorAndShowsStorageError() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(WorkPersistence(date, failDelete = true), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }
        composeRule.onNodeWithTag("work-entry-1").performClick()
        composeRule.onNodeWithTag("delete-work").performScrollTo().performClick()

        composeRule.waitUntil(5_000) { viewModel.state.value.error != null }
        composeRule.onNodeWithTag("delete-work").assertDoesNotExist()
        composeRule.onNodeWithText("Kunne ikke lagre endringene").assertIsDisplayed()
    }

    @Test
    fun existingWorkChangesCanBeSaved() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(WorkPersistence(date), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }
        composeRule.onNodeWithTag("work-entry-1").performClick()
        composeRule.onNodeWithTag("work-description").performScrollTo().performTextReplacement("Updated")
        composeRule.onNodeWithTag("save-work").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { viewModel.state.value.works.single().description == "Updated" }
        composeRule.onNodeWithTag("save-work").assertDoesNotExist()
    }

    @Test
    fun singleDatePickerAppliesSelectionBeforeDismissal() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(WorkPersistence(date), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }
        composeRule.onNodeWithTag("work-entry-1").performClick()
        composeRule.onNodeWithTag("work-date-picker-field").performScrollTo().performClick()
        composeRule.onNodeWithText("OK").assertIsDisplayed()
        composeRule.onNodeWithText("Sunday, November 15, 2026", useUnmergedTree = true)
            .performClick()
            .assertIsSelected()

        composeRule.waitUntil(5_000) { viewModel.state.value.workDate == "2026-11-15" }
        composeRule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun timePickersOpenAtMidnightAndApplyOnlyAfterConfirmation() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(WorkPersistence(date), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }
        composeRule.onNodeWithTag("work-entry-1").performClick()

        composeRule.onNodeWithTag("work-duration-picker-field").performScrollTo().performClick()
        assertEquals("1", viewModel.state.value.workHours)
        composeRule.onNodeWithText("Ferdig").performClick()
        composeRule.waitUntil(5_000) {
            viewModel.state.value.workHours == "0" && viewModel.state.value.workMinutes == "0"
        }

        composeRule.onNodeWithTag("work-start-time-picker-field").performScrollTo().performClick()
        assertEquals("07:00", viewModel.state.value.workStartTime)
        composeRule.onNodeWithText("Ferdig").performClick()
        composeRule.waitUntil(5_000) { viewModel.state.value.workStartTime == "00:00" }
    }

    @Test
    fun existingWorkKeepsLegacyPeriodRegistrationOption() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(WorkPersistence(date), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }
        composeRule.onNodeWithTag("work-entry-1").performClick()

        composeRule.onNodeWithText("Perioderegistrering").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Velg start- og sluttdato").assertIsDisplayed()
        composeRule.onNodeWithText("Periode").assertIsDisplayed()
        composeRule.onNodeWithText("Perioderegistrering").assertIsDisplayed()
    }

    @Test
    fun periodDatePickerUsesLegacyRangeTitle() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(WorkPersistence(date), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }
        composeRule.onNodeWithTag("work-entry-1").performClick()
        composeRule.onNodeWithText("Perioderegistrering").performScrollTo().performClick()
        composeRule.onNodeWithText("Velg start- og sluttdato").performClick()

        composeRule.onNodeWithText("Velg datointervall").assertIsDisplayed()
        composeRule.onNodeWithText("Avbryt").assertIsDisplayed()
        composeRule.onNodeWithText("Ferdig").assertIsDisplayed()
    }
}

private class WorkPersistence(
    date: LocalDate,
    private val failDelete: Boolean = false,
    private val projects: List<StoredProject> = listOf(StoredProject(description = "", name = "Test", id = 1)),
) : TimnorPersistence {
    var sharedDeletionConfirmed = false
    private val project = projects.first()
    private val works = MutableStateFlow(
        listOf(
            StoredWork(
                description = "Entry",
                dateEpochSeconds = utcEpochSecondsFromLocalDateTime(LocalDateTime(date, LocalTime(7, 0))),
                project = project.id,
                task = 0,
                time = 3_600,
                id = 1,
            ),
        ),
    )
    override fun observeProjects(): Flow<List<StoredProject>> = flowOf(projects)
    override fun observeWorks(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>): Flow<List<StoredWork>> = works
    override suspend fun snapshotAll() = TimnorDataSnapshot(projects, works.value)
    override suspend fun hoursForRange(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>) = 0
    override suspend fun upsertProject(project: StoredProject) = Unit
    override suspend fun deleteProject(project: StoredProject) = Unit
    override suspend fun upsertWork(work: StoredWork) { works.value = works.value.map { if (it.id == work.id) work else it } }
    override suspend fun upsertWorks(works: List<StoredWork>) = Unit
    override suspend fun deleteWork(work: StoredWork) {
        if (failDelete) error("Delete failed")
        works.value = works.value.filterNot { it.id == work.id }
    }
    override suspend fun deleteWork(work: StoredWork, sharedDeletionConfirmed: Boolean) {
        this.sharedDeletionConfirmed = sharedDeletionConfirmed
        deleteWork(work)
    }
    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) = Unit
}
