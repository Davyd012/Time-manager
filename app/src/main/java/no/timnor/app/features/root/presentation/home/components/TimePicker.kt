package no.timnor.app.features.root.presentation.home.components

import android.app.TimePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import no.timnor.app.core.service.util.pad

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