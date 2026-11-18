package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import no.timnor.app.shared.domain.work.formatWorkDateInput
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.calendar
import no.timnor.app.shared.generated.resources.clock_outline
import no.timnor.app.shared.generated.resources.work_date_hint
import no.timnor.app.shared.generated.resources.work_hours_hint
import no.timnor.app.shared.generated.resources.work_start_time_hint
import no.timnor.app.shared.generated.resources.work_end_time_hint
import no.timnor.app.shared.generated.resources.work_end_next_day
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.DrawableResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkEntryManualFields(
    date: String,
    onDateChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    onDurationChange: (Int, Int) -> Unit,
    hours: String,
    minutes: String,
    periodRegistration: Boolean,
    isEditing: Boolean,
    onOpenDurationPicker: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var endTime by rememberSaveable { mutableStateOf<String?>(null) }
    fun applyDuration(start: String, end: String) {
        val total = durationBetweenWorkTimes(start, end) ?: return
        onDurationChange(total / 60, total % 60)
    }
    val animationsEnabled = rememberSystemAnimationsEnabled()

    androidx.compose.animation.AnimatedVisibility(
        visible = !periodRegistration,
        enter = if (animationsEnabled) {
            androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically()
        } else androidx.compose.animation.EnterTransition.None,
        exit = if (animationsEnabled) {
            androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
        } else androidx.compose.animation.ExitTransition.None,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            WorkEntryPickerField(
                value = "${hours.padStart(2, '0')}:${minutes.padStart(2, '0')}",
                label = stringResource(Res.string.work_hours_hint),
                icon = Res.drawable.clock_outline,
                onClick = { endTime = null; onOpenDurationPicker() },
                modifier = Modifier.testTag("work-duration-picker-field"),
            )
            WorkEntryPickerField(
                value = formatWorkDateInput(date),
                label = stringResource(Res.string.work_date_hint),
                icon = Res.drawable.calendar,
                onClick = { showDatePicker = true },
                modifier = Modifier.testTag("work-date-picker-field"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                WorkTimeField(startTime, stringResource(Res.string.work_start_time_hint),
                    { showTimePicker = true }, Modifier.weight(1f).testTag("work-start-time-picker-field"))
                WorkTimeField(endTime ?: "—", stringResource(Res.string.work_end_time_hint),
                    { showEndTimePicker = true }, Modifier.weight(1f).testTag("work-end-time-picker-field"))
            }
            if (endTime != null && endTime!! < startTime) {
                Text(stringResource(Res.string.work_end_next_day), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    WorkEntryDialogs(
        date = date,
        isEditing = isEditing,
        onDateChange = onDateChange,
        startTime = startTime,
        onStartTimeChange = { start ->
            onStartTimeChange(start)
            endTime?.let { applyDuration(start, it) }
        },
        showDatePicker = showDatePicker,
        onDismissDatePicker = { showDatePicker = false },
        showTimePicker = showTimePicker,
        onDismissTimePicker = { showTimePicker = false },
    )
    if (showEndTimePicker) {
        val initial = endTime ?: startTime
        WorkTimePickerDialog(
            initialHour = initial.substringBefore(":").toIntOrNull() ?: 0,
            initialMinute = initial.substringAfter(":", "0").toIntOrNull() ?: 0,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                val selected = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                endTime = selected
                onStartTimeChange(startTime)
                applyDuration(startTime, selected)
                showEndTimePicker = false
            },
        )
    }
}

internal fun durationBetweenWorkTimes(start: String, end: String): Int? {
    val startTime = runCatching { kotlinx.datetime.LocalTime.parse(start) }.getOrNull() ?: return null
    val endTime = runCatching { kotlinx.datetime.LocalTime.parse(end) }.getOrNull() ?: return null
    val startMinutes = startTime.hour * 60 + startTime.minute
    val endMinutes = endTime.hour * 60 + endTime.minute
    return (endMinutes - startMinutes + 24 * 60) % (24 * 60)
}

@Composable
private fun WorkTimeField(value: String, label: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(androidx.compose.ui.unit.Dp.Hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(MaterialTheme.timnorSpacing.medium)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun WorkEntryPickerField(
    value: String,
    label: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(androidx.compose.ui.unit.Dp.Hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        ListItem(
            headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
            leadingContent = { Icon(painterResource(icon), contentDescription = null) },
            trailingContent = { Text(value, style = MaterialTheme.typography.labelLarge) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        )
    }
}
