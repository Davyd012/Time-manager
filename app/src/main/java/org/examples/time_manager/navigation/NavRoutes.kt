package org.examples.time_manager.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Root

@Serializable
data object Calendar

sealed class NavHomeRoutes {
    @Serializable
    data object Home : NavHomeRoutes()

    @Serializable
    data object Settings : NavHomeRoutes()

    @Serializable
    data object Stopwatch : NavHomeRoutes()
}
