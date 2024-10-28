package org.examples.time_manager.features.root.presentation.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.RootScreenEvents
import org.examples.time_manager.features.root.presentation.home.components.getTimePicker
import org.examples.time_manager.features.root.presentation.utils.DateUtils
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorkInput(onDismiss: () -> Unit, vm: HomeViewModel, projects: List<Project>, day: Int) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var selectedProject by remember { mutableIntStateOf(projects.firstOrNull()?.id ?: 0) }
    var countHours by remember { mutableIntStateOf(0) }

    val currentDate = LocalDateTime.now()
    val dateUtils = DateUtils()
    val dateState = rememberDatePickerState()

    val millisToLocalDate: LocalDateTime = dateState.selectedDateMillis?.let {
        dateUtils.convertMillisToLocalDate(it / 1000)
    } ?: LocalDateTime.of(currentDate.year, currentDate.month, day, 0, 0)

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

    var time by remember { mutableStateOf("00:00") }
    val timePickerDialog = getTimePicker(updateTime = { it: String -> time = it })

    val scrollState = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surface,
        contentColor = colors.onSurface,
        shape = RoundedCornerShape(30.dp),
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
                    style = typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                )
            }
            Spacer(modifier = Modifier.height(5.dp))

            Text("Prosjekt", style = typography.titleMedium.copy(color = colors.onSurface))
            Spacer(modifier = Modifier.height(5.dp))
            Row(modifier = Modifier.horizontalScroll(state = scrollState)) {
                projects.forEachIndexed { i, it ->
                    val paddingModifier = Modifier.padding(
                        start = if (i == projects.size) 5.dp else 0.dp,
                        top = 5.dp,
                        end = 7.dp,
                        bottom = 5.dp
                    )

                    Text(
                        text = it.name,
                        style = typography.titleMedium.copy(color = if (it.id == selectedProject) colors.onPrimaryContainer else colors.onSecondaryContainer),
                        modifier = paddingModifier
                            .clip(
                                RoundedCornerShape(5.dp)
                            )
                            .clickable { selectedProject = it.id }
                            .background(if (it.id == selectedProject) colors.primaryContainer else colors.secondaryContainer)
                            .padding(15.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            Text("Timer", style = typography.titleMedium.copy(color = colors.onSurface))
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                time,
                style = typography.titleSmall.copy(color = colors.onSurface),
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.primaryContainer.copy(alpha = 0.7f))
                    .clickable { timePickerDialog.show() }
                    .padding(15.dp),
            )
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Icon(
//                    imageVector = Icons.Default.Delete,
//                    contentDescription = "Minus hours",
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(25.dp))
//                        .background(colors.secondaryContainer)
//                        .clickable { if (countHours > 0) countHours-- }
//                        .padding(10.dp)
//                        .size(25.dp),
//                )
//                Spacer(modifier = Modifier.width(5.dp))
//                InfoTextField(
//                    countHours.toString(), "", { countHours = it.toInt() }, 1,
//                    background = colors.surface,
//                    modifier = Modifier.width(70.dp),
//                    intInput = true,
//                )
//                Spacer(modifier = Modifier.width(5.dp))
//                Icon(
//                    imageVector = Icons.Default.Add,
//                    contentDescription = "Add hours",
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(25.dp))
//                        .background(colors.secondaryContainer)
//                        .clickable { countHours++ }
//                        .padding(10.dp)
//                        .size(30.dp),
//                )
//            }
            Spacer(modifier = Modifier.height(10.dp))

            Text("Date", style = typography.titleMedium.copy(color = colors.onSurface))
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                dateUtils.formatLocalDateTime(millisToLocalDate),
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
                            hours = time.split(":").let {
                                it[0].toInt() * 3600 + it[1].toInt() * 60
                            },
                            date = millisToLocalDate
                        )
                    )
                },
                colors = ButtonDefaults.elevatedButtonColors()
                    .copy(containerColor = colors.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Ferdig",
                    modifier = Modifier.padding(10.dp),
                    style = typography.titleMedium.copy(color = colors.onPrimary)
                )
            }
        }
    }
}