package org.examples.time_manager.features.root.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.focus.onFocusChanged
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
import org.examples.time_manager.ui.theme.dateRangeIcon
import org.examples.time_manager.ui.theme.timerIcon
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

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

    val dateContent = when {
        !updateRange -> millisToLocalDate.formatDate()
        datesRange.isEmpty() -> "Velg datoer"
        datesRange.size == 1 -> datesRange.first().formatDate()
        else -> "${datesRange.first().formatDate()} / ${datesRange.last().formatDate()}"
    }

    val dateHeaderText = when {
        !updateRange -> millisToLocalDate.formatLongDate()
        datesRange.isEmpty() -> "Velg datoer"
        datesRange.size == 1 -> datesRange.first().formatLongDate()
        else -> "${datesRange.first().formatDate()} - ${datesRange.last().formatDate()}"
    }

    val formattedTime = work?.let {
        val hours = (it.time / 3600).pad()
        val minutes = ((it.time % 3600) / 60).pad()
        "$hours:$minutes"
    } ?: "00:00"

    var time by remember { mutableStateOf(formattedTime) }
    var notes by remember { mutableStateOf(work?.description ?: "") }
    var notesFocused by remember { mutableStateOf(false) }
    val timePickerDialog = getTimePicker(updateTime = { it: String -> time = it })

    val formattedStartTime = work?.let {
        val hours = it.date.hour.pad()
        val minutes = it.date.minute.pad()
        "$hours:$minutes"
    }
    var startedJob by remember { mutableStateOf(formattedStartTime) }
    val startTimePickerDialog = getTimePicker(
        updateTime = { it: String -> startedJob = it }
    )

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        shape = sheetShape,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(colors.surfaceContainerHigh)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .heightIn(min = 500.dp)
        ) {
            Text(
                text = if (updateRange) "Periode" else "Dato",
                style = typography.labelLarge.copy(color = colors.onSurfaceVariant)
            )
            Text(
                text = dateHeaderText,
                style = typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Registrere timer",
                style = typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Prosjekt", style = typography.titleSmall.copy(color = colors.onSurfaceVariant))
            Spacer(modifier = Modifier.height(6.dp))
            ListOfProjects(
                projects,
                listOf(selectedProject),
                selectProject = { it: Int -> selectedProject = it },
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Tid & detaljer", style = typography.titleSmall.copy(color = colors.onSurfaceVariant))
            Spacer(modifier = Modifier.height(6.dp))
            AnimatedVisibility(visible = !updateRange) {
                Column {
                    OutlinedButton(
                        value = time,
                        text = "Timer",
                        icon = timerIcon(),
                        action = { timePickerDialog.show() },
                        isPrimary = true,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            OutlinedButton(
                value = dateContent,
                text = if (updateRange) "Periode" else "Dag",
                icon = dateRangeIcon(),
                action = { showDatePicker = true },
            )

            AnimatedVisibility(visible = !updateRange) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        value = startedJob ?: "00:00",
                        text = "Start arbeidet",
                        icon = timerIcon(),
                        action = { startTimePickerDialog.show() },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = {
                    Text("Kommentar", color = colors.onSurfaceVariant)
                },
                placeholder = {
                    Text("Skriv en kommentar...", color = colors.onSurfaceVariant)
                },
                minLines = if (notesFocused || notes.isNotBlank()) 4 else 2,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.outlineVariant,
                    unfocusedBorderColor = colors.outlineVariant,
                    focusedContainerColor = colors.surfaceContainerLow,
                    unfocusedContainerColor = colors.surfaceContainerLow,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { notesFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Periode registrering",
                    style = typography.titleSmall.copy(color = colors.onSurface)
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = updateRange,
                    onCheckedChange = { updateRange = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onPrimary,
                        checkedTrackColor = colors.primary,
                        uncheckedThumbColor = colors.onSurfaceVariant,
                        uncheckedTrackColor = colors.surfaceVariant,
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            SaveButtons(
                work = work,
                onDismiss = onDismiss,
                vm = vm,
                selectedProject = selectedProject,
                time = time,
                millisToLocalDate = millisToLocalDate,
                startedJob = startedJob,
                notes = notes,
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

private fun LocalDateTime.formatLongDate(): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.getDefault())
    return format(formatter)
}
