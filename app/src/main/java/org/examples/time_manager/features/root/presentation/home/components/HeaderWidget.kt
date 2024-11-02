package org.examples.time_manager.features.root.presentation.home.components

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import org.examples.time_manager.features.root.presentation.home.MonthSelectorDialog
import org.examples.time_manager.ui.theme.exportIcon

@Composable
fun HeaderWidget(
    state: HomeState,
    listState: LazyListState,
    vm: HomeViewModel,
) {
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography

    var showMonthPicker by remember { mutableStateOf(false) }

    if (showMonthPicker) {
        MonthSelectorDialog(
            selectedMonth = "March",
            onMonthSelected = { a: String -> Log.d("MainPageViewModel", a) },
            onDismiss = { showMonthPicker = false },
            vm = vm,
        )
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(bottomEnd = 30.dp, bottomStart = 30.dp))
            .background(colors.primary)
            .padding(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.padding(start = 2.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                state.today.weekDay,
                style = texts.headlineSmall.copy(color = colors.onPrimary)
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