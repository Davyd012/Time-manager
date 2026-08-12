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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import org.examples.time_manager.R
import org.examples.time_manager.features.components.MonthDaysList
import org.examples.time_manager.features.month_view.data.MonthViewState
import org.examples.time_manager.features.month_view.data.MonthViewIntent
import org.examples.time_manager.features.month_view.data.MonthViewIntent.CreateExcelDocument
import org.examples.time_manager.features.month_view.data.MonthViewIntent.SelectDay
import org.examples.time_manager.navigation.Navigator
import org.examples.time_manager.ui.theme.arrowLeftIcon

@Composable
fun HeaderWidget(
    state: MonthViewState,
    listState: LazyListState,
    onIntent: (MonthViewIntent) -> Unit,
    navigator: Navigator,
) {
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography

    var showMonthPicker by remember { mutableStateOf(false) }

    val months = stringArrayResource(R.array.months_array).toList()

    val context = LocalContext.current
    val saveFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("MonthPickerViewModel", "Got a result")
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data?.getStringExtra("stringKey")?.toInt()
                Log.d("MonthPickerViewModel", "Got a result $data")
                result.data?.data?.let { uri ->
                    onIntent(CreateExcelDocument(uri.toString()))
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
        showMonthPicker = false
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

    Column(
        modifier = Modifier
            .clip(
                if (isExpanded) RectangleShape else MaterialTheme.shapes.large
            )
            .background(colors.primary)
            .statusBarsPadding()
            .padding(10.dp)
            .then(if (isExpanded) Modifier.height(animatedHeight) else Modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable {
                    isExpanded = true
                }
        ) {
            Spacer(modifier = Modifier.width(7.dp))
            IconButton(
                onClick = { navigator.goBack() },
                content = {
                    Icon(
                        arrowLeftIcon(),
                        contentDescription = stringResource(R.string.back_cd),
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
////                vm.onIntent(MonthViewIntent.ModifyWorkState(show = true))
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
            selectDay = { index: Int -> onIntent(SelectDay(index + 1)) }
        )
    }
}
