package org.examples.time_manager.features.root.presentation.home.new_work

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.examples.time_manager.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DatePickerWidget(
    showDatePicker: Boolean,
    updateRange: Boolean,
    onDismissDateRange: () -> Unit,
    onDismissDate: () -> Unit,
    state: DateRangePickerState,
    dateState: DatePickerState
) {
    val colors = MaterialTheme.colorScheme

    if (!showDatePicker) return

    if (!updateRange) {
        DatePickerDialog(
            onDismissRequest = onDismissDate,
            confirmButton = {
                TextButton(
                    onClick = onDismissDate,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryContainer)
                ) {
                    Text(stringResource(R.string.ok), color = colors.onSecondaryContainer)
                }
            },
        ) {
            DatePicker(
                state = dateState,
                showModeToggle = true,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = colors.secondaryContainer,
                    selectedDayContentColor = colors.onSecondaryContainer,
                    todayContentColor = colors.secondaryContainer,
                    todayDateBorderColor = colors.onSecondaryContainer,
                )
            )
        }
        return
    }

    Log.d("NewWordInputViewModel", "Showing a date range picker")
    DatePickerDialog(
        onDismissRequest = onDismissDate,
        confirmButton = {
            TextButton(
                onClick = onDismissDate,
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryContainer)
            ) {
                Text(stringResource(R.string.ok), color = colors.onSecondaryContainer)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissDate,
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryContainer)
            ) {
                Text(stringResource(R.string.cancel), color = colors.onSecondaryContainer)
            }
        }
    ) {
        DateRangePicker(
            state = state,
            title = {
                Text(
                    text = stringResource(R.string.select_date_range_title),
                    style = MaterialTheme.typography.bodyLarge.copy(color = colors.onPrimary),
                    modifier = Modifier.padding(10.dp)
                )
            },
            showModeToggle = false,
            modifier = Modifier
                .height(500.dp),
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = colors.secondaryContainer,
                selectedDayContentColor = colors.onSecondaryContainer,
                todayContentColor = colors.secondaryContainer,
                todayDateBorderColor = colors.onSecondaryContainer,
            )
        )
    }
}