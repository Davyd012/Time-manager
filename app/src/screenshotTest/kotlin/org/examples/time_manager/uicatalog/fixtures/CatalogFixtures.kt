package org.examples.time_manager.uicatalog.fixtures

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.dates.models.DayModel
import org.examples.time_manager.core.dates.models.Today
import org.examples.time_manager.features.month_view.data.MonthViewUiState
import org.examples.time_manager.features.root.data.HomeUiState
import org.examples.time_manager.features.root.data.StopwatchUiState
import org.examples.time_manager.features.root.presentation.home.CalendarExpansion
import org.examples.time_manager.navigation.Navigator
import org.examples.time_manager.navigation.rememberNavigationState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

object CatalogFixtures {
    val date: LocalDate = LocalDate.of(2026, 8, 5)
    val dateTime: LocalDateTime = date.atTime(9, 15)
    val month: YearMonth = YearMonth.of(2026, 8)
    val locale: Locale = Locale.Builder().setLanguage("nb").setRegion("NO").build()

    val projects = listOf(
        Project(id = 1, name = "Nordlys redesign", description = "Produktdesign og utvikling"),
        Project(id = 2, name = "Kundemøter", description = "Planlegging og oppfølging"),
        Project(id = 3, name = "Dokumentasjon", description = "Teknisk dokumentasjon"),
    )

    val works = listOf(
        Work(
            id = 1,
            project = 1,
            date = date.atTime(9, 15),
            time = 2 * 3600 + 15 * 60,
            description = "Skisserte den nye oversikten",
        ),
        Work(
            id = 2,
            project = 2,
            date = date.atTime(13, 0),
            time = 75 * 60,
            description = "Statusmøte med teamet",
        ),
    )

    val days: List<DayModel> = (1..month.lengthOfMonth()).map { day ->
        val currentDate = month.atDay(day)
        DayModel(
            day = currentDate.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
            date = currentDate,
            time = works.filter { it.date.toLocalDate() == currentDate }.sumOf(Work::time),
        )
    }

    val home = HomeUiState(
        isLoading = false,
        counting = false,
        elapsedSeconds = 0,
        dayPerMonth = days,
        selectedDay = date.dayOfMonth,
        selectedProject = projects.first().id,
        projects = projects,
        workQueries = works,
        today = Today("Onsdag", date.dayOfMonth, "august", date.year),
        calendarMonth = month,
        calendarDays = days,
        calendarTotalHours = 3.5,
        calendarProjectHours = mapOf(1 to 2.25, 2 to 1.25, 3 to 0.0),
    )

    val stopwatch = StopwatchUiState(
        isLoading = false,
        elapsedSeconds = 2 * 3600 + 17 * 60,
        isRunning = false,
        selectedProject = projects.first().id,
        projects = projects,
    )

    val stopwatchRunning = stopwatch.copy(isRunning = true)

    val monthView = MonthViewUiState(
        isLoading = false,
        month = "august",
        dayPerMonth = days,
        selectedDay = date.dayOfMonth,
        selectedProject = projects.first().id,
        projects = projects,
        workQueries = works,
        today = Today("Onsdag", date.dayOfMonth, "august", date.year),
    )

    val monthViewEmpty = monthView.copy(workQueries = emptyList())
}

@Composable
fun rememberCatalogNavigator(): Navigator {
    val navigationState = rememberNavigationState()
    return remember(navigationState) { Navigator(navigationState) }
}

@Composable
fun rememberCatalogExpansion(initial: CalendarExpansion): AnchoredDraggableState<CalendarExpansion> =
    rememberSaveable(saver = AnchoredDraggableState.Saver()) {
        AnchoredDraggableState(initial)
    }
