package org.examples.time_manager.features.month_view.presentation.components

import android.app.TimePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.examples.time_manager.core.service.util.pad

@Composable
fun getTimePicker(updateTime: (String) -> Unit): TimePickerDialog {
    val context = LocalContext.current

    return TimePickerDialog(
        context,
        { _, mHour: Int, mMinute: Int ->
            updateTime("${mHour.pad()}:${mMinute.pad()}")
        }, 0, 0, true
    )
}