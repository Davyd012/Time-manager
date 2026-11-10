package no.timnor.app.shared.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.clock_filled
import no.timnor.app.shared.generated.resources.clock_outline
import no.timnor.app.shared.generated.resources.home_filled
import no.timnor.app.shared.generated.resources.home_outline
import no.timnor.app.shared.generated.resources.home_tab
import no.timnor.app.shared.generated.resources.stopwatch_title
import no.timnor.app.shared.features.month_view.presentation.TimnorMonthViewScreen
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.home.TimnorHomeScreen
import no.timnor.app.shared.features.root.presentation.stopwatch.TimnorStopwatchScreen
import no.timnor.app.shared.features.settings.presentation.TimnorSettingsSection
import no.timnor.app.shared.features.settings.domain.TimnorSettingsEffect
import no.timnor.app.shared.features.settings.domain.TimnorSettingsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class AppDestination { Home, Stopwatch, MonthView, Settings }

/** Shared application shell. Screen state and business actions remain in the feature ViewModel. */
@Composable
fun TimnorApp(
    viewModel: TimnorHomeViewModel,
    settingsViewModel: TimnorSettingsViewModel,
    onSettingsEffect: suspend (TimnorSettingsEffect) -> Unit = {},
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    var homeReselectionCount by rememberSaveable { mutableIntStateOf(0) }
    val state by viewModel.state.collectAsState()
    val settingsState = settingsViewModel.state.collectAsState().value
    val screenStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.effects.collect { effect -> onSettingsEffect(effect) }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    NavigationItem(
                        AppDestination.Home,
                        stringResource(Res.string.home_tab),
                        Res.drawable.home_outline,
                        Res.drawable.home_filled,
                    ),
                    NavigationItem(
                        AppDestination.Stopwatch,
                        stringResource(Res.string.stopwatch_title),
                        Res.drawable.clock_outline,
                        Res.drawable.clock_filled,
                    ),
                ).forEach { item ->
                    NavigationBarItem(
                        selected = destination == item.destination,
                        onClick = {
                            if (item.destination == AppDestination.Home) {
                                if (destination == AppDestination.Home || destination == AppDestination.MonthView) {
                                    homeReselectionCount++
                                }
                            }
                            destination = item.destination
                        },
                        icon = {
                            Icon(
                                painter = painterResource(if (destination == item.destination) item.selectedIcon else item.icon),
                                contentDescription = null,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { insets ->
        screenStateHolder.SaveableStateProvider(destination.name) {
            when (destination) {
                AppDestination.Home -> TimnorHomeScreen(
                    state = state,
                    viewModel = viewModel,
                    homeReselectionCount = homeReselectionCount,
                    onMonthView = { date ->
                        viewModel.selectCalendarDate(date)
                        destination = AppDestination.MonthView
                    },
                    onSettings = { destination = AppDestination.Settings },
                    onExportSpreadsheet = { year, month, projectIds ->
                        settingsViewModel.exportSpreadsheet(year, month, projectIds)
                    },
                    onShareMonth = settingsViewModel::shareText,
                    modifier = Modifier.padding(insets),
                )
                AppDestination.Stopwatch -> TimnorStopwatchScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(insets),
                )
                AppDestination.MonthView -> TimnorMonthViewScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(insets),
                    onBack = { destination = AppDestination.Home },
                )
                AppDestination.Settings -> {
                    TimnorSettingsSection(
                        state = settingsState,
                        modifier = Modifier.padding(insets),
                        onBack = { destination = AppDestination.Home },
                        onCreateBackup = settingsViewModel::createBackup,
                        onRestoreBackup = settingsViewModel::requestBackupRestore,
                        onShareBackup = settingsViewModel::shareBackup,
                        onExportSpreadsheet = {
                            val today = Clock.System.now()
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                            settingsViewModel.exportSpreadsheet(today.year, today.month.ordinal + 1)
                        },
                        onConfirmRestore = settingsViewModel::restoreBackup,
                        onCancelRestore = settingsViewModel::cancelRestorePreview,
                        onGoToStopwatch = { destination = AppDestination.Stopwatch },
                        onDismissMessage = settingsViewModel::dismissMessage,
                    )
                }
            }
        }
    }
}
private data class NavigationItem(
    val destination: AppDestination,
    val label: String,
    val icon: org.jetbrains.compose.resources.DrawableResource,
    val selectedIcon: org.jetbrains.compose.resources.DrawableResource,
)

