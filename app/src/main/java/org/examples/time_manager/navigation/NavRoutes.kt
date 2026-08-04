package org.examples.time_manager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Home : AppRoute, TopLevelRoute

    @Serializable
    data object Stopwatch : AppRoute, TopLevelRoute

    @Serializable
    data class MonthView(
        val year: Int,
        val month: Int,
        val selectedDay: Int,
    ) : AppRoute
}

sealed interface TopLevelRoute : NavKey

/** Saveable state for the two top-level destinations. */
class NavigationState(
    val homeStack: NavBackStack<NavKey>,
    val stopwatchStack: NavBackStack<NavKey>,
    selectedRoute: MutableState<NavKey>,
) {
    private val routeState = selectedRoute
    var selectedTopLevelRoute: TopLevelRoute
        get() = routeState.value as TopLevelRoute
        private set(value) {
            routeState.value = value
        }

    val backStacks: Map<TopLevelRoute, NavBackStack<NavKey>>
        get() = mapOf(
            AppRoute.Home to homeStack,
            AppRoute.Stopwatch to stopwatchStack,
        )

    fun select(route: TopLevelRoute) {
        selectedTopLevelRoute = route
    }

    @Composable
    fun toDecoratedEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>,
    ): List<NavEntry<NavKey>> {
        val decoratedStacks = backStacks.mapValues { (_, stack) ->
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                    rememberViewModelStoreNavEntryDecorator<NavKey>(),
                ),
                entryProvider = entryProvider,
            )
        }
        return if (selectedTopLevelRoute == AppRoute.Home) {
            decoratedStacks.getValue(AppRoute.Home)
        } else {
            decoratedStacks.getValue(AppRoute.Home) + decoratedStacks.getValue(AppRoute.Stopwatch)
        }
    }
}

@Composable
fun rememberNavigationState(): NavigationState {
    val selectedRoute = rememberSerializable(
        serializer = MutableStateSerializer(NavKeySerializer()),
    ) { mutableStateOf<NavKey>(AppRoute.Home) }
    val homeStack = rememberNavBackStack(AppRoute.Home)
    val stopwatchStack = rememberNavBackStack(AppRoute.Stopwatch)
    return remember(homeStack, stopwatchStack, selectedRoute) {
        NavigationState(homeStack, stopwatchStack, selectedRoute)
    }
}
