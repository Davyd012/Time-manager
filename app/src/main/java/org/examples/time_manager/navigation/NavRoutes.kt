package org.examples.time_manager.navigation

import kotlinx.serialization.Serializable

sealed class NavRoutes(internal open val path: String) {
    data object Root : NavRoutes("/")
    data object BooksList : NavRoutes("/books")
}

@Serializable
data object Root

@Serializable
data object BooksList

sealed class NavHomeRoutes {
    @Serializable
    data object Home : NavHomeRoutes()

    @Serializable
    data object Settings : NavHomeRoutes()

    @Serializable
    data object Timer : NavHomeRoutes()
}
