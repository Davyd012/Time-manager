package org.examples.time_manager.features.root.presentation.home.components

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.examples.time_manager.features.root.HomeState
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.RootScreenEvents.CreateExcelDocumentEvent
import org.examples.time_manager.ui.theme.exportIcon

@Composable
fun HeaderWidget(
    state: HomeState,
    listState: LazyListState,
    vm: HomeViewModel,
    showNewCategoryModal: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val texts = MaterialTheme.typography

    val context = LocalContext.current
    val saveFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    vm.onEvent(CreateExcelDocumentEvent(context, uri))
                }
            }
        }

    val openSaveFilePicker = remember {
        {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_TITLE, "sample_data.xlsx")  // Suggest a file name
            }
            saveFileLauncher.launch(intent)
        }
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
            //        Box {
            //            Text(
            //                "Bedriften Min AS",
            //                style = style.bodyLarge.copy(color = colors.onPrimaryContainer)
            //            )
            //        }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                state.today.weekDay,
                style = texts.headlineMedium.copy(color = colors.onPrimary)
            )
            Icon(
                Icons.Default.KeyboardArrowDown, contentDescription = null,
                tint = colors.onPrimary,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = openSaveFilePicker) {
                Icon(
                    exportIcon(),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            IconButton(onClick = { showNewCategoryModal() }) {
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