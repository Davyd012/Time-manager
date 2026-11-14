package no.timnor.app.shared.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.features.month_view.presentation.TimnorMonthViewScreen
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.home.TimnorHomeScreen
import no.timnor.app.shared.features.root.presentation.stopwatch.TimnorStopwatchScreen
import no.timnor.app.shared.features.settings.presentation.TimnorSettingsSection
import no.timnor.app.shared.features.settings.domain.TimnorSettingsEffect
import no.timnor.app.shared.features.settings.domain.TimnorSettingsViewModel

/** Shared application shell. Screen state and business actions remain in the feature ViewModel. */
@Composable
fun TimnorApp(
    viewModel: TimnorHomeViewModel,
    settingsViewModel: TimnorSettingsViewModel,
    onSettingsEffect: suspend (TimnorSettingsEffect) -> Unit = {},
    onPlatformBackHandlerChanged: (canGoBack: Boolean, handleBack: () -> Unit) -> Unit = { _, _ -> },
    additionalSettingsContent: @Composable ColumnScope.() -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(SharedAppTab.Home) }
    var homeRoute by rememberSaveable { mutableStateOf(SharedHomeRoute.Home) }
    var homeReselectionCount by rememberSaveable { mutableIntStateOf(0) }
    var monthViewEntryKey by rememberSaveable { mutableIntStateOf(0) }
    var calendarBackEnabled by rememberSaveable { mutableStateOf(false) }
    val navigationState = SharedAppNavigationState(selectedTab, homeRoute)
    val destination = navigationState.destination
    val state by viewModel.state.collectAsState()
    val settingsState = settingsViewModel.state.collectAsState().value
    val screenStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.effects.collect { effect -> onSettingsEffect(effect) }
    }

    val canHandlePlatformBack = selectedTab == SharedAppTab.Home &&
        (homeRoute != SharedHomeRoute.Home || calendarBackEnabled)
    val handlePlatformBack: () -> Unit = {
        if (selectedTab == SharedAppTab.Home && homeRoute == SharedHomeRoute.Home && calendarBackEnabled) {
            homeReselectionCount++
        } else {
            navigationState.onPlatformBack()?.let { next ->
                selectedTab = next.selectedTab
                homeRoute = next.homeRoute
            }
        }
    }
    SideEffect {
        onPlatformBackHandlerChanged(canHandlePlatformBack, handlePlatformBack)
    }
    DisposableEffect(onPlatformBackHandlerChanged) {
        onDispose { onPlatformBackHandlerChanged(false) {} }
    }

    val onSelectTab: (SharedAppTab) -> Unit = { destination ->
        if (destination == SharedAppTab.Home && selectedTab == SharedAppTab.Home) {
            homeReselectionCount++
        }
        val next = navigationState.selectTab(destination)
        selectedTab = next.selectedTab
        homeRoute = next.homeRoute
    }

    Scaffold(
        bottomBar = { SharedNavigationBar(selectedTab, onSelectTab) },
    ) { insets ->
        screenStateHolder.SaveableStateProvider(destination.name) {
            when (destination) {
                SharedAppDestination.Home -> TimnorHomeScreen(
                    state = state,
                    viewModel = viewModel,
                    homeReselectionCount = homeReselectionCount,
                    onMonthView = { date ->
                        viewModel.selectCalendarDate(date)
                        monthViewEntryKey++
                        calendarBackEnabled = false
                        val next = navigationState.openHomeRoute(SharedHomeRoute.MonthView)
                        selectedTab = next.selectedTab
                        homeRoute = next.homeRoute
                    },
                    onSettings = {
                        calendarBackEnabled = false
                        val next = navigationState.openHomeRoute(SharedHomeRoute.Settings)
                        selectedTab = next.selectedTab
                        homeRoute = next.homeRoute
                    },
                    onExportSpreadsheet = { year, month, projectIds ->
                        settingsViewModel.exportSpreadsheet(year, month, projectIds)
                    },
                    onShareMonth = settingsViewModel::shareText,
                    onCalendarBackEnabledChange = { calendarBackEnabled = it },
                    modifier = Modifier.padding(insets),
                )
                SharedAppDestination.Stopwatch -> TimnorStopwatchScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(insets),
                )
                SharedAppDestination.MonthView -> TimnorMonthViewScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(insets),
                    scrollToSelectedDateKey = monthViewEntryKey,
                    onBack = {
                        val next = navigationState.backToHomeRoot()
                        selectedTab = next.selectedTab
                        homeRoute = next.homeRoute
                    },
                )
                SharedAppDestination.Settings -> {
                    TimnorSettingsSection(
                        state = settingsState,
                        modifier = Modifier.padding(insets),
                        onBack = {
                            val next = navigationState.backToHomeRoot()
                            selectedTab = next.selectedTab
                            homeRoute = next.homeRoute
                        },
                        onCreateBackup = settingsViewModel::createBackup,
                        onRestoreBackup = settingsViewModel::requestBackupRestore,
                        onConfirmRestore = settingsViewModel::restoreBackup,
                        onCancelRestore = settingsViewModel::cancelRestorePreview,
                        onGoToStopwatch = { selectedTab = SharedAppTab.Stopwatch },
                        additionalContent = additionalSettingsContent,
                    )
                }
            }
        }
    }
}

