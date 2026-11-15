package no.timnor.app.navigation

class Navigator(private val state: NavigationState) {
    internal fun select(route: TopLevelRoute) {
        if (
            route == AppRoute.Home &&
            state.selectedTopLevelRoute == AppRoute.Home &&
            state.homeStack.size > 1
        ) {
            goBack()
            return
        }
        state.select(route)
    }

    fun openMonth(year: Int, month: Int, selectedDay: Int) {
        val route = AppRoute.MonthView(year, month, selectedDay)
        val stack = state.homeStack
        if (stack.lastOrNull() != route) stack.add(route)
        state.select(AppRoute.Home)
    }

    fun openSettings() {
        openSettingsDestination(AppRoute.Settings)
    }

    fun openSettingsDestination(route: AppRoute) {
        require(route in setOf(
            AppRoute.Settings,
            AppRoute.PlannorAccount,
            AppRoute.Projects,
            AppRoute.ShareTime,
            AppRoute.CreateSharedTime,
            AppRoute.AllocationsReview,
            AppRoute.BackupRestore,
        ))
        val stack = state.homeStack
        if (stack.lastOrNull() != route) stack.add(route)
        state.select(AppRoute.Home)
    }

    fun goBack(): Boolean {
        return when (state.selectedTopLevelRoute) {
            AppRoute.Home -> {
                if (state.homeStack.size > 1) {
                    state.homeStack.removeAt(state.homeStack.lastIndex)
                    true
                } else {
                    false
                }
            }
            AppRoute.Stopwatch -> {
                state.select(AppRoute.Home)
                true
            }
        }
    }

    fun returnHome() {
        state.select(AppRoute.Home)
    }
}

class PageNavigator(private val navigator: Navigator) {
    fun select(route: TopLevelRoute) {
        navigator.select(route)
    }
}
