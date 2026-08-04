package org.examples.time_manager.features.root.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.HomeUiState
import org.examples.time_manager.features.root.data.HomeIntent.ModifyWorkState
import org.examples.time_manager.features.root.presentation.home.components.HeaderWidget
import org.examples.time_manager.features.root.presentation.home.components.ListOfWorks
import org.examples.time_manager.navigation.Navigator
import java.time.LocalDate

enum class CalendarExpansion {
    Collapsed,
    Expanded,
}

@Composable
fun MainPage(
    vm: HomeViewModel,
    state: HomeUiState,
    modifier: Modifier,
    navigator: Navigator,
    expansionState: AnchoredDraggableState<CalendarExpansion>,
    onSetCalendarExpansion: (CalendarExpansion) -> Unit,
) {
    val works = state.workQueries
    val projects = state.projects
    val density = LocalDensity.current
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

    fun toggleCalendar() {
        val target = when {
            expansionState.targetValue == CalendarExpansion.Expanded ->
                CalendarExpansion.Collapsed
            expansionState.currentValue == CalendarExpansion.Expanded ->
                CalendarExpansion.Collapsed
            else -> CalendarExpansion.Expanded
        }
        onSetCalendarExpansion(target)
    }

    BackHandler(enabled = progress > 0.001f) {
        onSetCalendarExpansion(CalendarExpansion.Collapsed)
    }

    if (state.selectedWork.showModal) {
        val index = state.selectedWork.selectedWork
        NewWorkInput(
            onDismiss = { vm.onIntent(ModifyWorkState(show = false)) },
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
                onCloseCalendar = {
                    onSetCalendarExpansion(CalendarExpansion.Collapsed)
                },
                onAddWork = { vm.onIntent(ModifyWorkState(show = true)) },
            )

            ListOfWorks(
                modifier = Modifier
                    .weight(1f),
                calendarProgress = progress,
                showWork = { i -> vm.onIntent(ModifyWorkState(selected = i, show = true)) },
                addWork = { vm.onIntent(ModifyWorkState(show = true)) },
                works = works,
                projects = projects,
            )
        }
    }
}
