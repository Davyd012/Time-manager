package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import no.timnor.app.shared.domain.work.formatWorkDateInput
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.calendar
import no.timnor.app.shared.generated.resources.clock_outline
import no.timnor.app.shared.generated.resources.work_date_hint
import no.timnor.app.shared.generated.resources.work_hours_hint
import no.timnor.app.shared.generated.resources.work_start_time_hint
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkEntryManualFields(
    date: String,
    onDateChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    hours: String,
    onHoursChange: (String) -> Unit,
    minutes: String,
    onMinutesChange: (String) -> Unit,
    periodRegistration: Boolean,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
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
            OutlinedTextField(
                value = "${hours.padStart(2, '0')}:${minutes.padStart(2, '0')}",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
                    .clickable { showDurationPicker = true }
                    .semantics { role = Role.Button },
                singleLine = true,
                label = { Text(stringResource(Res.string.work_hours_hint)) },
                trailingIcon = { Icon(painterResource(Res.drawable.clock_outline), contentDescription = null) },
            )
            OutlinedTextField(
                value = formatWorkDateInput(date),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .semantics { role = Role.Button },
                singleLine = true,
                label = { Text(stringResource(Res.string.work_date_hint)) },
                trailingIcon = { Icon(painterResource(Res.drawable.calendar), contentDescription = null) },
            )
            OutlinedTextField(
                value = startTime,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
                    .clickable { showTimePicker = true }
                    .semantics { role = Role.Button },
                singleLine = true,
                label = { Text(stringResource(Res.string.work_start_time_hint)) },
                trailingIcon = { Icon(painterResource(Res.drawable.clock_outline), contentDescription = null) },
            )
        }
    }

    WorkEntryDialogs(
        date = date,
        onDateChange = onDateChange,
        startTime = startTime,
        onStartTimeChange = onStartTimeChange,
        hours = hours,
        onHoursChange = onHoursChange,
        minutes = minutes,
        onMinutesChange = onMinutesChange,
        showDatePicker = showDatePicker,
        onDismissDatePicker = { showDatePicker = false },
        showTimePicker = showTimePicker,
        onDismissTimePicker = { showTimePicker = false },
        showDurationPicker = showDurationPicker,
        onDismissDurationPicker = { showDurationPicker = false },
    )
}
