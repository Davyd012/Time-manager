package no.timnor.app.features.root

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
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
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.home.TimnorHomeScreen
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform
import no.timnor.app.shared.platform.stopwatch.StopwatchSnapshot
import no.timnor.app.shared.ui.theme.TimnorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

class SharedCalendarGestureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun draggingEmptyHoursOpensCalendarAndDraggingMonthUpClosesIt() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        var expanded by mutableStateOf(false)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(
                    state = state,
                    viewModel = viewModel,
                    homeReselectionCount = 0,
                    onMonthView = {},
                    onCalendarExpansionChange = { expanded = it },
                )
            }
        }

        composeRule.onNodeWithTag("home-empty-work-drag-surface").performTouchInput {
            swipe(Offset(width * 0.1f, height * 0.1f), Offset(width * 0.1f, height * 0.8f), 350)
        }
        composeRule.waitUntil(5_000) { expanded }
        composeRule.onNodeWithTag("calendar-month-content").performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.9f), Offset(width * 0.5f, height * 0.1f), 150)
        }
        composeRule.waitUntil(5_000) { !expanded }
        composeRule.onNodeWithTag("home-empty-work-drag-surface").assertExists()
    }

    @Test
    fun collapsedHomeHeaderIncludesLegacyVerticalInsets() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }

        composeRule.onNodeWithTag("home-header-bar").assertHeightIsEqualTo(68.dp)
    }

    @Test
    fun homeWorkRowsMatchLegacyCardAndProjectMarkerWidths() {
        val date = LocalDate(2026, 11, 10)
        val works = listOf(
            StoredWork(
                description = "Design work",
                dateEpochSeconds = utcEpochSecondsFromLocalDateTime(LocalDateTime(date, LocalTime(9, 0))),
                project = 1,
                task = 0,
                time = 3_600,
                id = 1,
            ),
        )
        val viewModel = TimnorHomeViewModel(ProjectPersistence(works = works), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }

        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }
        composeRule.onNodeWithTag("work-entry-1").assertHeightIsEqualTo(60.dp)
        composeRule.onNodeWithTag("work-entry-project-marker-1").assertWidthIsEqualTo(10.dp)
    }

    @Test
    fun pullHandleOpensCalendarAndHorizontalSwipesChangeMonth() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(
                    state = state,
                    viewModel = viewModel,
                    homeReselectionCount = 0,
                    onMonthView = {},
                )
            }
        }

        composeRule.onNodeWithTag("home-pull-handle").performTouchInput {
            down(center)
            advanceEventTime(100)
            moveBy(Offset(0f, 900f))
        }
        composeRule.onNodeWithTag("calendar-expanded-toolbar").assertExists()
        composeRule.onNodeWithTag("calendar-expanded-toolbar").performTouchInput { up() }
        composeRule.onNodeWithTag("calendar-month-content").assertExists()
        composeRule.onNodeWithTag("calendar-month-content").performTouchInput {
            swipe(Offset(width * 0.8f, height * 0.5f), Offset(width * 0.2f, height * 0.5f))
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.calendarMonth == LocalDate(2026, 12, 1) }
        composeRule.onNodeWithTag("calendar-month-content").performTouchInput {
            swipe(Offset(width * 0.2f, height * 0.5f), Offset(width * 0.8f, height * 0.5f))
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.calendarMonth == LocalDate(2026, 11, 1) }
    }

    @Test
    fun draggingWeekAndToolbarExpandsAndCollapsesCalendar() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }

        composeRule.onNodeWithTag("home-week-strip").performTouchInput {
            val start = center
            swipe(start, Offset(start.x, start.y + 450f), durationMillis = 350)
        }
        composeRule.onNodeWithTag("calendar-expanded-toolbar").assertExists()
        composeRule.onNodeWithTag("calendar-expanded-toolbar").performTouchInput {
            val start = Offset(center.x, height * 0.9f)
            swipe(start, Offset(start.x, start.y - 150f), durationMillis = 100)
        }
        composeRule.onNodeWithTag("home-pull-handle").assertExists()
    }

    @Test
    fun backEligibilityBeginsBeforeCalendarHeaderSwitchesAtMidpoint() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        var backEnabled by mutableStateOf(false)
        var calendarExpanded by mutableStateOf(false)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(
                    state = state,
                    viewModel = viewModel,
                    homeReselectionCount = 0,
                    onMonthView = {},
                    onCalendarExpansionChange = { calendarExpanded = it },
                    onCalendarBackEnabledChange = { backEnabled = it },
                )
            }
        }

        composeRule.onNodeWithTag("home-week-strip").performTouchInput {
            down(center)
            advanceEventTime(100)
            moveBy(Offset(0f, 40f))
        }
        composeRule.waitUntil(5_000) { backEnabled && !calendarExpanded }
        composeRule.runOnIdle {
            assertTrue(backEnabled)
            assertFalse(calendarExpanded)
        }
        composeRule.onNodeWithTag("home-week-strip").performTouchInput { up() }
    }

    @Test
    fun selectingMonthGridDayOnlyChangesGridHighlightLikeLegacy() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        var monthViewOpened = false
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(
                    state = state,
                    viewModel = viewModel,
                    homeReselectionCount = 0,
                    onMonthView = { monthViewOpened = true },
                )
            }
        }

        composeRule.onNodeWithTag("home-pull-handle").performClick()
        composeRule.onNodeWithTag("calendar-month-day-12").performClick()

        composeRule.onNodeWithTag("calendar-month-day-12").assertIsSelected()
        composeRule.runOnIdle {
            assertEquals(LocalDate(2026, 11, 10), viewModel.state.value.selectedCalendarDate)
            assertFalse(monthViewOpened)
        }
    }

    @Test
    fun regularWeekStripShowsZeroHoursForInMonthDay() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }

        val dayCell = composeRule.onNodeWithTag("calendar-week-day-2026-11-9")
        val cellWidthDp = with(composeRule.density) {
            dayCell.fetchSemanticsNode().boundsInRoot.width.toDp().value
        }
        if (cellWidthDp >= 48f) {
            dayCell.assertTextContains("0")
        }
    }

    @Test
    fun homeHeaderUsesHostLocaleWeekdayLikeLegacyHeader() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        val locale = composeRule.activity.resources.configuration.locales[0] ?: Locale.getDefault()
        val expected = DayOfWeek.of(date.dayOfWeek.ordinal + 1)
            .getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { character ->
                if (character.isLowerCase()) character.titlecase(locale) else character.toString()
            }

        composeRule.onNodeWithTag("home-header").assertTextContains(expected)
        composeRule.onNodeWithTag("home-week-strip").assertTextContains("on")
    }

    @Test
    fun homeWeekdayLabelsMatchLegacyMonthAndAdjacentMonthRules() {
        val date = LocalDate(2026, 11, 1)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }

        composeRule.onNodeWithTag("calendar-week-day-2026-11-1").assertTextContains("SUN")
        composeRule.onNodeWithTag("calendar-week-day-2026-10-26").assertTextContains("Ma")
    }

    @Test
    fun deselectingAllProjectsAndApplyingRestoresLegacyAllProjects() {
        val viewModel = TimnorHomeViewModel(ProjectPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.projects.isNotEmpty() }
        composeRule.onNodeWithTag("home-pull-handle").performClick()
        composeRule.onNodeWithTag("calendar-filter").performClick()
        composeRule.onNodeWithTag("project-filter-select-all").performClick()
        composeRule.onNodeWithTag("project-filter-back").performClick()
        composeRule.runOnIdle { assertEquals(null, viewModel.state.value.selectedCalendarProjectIds) }
    }
}

internal class EmptyPersistence : TimnorPersistence {
    override fun observeProjects(): Flow<List<StoredProject>> = flowOf(emptyList())
    override fun observeWorks(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>): Flow<List<StoredWork>> =
        flowOf(emptyList())
    override suspend fun snapshotAll() = TimnorDataSnapshot(emptyList(), emptyList())
    override suspend fun hoursForRange(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>) = 0
    override suspend fun upsertProject(project: StoredProject) = Unit
    override suspend fun deleteProject(project: StoredProject) = Unit
    override suspend fun upsertWork(work: StoredWork) = Unit
    override suspend fun upsertWorks(works: List<StoredWork>) = Unit
    override suspend fun deleteWork(work: StoredWork) = Unit
    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) = Unit
}

internal class IdleStopwatch : StopwatchPlatform {
    override val state = MutableStateFlow(StopwatchSnapshot())
    override fun start() = Unit
    override fun pause() = Unit
    override fun cancel() = Unit
    override fun timerSaved() = Unit
    override fun selectProject(projectId: Int) = Unit
}
