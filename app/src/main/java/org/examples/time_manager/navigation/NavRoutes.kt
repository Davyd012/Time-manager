package org.examples.time_manager.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface RootRoute : NavKey

@Serializable
sealed interface HomeRoute : NavKey

@Serializable
data object Root : RootRoute

@Serializable
data object Calendar : RootRoute

@Serializable
data class MonthView(val date: String, val day: Int = 1) : RootRoute

@Serializable
data object HomeTab : HomeRoute

@Serializable
data object SettingsTab : HomeRoute

@Serializable
data object StopwatchTab : HomeRoute

val navSavedStateConfiguration: SavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Root::class, Root.serializer())
                    subclass(Calendar::class, Calendar.serializer())
                    subclass(MonthView::class, MonthView.serializer())
                    subclass(HomeTab::class, HomeTab.serializer())
                    subclass(SettingsTab::class, SettingsTab.serializer())
                    subclass(StopwatchTab::class, StopwatchTab.serializer())
                }
            }
    }
