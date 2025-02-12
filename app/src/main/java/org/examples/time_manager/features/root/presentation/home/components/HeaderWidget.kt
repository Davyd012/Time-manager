package org.examples.time_manager.features.root.presentation.home.components

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.HomeState
import org.examples.time_manager.features.root.data.RootScreenEvents.CreateExcelDocumentEvent
import org.examples.time_manager.features.root.data.RootScreenEvents.ModifyWorkStateEvent
import org.examples.time_manager.features.root.presentation.home.MonthPickerDialog
import org.examples.time_manager.navigation.Navigator
import org.examples.time_manager.ui.theme.exportIcon

@Composable
fun HeaderWidget(
    state: HomeState,
    listState: LazyListState,
    vm: HomeViewModel,
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
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data = result.data?.getStringExtra("stringKey")?.toInt()
                Log.d("MonthPickerViewModel", "Got a result $data")
                result.data?.data?.let { uri ->
                    vm.onEvent(
                        CreateExcelDocumentEvent(context, uri, data)
                    )
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
        MonthPickerDialog(
            onDismiss = { month: Int ->
                if (month >= 0) {
                    openSaveFilePicker(month)
                }
                showMonthPicker = false
            },
            onEvent = vm::onEvent,
            projectValues = state.projects,
        )
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
            kotlinx.coroutines.delay(100)
            navigator.toCalendar()
        }
    }

    Column(
        modifier = Modifier
            .clip(if (isExpanded) RoundedCornerShape(0.dp) else RoundedCornerShape(bottomEnd = 30.dp, bottomStart = 30.dp))
            .background(colors.primary)
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
            Spacer(modifier = Modifier.padding(start = 2.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                state.today.weekDay,
                style = texts.headlineSmall.copy(color = colors.onPrimary),
            )
            Icon(
                Icons.Default.KeyboardArrowDown, contentDescription = null,
                tint = colors.onPrimary,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showMonthPicker = true }) {
                Icon(
                    exportIcon(),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            IconButton(onClick = { vm.onEvent(ModifyWorkStateEvent(show = true)) }) {
                Icon(
                    Icons.Outlined.AddCircle,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        MonthDaysList(
            listState, state, vm
        )
    }
}