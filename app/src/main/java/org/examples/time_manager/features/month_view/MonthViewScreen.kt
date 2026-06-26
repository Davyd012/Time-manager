package org.examples.time_manager.features.month_view

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.examples.time_manager.features.month_view.presentation.components.ListOfWorks
import org.examples.time_manager.features.month_view.presentation.components.HeaderWidget
import org.examples.time_manager.features.month_view.presentation.NewWorkInput
import org.examples.time_manager.features.root.data.RootScreenEvents.ModifyWorkStateEvent
import org.examples.time_manager.navigation.Navigator

@Composable
fun MonthViewScreen(vm: MonthViewModel, modifier: Modifier, navigator: Navigator) {
    val colors = MaterialTheme.colorScheme
    val state by vm.state.collectAsState()
    val works by state.workQueries.collectAsState(initial = emptyList())
    val projects by state.projects.collectAsState(initial = emptyList())

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            Log.d("MonthViewScreen", "Scrolling to ${state.selectedDay - 4}")
            if (state.selectedDay > 4) {
                listState.scrollToItem(state.selectedDay - 4)
            }
        }
    }

    if (state.selectedWork.showModal) {
        val index = state.selectedWork.selectedWork
        NewWorkInput(
            onDismiss = {
                vm.onEvent(ModifyWorkStateEvent(show = false))
            },
            vm = vm,
            projects = projects,
            day = state.selectedDay,
            selectedDate = state.dayPerMonth.getOrNull(state.selectedDay - 1)?.date,
            work = works.takeIf { index >= 0 }?.elementAt(index)
        )
    }

    Column(
        modifier = modifier
            .background(colors.surface)
            .fillMaxSize()
    ) {
        HeaderWidget(state, listState, vm = vm, navigator = navigator)
        Spacer(modifier = Modifier.height(5.dp))
        ListOfWorks(
            showWork = { i: Int ->
                vm.onEvent(ModifyWorkStateEvent(selected = i, show = true))
            },
            addWork = { vm.onEvent(ModifyWorkStateEvent(show = true)) },
            works = works,
            projects = projects
        )
    }
}
