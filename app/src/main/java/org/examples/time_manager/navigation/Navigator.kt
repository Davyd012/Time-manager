package org.examples.time_manager.navigation

import androidx.navigation.NavHostController

class Navigator(
    private val navController: NavHostController
) {
    fun close() {
        navController.popBackStack()
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

    fun close() {
        navController.popBackStack()
    }
}