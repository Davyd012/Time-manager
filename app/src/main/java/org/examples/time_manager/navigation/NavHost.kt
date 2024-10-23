package org.examples.time_manager.navigation

import android.os.Build
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
) {
    val navigator = Navigator(navController)
    NavHost(navController = navController, startDestination = Root) {
        composable<Root> {
            val vm = remember { HomeViewModel() }
            val homeNavController = rememberNavController()
            HomeScreen(
                vm = vm, rootNav = homeNavController,
                content = { contentPadding: PaddingValues ->
                    NavHost(
                        navController = homeNavController,
                        startDestination = NavHomeRoutes.Home
                    ) {
                        composable<NavHomeRoutes.Home> {
                            MainPage(vm, modifier = Modifier.padding(contentPadding))
                        }
                        composable<NavHomeRoutes.Timer> {
                            Stopwatch(vm, modifier = Modifier.padding(contentPadding))
                        }
                        composable<NavHomeRoutes.Settings> {
                            NewProject(vm, modifier = Modifier.padding(contentPadding))
                        }
                    }
                },
            )
        }
    }
}