package org.examples.time_manager.features.root.presentation.home

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.Today
import org.examples.time_manager.features.root.presentation.home.components.HeaderInformation
import org.examples.time_manager.features.root.presentation.home.components.ListOfWorks
import org.examples.time_manager.features.root.presentation.home.components.MonthDaysList

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainPage(vm: HomeViewModel, modifier: Modifier) {
    val style = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    val state = vm.state.collectAsState().value
    val works by state.workQueries.collectAsState(initial = emptyList())
    val projects by state.projects.collectAsState(initial = emptyList())
    Log.d("HomeViewModel", "List of projects: $projects")

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val index = if (state.selectedDay > 4) state.selectedDay - 4 else state.selectedDay
            listState.scrollToItem(index)
        }
    }

    var showNewCategoryModal by remember { mutableStateOf(false) }

    if (showNewCategoryModal) {
        NewWorkInput(onDismiss = { showNewCategoryModal = false }, vm = vm, projects = projects)
    }

    Column(modifier = modifier.background(colors.surface)) {
        HeaderInformation(showNewCategoryModal = { showNewCategoryModal = true })
        Box(modifier = Modifier.padding(start = 15.dp)) {
            Text(
                state.today.weekDay,
                style = style.titleMedium.copy(color = colors.onPrimaryContainer)
            )
        }
        Row {
            Spacer(modifier = Modifier.width(15.dp))
            Box {
                Text(
                    getDatString(state.today),
                    style = style.titleMedium.copy(color = colors.onPrimaryContainer)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box {
                Text("I dag", style = style.titleMedium.copy(color = colors.tertiary))
            }
            Spacer(modifier = Modifier.width(15.dp))
        }
        Spacer(modifier = Modifier.height(5.dp))
        MonthDaysList(listState, state, vm)
        Spacer(modifier = Modifier.height(5.dp))

        ListOfWorks(works = works, projects = projects)
    }
}


private fun getDatString(today: Today) =
    today.day.toString() + ". " + today.month.lowercase() + " " + today.year