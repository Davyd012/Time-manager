package org.examples.time_manager.features.root

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.drop
import org.examples.time_manager.features.root.presentation.components.BottomBar
import org.examples.time_manager.navigation.HomeRoute
import org.examples.time_manager.navigation.HomeTab
import org.examples.time_manager.navigation.PageNavigator

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    pageBackStack: NavBackStack<NavKey>,
    content: @Composable (PaddingValues) -> Unit,
) {
    val pageNavigator = remember(pageBackStack) { PageNavigator(pageBackStack) }
    val currentRoute = pageBackStack.lastOrNull() as? HomeRoute ?: HomeTab

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.snackbarMessage.drop(1).collect { snackbarMessage ->
            snackbarMessage.let {
                snackbarHostState.showSnackbar(
                    message = "Skap et projekt først",
                    actionLabel = "Click me",
                    duration = SnackbarDuration.Short
                )
        //            viewModel.showSnackbar(null) // Clear the message after showing it
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomBar(pageNavigator, currentRoute)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        content.invoke(it)
    }
}
