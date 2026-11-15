package no.timnor.app.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
    @Test
    fun routeSerialNamesRemainCompatibleWithSavedNavigationState() {
        assertEquals("org.examples.time_manager.navigation.AppRoute", AppRoute.serializer().descriptor.serialName)
        assertEquals("org.examples.time_manager.navigation.AppRoute.Home", AppRoute.Home.serializer().descriptor.serialName)
        assertEquals("org.examples.time_manager.navigation.AppRoute.Stopwatch", AppRoute.Stopwatch.serializer().descriptor.serialName)
        assertEquals("org.examples.time_manager.navigation.AppRoute.MonthView", AppRoute.MonthView.serializer().descriptor.serialName)
        assertEquals("org.examples.time_manager.navigation.AppRoute.Settings", AppRoute.Settings.serializer().descriptor.serialName)
        assertEquals("org.examples.time_manager.navigation.AppRoute.PlannorAccount", AppRoute.PlannorAccount.serializer().descriptor.serialName)
        assertEquals("org.examples.time_manager.navigation.AppRoute.Projects", AppRoute.Projects.serializer().descriptor.serialName)
        assertEquals("org.examples.time_manager.navigation.AppRoute.ShareTime", AppRoute.ShareTime.serializer().descriptor.serialName)
        assertEquals("org.examples.time_manager.navigation.AppRoute.AllocationsReview", AppRoute.AllocationsReview.serializer().descriptor.serialName)
        assertEquals("org.examples.time_manager.navigation.AppRoute.BackupRestore", AppRoute.BackupRestore.serializer().descriptor.serialName)
    }

    @Test
    fun selectingTabsPreservesTheHomeStack() {
        val state = navigationState()
        val navigator = Navigator(state)

        navigator.openMonth(2026, 2, 7)
        PageNavigator(navigator).select(AppRoute.Stopwatch)
        PageNavigator(navigator).select(AppRoute.Home)

        assertEquals(AppRoute.MonthView(2026, 2, 7), state.homeStack.last())
    }

    @Test
    fun openingTheSameMonthDoesNotDuplicateIt() {
        val state = navigationState()
        val navigator = Navigator(state)

        navigator.openMonth(2026, 2, 7)
        navigator.openMonth(2026, 2, 7)

        assertEquals(2, state.homeStack.size)
    }

    @Test
    fun selectingHomeFromMonthReturnsToHomeRoot() {
        val state = navigationState()
        val navigator = Navigator(state)

        navigator.openMonth(2026, 2, 7)
        PageNavigator(navigator).select(AppRoute.Home)

        assertEquals(AppRoute.Home, state.homeStack.last())
        assertEquals(1, state.homeStack.size)
    }

    @Test
    fun backFromMonthReturnsToHomeAndBackFromStopwatchSelectsHome() {
        val state = navigationState()
        val navigator = Navigator(state)

        navigator.openMonth(2026, 2, 7)
        assertTrue(navigator.goBack())
        PageNavigator(navigator).select(AppRoute.Stopwatch)
        assertTrue(navigator.goBack())
        assertEquals(AppRoute.Home, state.selectedTopLevelRoute)
    }

    @Test
    fun settingsDestinationsPushAndPopWithoutReplacingTheSettingsOverview() {
        val state = navigationState()
        val navigator = Navigator(state)

        navigator.openSettings()
        navigator.openSettingsDestination(AppRoute.Projects)
        navigator.openSettingsDestination(AppRoute.PlannorAccount)

        assertEquals(AppRoute.PlannorAccount, state.homeStack.last())
        assertTrue(navigator.goBack())
        assertEquals(AppRoute.Projects, state.homeStack.last())
        assertTrue(navigator.goBack())
        assertEquals(AppRoute.Settings, state.homeStack.last())
    }

    @Test
    fun backAtHomeRootRequestsSystemExit() {
        val state = navigationState()
        assertFalse(Navigator(state).goBack())
    }

    private fun navigationState(): NavigationState = NavigationState(
        homeStack = NavBackStack(AppRoute.Home),
        stopwatchStack = NavBackStack(AppRoute.Stopwatch),
        selectedRoute = mutableStateOf<NavKey>(AppRoute.Home),
    )
}
