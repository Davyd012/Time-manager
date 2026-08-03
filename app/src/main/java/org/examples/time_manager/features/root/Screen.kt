package org.examples.time_manager.features.root

import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.res.stringResource
import org.examples.time_manager.R
import org.examples.time_manager.features.root.presentation.components.BottomBar
import org.examples.time_manager.features.root.presentation.home.CalendarExpansion
import org.examples.time_manager.navigation.PageNavigator
import org.examples.time_manager.navigation.Route
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    pageBackStack: NavBackStack<NavKey>,
    content: @Composable (
        PaddingValues,
        AnchoredDraggableState<CalendarExpansion>,
        (CalendarExpansion) -> Unit,
    ) -> Unit,
) {
    val pageNavigator = remember(pageBackStack) { PageNavigator(pageBackStack) }
    val currentRoute = pageBackStack.lastOrNull() as? Route ?: Route.HomeRoute.HomeTab

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val expansionState = rememberSaveable(
        saver = AnchoredDraggableState.Saver<CalendarExpansion>(),
    ) {
        AnchoredDraggableState(CalendarExpansion.Collapsed)
    }
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }

    fun setCalendarExpansion(target: CalendarExpansion) {
        coroutineScope.launch {
            expansionState.animateTo(
                targetValue = target,
                animationSpec = if (animationsEnabled) {
                    spring(dampingRatio = 0.82f, stiffness = 420f)
                } else {
                    snap()
                },
            )
        }
    }

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
            BottomBar(
                navigator = pageNavigator,
                currentRoute = currentRoute,
                onHomeClick = {
                    setCalendarExpansion(CalendarExpansion.Collapsed)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        content.invoke(it, expansionState, ::setCalendarExpansion)
    }
}
