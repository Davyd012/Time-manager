package no.timnor.app.shared.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedAppNavigationStateTest {
    @Test
    fun returningToHomeTabRestoresItsNestedRoute() {
        val settings = SharedAppNavigationState().openHomeRoute(SharedHomeRoute.Settings)
        val stopwatch = settings.selectTab(SharedAppTab.Stopwatch)

        assertEquals(SharedAppDestination.Stopwatch, stopwatch.destination)
        assertEquals(SharedAppDestination.Settings, stopwatch.selectTab(SharedAppTab.Home).destination)
    }

    @Test
    fun homeReselectionPopsOneNestedHomeRoute() {
        val nested = SharedAppNavigationState().openHomeRoute(SharedHomeRoute.MonthView)

        assertEquals(SharedAppDestination.Home, nested.selectTab(SharedAppTab.Home).destination)
    }

    @Test
    fun openingAndBackingOutOfHomeRoutesSelectsTheHomeTab() {
        val stopwatch = SharedAppNavigationState().selectTab(SharedAppTab.Stopwatch)
        val month = stopwatch.openHomeRoute(SharedHomeRoute.MonthView)

        assertEquals(SharedAppTab.Home, month.selectedTab)
        assertEquals(SharedAppDestination.MonthView, month.destination)
        assertEquals(SharedAppDestination.Home, month.backToHomeRoot().destination)
    }

    @Test
    fun platformBackPopsNestedHomeRouteButDoesNothingAtRootOrOnStopwatch() {
        val month = SharedAppNavigationState().openHomeRoute(SharedHomeRoute.MonthView)
        val stopwatch = SharedAppNavigationState().selectTab(SharedAppTab.Stopwatch)

        assertEquals(SharedAppDestination.Home, month.onPlatformBack()?.destination)
        assertEquals(null, SharedAppNavigationState().onPlatformBack())
        assertEquals(null, stopwatch.onPlatformBack())
    }

    @Test
    fun navigationContentAlphaMatchesLegacySelectedAndUnselectedStates() {
        assertEquals(1f, sharedNavigationContentAlpha(selected = true))
        assertEquals(0.55f, sharedNavigationContentAlpha(selected = false))
    }
}
