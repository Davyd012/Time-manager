package org.examples.time_manager.navigation

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
    navController: NavHostController,
) {
    val navigator = Navigator(navController)
    Log.d("NavHost", "Created a Nav host")
    NavHost(navController = navController, startDestination = Root) {
        composable<Root> {
            Log.d("NavHost", "Created a Root composable")
            val vm = viewModel<HomeViewModel>(
                factory = getHomeViewModelFactory(
                    diContainer = diContainer,
                )
            )
            val homeNavController = rememberNavController()
            HomeScreen(
                vm = vm, rootNav = homeNavController,
                content = { contentPadding: PaddingValues ->
                    Log.d("NavHost", "Created a Home Nav host")
                    NavHost(
                        navController = homeNavController,
                        startDestination = NavHomeRoutes.Home
                    ) {
                        composable<NavHomeRoutes.Home> {
                            Log.d("NavHost", "Created a Main page composable")
                            MainPage(
                                vm,
                                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
                                navigator = navigator
                            )
                        }
                        composable<NavHomeRoutes.Stopwatch> {
                            Log.d("NavHost", "Created a Stopwatch page composable")
                            Stopwatch(
                                vm,
                                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding())
                            )
                        }
                        composable<NavHomeRoutes.Settings> {
                            Log.d("NavHost", "Created a New project page composable")
                            NewProject(
                                vm,
                                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding())
                            )
                        }
                    }
                },
            )
        }

        composable<Calendar> {
            val vm = viewModel<CalendarViewModel>(
                factory = getCalendarViewModelFactory(
                    diContainer = diContainer,
                )
            )
            CalendarScreen(
                vm = vm,
                navigator = navigator
            )
        }

        composable<MonthView> {
            val monthDetail = requireNotNull(it.toRoute<MonthView>())
            Log.d("NavHost", "Created a Month view composable ${monthDetail.date}")
            val month = LocalDate.parse(monthDetail.date)
            val vm = viewModel<MonthViewModel>(
                factory = getMonthViewModelFactory(
                    diContainer = diContainer,
                    month = month,
                    selectedDay = monthDetail.day,
                )
            )
            Log.d("NavHost", "Created a Month view composable $month")
            MonthViewScreen(vm = vm, modifier = Modifier, navigator = navigator)
        }
    }
}