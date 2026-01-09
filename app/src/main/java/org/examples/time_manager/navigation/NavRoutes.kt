package org.examples.time_manager.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Root

@Serializable
data object Calendar

@Serializable
data class MonthView(val date: String, val day: Int = 1)

sealed class NavHomeRoutes {
    @Serializable
    data object Home : NavHomeRoutes()

    @Serializable
    data object Settings : NavHomeRoutes()

    @Serializable
    data object Stopwatch : NavHomeRoutes()
}
