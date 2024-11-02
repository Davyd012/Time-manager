package org.examples.time_manager.features.root.presentation.home

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.RootScreenEvents.CreateExcelDocumentEvent
import java.time.LocalDate

@Composable
fun MonthSelectorDialog(
    selectedMonth: String,
    onMonthSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    vm: HomeViewModel
) {
    val currentMonth = LocalDate.now().month.ordinal

    val months = remember {
        listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }

    val state = rememberLazyListState(1200 + currentMonth - 1)

    var displayIndices by remember { mutableStateOf((0..7).toList()) }

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


    val context = LocalContext.current
    val saveFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("MonthPickerViewModel", "Got a result")
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    vm.onEvent(
                        CreateExcelDocumentEvent(context, uri, lastSelectedIndex % 12 + 1)
                    )
                }
            }
        }

    val openSaveFilePicker = remember {
        { month: String ->
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_TITLE, "$month.xlsx")  // Suggest a file name
            }
            saveFileLauncher.launch(intent)
        }
    }








    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month") },
        text = {
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
//                                4 = displayIndex
//                                onMonthSelected(displayIndex)
                                openSaveFilePicker(months.elementAt(lastSelectedIndex % 12))
                                onDismiss()
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

        },
        confirmButton = { TextButton(onClick = {
            openSaveFilePicker(months.elementAt(lastSelectedIndex % 12))
//            onDismiss()
        }) { Text("OK") } }
    )
}