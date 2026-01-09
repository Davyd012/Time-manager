package org.examples.time_manager.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class Navigator(
    private val backStack: NavBackStack<NavKey>
) {
    fun toCalendar() {
        navigateTo(Calendar)
    }

    fun toMonthView(date: String, day: Int = 1) {
        navigateTo(MonthView(date = date, day = day))
    }

    fun close() {
        if (backStack.size > 1) {
            backStack.removeLast()
        }
    }

    private fun navigateTo(route: RootRoute) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }
}

class PageNavigator(
    private val backStack: NavBackStack<NavKey>
) {
    fun changePage(route: HomeRoute) {
        if (backStack.isEmpty()) {
            backStack.add(route)
            return
        }

        while (backStack.size > 1) {
            backStack.removeLast()
        }

        if (backStack.lastOrNull() != route) {
            backStack[backStack.lastIndex] = route
        }
    }
}
