package no.timnor.app.features.root

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
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

    private fun work(id: Int, project: Int, date: LocalDate, seconds: Int) = StoredWork(
        description = "",
        dateEpochSeconds = date.toEpochDays() * 86_400L,
        project = project,
        task = 0,
        time = seconds,
        id = id,
    )
}
