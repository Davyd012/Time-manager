package org.examples.time_manager.navigation

import androidx.navigation.NavHostController

class Navigator(
    private val navController: NavHostController
) {
    fun toCalendar() {
        navController.navigate(Calendar) {
            launchSingleTop = true
        }
    }

    fun toMonthView(date: String, day: Int = 1) {
        val route = MonthView(date = date, day = day)
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun close() {
        navController.navigateUp()
    }
}

class PageNavigator(
    private val navController: NavHostController
) {
    fun changePage(route: NavHomeRoutes) {
        navController.navigate(route) {
            popUpTo(NavHomeRoutes.Home) {
                saveState = true
            }

            launchSingleTop = true
            restoreState = true
        }
    }

}