package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.done
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WorkEntryDialogs(
    date: String,
    onDateChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    hours: String,
    onHoursChange: (String) -> Unit,
    minutes: String,
    onMinutesChange: (String) -> Unit,
    showDatePicker: Boolean,
    onDismissDatePicker: () -> Unit,
    showTimePicker: Boolean,
    onDismissTimePicker: () -> Unit,
    showDurationPicker: Boolean,
    onDismissDurationPicker: () -> Unit,
) {
    if (showDatePicker) {
        val initialDate = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate(2000, 1, 1) }
        key(date) {
            val pickerState = androidx.compose.material3.rememberDatePickerState(
                initialSelectedDateMillis = initialDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            )
            DatePickerDialog(
                onDismissRequest = onDismissDatePicker,
                confirmButton = {
                    TextButton(
                        onClick = {
                            pickerState.selectedDateMillis?.let { selectedMillis ->
                                onDateChange(
                                    Instant.fromEpochMilliseconds(selectedMillis)
                                        .toLocalDateTime(TimeZone.UTC).date.toString(),
                                )
                            }
                            onDismissDatePicker()
                        },
                        enabled = pickerState.selectedDateMillis != null,
                    ) { Text(stringResource(Res.string.done)) }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDatePicker) {
                        Text(stringResource(Res.string.cancel))
                    }
                },
            ) {
                DatePicker(state = pickerState)
            }
        }
    }

    if (showTimePicker) {
        val parsedTime = runCatching { LocalTime.parse(startTime) }
            .getOrElse { LocalTime(7, 0) }
        key(startTime) {
            WorkTimePickerDialog(
                initialHour = parsedTime.hour,
                initialMinute = parsedTime.minute,
                onDismiss = onDismissTimePicker,
                onConfirm = { hour, minute ->
                    onStartTimeChange("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}")
                    onDismissTimePicker()
                },
            )
        }
    }

    if (showDurationPicker) {
        key(hours, minutes) {
            WorkTimePickerDialog(
                initialHour = hours.toIntOrNull()?.coerceIn(0, 23) ?: 0,
                initialMinute = minutes.toIntOrNull()?.coerceIn(0, 59) ?: 0,
                onDismiss = onDismissDurationPicker,
                onConfirm = { hour, minute ->
                    onHoursChange(hour.toString())
                    onMinutesChange(minute.toString())
                    onDismissDurationPicker()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val pickerState = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                Text(stringResource(Res.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        text = { TimePicker(state = pickerState) },
    )
}
