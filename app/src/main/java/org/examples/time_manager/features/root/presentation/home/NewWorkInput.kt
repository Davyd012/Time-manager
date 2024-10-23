package org.examples.time_manager.features.root.presentation.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.RootScreenEvents
import org.examples.time_manager.features.root.presentation.newProject.InfoTextField
import org.examples.time_manager.features.root.presentation.utils.DateUtils
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorkInput(onDismiss: () -> Unit, vm: HomeViewModel, projects: List<Project>) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var selectedProject by remember { mutableIntStateOf(0) }
    var countHours by remember { mutableIntStateOf(0) }


    val dateState = rememberDatePickerState()
    val millisToLocalDate = dateState.selectedDateMillis?.let {
        DateUtils().convertMillisToLocalDate(it)
    }
//    val dateToString = millisToLocalDate?.let {
//        DateUtils().dateToString(millisToLocalDate)
//    } ?: "Choose date"

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { /*TODO*/ }) {
            DatePicker(
                state = dateState,
                showModeToggle = true
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color.Transparent,
        contentColor = colors.onSurface,
        shape = RectangleShape,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(colors.surface)
                .padding(10.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Registrere timer",
                    style = typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                )
            }
            Spacer(modifier = Modifier.height(5.dp))

            Text("Prosjekt", style = typography.titleMedium.copy(color = colors.onSurface))
            Spacer(modifier = Modifier.height(5.dp))
            projects.forEachIndexed { i, it ->
                Text(
                    text = it.name,
                    style = typography.titleMedium.copy(color = colors.onSecondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(
                            RoundedCornerShape(5.dp)
                        ).clickable { selectedProject = i }
                        .background(if (i == selectedProject) colors.primaryContainer else colors.secondaryContainer)
                        .padding(15.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Text("Timer", style = typography.titleMedium.copy(color = colors.onSurface))
            Spacer(modifier = Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Minus hours",
                    modifier = Modifier
                        .clip(RoundedCornerShape(25.dp))
                        .background(colors.secondaryContainer)
                        .clickable { if (countHours > 0) countHours-- }
                        .padding(10.dp)
                        .size(25.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                InfoTextField(
                    countHours.toString(), "", { countHours = it.toInt() }, 1,
                    background = colors.inverseSurface,
                    modifier = Modifier.width(70.dp),
                    intInput = true,
                )
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add hours",
                    modifier = Modifier
                        .clip(RoundedCornerShape(25.dp))
                        .background(colors.secondaryContainer)
                        .clickable { countHours++ }
                        .padding(10.dp)
                        .size(30.dp),
                )
            }

            Text("Date", style = typography.titleMedium.copy(color = colors.onSurface))
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                millisToLocalDate.toString(),
                style = typography.titleSmall.copy(color = colors.onSurface),
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.primaryContainer.copy(alpha = 0.7f))
                    .clickable { showDatePicker = true }
                    .padding(15.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    onDismiss()
                    vm.onEvent(
                        RootScreenEvents.WriteWorkEvent(
                            project = selectedProject,
                            hours = countHours * 60,
                            date = millisToLocalDate ?: LocalDateTime.now()
                        )
                    )
                },
                colors = ButtonDefaults.elevatedButtonColors()
                    .copy(containerColor = colors.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Ferdig",
                    modifier = Modifier.padding(10.dp),
                    style = typography.titleMedium.copy(color = colors.onSecondaryContainer)
                )
            }

        }
    }
}