package org.examples.time_manager.features.root.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.features.root.data.HomeIntent
import org.examples.time_manager.features.root.data.HomeIntent.ModifyExportProjects
import org.examples.time_manager.features.root.presentation.home.new_work.ListOfProjects
import java.time.LocalDate
import kotlin.reflect.KFunction1

@Composable
fun MonthPickerDialog(
    onDismiss: (Int) -> Unit,
    onIntent: KFunction1<HomeIntent, Unit>,
    projectValues: List<Project>
) {
    val currentMonth = LocalDate.now().month.ordinal

    val months = stringArrayResource(org.examples.time_manager.R.array.months_array).toList()

    val state = rememberLazyListState(1200 + currentMonth - 2)

    var displayIndices by remember { mutableStateOf((0..7).toList()) }
    val projects = projectValues

    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .collect { visible ->
//                Log.d("LaunchedEffectViewModel", "Visible first item:$visible")
//                Log.d("LaunchedEffectViewModel", "Size: " + (-4..4).toList().size)
                displayIndices = (-4..4).map { (visible + 2 + it).mod(12) }
            }
    }

    var lastSelectedIndex by remember { mutableIntStateOf(0) }
    val numberOfDisplayedItems = 9
    val itemHeight = 35.dp
    val itemHalfHeight = LocalDensity.current.run { itemHeight.toPx() / 2f }

    var exportProjects by remember { mutableStateOf(emptyList<Project>()) }

    AlertDialog(
        onDismissRequest = { onDismiss(-1) },
        title = { Text(stringResource(org.examples.time_manager.R.string.select_month_title)) },
        text = {
            Column {
                ListOfProjects(
                    projects,
                    exportProjects.map { it.id },
                    selectProject = { projectId: Int ->
                        val selected = exportProjects.firstOrNull { it.id == projectId }
                        exportProjects =
                            if (selected != null) exportProjects.filter { it.id != projectId }
                            else exportProjects.plus(projects.first { it.id == projectId })
                        onIntent(ModifyExportProjects(projects.first { it.id == projectId }))
                    },
                )
                LazyColumn(
                    modifier = Modifier
                        .height(250.dp)
                        .width(300.dp),
                    state = state,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    items(count = Int.MAX_VALUE) { i ->
                        val item = months[i % months.size]
                        Text(
                            text = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss(lastSelectedIndex % 12)
                                }
                                .padding(itemHeight / 2f)
                                .onGloballyPositioned { coordinates ->
                                    val y = coordinates.positionInParent().y - itemHalfHeight
                                    val parentHalfHeight = (itemHalfHeight * numberOfDisplayedItems)
                                    val isSelected =
                                        (y > parentHalfHeight - itemHalfHeight && y < parentHalfHeight + itemHalfHeight)
                                    val index = i - 1
                                    if (isSelected && lastSelectedIndex != index) {
                                        //                                        onItemSelected(index % itemsState.size, item)
                                        lastSelectedIndex = index
                                    }
                                },
                            textAlign = TextAlign.Center,
                            fontSize = if (lastSelectedIndex == i) 20.sp else 16.sp,
                            fontWeight = if (lastSelectedIndex == i) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss(lastSelectedIndex % 12)
            }) { Text(stringResource(org.examples.time_manager.R.string.ok)) }
        }
    )
}
