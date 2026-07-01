package org.examples.time_manager.features.root.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import org.examples.time_manager.App
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.RootScreenEvents.ModifyWorkStateEvent
import org.examples.time_manager.features.root.presentation.home.components.ListOfWorks
import org.examples.time_manager.features.root.presentation.home.components.HeaderWidget
import org.examples.time_manager.navigation.Navigator

@Composable
fun MainPage(vm: HomeViewModel, modifier: Modifier, navigator: Navigator) {
    val colors = MaterialTheme.colorScheme
    val state = vm.state.collectAsState().value
    val works by state.workQueries.collectAsState(initial = emptyList())
    val projects by state.projects.collectAsState(initial = emptyList())

    if (state.selectedWork.showModal) {
        val index = state.selectedWork.selectedWork
        NewWorkInput(
            onDismiss = { vm.onEvent(ModifyWorkStateEvent(show = false)) },
            vm = vm,
            projects = projects,
            day = state.selectedDay,
            work = works.takeIf { index >= 0 }?.elementAt(index)
        )
    }

    Box(
        modifier = modifier
            .background(colors.surface)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(App.statusBarHeight + 170.dp))
            ListOfWorks(
                showWork = { i: Int -> vm.onEvent(ModifyWorkStateEvent(selected = i, show = true)) },
                addWork = { vm.onEvent(ModifyWorkStateEvent(show = true)) },
                works = works,
                projects = projects
            )
        }

        HeaderWidget(
            state = state,
            vm = vm,
            navigator = navigator,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f)
        )
    }
}
