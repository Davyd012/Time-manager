package no.timnor.app.features.sharedhours

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import no.timnor.app.R
import no.timnor.app.shared.ui.theme.timnorSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SharedWorkDateControl(value: String, onDateSelected: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val date = remember(value) { runCatching { LocalDate.parse(value) }.getOrNull() }
    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            Text(stringResource(R.string.plannor_shared_time_date))
            Text(
                date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(context.resources.configuration.locales[0])) ?: value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.plannor_shared_time_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.plannor_import_cancel))
                }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SharedDurationControl(valueMinutes: String, onDurationSelected: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val minutes = valueMinutes.toIntOrNull()?.coerceIn(1, 1439) ?: 30
    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            Text(stringResource(R.string.plannor_shared_time_duration))
            Text(durationLabel(minutes))
        }
    }
    if (showPicker) {
        val state = rememberTimePickerState(
            initialHour = minutes / 60,
            initialMinute = minutes % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.plannor_shared_time_duration)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMinutes = state.hour * 60 + state.minute
                    if (selectedMinutes > 0) onDurationSelected(selectedMinutes.toString())
                    showPicker = false
                }) { Text(stringResource(R.string.plannor_shared_time_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.plannor_import_cancel))
                }
            },
        )
    }
}

@Composable
private fun durationLabel(minutes: Int): String = when {
    minutes >= 60 -> stringResource(R.string.plannor_shared_time_duration_hours, minutes / 60, minutes % 60)
    else -> stringResource(R.string.plannor_shared_time_duration_minutes, minutes)
}

/** Duration picker for existing records, which may exceed the 24-hour clock range. */
@Composable
internal fun ExistingDurationControl(seconds: Long, onDurationSelected: (Long) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            Text(stringResource(R.string.plannor_shared_time_duration))
            Text(durationText(seconds))
        }
    }
    if (showPicker) {
        var hours by remember(seconds) { mutableStateOf((seconds / 3600).toString()) }
        var minutes by remember(seconds) { mutableStateOf(((seconds % 3600) / 60).toString()) }
        val selected = hours.toLongOrNull()?.let { h -> minutes.toLongOrNull()?.takeIf { it in 0..59 }?.let { h * 3600 + it * 60 } }
        AlertDialog(onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.plannor_shared_time_duration)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                    OutlinedTextField(value = hours, onValueChange = { hours = it.filter(Char::isDigit).take(6) },
                        label = { Text(stringResource(R.string.duration_hours)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = minutes, onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.duration_minutes)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        isError = minutes.toIntOrNull()?.let { it > 59 } == true)
                }
            },
            confirmButton = { TextButton(onClick = { selected?.let(onDurationSelected); showPicker = false },
                enabled = selected != null && selected in 1..9_999_999_999L) { Text(stringResource(R.string.plannor_shared_time_confirm)) } },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.plannor_import_cancel)) } })
    }
}
