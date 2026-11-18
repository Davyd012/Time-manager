package no.timnor.app.features.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import android.graphics.Bitmap
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.semantics.SemanticsProperties
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
import no.timnor.app.R
import no.timnor.app.features.root.presentation.components.BottomBar
import no.timnor.app.features.root.presentation.components.BottomBarItem
import no.timnor.app.navigation.AppRoute
import no.timnor.app.ui.theme.homeIcon
import no.timnor.app.ui.theme.watchIcon
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlin.math.abs

class SharedCalendarGestureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun draggingFromHomeCenterOpensCalendar() {
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
        composeRule.onNodeWithTag("home-calendar-gesture-surface").performTouchInput {
            val start = center
            swipe(start, Offset(start.x, height * 0.95f), durationMillis = 200)
        }
        composeRule.waitUntil(5_000) { expanded }
    }

    @Test
    fun expandedCalendarScrollModePreservesOpenAnchorWhileScrolling() {
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
        composeRule.onNodeWithTag("home-pull-handle").performClick()
        composeRule.waitUntil(5_000) { expanded }
        composeRule.onNodeWithTag("calendar-expanded-toolbar").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar-scroll-mode").performClick()
        composeRule.onNodeWithTag("calendar-month-content").performTouchInput {
            swipe(Offset(center.x, height * 0.85f), Offset(center.x, height * 0.15f), durationMillis = 300)
        }
        composeRule.runOnIdle { assertTrue(expanded) }
    }

    @Test
    fun shortExpandedCalendarScrollsGridWhileToolbarAndMonthSelectorStayFixed() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                Box(Modifier.width(360.dp).height(480.dp)) {
                    TimnorHomeScreen(
                        state = state,
                        viewModel = viewModel,
                        homeReselectionCount = 0,
                        onMonthView = {},
                        testExpansionProgress = 1f,
                    )
                }
            }
        }
        val toolbar = composeRule.onNodeWithTag("calendar-expanded-toolbar")
        val selector = composeRule.onNodeWithTag("calendar-month-selector")
        val date = composeRule.onNodeWithTag("calendar-month-day-2026-11-20")
        val toolbarBefore = toolbar.fetchSemanticsNode().boundsInRoot
        val selectorBefore = selector.fetchSemanticsNode().boundsInRoot
        val dateTopBefore = date.fetchSemanticsNode().boundsInRoot.top

        composeRule.onNodeWithTag("calendar-scroll-content").performTouchInput {
            swipe(Offset(center.x, height * 0.8f), Offset(center.x, height * 0.3f), durationMillis = 300)
        }

        assertEquals(toolbarBefore, toolbar.fetchSemanticsNode().boundsInRoot)
        assertEquals(selectorBefore, selector.fetchSemanticsNode().boundsInRoot)
        assertTrue(date.fetchSemanticsNode().boundsInRoot.top < dateTopBefore)
    }

    @Test
    fun draggingEmptyHoursDownOpensCalendarAndToolbarDragClosesIt() {
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
        composeRule.onNodeWithTag("calendar-expanded-toolbar").performTouchInput {
            swipe(center, Offset(center.x, center.y - height * 3f), 150)
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
        composeRule.onNodeWithTag("work-entry-project-marker-1", useUnmergedTree = true).assertWidthIsEqualTo(10.dp)
    }

    @Test
    fun emptyStateIllustrationTitleAndAddButtonStartCalendarDragWithoutActivatingButton() {
        val viewModel = TimnorHomeViewModel(ProjectPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
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

        listOf(
            "home-empty-state-illustration",
            "home-empty-state-title",
            "home-add-hours-action",
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).performTouchInput {
                val start = center
                val travel = with(composeRule.density) { 320.dp.toPx() }
                swipe(start, Offset(start.x, start.y + travel), durationMillis = 70)
            }
            composeRule.waitUntil(5_000) { expanded }
            composeRule.onNodeWithText("Registrer timer").assertDoesNotExist()
            composeRule.onNodeWithTag("calendar-expanded-toolbar").performTouchInput {
                val start = center
                val travel = with(composeRule.density) { 320.dp.toPx() }
                swipe(start, Offset(start.x, start.y - travel), durationMillis = 70)
            }
            composeRule.waitUntil(5_000) { !expanded }
        }
    }

    @Test
    fun workEntryCardStartsCalendarDragAndDoesNotOpenEditor() {
        val date = LocalDate(2026, 11, 10)
        val work = StoredWork(
            description = "Design work",
            dateEpochSeconds = utcEpochSecondsFromLocalDateTime(LocalDateTime(date, LocalTime(9, 0))),
            project = 1,
            task = 0,
            time = 3_600,
            id = 1,
        )
        val viewModel = TimnorHomeViewModel(ProjectPersistence(works = listOf(work)), IdleStopwatch(), date)
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
        composeRule.waitUntil(5_000) { viewModel.state.value.works.isNotEmpty() }

        composeRule.onNodeWithTag("work-entry-1").performTouchInput {
            val start = center
            val travel = with(composeRule.density) { 320.dp.toPx() }
            swipe(start, Offset(start.x, start.y + travel), durationMillis = 70)
        }
        composeRule.waitUntil(5_000) { expanded }
        composeRule.onNodeWithText("Registrer timer").assertDoesNotExist()
    }

    @Test
    fun draggingUpFromMonthDateCollapsesWithoutSelectingThatDate() {
        val selectedDate = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), selectedDate)
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

        composeRule.onNodeWithTag("home-pull-handle").performClick()
        composeRule.waitUntil(5_000) { expanded }
        val calendarDate = composeRule.onNodeWithTag("calendar-month-day-2026-11-12")
        calendarDate.assertIsDisplayed()
        calendarDate.performTouchInput {
            val start = center
            val travel = with(composeRule.density) { 320.dp.toPx() }
            swipe(start, Offset(start.x, start.y - travel), durationMillis = 70)
        }
        composeRule.waitUntil(5_000) { !expanded }
        composeRule.runOnIdle {
            assertEquals(selectedDate, viewModel.state.value.selectedCalendarDate)
        }
    }

    @Test
    fun draggingUpFromEmptyMonthGridCellCollapsesCalendar() {
        val selectedDate = LocalDate(2026, 11, 11)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), selectedDate)
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

        composeRule.onNodeWithTag("home-pull-handle").performClick()
        composeRule.waitUntil(5_000) { expanded }
        composeRule.onNodeWithTag("calendar-morph-grid").performTouchInput {
            val start = Offset(width * 0.05f, height * 0.15f)
            val travel = with(composeRule.density) { 320.dp.toPx() }
            swipe(start, Offset(start.x, start.y - travel), durationMillis = 70)
        }
        composeRule.waitUntil(5_000) { !expanded }
        composeRule.runOnIdle {
            assertEquals(selectedDate, viewModel.state.value.selectedCalendarDate)
        }
    }

    @Test
    fun partialWeekDragCanReverseDirectionBeforeRelease() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        var backEnabled by mutableStateOf(false)
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
                    onCalendarBackEnabledChange = { backEnabled = it },
                )
            }
        }

        val header = composeRule.onNodeWithTag("home-header-bar")
        val collapsedHeaderHeight = header.fetchSemanticsNode().boundsInRoot.height
        composeRule.onNodeWithTag("home-week-strip").performTouchInput {
            down(center)
            advanceEventTime(100)
            moveBy(Offset(0f, 100f))
        }
        composeRule.waitUntil(5_000) { backEnabled && !expanded }
        composeRule.waitForIdle()
        val expandedHeaderHeight = header.fetchSemanticsNode().boundsInRoot.height
        composeRule.onNodeWithTag("home-week-strip").performTouchInput {
            advanceEventTime(100)
            moveBy(Offset(0f, -60f))
        }
        composeRule.waitForIdle()
        val reversedHeaderHeight = header.fetchSemanticsNode().boundsInRoot.height

        assertTrue("Downward movement should reduce the Home toolbar height", expandedHeaderHeight < collapsedHeaderHeight)
        assertTrue("Reversing direction should restore some Home toolbar height", reversedHeaderHeight > expandedHeaderHeight)
        composeRule.onNodeWithTag("home-week-strip").performTouchInput { up() }
        composeRule.waitUntil(5_000) { !backEnabled && !expanded }
    }

    @Test
    fun calendarCanOpenAndCloseTwentyTimesWithoutLosingItsAnchor() {
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

        repeat(20) {
            composeRule.onNodeWithTag("home-pull-handle").performClick()
            composeRule.waitUntil(5_000) { expanded }
            composeRule.onNodeWithTag("calendar-close").performClick()
            composeRule.waitUntil(5_000) { !expanded }
        }
        composeRule.onNodeWithTag("home-pull-handle").assertIsDisplayed()
    }

    @Test
    fun capturesAllSevenMorphStagesAndKeepsForwardReverseDateGeometryIdentical() {
        val selectedDate = LocalDate(2026, 11, 11)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), selectedDate)
        var progress by mutableFloatStateOf(0f)
        val stages = listOf(0f, 0.15f, 0.30f, 0.50f, 0.75f, 0.90f, 1f)
        val selectedTag = "calendar-month-day-2026-11-11"
        val homeLabel = composeRule.activity.getString(R.string.home_tab)
        val stopwatchLabel = composeRule.activity.getString(R.string.stopwatch_tab)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = true) {
                Column(Modifier.fillMaxSize()) {
                    TimnorHomeScreen(
                        state = state,
                        viewModel = viewModel,
                        homeReselectionCount = 0,
                        onMonthView = {},
                        testExpansionProgress = progress,
                        modifier = Modifier.weight(1f),
                    )
                    BottomBar(
                        items = listOf(
                            BottomBarItem(AppRoute.Home, homeIcon(false), homeIcon(), homeLabel),
                            BottomBarItem(AppRoute.Stopwatch, watchIcon(false), watchIcon(), stopwatchLabel),
                        ),
                        isCurrent = { it == AppRoute.Home },
                        onSelect = {},
                    )
                }
            }
        }
        val contentResolver = composeRule.activity.contentResolver
        val forwardBounds = mutableMapOf<Float, androidx.compose.ui.geometry.Rect>()
        val bottomBarBounds = mutableMapOf<Float, androidx.compose.ui.geometry.Rect>()

        stages.forEach { stage ->
            composeRule.runOnIdle { progress = stage }
            composeRule.waitForIdle()
            repeat(2) { composeRule.mainClock.advanceTimeByFrame() }
            composeRule.waitForIdle()
            val dateNodes = composeRule.onAllNodesWithTag(selectedTag).fetchSemanticsNodes()
            assertEquals("Expected one semantic node for $selectedTag at p=$stage", 1, dateNodes.size)
            if (stage == 0.30f) {
                assertEquals("Only one weekday row should be visible during the morph", 1,
                    composeRule.onAllNodesWithText("Man").fetchSemanticsNodes().size)
                listOf("Man", "Tir", "Ons", "Tor", "Fre", "Lør", "Søn").forEach { weekdayLabel ->
                    assertEquals(
                        "Exactly one localized weekday label $weekdayLabel should be visible at p=.30",
                        1,
                        composeRule.onAllNodesWithText(weekdayLabel, useUnmergedTree = true).fetchSemanticsNodes().size,
                    )
                }
            }
            if (stage == 1f) {
                composeRule.onNodeWithText("Totalt for måneden: 0 t 0 m").assertIsDisplayed()
            }
            forwardBounds[stage] = dateNodes.single().boundsInRoot
            bottomBarBounds[stage] = composeRule.onNodeWithTag("app-bottom-navigation")
                .fetchSemanticsNode().boundsInRoot

            val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
            val stageLabel = (stage * 100).toInt().toString().padStart(3, '0')
            val imageUri = requireNotNull(
                contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "week-to-month-morph-$stageLabel.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/week-to-month-morph")
                    },
                ),
            ) { "Unable to create p=$stage screenshot" }
            contentResolver.openOutputStream(imageUri).use { output ->
                requireNotNull(output) { "Unable to open p=$stage screenshot" }
                assertTrue("Unable to save p=$stage capture", bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            bitmap.recycle()
        }

        stages.asReversed().forEach { stage ->
            composeRule.runOnIdle { progress = stage }
            composeRule.waitForIdle()
            val reverseBounds = composeRule.onNodeWithTag(selectedTag).fetchSemanticsNode().boundsInRoot
            val reverseBottomBarBounds = composeRule.onNodeWithTag("app-bottom-navigation")
                .fetchSemanticsNode().boundsInRoot
            val forward = requireNotNull(forwardBounds[stage])
            val forwardBottomBar = requireNotNull(bottomBarBounds[stage])
            assertEquals("Left edge differs in reverse path at p=$stage", forward.left, reverseBounds.left, 0.5f)
            assertEquals("Top edge differs in reverse path at p=$stage", forward.top, reverseBounds.top, 0.5f)
            assertEquals("Width differs in reverse path at p=$stage", forward.width, reverseBounds.width, 0.5f)
            assertEquals("Height differs in reverse path at p=$stage", forward.height, reverseBounds.height, 0.5f)
            assertEquals("Bottom bar moves in reverse path at p=$stage", forwardBottomBar, reverseBottomBarBounds)
        }
        bottomBarBounds.values.forEach { bounds ->
            assertEquals("Bottom bar left edge changes during morph", bottomBarBounds.values.first().left, bounds.left, 0.5f)
            assertEquals("Bottom bar top edge changes during morph", bottomBarBounds.values.first().top, bounds.top, 0.5f)
            assertEquals("Bottom bar width changes during morph", bottomBarBounds.values.first().width, bounds.width, 0.5f)
            assertEquals("Bottom bar height changes during morph", bottomBarBounds.values.first().height, bounds.height, 0.5f)
        }
        composeRule.runOnIdle { assertEquals(selectedDate, viewModel.state.value.selectedCalendarDate) }
    }

    @Test
    fun bottomNavigationSwipeDoesNotStartCalendarExpansion() {
        val selectedDate = LocalDate(2026, 11, 11)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), selectedDate)
        var expanded by mutableStateOf(false)
        val homeLabel = composeRule.activity.getString(R.string.home_tab)
        val stopwatchLabel = composeRule.activity.getString(R.string.stopwatch_tab)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                Column(Modifier.fillMaxSize()) {
                    TimnorHomeScreen(
                        state = state,
                        viewModel = viewModel,
                        homeReselectionCount = 0,
                        onMonthView = {},
                        onCalendarExpansionChange = { expanded = it },
                        modifier = Modifier.weight(1f),
                    )
                    BottomBar(
                        items = listOf(
                            BottomBarItem(AppRoute.Home, homeIcon(false), homeIcon(), homeLabel),
                            BottomBarItem(AppRoute.Stopwatch, watchIcon(false), watchIcon(), stopwatchLabel),
                        ),
                        isCurrent = { it == AppRoute.Home },
                        onSelect = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("app-bottom-navigation").performTouchInput {
            val start = center
            swipe(start, Offset(start.x, start.y - height * 2f), durationMillis = 300)
        }
        composeRule.runOnIdle {
            assertFalse(expanded)
            assertEquals(selectedDate, viewModel.state.value.selectedCalendarDate)
        }
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
    fun browsingSeveralMonthsThenCollapsingKeepsHomeDateAndBrowseMonthIndependent() {
        val selectedHomeDate = LocalDate(2026, 11, 11)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), selectedHomeDate)
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

        composeRule.onNodeWithTag("home-pull-handle").performClick()
        composeRule.waitUntil(5_000) { expanded }
        repeat(2) {
            val previousMonth = viewModel.state.value.calendarMonth
            composeRule.onNodeWithTag("calendar-month-content").performTouchInput {
                swipe(Offset(width * 0.8f, height * 0.5f), Offset(width * 0.2f, height * 0.5f))
            }
            composeRule.waitUntil(5_000) { viewModel.state.value.calendarMonth != previousMonth }
        }
        assertEquals(LocalDate(2027, 1, 1), viewModel.state.value.calendarMonth)
        assertFalse(
            composeRule.onNodeWithTag("calendar-month-day-2027-01-11")
                .fetchSemanticsNode().config[SemanticsProperties.Selected],
        )

        composeRule.onNodeWithTag("calendar-close").performClick()
        composeRule.waitUntil(5_000) { !expanded }
        composeRule.runOnIdle {
            assertEquals(selectedHomeDate, viewModel.state.value.selectedCalendarDate)
            assertEquals(LocalDate(2027, 1, 1), viewModel.state.value.calendarMonth)
        }

        composeRule.onNodeWithTag("home-pull-handle").performClick()
        composeRule.waitUntil(5_000) { expanded }
        composeRule.runOnIdle {
            assertEquals(selectedHomeDate, viewModel.state.value.selectedCalendarDate)
            assertEquals(LocalDate(2027, 1, 1), viewModel.state.value.calendarMonth)
        }
        assertFalse(
            composeRule.onNodeWithTag("calendar-month-day-2027-01-11")
                .fetchSemanticsNode().config[SemanticsProperties.Selected],
        )
    }

    @Test
    fun partialMonthBoundaryWeekKeepsAllSevenDateNodesAtExpandedAnchor() {
        val selectedDate = LocalDate(2026, 11, 30)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), selectedDate)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(
                    state = state,
                    viewModel = viewModel,
                    homeReselectionCount = 0,
                    onMonthView = {},
                    testExpansionProgress = 1f,
                )
            }
        }

        listOf(
            "calendar-month-day-2026-11-30",
            "calendar-date-2026-12-01",
            "calendar-date-2026-12-02",
            "calendar-date-2026-12-03",
            "calendar-date-2026-12-04",
            "calendar-date-2026-12-05",
            "calendar-date-2026-12-06",
        ).forEach { dateTag ->
            assertEquals("Expected one date-keyed node for $dateTag at p=1", 1,
                composeRule.onAllNodesWithTag(dateTag).fetchSemanticsNodes().size)
        }
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        listOf(
            "calendar-date-2026-12-01",
            "calendar-date-2026-12-02",
            "calendar-date-2026-12-03",
            "calendar-date-2026-12-04",
            "calendar-date-2026-12-05",
            "calendar-date-2026-12-06",
        ).forEach { dateTag ->
            val bounds = composeRule.onNodeWithTag(dateTag).fetchSemanticsNode().boundsInRoot
            val cellCenterX = bounds.center.x.toInt()
            val centerY = bounds.top.toInt() + with(composeRule.density) { 28.dp.roundToPx() }
            val backgroundX = cellCenterX + with(composeRule.density) { 18.dp.roundToPx() }
            val backgroundColor = bitmap.getPixel(backgroundX, centerY)
            val textTop = bounds.top.toInt() + with(composeRule.density) { 18.dp.roundToPx() }
            val textBottom = bounds.top.toInt() + with(composeRule.density) { 38.dp.roundToPx() }
            val textHalfWidth = with(composeRule.density) { 8.dp.roundToPx() }
            val visibleGlyphPixels = (textTop..textBottom).sumOf { y ->
                (cellCenterX - textHalfWidth..cellCenterX + textHalfWidth).count { x ->
                    bitmap.getPixel(x, y) != backgroundColor
                }
            }
            assertTrue("$dateTag should render its date text at p=1", visibleGlyphPixels > 0)
        }
        val screenshotUri = requireNotNull(
            composeRule.activity.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "week-to-month-morph-partial-boundary.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/week-to-month-morph")
                },
            ),
        )
        composeRule.activity.contentResolver.openOutputStream(screenshotUri).use { output ->
            requireNotNull(output)
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        bitmap.recycle()
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
    fun selectingMonthGridDayUpdatesSelectedDateAndHomeContent() {
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
        composeRule.onNodeWithTag("calendar-month-day-2026-11-12").performClick()

        composeRule.onNodeWithTag("calendar-month-day-2026-11-12").assertIsSelected()
        composeRule.runOnIdle {
            assertEquals(LocalDate(2026, 11, 12), viewModel.state.value.selectedCalendarDate)
            assertFalse(monthViewOpened)
        }
    }

    @Test
    fun swipingCompactCalendarChangesWeekAndSelectedDate() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }

        composeRule.onNodeWithTag("calendar-morph-grid").performTouchInput {
            swipe(start = center, end = Offset(center.x - width * 0.6f, center.y), durationMillis = 220)
        }
        composeRule.waitUntil(5_000) {
            viewModel.state.value.selectedCalendarDate == LocalDate(2026, 11, 17)
        }
        composeRule.onNodeWithTag("calendar-month-day-2026-11-17").assertIsSelected()
    }

    @Test
    fun compactWeekStripSpacesSevenDaysWithoutClippingOnNarrowScreens() {
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), LocalDate(2026, 11, 10))
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                Box(Modifier.width(320.dp).height(800.dp)) {
                    TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
                }
            }
        }

        val dates = (9..15).map { day ->
            val dayOfMonth = day.toString().padStart(2, '0')
            composeRule.onNodeWithTag("calendar-month-day-2026-11-$dayOfMonth").fetchSemanticsNode().boundsInRoot
        }
        val calendarBounds = composeRule.onNodeWithTag("calendar-month-content").fetchSemanticsNode().boundsInRoot
        val widthsDp = dates.map { with(composeRule.density) { it.width.toDp().value } }
        val gapsDp = dates.zipWithNext { first, second ->
            with(composeRule.density) { (second.left - first.right).toDp().value }
        }
        composeRule.runOnIdle {
            assertEquals("The week strip should show all seven dates", 7, dates.size)
            assertTrue("Day chips should have consistent widths", widthsDp.maxOrNull()!! - widthsDp.minOrNull()!! <= 1f)
            assertTrue("Adjacent day chips should have a 4dp gap", gapsDp.all { abs(it - 4f) <= 0.6f })
            assertTrue("The first day chip should stay inside the calendar padding", dates.first().left > calendarBounds.left)
            assertTrue("The last day chip should stay inside the calendar padding", dates.last().right < calendarBounds.right)
        }
        composeRule.onAllNodesWithText("0t").assertCountEquals(0)
    }

    @Test
    fun homeHeaderUsesNorwegianFullWeekday() {
        val date = LocalDate(2026, 11, 10)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), date)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
            }
        }
        composeRule.onNodeWithTag("home-header").assertTextContains("Tirsdag")
        composeRule.onNodeWithTag("calendar-month-day-2026-11-10").assertTextContains("Tir")
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

        composeRule.onNodeWithTag("calendar-month-day-2026-11-01").assertTextContains("Søn")
        composeRule.onNodeWithTag("calendar-date-2026-10-26").assertTextContains("Man")
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
