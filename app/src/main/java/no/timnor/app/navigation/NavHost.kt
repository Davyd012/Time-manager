package no.timnor.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import no.timnor.app.R
import no.timnor.app.di.DIContainer
import no.timnor.app.features.month_view.MonthViewModel
import no.timnor.app.features.month_view.MonthViewScreen
import no.timnor.app.features.root.HomeViewModel
import no.timnor.app.features.root.HomeScreen
import no.timnor.app.features.root.StopwatchViewModel
import no.timnor.app.features.settings.SettingsViewModel
import no.timnor.app.features.settings.SettingsScreen
import no.timnor.app.features.root.presentation.components.BottomBar
import no.timnor.app.features.root.presentation.components.BottomBarItem
import no.timnor.app.features.root.presentation.stopwatch.Stopwatch
import no.timnor.app.ui.theme.homeIcon
import no.timnor.app.ui.theme.watchIcon
import java.time.YearMonth

@Composable
fun AppNavHost(
    diContainer: DIContainer,
    onExit: () -> Unit,
) {
    val navigationState = rememberNavigationState()
    val navigator = remember(navigationState) { Navigator(navigationState) }
    var homeReselectionCount by remember { mutableStateOf(0) }
    val homeViewModel = viewModel<HomeViewModel>(
        factory = getHomeViewModelFactory(diContainer),
    )
    val stopwatchViewModel = viewModel<StopwatchViewModel>(
        factory = getStopwatchViewModelFactory(diContainer),
    )

    val entries = navigationState.toDecoratedEntries(
        entryProvider {
            entry<AppRoute.Home> {
                HomeScreen(
                    vm = homeViewModel,
                    navigator = navigator,
                    homeReselectionCount = homeReselectionCount,
                )
            }
            entry<AppRoute.Stopwatch> {
                Stopwatch(
                    vm = stopwatchViewModel,
                    modifier = Modifier,
                )
            }
            entry<AppRoute.MonthView> { route ->
                val safeMonth = YearMonth.of(
                    route.year,
                    route.month.coerceIn(1, 12),
                )
                val safeDay = route.selectedDay.coerceIn(1, safeMonth.lengthOfMonth())
                val monthViewModel = viewModel<MonthViewModel>(
                    factory = getMonthViewModelFactory(
                        diContainer = diContainer,
                        month = safeMonth.atDay(1),
                        selectedDay = safeDay,
                    ),
                )
                MonthViewScreen(
                    vm = monthViewModel,
                    modifier = Modifier,
                    navigator = navigator,
                )
            }
            entry<AppRoute.Settings> {
                val settingsViewModel = viewModel<SettingsViewModel>(factory = getSettingsViewModelFactory(diContainer))
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navigator.goBack() },
                    onGoToTimer = { navigator.select(AppRoute.Stopwatch) },
                    onRestored = homeViewModel::onBackupRestored,
                )
            }
        },
    )

    Scaffold(
        bottomBar = {
            val items = listOf(
                BottomBarItem(
                    direction = AppRoute.Home,
                    icon = homeIcon(filled = false),
                    activeIcon = homeIcon(),
                    label = stringResource(R.string.home_tab)
                ),
                BottomBarItem(
                    direction = AppRoute.Stopwatch,
                    icon = watchIcon(filled = false),
                    activeIcon = watchIcon(),
                    label = stringResource(R.string.stopwatch_tab)
                )
            )

            BottomBar(
                items = items,
                isCurrent = { route -> navigationState.selectedTopLevelRoute == route },
                onSelect = { route: TopLevelRoute ->
                    if (
                        route == AppRoute.Home &&
                        navigationState.selectedTopLevelRoute == AppRoute.Home
                    ) {
                        homeReselectionCount++
                    }
                    PageNavigator(navigator).select(route)
                },
                modifier = Modifier
            )
        },
    ) { paddingValues ->
        NavDisplay(
            entries = entries,
            onBack = { if (!navigator.goBack()) onExit() },
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
        )
    }
}
