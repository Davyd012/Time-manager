package org.examples.time_manager.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import org.examples.time_manager.R
import org.examples.time_manager.di.DIContainer
import org.examples.time_manager.features.month_view.MonthViewModel
import org.examples.time_manager.features.month_view.MonthViewScreen
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.HomeScreen
import org.examples.time_manager.features.root.StopwatchViewModel
import org.examples.time_manager.features.root.presentation.components.BottomBar
import org.examples.time_manager.features.root.presentation.components.BottomBarItem
import org.examples.time_manager.features.root.presentation.stopwatch.Stopwatch
import org.examples.time_manager.ui.theme.homeIcon
import org.examples.time_manager.ui.theme.watchIcon
import java.time.YearMonth

@Composable
fun AppNavHost(
    diContainer: DIContainer,
    onExit: () -> Unit,
) {
    val navigationState = rememberNavigationState()
    val navigator = remember(navigationState) { Navigator(navigationState) }
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
                    PageNavigator(navigator).select(route)
                },
                modifier = Modifier
            )
        },
    ) { paddingValues ->
        NavDisplay(
            entries = entries,
            onBack = { if (!navigator.goBack()) onExit() },
            modifier = Modifier,
        )
    }
}
