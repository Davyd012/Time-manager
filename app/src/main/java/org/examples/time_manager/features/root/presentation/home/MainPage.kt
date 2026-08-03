package org.examples.time_manager.features.root.presentation.home

import androidx.activity.compose.BackHandler
import android.provider.Settings
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.animateTo
import kotlinx.coroutines.launch
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.RootScreenEvents.ModifyWorkStateEvent
import org.examples.time_manager.features.root.presentation.home.components.HeaderWidget
import org.examples.time_manager.features.root.presentation.home.components.ListOfWorks
import org.examples.time_manager.navigation.Navigator
import java.time.LocalDate

enum class CalendarExpansion {
    Collapsed,
    Expanded,
}

@Composable
fun MainPage(vm: HomeViewModel, modifier: Modifier, navigator: Navigator) {
    val state by vm.state.collectAsState()
    val works by state.workQueries.collectAsState(initial = emptyList())
    val projects by state.projects.collectAsState(initial = emptyList())
    val density = LocalDensity.current
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    val coroutineScope = rememberCoroutineScope()
    val expansionState = rememberSaveable(
        saver = AnchoredDraggableState.Saver<CalendarExpansion>(),
    ) {
        AnchoredDraggableState(CalendarExpansion.Collapsed)
    }
    var selectedCalendarDateText by rememberSaveable { mutableStateOf<String?>(null) }

    val progress = run {
        val distance = expansionState.anchors.positionOf(CalendarExpansion.Expanded)
        val offset = expansionState.offset.takeUnless { it.isNaN() } ?: 0f
        if (distance.isNaN() || distance <= 0f) {
            if (expansionState.currentValue == CalendarExpansion.Expanded) 1f else 0f
        } else {
            (offset / distance).coerceIn(0f, 1f)
        }
    }

    fun settle(to: CalendarExpansion) {
        coroutineScope.launch {
            expansionState.animateTo(
                targetValue = to,
                animationSpec = if (animationsEnabled) {
                    spring(dampingRatio = 0.82f, stiffness = 420f)
                } else {
                    snap()
                },
            )
        }
    }

    fun toggleCalendar() {
        settle(
            if (progress < 0.5f) CalendarExpansion.Expanded
            else CalendarExpansion.Collapsed,
        )
    }

    BackHandler(enabled = progress > 0.001f) {
        settle(CalendarExpansion.Collapsed)
    }

    if (state.selectedWork.showModal) {
        val index = state.selectedWork.selectedWork
        NewWorkInput(
            onDismiss = { vm.onEvent(ModifyWorkStateEvent(show = false)) },
            vm = vm,
            projects = projects,
            day = state.selectedDay,
            work = works.takeIf { index >= 0 }?.elementAt(index),
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
            .fillMaxSize(),
    ) {
        val availableHeightPx = with(density) { maxHeight.roundToPx() }
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderWidget(
                state = state,
                vm = vm,
                navigator = navigator,
                expansionState = expansionState,
                availableHeightPx = availableHeightPx,
                selectedCalendarDate = selectedCalendarDateText?.let(LocalDate::parse),
                onCalendarDateSelected = { selectedCalendarDateText = it.toString() },
                onToggleCalendar = ::toggleCalendar,
                onAddWork = { vm.onEvent(ModifyWorkStateEvent(show = true)) },
            )

            ListOfWorks(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp),
                calendarProgress = progress,
                showWork = { i -> vm.onEvent(ModifyWorkStateEvent(selected = i, show = true)) },
                addWork = { vm.onEvent(ModifyWorkStateEvent(show = true)) },
                works = works,
                projects = projects,
            )
        }
    }
}
