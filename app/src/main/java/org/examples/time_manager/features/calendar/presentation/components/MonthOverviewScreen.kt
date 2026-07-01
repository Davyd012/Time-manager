package org.examples.time_manager.features.calendar.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.ui.theme.TimeMangerTheme

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
    onClearSelection: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalHours by remember(days) {
        derivedStateOf { days.sumOf { it.hours } }
    }

    val monthColors = MonthViewColors.defaults()
    val density = LocalDensity.current
    val closeThresholdPx = remember(density) { with(density) { 56.dp.toPx() } }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    Scaffold(
        modifier = Modifier.pointerInput(closeThresholdPx) {
            detectVerticalDragGestures(
                onDragStart = { dragTotal = 0f },
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    dragTotal += dragAmount
                },
                onDragEnd = {
                    if (dragTotal < -closeThresholdPx) {
                        onClose()
                    }
                    dragTotal = 0f
                },
                onDragCancel = { dragTotal = 0f },
            )
        },
        containerColor = monthColors.background,
        topBar = {
            MonthHeader(
                onClose = onClose,
                colors = monthColors,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                monthColors.background,
                                monthColors.backgroundGlow,
                                monthColors.background,
                            ),
                        ),
                    )
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalendarSelectorRow(
                monthYear = monthYear,
                onClose = onClose,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onViewMonth = onViewMonth,
                monthColors = monthColors,
            )

            CategorySegments(
                projects = projects,
                selectedProjects = selectedProjects,
                onSelectProject = onSelectProject,
                onClearSelection = onClearSelection,
                monthColors = monthColors,
            )

            WeekdayRow(
                colors = monthColors,
            )

            MonthGrid(
                monthYear = monthYear,
                days = days,
                selectedDate = selectedDate,
                onSelectDate = onSelectDate,
                totalHours = totalHours,
                monthColors = monthColors,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F10)
@Composable
fun MonthOverviewPreview() {
    val month = YearMonth.of(2026, 1)
    val days =
        listOf(
            DayWork(month.atDay(1), 10.0),
            DayWork(month.atDay(2), 8.0),
        )
    val projects =
        listOf(
            Project(id = 1, name = "Communication"),
            Project(id = 2, name = "Testing time"),
            Project(id = 3, name = "Service"),
        )

    TimeMangerTheme(
        darkTheme = true,
    ) {
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
            onClearSelection = {},
            onSelectDate = {},
        )
    }
}
