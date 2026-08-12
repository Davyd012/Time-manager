package org.examples.time_manager.uicatalog.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.examples.time_manager.features.calendar.presentation.components.ProjectFilterSheetContent
import org.examples.time_manager.features.month_view.MonthViewContent
import org.examples.time_manager.features.root.data.HomeIntent
import org.examples.time_manager.features.root.data.StopwatchIntent
import org.examples.time_manager.features.root.presentation.home.MainPageContent
import org.examples.time_manager.features.root.presentation.home.CalendarExpansion
import org.examples.time_manager.features.root.presentation.stopwatch.StopwatchContent
import org.examples.time_manager.features.root.presentation.stopwatch.NewProjectSheetContent
import org.examples.time_manager.ui.theme.TimeMangerTheme
import org.examples.time_manager.uicatalog.fixtures.CatalogFixtures
import org.examples.time_manager.uicatalog.fixtures.rememberCatalogExpansion
import org.examples.time_manager.uicatalog.fixtures.rememberCatalogNavigator

@PreviewTest
@Preview(name = "Home default", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun catalogHomeDefault() {
    TimeMangerTheme(darkTheme = true) {
        val expansionState = rememberCatalogExpansion(CalendarExpansion.Collapsed)
        MainPageContent(
            state = CatalogFixtures.home,
            onIntent = { _: HomeIntent -> },
            modifier = Modifier.fillMaxSize(),
            navigator = rememberCatalogNavigator(),
            expansionState = expansionState,
            onSetCalendarExpansion = {},
            initialDateTime = CatalogFixtures.dateTime,
        )
    }
}

@PreviewTest
@Preview(name = "Home calendar expanded", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun catalogHomeCalendarExpanded() {
    TimeMangerTheme(darkTheme = true) {
        val expansionState = rememberCatalogExpansion(CalendarExpansion.Expanded)
        MainPageContent(
            state = CatalogFixtures.home,
            onIntent = { _: HomeIntent -> },
            modifier = Modifier.fillMaxSize(),
            navigator = rememberCatalogNavigator(),
            expansionState = expansionState,
            onSetCalendarExpansion = {},
            initialDateTime = CatalogFixtures.dateTime,
        )
    }
}

@PreviewTest
@Preview(name = "Stopwatch default", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun catalogStopwatchDefault() {
    TimeMangerTheme(darkTheme = true) {
        StopwatchContent(
            state = CatalogFixtures.stopwatch,
            modifier = Modifier.fillMaxSize(),
            onIntent = { _: StopwatchIntent -> },
        )
    }
}

@PreviewTest
@Preview(name = "Stopwatch running dark", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun catalogStopwatchRunningDark() {
    TimeMangerTheme(darkTheme = true) {
        StopwatchContent(
            state = CatalogFixtures.stopwatchRunning,
            modifier = Modifier.fillMaxSize(),
            onIntent = { _: StopwatchIntent -> },
        )
    }
}

@PreviewTest
@Preview(name = "New project sheet", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun catalogStopwatchNewProjectSheet() {
    TimeMangerTheme(darkTheme = true) {
        CatalogOverlayFrame {
            NewProjectSheetContent(
                project = null,
                onIntent = {},
                onDismiss = {},
            )
        }
    }
}

@PreviewTest
@Preview(name = "Month view default", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun catalogMonthViewDefault() {
    TimeMangerTheme(darkTheme = true) {
        MonthViewContent(
            state = CatalogFixtures.monthView,
            modifier = Modifier.fillMaxSize(),
            navigator = rememberCatalogNavigator(),
            onIntent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Month view empty", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun catalogMonthViewEmpty() {
    TimeMangerTheme(darkTheme = true) {
        MonthViewContent(
            state = CatalogFixtures.monthViewEmpty,
            modifier = Modifier.fillMaxSize(),
            navigator = rememberCatalogNavigator(),
            onIntent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Project filter sheet", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun catalogProjectFilterSheet() {
    TimeMangerTheme(darkTheme = true) {
        CatalogOverlayFrame {
            ProjectFilterSheetContent(
            projects = CatalogFixtures.projects,
            selectedProjects = CatalogFixtures.projects.take(2),
            projectHours = mapOf(1 to 2.25, 2 to 1.25, 3 to 0.0),
            onApply = {},
            onDismiss = {},
            )
        }
    }
}

@Composable
private fun CatalogOverlayFrame(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        StopwatchContent(
            state = CatalogFixtures.stopwatch,
            modifier = Modifier.fillMaxSize(),
            onIntent = { _: StopwatchIntent -> },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        )
        Surface(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        ) {
            content()
        }
    }
}
