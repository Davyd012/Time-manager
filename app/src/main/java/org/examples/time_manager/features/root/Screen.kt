package org.examples.time_manager.features.root

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
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.drop
import org.examples.time_manager.features.root.presentation.components.BottomBar
import org.examples.time_manager.navigation.PageNavigator

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    rootNav: NavHostController,
    content: @Composable (PaddingValues) -> Unit,
) {
    val pageNavigator = PageNavigator(rootNav)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.snackbarMessage.drop(1).collect { snackbarMessage ->
            snackbarMessage.let {
                snackbarHostState.showSnackbar(
                    message = "Skap et project først",
                    actionLabel = "Click me",
                    duration = SnackbarDuration.Short
                )
        //            viewModel.showSnackbar(null) // Clear the message after showing it
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomBar(pageNavigator)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        content.invoke(it)
    }
}