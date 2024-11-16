package org.examples.time_manager.features.root.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
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
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.service.util.pad
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.presentation.home.components.OutlinedButton
import org.examples.time_manager.features.root.presentation.home.components.getTimePicker
import org.examples.time_manager.features.root.presentation.home.new_work.DatePickerWidget
import org.examples.time_manager.features.root.presentation.home.new_work.ListOfProjects
import org.examples.time_manager.features.root.presentation.home.new_work.SaveButtons
import org.examples.time_manager.features.root.presentation.utils.DateUtils
import org.examples.time_manager.ui.theme.timerIcon
import java.time.LocalDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorkInput(
    onDismiss: () -> Unit,
    vm: HomeViewModel,
    projects: List<Project>,
    day: Int,
    work: Work?
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var selectedProject by remember {
        mutableIntStateOf(
            work?.project ?: projects.firstOrNull()?.id ?: 0
        )
    }

    val currentDate = LocalDateTime.now().withDayOfMonth(day).withHour(0).withMinute(0)
    val dateUtils = DateUtils()
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = work?.date?.toEpochSecond(ZoneOffset.UTC)?.times(1000)
    )

    val millisToLocalDate: LocalDateTime = dateState.selectedDateMillis?.let {
        dateUtils.convertMillisToLocalDate(it / 1000)
    } ?: currentDate

    var showDatePicker by remember { mutableStateOf(false) }


    val state = rememberDateRangePickerState()
    val datesRange = state.let {
        if (it.selectedEndDateMillis == null || it.selectedStartDateMillis == null) emptyList()
        else extractDates(dateUtils, it)
    }


    var updateRange by remember { mutableStateOf(false) }
    DatePickerWidget(
        showDatePicker = showDatePicker,
        updateRange = updateRange,
        onDismissDateRange = { updateRange = false },
        onDismissDate = { showDatePicker = false },
        state = state,
        dateState = dateState,
    )

    val formattedTime = work?.let {
        val hours = (it.time / 3600).pad()
        val minutes = ((it.time % 3600) / 60).pad()
        "$hours:$minutes"
    } ?: "00:00"

    var time by remember { mutableStateOf(formattedTime) }
    val timePickerDialog = getTimePicker(updateTime = { it: String -> time = it })

    val formattedStartTime = work?.let {
        val hours = it.date.hour.pad()
        val minutes = it.date.minute.pad()
        "$hours:$minutes"
    } ?: "00:00"
    var startedJob by remember { mutableStateOf(formattedStartTime) }
    val startTimePickerDialog = getTimePicker(updateTime = { it: String ->
        startedJob = it
    })

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
                .heightIn(min = 350.dp)
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
            ListOfProjects(
                projects,
                selectedProject,
                selectProject = { it: Int -> selectedProject = it },
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text("Tid & dato", style = typography.titleMedium.copy(color = colors.onSurface))
            Spacer(modifier = Modifier.height(5.dp))
            OutlinedButton(
                value = time,
                text = "Timer",
                icon = timerIcon(),
                action = { timePickerDialog.show() },
            )

            Spacer(modifier = Modifier.height(5.dp))
            val dateContent = when {
                updateRange.not() -> millisToLocalDate.formatDate()
                datesRange.isEmpty() || datesRange.size == 1 -> "Velg datoer"
                else -> "${datesRange.first().formatDate()} / ${datesRange.last().formatDate()}"
            }
            OutlinedButton(
                value = dateContent,
                text = "Dag",
                icon = Icons.Default.DateRange,
                action = { showDatePicker = true },
            )

            Spacer(modifier = Modifier.height(5.dp))
            OutlinedButton(
                value = startedJob,
                text = "Start arbeidet",
                icon = timerIcon(),
                action = { startTimePickerDialog.show() },
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { updateRange = !updateRange }
            ) {
                Box {
                    Checkbox(
                        checked = updateRange,
                        onCheckedChange = { updateRange = !updateRange },
                        enabled = true,
                        colors = CheckboxDefaults.colors(colors.onPrimaryContainer),
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .size(3.dp),
                    )
                }
                Text(
                    text = "Periode registrering",
                    modifier = Modifier
                        .padding(10.dp)
                        .padding(start = 15.dp),
                    style = typography.titleSmall.copy(color = colors.onSurface)
                )
            }

            Spacer(modifier = Modifier.height(50.dp))
            SaveButtons(
                work,
                onDismiss,
                vm,
                selectedProject,
                time,
                millisToLocalDate,
                startedJob,
                dates = datesRange,
                updateRange = updateRange,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun extractDates(
    dateUtils: DateUtils,
    it: DateRangePickerState
): List<LocalDateTime> {
    val startDate = dateUtils.convertMillisToLocalDate(it.selectedStartDateMillis!! / 1000)
    val endDate = dateUtils.convertMillisToLocalDate(it.selectedEndDateMillis!! / 1000)

    var current = startDate
    var rangeOfDates = emptyList<LocalDateTime>()

    while (!current.isAfter(endDate)) {
        rangeOfDates = rangeOfDates.plus(current)
        current = current.plusDays(1)
    }
    return rangeOfDates
}

fun LocalDateTime.formatDate(): String {
    val dateUtils = DateUtils()
    return dateUtils.formatLocalDateTime(this)
}