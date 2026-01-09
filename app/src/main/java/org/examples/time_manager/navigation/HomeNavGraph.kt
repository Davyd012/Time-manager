package org.examples.time_manager.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.examples.time_manager.di.DIContainer
import org.examples.time_manager.features.root.HomeScreen
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.presentation.home.MainPage
import org.examples.time_manager.features.root.presentation.newProject.NewProject
import org.examples.time_manager.features.root.presentation.stopwatch.Stopwatch

@Composable
fun HomeNavGraph(
    diContainer: DIContainer,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val homeBackStack = rememberNavBackStack(navSavedStateConfiguration, Route.HomeRoute.HomeTab)
    val homeEntryDecorators =
        listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )
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
            NavDisplay(
                backStack = homeBackStack,
                entryDecorators = homeEntryDecorators,
                entryProvider =
                    entryProvider {
                        entry<Route.HomeRoute.HomeTab> {
                            MainPage(
                                vm,
                                modifier =
                                    modifier.padding(
                                        bottom = contentPadding.calculateBottomPadding()
                                    ),
                                navigator = navigator,
                            )
                        }
                        entry<Route.HomeRoute.StopwatchTab> {
                            Stopwatch(
                                vm,
                                modifier =
                                    modifier.padding(
                                        bottom = contentPadding.calculateBottomPadding()
                                    ),
                            )
                        }
                        entry<Route.HomeRoute.SettingsTab> {
                            NewProject(
                                vm,
                                modifier =
                                    modifier.padding(
                                        bottom = contentPadding.calculateBottomPadding()
                                    ),
                            )
                        }
                    },
            )
        },
    )
}
