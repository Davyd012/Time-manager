package org.examples.time_manager.features.calendar

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.examples.time_manager.App
import org.examples.time_manager.features.calendar.data.CalendarScreenEvents
import org.examples.time_manager.features.calendar.presentation.components.DayWork
import org.examples.time_manager.features.calendar.presentation.components.MonthOverviewScreen
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.navigation.Navigator
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun CalendarScreen(
    vm: CalendarViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    var selectedDate by rememberSaveable { mutableStateOf<java.time.LocalDate?>(null) }

    val monthYear = remember(state.currentDate) { YearMonth.from(state.currentDate) }
    val dayWorks =
        remember(state.dayPerMonth) {
            state.dayPerMonth.map { day ->
                DayWork(
                    date = day.date,
                    hours = (day.time / 3600.0).roundToInt(),
                )
            }
        }
    val projects = state.projects.collectAsState(initial = emptyList<Project>()).value

    MonthOverviewScreen(
        monthYear = monthYear,
        days = dayWorks,
        projects = projects,
        selectedProjects = state.selectedProjects,
        selectedDate = selectedDate,
        onClose = { navigator.close() },
        onPrevMonth = { vm.onEvent(CalendarScreenEvents.UpdateMonth(-1)) },
        onNextMonth = { vm.onEvent(CalendarScreenEvents.UpdateMonth(1)) },
        onViewMonth = { navigator.toMonthView(date = monthYear.atDay(1).toString(), day = 1) },
        onSelectProject = { project -> vm.onEvent(CalendarScreenEvents.UpdateSelectedProjects(project)) },
        onSelectDate = { date ->
            selectedDate = date
            navigator.toMonthView(date = monthYear.atDay(1).toString(), day = date.dayOfMonth)
        },
        modifier = modifier.padding(top = App.statusBarHeight),
    )
}
