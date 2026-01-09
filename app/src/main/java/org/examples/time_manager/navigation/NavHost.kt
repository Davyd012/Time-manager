package org.examples.time_manager.navigation

import android.util.Log
import androidx.compose.foundation.layout.padding
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
import org.examples.time_manager.features.root.HomeScreen
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.presentation.home.MainPage
import org.examples.time_manager.features.root.presentation.newProject.NewProject
import org.examples.time_manager.features.root.presentation.stopwatch.Stopwatch
import java.time.LocalDate

@Composable
fun AppNavHost(
    diContainer: DIContainer,
) {
    val rootBackStack = rememberNavBackStack(navSavedStateConfiguration, Root)
    val homeBackStack = rememberNavBackStack(navSavedStateConfiguration, HomeTab)
    val navigator = remember(rootBackStack) { Navigator(rootBackStack) }
    val rootEntryDecorators =
        listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )
    val homeEntryDecorators =
        listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )

    Log.d("NavHost", "Created a Nav host")
    NavDisplay(
        backStack = rootBackStack,
        entryDecorators = rootEntryDecorators,
        entryProvider =
            entryProvider {
                entry<Root> {
                    Log.d("NavHost", "Created a Root composable")
                    val vm =
                        viewModel<HomeViewModel>(
                            factory =
                                getHomeViewModelFactory(
                                    diContainer = diContainer,
                                )
                        )
                    HomeScreen(
                        vm = vm,
                        pageBackStack = homeBackStack,
                        content = { contentPadding ->
                            Log.d("NavHost", "Created a Home Nav host")
                            NavDisplay(
                                backStack = homeBackStack,
                                entryDecorators = homeEntryDecorators,
                                entryProvider =
                                    entryProvider {
                                        entry<HomeTab> {
                                            Log.d("NavHost", "Created a Main page composable")
                                            MainPage(
                                                vm,
                                                modifier =
                                                    Modifier.padding(
                                                        bottom = contentPadding.calculateBottomPadding()
                                                    ),
                                                navigator = navigator,
                                            )
                                        }
                                        entry<StopwatchTab> {
                                            Log.d("NavHost", "Created a Stopwatch page composable")
                                            Stopwatch(
                                                vm,
                                                modifier =
                                                    Modifier.padding(
                                                        bottom = contentPadding.calculateBottomPadding()
                                                    ),
                                            )
                                        }
                                        entry<SettingsTab> {
                                            Log.d("NavHost", "Created a New project page composable")
                                            NewProject(
                                                vm,
                                                modifier =
                                                    Modifier.padding(
                                                        bottom = contentPadding.calculateBottomPadding()
                                                    ),
                                            )
                                        }
                                    },
                            )
                        },
                    )
                }

                entry<Calendar> {
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
                    )
                }

                entry<MonthView> { monthDetail ->
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
