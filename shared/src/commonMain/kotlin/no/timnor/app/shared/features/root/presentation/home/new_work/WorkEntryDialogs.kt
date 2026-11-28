package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.done
import no.timnor.app.shared.generated.resources.ok
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkEntryDialogs(
    date: String,
    isEditing: Boolean,
    onDateChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    showDatePicker: Boolean,
    onDismissDatePicker: () -> Unit,
    showTimePicker: Boolean,
    onDismissTimePicker: () -> Unit,
) {
    if (showDatePicker) {
        val initialDate = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate(2000, 1, 1) }
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = if (isEditing) {
                initialDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
            } else {
                null
            },
        )
        val currentDate by rememberUpdatedState(date)
        val currentOnDateChange by rememberUpdatedState(onDateChange)
        LaunchedEffect(pickerState.selectedDateMillis) {
            pickerState.selectedDateMillis?.let { millis ->
                val selectedDate = Instant.fromEpochMilliseconds(millis)
                    .toLocalDateTime(TimeZone.UTC).date.toString()
                if (selectedDate != currentDate) currentOnDateChange(selectedDate)
            }
        }
        DatePickerDialog(
            onDismissRequest = onDismissDatePicker,
            confirmButton = {
                TextButton(onClick = onDismissDatePicker) {
                    Text(stringResource(Res.string.ok))
                }
            },
        ) {
            DatePicker(
                state = pickerState,
                showModeToggle = true,
            )
        }
    }

    if (showTimePicker) {
        key(startTime) {
            WorkTimePickerDialog(
                initialHour = startTime.substringBefore(":").toIntOrNull()?.coerceIn(0, 23) ?: 0,
                initialMinute = startTime.substringAfter(":", "0").toIntOrNull()?.coerceIn(0, 59) ?: 0,
                onDismiss = onDismissTimePicker,
                onConfirm = { hour, minute ->
                    onStartTimeChange("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}")
                    onDismissTimePicker()
                },
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkTimePickerDialog(
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
