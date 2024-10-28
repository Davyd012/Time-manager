package org.examples.time_manager.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.examples.time_manager.core.service.StopwatchService
import org.examples.time_manager.di.DIContainer
import org.examples.time_manager.features.root.HomeScreen
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.presentation.home.MainPage
import org.examples.time_manager.features.root.presentation.newProject.NewProject
import org.examples.time_manager.features.root.presentation.stopwatch.Stopwatch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(
    diContainer: DIContainer,
    navController: NavHostController,
    stopwatchService: StopwatchService?,
) {
    val navigator = Navigator(navController)
    Log.d("NavHost", "Created a Nav host")
    NavHost(navController = navController, startDestination = Root) {
        composable<Root> {
            Log.d("NavHost", "Created a Root composable")
            val vm = remember { HomeViewModel(stopwatchService) }
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
                            MainPage(vm, modifier = Modifier.padding(contentPadding))
                        }
                        composable<NavHomeRoutes.Timer> {
                            Log.d("NavHost", "Created a Stopwatch page composable")
                            Stopwatch(vm, modifier = Modifier.padding(contentPadding))
                        }
                        composable<NavHomeRoutes.Settings> {
                            Log.d("NavHost", "Created a New project page composable")
                            NewProject(vm, modifier = Modifier.padding(contentPadding))
                        }
                    }
                },
            )
        }
    }
}