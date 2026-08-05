package org.examples.time_manager.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
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
