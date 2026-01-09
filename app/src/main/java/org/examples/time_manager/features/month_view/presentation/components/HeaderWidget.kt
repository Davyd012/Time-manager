package org.examples.time_manager.features.month_view.presentation.components

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.examples.time_manager.App
import org.examples.time_manager.features.components.MonthDaysList
import org.examples.time_manager.features.month_view.MonthViewModel
import org.examples.time_manager.features.month_view.data.MonthViewState
import org.examples.time_manager.features.root.data.RootScreenEvents.SelectDayEvent
import org.examples.time_manager.navigation.Navigator
import org.examples.time_manager.ui.theme.exportIcon

@Composable
fun HeaderWidget(
    state: MonthViewState,
    listState: LazyListState,
    vm: MonthViewModel,
    navigator: Navigator,
) {
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography

    var showMonthPicker by remember { mutableStateOf(false) }

    val months = remember {
        listOf(
            "Januar", "Februar", "Mars", "April", "Mai", "Juni",
            "Juli", "August", "September", "Oktober", "November", "Desember"
        )
    }

    val context = LocalContext.current
    val saveFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("MonthPickerViewModel", "Got a result")
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.getStringExtra("stringKey")?.toInt()
                Log.d("MonthPickerViewModel", "Got a result $data")
                result.data?.data?.let { uri ->
                    TODO("Create excel document function")
//                    vm.onEvent(
//                        CreateExcelDocumentEvent(context, uri, data)
//                    )
                }
            }
        }

    val openSaveFilePicker = remember {
        { month: Int ->
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_TITLE, "${months.elementAt(month)}.xlsx")
                putExtra("stringKey", month)
            }
            saveFileLauncher.launch(intent)
        }
    }

    if (showMonthPicker) {
        TODO("Create month picker")
//        MonthPickerDialog(
//            onDismiss = { month: Int ->
//                if (month >= 0) {
//                    openSaveFilePicker(month)
//                }
//                showMonthPicker = false
//            },
//            onEvent = vm::onEvent,
//            projectValues = state.projects,
//        )
    }

    var isExpanded by remember { mutableStateOf(false) }
    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) 1000.dp else 100.dp,
        label = ""
    )

    // Trigger navigation when expanded to full size
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            // Small delay to let animation complete visually before navigating
            delay(100)
            navigator.toCalendar()
        }
    }

    Column(
        modifier = Modifier
            .clip(
                if (isExpanded) RoundedCornerShape(0.dp) else RoundedCornerShape(
                    bottomEnd = 30.dp,
                    bottomStart = 30.dp
                )
            )
            .background(colors.primary)
            .padding(top = App.statusBarHeight)
            .padding(10.dp)
            .then(if (isExpanded) Modifier.height(animatedHeight) else Modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    isExpanded = true
                }
        ) {
            Spacer(modifier = Modifier.width(7.dp))
            IconButton(
                onClick = { navigator.close() },
                content = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                },
            )
            Spacer(modifier = Modifier.weight(1f))
//            IconButton(
//                onClick = { showMonthPicker = true },
//                content = {
//                    Icon(
//                        exportIcon(),
//                        contentDescription = null,
//                        tint = colors.onPrimary,
//                        modifier = Modifier.size(30.dp)
//                    )
//                },
//            )
//            IconButton(
//                onClick = {
//                    TODO("Fix events")
////                vm.onEvent(ModifyWorkStateEvent(show = true))
//                },
//                content = {
//                    Icon(
//                        Icons.Outlined.AddCircle,
//                        contentDescription = null,
//                        tint = colors.onPrimary,
//                        modifier = Modifier.size(30.dp)
//                    )
//                },
//            )
        }

        MonthDaysList(
            listState,
            monthDays = state.dayPerMonth,
            selectedDay = state.selectedDay,
            selectDay = { index: Int -> vm.onEvent(SelectDayEvent(index + 1)) }
        )
    }
}