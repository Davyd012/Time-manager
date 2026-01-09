package org.examples.time_manager.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass


@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object RootRoute : Route, NavKey {

        @Serializable
        data object Root : Route, NavKey
    }

    @Serializable
    data object HomeRoute : Route, NavKey {

        @Serializable
        data object HomeTab : Route, NavKey

        @Serializable
        data object SettingsTab : Route, NavKey

        @Serializable
        data object StopwatchTab : Route, NavKey
    }

    @Serializable
    data object Calendar : Route, NavKey

    @Serializable
    data class MonthView(val date: String, val day: Int = 1) : Route, NavKey
}

val navSavedStateConfiguration: SavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Route.RootRoute.Root::class, Route.RootRoute.Root.serializer())
                    subclass(Route.Calendar::class, Route.Calendar.serializer())
                    subclass(Route.MonthView::class, Route.MonthView.serializer())
                    subclass(Route.HomeRoute.HomeTab::class, Route.HomeRoute.HomeTab.serializer())
                    subclass(Route.HomeRoute.SettingsTab::class, Route.HomeRoute.SettingsTab.serializer())
                    subclass(Route.HomeRoute.StopwatchTab::class, Route.HomeRoute.StopwatchTab.serializer())
                }
            }
    }
