package no.timnor.app.features.root

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.features.root.domain.TimnorHomeUiState
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.home.TimnorHomeScreen
import no.timnor.app.shared.ui.theme.TimnorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SharedCalendarMonthSummaryTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sixWeekMonthShowsFilteredTotalsAndSharesAtLargeTextSize() {
        val month = LocalDate(2026, 11, 1)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), month)
        var filter by mutableStateOf<Set<Int>?>(setOf(1, 2))
        var shareCount = 0
        val state = TimnorHomeUiState(
            calendarMonth = month,
            selectedCalendarDate = LocalDate(2026, 11, 25),
            projects = listOf(
                StoredProject("", "Weekend project", 1, hourlyRateOre = 22_000),
                StoredProject("", "Weekday project", 2, hourlyRateOre = 18_000),
                StoredProject("", "Unrated project", 3),
            ),
            works = listOf(
                work(id = 1, project = 1, date = LocalDate(2026, 11, 7), seconds = 7 * 3600),
                work(id = 2, project = 1, date = LocalDate(2026, 11, 14), seconds = 6 * 3600),
                work(id = 3, project = 2, date = LocalDate(2026, 11, 13), seconds = 4 * 3600 + 10 * 60),
                work(id = 4, project = 3, date = LocalDate(2026, 11, 15), seconds = 3600),
                work(id = 5, project = 1, date = LocalDate(2026, 12, 1), seconds = 3600),
            ),
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                TimnorTheme(darkTheme = true) {
                    Box(Modifier.width(360.dp)) {
                        TimnorHomeScreen(
                            state = state.copy(selectedCalendarProjectIds = filter),
                            viewModel = viewModel,
                            homeReselectionCount = 0,
                            onMonthView = {},
                            onShareMonth = { _, _ -> shareCount++ },
                            testExpansionProgress = 1f,
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("17 t 10 min").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3\u00A0610 kr").assertIsDisplayed()
        composeRule.onNodeWithText("Beregnet betaling").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Weekend project").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Del måned").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, shareCount)
            filter = null
        }

        composeRule.onNodeWithText("18 t 10 min").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Delvis beregnet").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Tid uten timesats: 1 t").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { filter = setOf(3) }

        composeRule.onNodeWithText("1 t").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3\u00A0610 kr").assertDoesNotExist()
        composeRule.onNodeWithText("Delvis beregnet").assertDoesNotExist()
    }

    @Test
    fun projectFilterUsesMonthlyHistoricalPayAndAppliesOnlyOnConfirmation() {
        val november = LocalDate(2026, 11, 1)
        val december = LocalDate(2026, 12, 1)
        val longProjectName = "Bakeriet med et veldig langt prosjektnavn"
        val projects = listOf(
            StoredProject("", longProjectName, 1, hourlyRateOre = 1, weekendHourlyRateOre = 2),
            StoredProject("", "Verksted", 2, hourlyRateOre = 3),
            StoredProject("", "Uten sats", 3),
        )
        val works = listOf(
            work(1, 1, LocalDate(2026, 11, 7), 3_600).copy(appliedHourlyRateOre = 98_765_400),
            work(2, 1, LocalDate(2026, 11, 9), 3_600),
            work(3, 2, LocalDate(2026, 11, 10), 3_600),
            work(4, 3, LocalDate(2026, 11, 11), 3_600),
            work(5, 1, LocalDate(2026, 11, 12), 3_600).copy(appliedHourlyRateOre = null),
            work(6, 1, december, 7_200),
        )
        val persistence = object : TimnorPersistence by EmptyPersistence() {
            override fun observeProjects() = flowOf(projects)
            override fun observeWorks(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>) =
                flowOf(works)
        }
        val viewModel = TimnorHomeViewModel(persistence, IdleStopwatch(), november)
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = true) {
                TimnorHomeScreen(
                    state = state,
                    viewModel = viewModel,
                    homeReselectionCount = 0,
                    onMonthView = {},
                    testExpansionProgress = 1f,
                )
            }
        }
        val inProjectList = hasAnyAncestor(hasTestTag("project-filter-list"))
        val filterButton = composeRule.onNodeWithTag("calendar-filter")
        val applyButton = composeRule.onNodeWithText("Bruk filter")
        val partialPay = composeRule.onNode(hasText("Delvis: 987\u00A0874 kr") and inProjectList)
        composeRule.waitUntil(5_000) {
            viewModel.state.value.projects.size == projects.size && viewModel.state.value.works.size == works.size
        }
        composeRule.runOnIdle { viewModel.setCalendarProjectSelection(setOf(2)) }

        filterButton.performClick()
        composeRule.onNode(hasTestTag("project-filter-select-all") and hasText("November 2026")).assertIsDisplayed()
        partialPay.assertIsDisplayed()
        composeRule.onNode(hasText("Uten sats") and hasText("1 t") and inProjectList).assertIsDisplayed()
        composeRule.onNodeWithText(longProjectName).performClick()
        composeRule.onNodeWithText("2 av 3 valgt").assertIsDisplayed()
        partialPay.assertIsDisplayed()
        composeRule.onNodeWithTag("project-filter-cancel").performClick()
        composeRule.runOnIdle { assertEquals(setOf(2), viewModel.state.value.selectedCalendarProjectIds) }

        filterButton.performClick()
        composeRule.onNodeWithTag("project-filter-search").performTextInput(" bakeriet ")
        composeRule.onNode(hasText("Verksted") and inProjectList).assertDoesNotExist()
        composeRule.onNodeWithTag("project-filter-select-all").performClick()
        composeRule.onNodeWithText("2 av 3 valgt").assertIsDisplayed()
        applyButton.performClick()
        composeRule.runOnIdle { assertEquals(setOf(1, 2), viewModel.state.value.selectedCalendarProjectIds) }
        composeRule.onNodeWithText("988\u00A0054 kr").performScrollTo().assertIsDisplayed()

        composeRule.runOnIdle { viewModel.updateCalendarMonth(december) }
        filterButton.performClick()
        composeRule.onNode(hasTestTag("project-filter-select-all") and hasText("Desember 2026")).assertIsDisplayed()
        partialPay.assertDoesNotExist()
        composeRule.onNode(hasText("440 kr") and inProjectList).assertIsDisplayed()
        composeRule.onNodeWithText("Nullstill").performClick()
        composeRule.onNodeWithText("3 av 3 valgt").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(setOf(1, 2), viewModel.state.value.selectedCalendarProjectIds) }
        applyButton.performClick()
        composeRule.runOnIdle { assertEquals(null, viewModel.state.value.selectedCalendarProjectIds) }
    }

    @Test
    fun configuredRateShowsPaymentSummaryForOlderEntriesWithoutEstimatingThem() {
        val day = LocalDate(2026, 11, 13)
        val month = LocalDate(2026, 11, 1)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), month)
        val state = TimnorHomeUiState(
            calendarMonth = month,
            selectedCalendarDate = day,
            projects = listOf(StoredProject("", "Bakeri", 2, hourlyRateOre = 18_000)),
            works = listOf(work(1, 2, day, 15_000).copy(appliedHourlyRateOre = null)),
        )
        composeRule.setContent {
            TimnorTheme(darkTheme = true) {
                TimnorHomeScreen(
                    state = state,
                    viewModel = viewModel,
                    homeReselectionCount = 0,
                    onMonthView = {},
                    testExpansionProgress = 1f,
                )
            }
        }

        composeRule.onNodeWithText("Beregnet betaling").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ingen registreringer med lagret sats").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("750 kr").assertDoesNotExist()
    }

    @Test
    fun darkHomeShowsHistoricalPayAtLargeTextSizeOnANarrowScreen() {
        val day = LocalDate(2027, 1, 1)
        val viewModel = TimnorHomeViewModel(EmptyPersistence(), IdleStopwatch(), day)
        val state = TimnorHomeUiState(
            calendarMonth = day,
            selectedCalendarDate = day,
            projects = listOf(StoredProject("", "Bakeriet med et veldig langt prosjektnavn", 2, hourlyRateOre = 99_000)),
            works = listOf(
                work(1, 2, day, 15_000),
                work(2, 2, day, 3600).copy(appliedHourlyRateOre = 0),
                work(3, 2, day, 3600).copy(appliedHourlyRateOre = null),
            ),
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                TimnorTheme(darkTheme = true) {
                    Box(Modifier.width(320.dp)) {
                        TimnorHomeScreen(state = state, viewModel = viewModel, homeReselectionCount = 0, onMonthView = {})
                    }
                }
            }
        }

        composeRule.onNodeWithText("Uke 53", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("4 t 10 min", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("08:00–12:10", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("750 kr", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("0 kr", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("work-entry-3").assertIsDisplayed()
        composeRule.onNodeWithText("990 kr", useUnmergedTree = true).assertDoesNotExist()
    }

    private fun work(id: Int, project: Int, date: LocalDate, seconds: Int) = StoredWork(
        description = "",
        dateEpochSeconds = date.toEpochDays() * 86_400L + 8 * 3_600L,
        project = project,
        task = 0,
        time = seconds,
        id = id,
        appliedHourlyRateOre = when (project) {
            1 -> 22_000
            2 -> 18_000
            else -> null
        },
    )
}
