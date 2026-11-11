package no.timnor.app.shared.navigation

/** The two destinations shown in the shared app's bottom navigation. */
enum class SharedAppTab { Home, Stopwatch }

/** Routes kept on the Home tab while the user visits other top-level tabs. */
internal enum class SharedHomeRoute { Home, MonthView, Settings }

internal enum class SharedAppDestination { Home, Stopwatch, MonthView, Settings }

/** Matches the legacy bottom bar's selected and unselected content contrast. */
internal fun sharedNavigationContentAlpha(selected: Boolean): Float = if (selected) 1f else 0.55f

internal data class SharedAppNavigationState(
    val selectedTab: SharedAppTab = SharedAppTab.Home,
    val homeRoute: SharedHomeRoute = SharedHomeRoute.Home,
) {
    val destination: SharedAppDestination
        get() = when (selectedTab) {
            SharedAppTab.Stopwatch -> SharedAppDestination.Stopwatch
            SharedAppTab.Home -> when (homeRoute) {
                SharedHomeRoute.Home -> SharedAppDestination.Home
                SharedHomeRoute.MonthView -> SharedAppDestination.MonthView
                SharedHomeRoute.Settings -> SharedAppDestination.Settings
            }
        }

    fun openHomeRoute(route: SharedHomeRoute): SharedAppNavigationState = copy(
        selectedTab = SharedAppTab.Home,
        homeRoute = route,
    )

    fun selectTab(tab: SharedAppTab): SharedAppNavigationState {
        val reselectingHome = tab == SharedAppTab.Home && selectedTab == SharedAppTab.Home
        return copy(
            selectedTab = tab,
            homeRoute = if (reselectingHome && homeRoute != SharedHomeRoute.Home) {
                SharedHomeRoute.Home
            } else {
                homeRoute
            },
        )
    }

    fun backToHomeRoot(): SharedAppNavigationState = copy(
        selectedTab = SharedAppTab.Home,
        homeRoute = SharedHomeRoute.Home,
    )

    /** Returns the next state for a platform Back action, or null when the shell is at a root. */
    fun onPlatformBack(): SharedAppNavigationState? =
        if (selectedTab == SharedAppTab.Home && homeRoute != SharedHomeRoute.Home) {
            backToHomeRoot()
        } else {
            null
        }
}
