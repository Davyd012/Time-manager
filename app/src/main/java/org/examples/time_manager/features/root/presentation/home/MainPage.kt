package org.examples.time_manager.features.root.presentation.home

import android.app.TimePickerDialog
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.Today
import org.examples.time_manager.features.root.presentation.home.components.ListOfWorks
import org.examples.time_manager.features.root.presentation.home.components.HeaderWidget

@Composable
fun MainPage(vm: HomeViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val state = vm.state.collectAsState().value
    val works by state.workQueries.collectAsState(initial = emptyList())
    val projects by state.projects.collectAsState(initial = emptyList())

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val index = if (state.selectedDay > 4) state.selectedDay - 4 else state.selectedDay
            listState.scrollToItem(index)
        }
    }

//    val systemUiController = rememberSystemUiController()
//    systemUiController.setStatusBarColor(color = colors.primary)

    var showNewCategoryModal by remember { mutableStateOf(false) }

    if (showNewCategoryModal) {
        NewWorkInput(
            onDismiss = { showNewCategoryModal = false },
            vm = vm,
            projects = projects,
            day = state.selectedDay
        )
    }

    Column(
        modifier = modifier
            .background(colors.surface)
            .fillMaxSize()
    ) {
        HeaderWidget(state, listState, vm, showNewCategoryModal = { showNewCategoryModal = true })
        Spacer(modifier = Modifier.height(5.dp))
        ListOfWorks(
            works = works,
            projects = projects
        )
    }
}


private fun getDatString(today: Today) =
    today.day.toString() + ". " + today.month.lowercase() + " " + today.year