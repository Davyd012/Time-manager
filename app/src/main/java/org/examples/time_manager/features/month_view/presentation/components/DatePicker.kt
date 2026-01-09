package org.examples.time_manager.features.month_view.presentation.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerWidget() {
    val dateState = rememberDatePickerState()
    DatePicker(
        state = dateState
    )
}