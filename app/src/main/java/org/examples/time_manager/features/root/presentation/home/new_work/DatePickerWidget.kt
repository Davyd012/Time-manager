package org.examples.time_manager.features.root.presentation.home.new_work

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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

    if (!showDatePicker) return

    Log.d("NewWordInputViewModel", "Showing a date range picker")
    if (!updateRange) {
        DatePickerDialog(
            onDismissRequest = onDismissDate,
            confirmButton = { /*TODO*/ }) {
            DatePicker(
                state = dateState,
                showModeToggle = true
            )
        }
        return
    }

    DatePickerDialog(
        onDismissRequest = onDismissDate,
        confirmButton = {
            TextButton(
                onClick = onDismissDate
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissDate) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = state,
            title = {
                Text(
                    text = "Select date range"
                )
            },
            showModeToggle = false,
            modifier = Modifier
                .height(500.dp)
        )
    }
}