package no.timnor.app.uicatalog.previews

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
import no.timnor.app.features.calendar.presentation.components.ProjectFilterSheetContent
import no.timnor.app.features.month_view.MonthViewContent
import no.timnor.app.features.root.data.HomeIntent
import no.timnor.app.features.root.data.StopwatchIntent
import no.timnor.app.features.root.presentation.home.MainPageContent
import no.timnor.app.features.root.presentation.home.CalendarExpansion
import no.timnor.app.features.root.presentation.stopwatch.StopwatchContent
import no.timnor.app.features.root.presentation.stopwatch.NewProjectSheetContent
import no.timnor.app.ui.theme.TimeMangerTheme
import no.timnor.app.uicatalog.fixtures.CatalogFixtures
import no.timnor.app.uicatalog.fixtures.rememberCatalogExpansion
import no.timnor.app.uicatalog.fixtures.rememberCatalogNavigator

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
