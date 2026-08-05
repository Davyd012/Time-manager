package org.examples.time_manager.features.root

import android.provider.Settings
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.examples.time_manager.features.root.presentation.home.CalendarExpansion
import org.examples.time_manager.features.root.presentation.home.MainPage
import org.examples.time_manager.navigation.Navigator

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    navigator: Navigator,
    homeReselectionCount: Int,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val message = state.messageResId?.let { stringResource(it) }
    val scope = rememberCoroutineScope()
    val expansionState = rememberSaveable(
        saver = AnchoredDraggableState.Saver<CalendarExpansion>(),
    ) { AnchoredDraggableState(CalendarExpansion.Collapsed) }
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it) }
    }

    fun setCalendarExpansion(target: CalendarExpansion) {
        scope.launch {
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

    LaunchedEffect(homeReselectionCount) {
        if (homeReselectionCount > 0) {
            setCalendarExpansion(CalendarExpansion.Collapsed)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        MainPage(
            vm = vm,
            state = state,
            modifier = Modifier,
            navigator = navigator,
            expansionState = expansionState,
            onSetCalendarExpansion = ::setCalendarExpansion,
        )
    }
}
