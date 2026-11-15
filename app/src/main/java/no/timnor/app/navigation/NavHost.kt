package no.timnor.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import no.timnor.app.R
import no.timnor.app.di.DIContainer
import no.timnor.app.features.settings.SharedSettingsScreen
import no.timnor.app.features.settings.AndroidSettingsOverview
import no.timnor.app.features.settings.PlannorSettingsScreen
import no.timnor.app.features.settings.AndroidTimnorDocumentEffectHandler
import no.timnor.app.features.sharedhours.PlannorConnectionViewModel
import no.timnor.app.features.sharedhours.PlannorSettingsDestination
import no.timnor.app.shared.features.settings.domain.TimnorSettingsViewModel
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.home.TimnorHomeScreen
import no.timnor.app.shared.features.month_view.presentation.TimnorMonthViewScreen
import no.timnor.app.shared.features.root.presentation.stopwatch.TimnorStopwatchScreen
import no.timnor.app.features.root.presentation.components.BottomBar
import no.timnor.app.features.root.presentation.components.BottomBarItem
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
    var homeReselectionCount by remember { mutableIntStateOf(0) }
    var sharedCalendarBackEnabled by remember { mutableStateOf(false) }
    val sharedHomeViewModel = viewModel<TimnorHomeViewModel>(
        factory = getSharedHomeViewModelFactory(diContainer),
    )
    val sharedHomeState by sharedHomeViewModel.state.collectAsStateWithLifecycle()
    val settingsViewModel = viewModel<TimnorSettingsViewModel>(
        factory = getSharedSettingsViewModelFactory(diContainer),
    )
    val plannorConnectionViewModel = viewModel<PlannorConnectionViewModel>(
        factory = getPlannorConnectionViewModelFactory(diContainer),
    )
    val plannorState by plannorConnectionViewModel.state.collectAsStateWithLifecycle()
    SharedWorkSyncTriggers(plannorConnectionViewModel)
    LaunchedEffect(plannorState.projects) {
        sharedHomeViewModel.refreshLinkedProjectNames()
    }
    LaunchedEffect(sharedHomeState.works, plannorState.projects, plannorState.session?.accountId) {
        plannorConnectionViewModel.requestSync()
    }
    AndroidTimnorDocumentEffectHandler(settingsViewModel)

    BackHandler(
        enabled = navigationState.selectedTopLevelRoute == AppRoute.Home &&
            navigationState.homeStack.lastOrNull() == AppRoute.Home && sharedCalendarBackEnabled,
    ) {
        homeReselectionCount++
    }
    val entries = navigationState.toDecoratedEntries(
        entryProvider {
            entry<AppRoute.Home> {
                TimnorHomeScreen(
                    state = sharedHomeState,
                    viewModel = sharedHomeViewModel,
                    homeReselectionCount = homeReselectionCount,
                    onMonthView = { date ->
                        sharedHomeViewModel.selectCalendarDate(date)
                        navigator.openMonth(
                            date.year,
                            date.month.ordinal + 1,
                            date.day,
                        )
                    },
                    onSettings = navigator::openSettings,
                    onExportSpreadsheet = { year, month, projectIds ->
                        settingsViewModel.exportSpreadsheet(year, month, projectIds)
                    },
                    onShareMonth = settingsViewModel::shareText,
                    onCalendarBackEnabledChange = { sharedCalendarBackEnabled = it },
                    modifier = Modifier.statusBarsPadding(),
                )
            }
            entry<AppRoute.Stopwatch> {
                TimnorStopwatchScreen(
                    state = sharedHomeState,
                    viewModel = sharedHomeViewModel,
                    modifier = Modifier.statusBarsPadding(),
                )
            }
            entry<AppRoute.MonthView> { route ->
                val safeMonth = YearMonth.of(
                    route.year,
                    route.month.coerceIn(1, 12),
                )
                val safeDay = route.selectedDay.coerceIn(1, safeMonth.lengthOfMonth())
                LaunchedEffect(route.year, route.month, route.selectedDay) {
                    sharedHomeViewModel.selectCalendarDate(
                        kotlinx.datetime.LocalDate(safeMonth.year, safeMonth.monthValue, safeDay),
                    )
                }
                TimnorMonthViewScreen(
                    state = sharedHomeState,
                    viewModel = sharedHomeViewModel,
                    modifier = Modifier.statusBarsPadding(),
                    onBack = { navigator.goBack() },
                )
            }
            entry<AppRoute.Settings> {
                AndroidSettingsOverview(
                    state = plannorState,
                    onBack = { navigator.goBack() },
                    onOpen = { destination ->
                        navigator.openSettingsDestination(
                            when (destination) {
                                PlannorSettingsDestination.Account -> AppRoute.PlannorAccount
                                PlannorSettingsDestination.Projects -> AppRoute.Projects
                                PlannorSettingsDestination.ShareTime -> AppRoute.ShareTime
                                PlannorSettingsDestination.CreateTime -> AppRoute.CreateSharedTime
                                PlannorSettingsDestination.AllocationsReview -> AppRoute.AllocationsReview
                            },
                        )
                    },
                    onBackup = { navigator.openSettingsDestination(AppRoute.BackupRestore) },
                )
            }
            entry<AppRoute.BackupRestore> {
                SharedSettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navigator.goBack() },
                    onGoToTimer = { navigator.select(AppRoute.Stopwatch) },
                    onRestored = {},
                )
            }
            entry<AppRoute.PlannorAccount> {
                PlannorSettingsScreen(
                    plannorConnectionViewModel,
                    PlannorSettingsDestination.Account,
                    onBack = { navigator.goBack() },
                    onConnectRequired = { navigator.openSettingsDestination(AppRoute.PlannorAccount) },
                    onOpenProjects = { navigator.openSettingsDestination(AppRoute.Projects) },
                    onCreateTime = { navigator.openSettingsDestination(AppRoute.CreateSharedTime) },
                )
            }
            entry<AppRoute.Projects> {
                PlannorSettingsScreen(
                    plannorConnectionViewModel,
                    PlannorSettingsDestination.Projects,
                    onBack = { navigator.goBack() },
                    onConnectRequired = { navigator.openSettingsDestination(AppRoute.PlannorAccount) },
                )
            }
            entry<AppRoute.ShareTime> {
                PlannorSettingsScreen(
                    plannorConnectionViewModel,
                    PlannorSettingsDestination.ShareTime,
                    onBack = { navigator.goBack() },
                    onConnectRequired = { navigator.openSettingsDestination(AppRoute.PlannorAccount) },
                )
            }
            entry<AppRoute.CreateSharedTime> {
                PlannorSettingsScreen(
                    plannorConnectionViewModel,
                    PlannorSettingsDestination.CreateTime,
                    onBack = { navigator.goBack() },
                    onConnectRequired = { navigator.openSettingsDestination(AppRoute.PlannorAccount) },
                )
            }
            entry<AppRoute.AllocationsReview> {
                PlannorSettingsScreen(
                    plannorConnectionViewModel,
                    PlannorSettingsDestination.AllocationsReview,
                    onBack = { navigator.goBack() },
                    onConnectRequired = { navigator.openSettingsDestination(AppRoute.PlannorAccount) },
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
        val statusBarSurface = if (navigationState.selectedTopLevelRoute == AppRoute.Home) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.background
        }
        NavDisplay(
            entries = entries,
            onBack = { if (!navigator.goBack()) onExit() },
            modifier = Modifier
                .background(statusBarSurface)
                .padding(bottom = paddingValues.calculateBottomPadding()),
        )
    }
}
