package org.examples.time_manager.features.root

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.res.stringResource
import org.examples.time_manager.R
import org.examples.time_manager.features.root.presentation.components.BottomBar
import org.examples.time_manager.navigation.PageNavigator
import org.examples.time_manager.navigation.Route

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    pageBackStack: NavBackStack<NavKey>,
    content: @Composable (PaddingValues) -> Unit,
) {
    val pageNavigator = remember(pageBackStack) { PageNavigator(pageBackStack) }
    val currentRoute = pageBackStack.lastOrNull() as? Route ?: Route.HomeRoute.HomeTab

    val snackbarHostState = remember { SnackbarHostState() }

    val createProjectMsg = stringResource(R.string.create_project_first_msg)
    val actionLabel = stringResource(R.string.click_me_action)

    LaunchedEffect(Unit) {
        vm.snackbarMessage.drop(1).collect { snackbarMessage ->
            snackbarMessage.let {
                snackbarHostState.showSnackbar(
                    message = createProjectMsg,
                    actionLabel = actionLabel,
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
