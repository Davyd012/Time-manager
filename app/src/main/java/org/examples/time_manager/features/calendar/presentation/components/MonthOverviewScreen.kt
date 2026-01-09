package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.examples.time_manager.core.database.project.Project

@Composable
fun MonthOverviewScreen(
    monthYear: YearMonth,
    days: List<DayWork>,
    projects: List<Project>,
    selectedProjects: List<Project>,
    selectedDate: LocalDate?,
    onClose: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onViewMonth: () -> Unit,
    onSelectProject: (Project) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    colors: MonthViewColors = MonthViewColors.defaults(),
) {
    val totalHours by remember(days) {
        derivedStateOf { days.sumOf { it.hours } }
    }

    Scaffold(
        topBar = {
            MonthHeader(
                title = formatMonthTitle(monthYear),
                onClose = onClose,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onViewMonth = onViewMonth,
                colors = colors,
            )
        }
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategorySegments(
                projects = projects,
                selectedProjects = selectedProjects,
                onSelectProject = onSelectProject,
                colors = colors,
            )

            WeekdayRow(
                colors = colors,
            )

            MonthGrid(
                monthYear = monthYear,
                days = days,
                selectedDate = selectedDate,
                onSelectDate = onSelectDate,
                colors = colors,
            )

            MonthlyTotalPill(
                totalHours = totalHours,
                colors = colors,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

private fun formatMonthTitle(monthYear: YearMonth): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("nb", "NO"))
    return monthYear.format(formatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("nb", "NO")) else it.toString()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F10)
@Composable
fun MonthOverviewPreview() {
    val month = YearMonth.of(2026, 1)
    val days =
        listOf(
            DayWork(month.atDay(1), 10),
            DayWork(month.atDay(2), 8),
        )
    val projects =
        listOf(
            Project(id = 1, name = "Communication"),
            Project(id = 2, name = "Testing time"),
            Project(id = 3, name = "Service"),
        )

    MonthOverviewScreen(
        monthYear = month,
        days = days,
        projects = projects,
        selectedProjects = projects.take(1),
        selectedDate = month.atDay(2),
        onClose = {},
        onPrevMonth = {},
        onNextMonth = {},
        onViewMonth = {},
        onSelectProject = {},
        onSelectDate = {},
    )
}
