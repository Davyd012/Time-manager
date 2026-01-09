package org.examples.time_manager.navigation

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.examples.time_manager.di.DIContainer
import org.examples.time_manager.features.calendar.CalendarScreen
import org.examples.time_manager.features.calendar.CalendarViewModel
import org.examples.time_manager.features.month_view.MonthViewModel
import org.examples.time_manager.features.month_view.MonthViewScreen
import java.time.LocalDate

@Composable
fun AppNavHost(
    diContainer: DIContainer,
) {
    val rootBackStack = rememberNavBackStack(navSavedStateConfiguration, Route.RootRoute.Root)
    val navigator = remember(rootBackStack) { Navigator(rootBackStack) }
    val rootEntryDecorators =
        listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )

    Log.d("NavHost", "Created a Nav host")
    NavDisplay(
        backStack = rootBackStack,
        entryDecorators = rootEntryDecorators,
        onBack = { rootBackStack.removeLastOrNull() },
        transitionSpec = {
            val fromKey = initialState.key
            val toKey = targetState.key
            if (fromKey == Route.RootRoute.Root.toString() && toKey == Route.Calendar.toString()) {
                Log.d("NavHost", "Created a Calendar composable")
                (slideInVertically(
                    initialOffsetY = { fullHeight -> -fullHeight },
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(120)))
                    .togetherWith(ExitTransition.KeepUntilTransitionsFinished)
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        popTransitionSpec = {
            val fromKey = initialState.key
            val toKey = targetState.key
            if (fromKey == Route.Calendar.toString() && toKey == Route.RootRoute.Root.toString()) {
                EnterTransition.None.togetherWith(
                    slideOutVertically(
                        targetOffsetY = { fullHeight -> -fullHeight },
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                    ) + fadeOut(animationSpec = tween(120)),
                )
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        predictivePopTransitionSpec = {
            val fromKey = initialState.key
            val toKey = targetState.key
            if (fromKey == Route.Calendar.toString() && toKey == Route.RootRoute.Root.toString()) {
                EnterTransition.None.togetherWith(
                    slideOutVertically(
                        targetOffsetY = { fullHeight -> -fullHeight },
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                    ) + fadeOut(animationSpec = tween(120)),
                )
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        entryProvider =
            entryProvider {
                entry<Route.RootRoute.Root> {
                    HomeNavGraph(
                        diContainer = diContainer,
                        navigator = navigator,
                    )
                }

                entry<Route.Calendar> {
                    val vm =
                        viewModel<CalendarViewModel>(
                            factory =
                                getCalendarViewModelFactory(
                                    diContainer = diContainer,
                                )
                        )
                    CalendarScreen(
                        vm = vm,
                        navigator = navigator,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                entry<Route.MonthView> { monthDetail ->
                    Log.d("NavHost", "Created a Month view composable ${monthDetail.date}")
                    val month = LocalDate.parse(monthDetail.date)
                    val vm =
                        viewModel<MonthViewModel>(
                            factory =
                                getMonthViewModelFactory(
                                    diContainer = diContainer,
                                    month = month,
                                    selectedDay = monthDetail.day,
                                )
                        )
                    Log.d("NavHost", "Created a Month view composable $month")
                    MonthViewScreen(vm = vm, modifier = Modifier, navigator = navigator)
                }
            },
    )
}
