package no.timnor.app.features.root

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorDataSnapshot
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.features.month_view.presentation.TimnorMonthViewScreen
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.ui.theme.TimnorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SharedMonthViewTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun firstDayRendersAndAnotherDayCanBeSelected() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 1))
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorMonthViewScreen(state = state, viewModel = viewModel, onBack = {})
            }
        }

        composeRule.onNodeWithTag("month-day-1").assertExists()
        composeRule.onNodeWithTag("month-day-1").assertIsSelected()
        composeRule.onNodeWithTag("month-day-2").performClick()
        composeRule.onNodeWithTag("month-day-2").assertIsSelected()
        composeRule.runOnIdle { assertEquals(LocalDate(2026, 11, 2), viewModel.state.value.selectedCalendarDate) }
    }

    @Test
    fun monthWeekdayLabelsMatchLegacyDayOfWeekAbbreviations() {
        val selectedDate = LocalDate(2026, 11, 1)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), selectedDate)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorMonthViewScreen(state = state, viewModel = viewModel, onBack = {})
            }
        }

        composeRule.onNodeWithText("MON").assertIsDisplayed()
        composeRule.onNodeWithText("Man").assertDoesNotExist()
    }

    @Test
    fun monthDayCellKeepsLegacyReadableMinimumWidth() {
        val selectedDate = LocalDate(2026, 11, 1)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), selectedDate)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorMonthViewScreen(state = state, viewModel = viewModel, onBack = {})
            }
        }

        val cellWidthDp = with(composeRule.density) {
            composeRule.onNodeWithTag("month-day-2").fetchSemanticsNode().boundsInRoot.width.toDp().value
        }
        assertTrue("Month View day cell should preserve the legacy readable width", cellWidthDp >= 56f)
    }

    @Test
    fun newMonthViewEntryScrollsToExternallySelectedDay() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 5))
        var scrollKey by mutableIntStateOf(0)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorMonthViewScreen(
                    state = state,
                    viewModel = viewModel,
                    scrollToSelectedDateKey = scrollKey,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("month-day-20").assertDoesNotExist()
        composeRule.runOnIdle {
            viewModel.selectCalendarDate(LocalDate(2026, 11, 20))
            scrollKey++
        }
        composeRule.onNodeWithTag("month-day-20").assertIsDisplayed()
    }

    @Test
    fun leapDayIsVisibleWhenOpeningLeapMonth() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2024, 2, 29))
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorMonthViewScreen(state = state, viewModel = viewModel, onBack = {})
            }
        }

        composeRule.onNodeWithTag("month-day-29").assertExists()
        composeRule.onNodeWithTag("month-day-28").performClick()
        composeRule.runOnIdle { assertEquals(LocalDate(2024, 2, 28), viewModel.state.value.selectedCalendarDate) }
    }

    @Test
    fun populatedDayDoesNotShowHeaderAddActionLikeLegacyMonthView() {
        val selectedDate = LocalDate(2026, 11, 1)
        val viewModel = TimnorHomeViewModel(
            SeededMonthViewPersistence(selectedDate),
            IdleStopwatch(),
            selectedDate,
        )
        val addDescription = composeRule.activity.getString(no.timnor.app.R.string.add_hours_btn)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorMonthViewScreen(state = state, viewModel = viewModel, onBack = {})
            }
        }

        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }
        composeRule.onNodeWithTag("work-entry-101").assertExists()
        composeRule.onNodeWithContentDescription(addDescription).assertDoesNotExist()
    }

    @Test
    fun monthViewWorkEntryOmitsLegacyHiddenDragHandle() {
        val selectedDate = LocalDate(2026, 11, 1)
        val viewModel = TimnorHomeViewModel(
            SeededMonthViewPersistence(selectedDate, hasWork = false),
            IdleStopwatch(),
            selectedDate,
        )
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorMonthViewScreen(state = state, viewModel = viewModel, onBack = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.projects.isNotEmpty() }
        composeRule.onNodeWithText("Legg til timer").performClick()

        composeRule.onNodeWithText("Registrer timer").assertIsDisplayed()
        composeRule.onNodeWithTag("work-entry-sheet-drag-handle").assertDoesNotExist()
    }

    @Test
    fun monthViewEmptyStateOpensEditorWithoutProjectsLikeLegacy() {
        val selectedDate = LocalDate(2026, 11, 1)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), selectedDate)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorMonthViewScreen(state = state, viewModel = viewModel, onBack = {})
            }
        }

        composeRule.onNodeWithText("Legg til timer").performClick()

        composeRule.onNodeWithText("Registrer timer").assertIsDisplayed()
    }
}

private class SeededMonthViewPersistence(selectedDate: LocalDate, hasWork: Boolean = true) : TimnorPersistence {
    private val project = StoredProject(description = "", name = "Project", id = 7)
    private val work = StoredWork(
        description = "Entry",
        dateEpochSeconds = selectedDate.toEpochDays() * 86_400L + 9 * 3_600L,
        project = project.id,
        task = 0,
        time = 3_600,
        id = 101,
    )
    private val works = if (hasWork) listOf(work) else emptyList()

    override fun observeProjects(): Flow<List<StoredProject>> = flowOf(listOf(project))
    override fun observeWorks(
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        projectIds: List<Int>,
    ): Flow<List<StoredWork>> = flowOf(works)

    override suspend fun snapshotAll() = TimnorDataSnapshot(listOf(project), works)
    override suspend fun hoursForRange(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>) =
        if (works.isEmpty()) 0 else 1
    override suspend fun upsertProject(project: StoredProject) = Unit
    override suspend fun deleteProject(project: StoredProject) = Unit
    override suspend fun upsertWork(work: StoredWork) = Unit
    override suspend fun upsertWorks(works: List<StoredWork>) = Unit
    override suspend fun deleteWork(work: StoredWork) = Unit
    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) = Unit
}
