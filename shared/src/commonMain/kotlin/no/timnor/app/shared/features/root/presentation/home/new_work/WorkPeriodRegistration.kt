package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import no.timnor.app.shared.domain.work.formatWorkDateInput
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.calendar
import no.timnor.app.shared.generated.resources.done
import no.timnor.app.shared.generated.resources.work_period_range_label
import no.timnor.app.shared.generated.resources.work_period_registration
import no.timnor.app.shared.generated.resources.work_period_select_range
import no.timnor.app.shared.generated.resources.work_period_select_range_title
import no.timnor.app.shared.generated.resources.work_period_single_date
import no.timnor.app.shared.platform.motion.rememberSystemAnimationsEnabled
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WorkPeriodRegistrationSwitch(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        Text(stringResource(Res.string.work_period_registration))
        Spacer(Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkPeriodRegistration(
    enabled: Boolean,
    rangeState: DateRangePickerState,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val animationsEnabled = rememberSystemAnimationsEnabled()

    AnimatedVisibility(
            visible = enabled,
            enter = if (animationsEnabled) fadeIn() + expandVertically() else EnterTransition.None,
            exit = if (animationsEnabled) fadeOut() + shrinkVertically() else ExitTransition.None,
    ) {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(Res.drawable.calendar), contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.timnorSpacing.small))
                val dates = periodDatesFromMillis(rangeState.selectedStartDateMillis, rangeState.selectedEndDateMillis)
                Text(
                    when {
                        dates.size == 1 -> stringResource(
                            Res.string.work_period_single_date,
                            formatWorkDateInput(dates.single().toString()),
                        )
                        dates.size > 1 -> stringResource(
                            Res.string.work_period_range_label,
                            formatWorkDateInput(dates.first().toString()),
                            formatWorkDateInput(dates.last().toString()),
                        )
                        else -> stringResource(Res.string.work_period_select_range)
                    },
                )
            }
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = { showPicker = false },
                    enabled = rangeState.selectedStartDateMillis != null && rangeState.selectedEndDateMillis != null,
                ) { Text(stringResource(Res.string.done)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(Res.string.cancel)) }
            },
        ) {
            DateRangePicker(
                state = rangeState,
                title = {
                    Text(
                        text = stringResource(Res.string.work_period_select_range_title),
                        modifier = Modifier.padding(MaterialTheme.timnorSpacing.small),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                showModeToggle = false,
            )
        }
    }
}

internal fun inclusivePeriodDates(start: LocalDate?, end: LocalDate?): List<LocalDate> {
    var current = start ?: return emptyList()
    val last = end ?: return emptyList()
    if (last < current) return emptyList()

    return buildList {
        while (current <= last) {
            add(current)
            current = current.plus(1, DateTimeUnit.DAY)
        }
    }
}

internal fun periodDatesFromMillis(startMillis: Long?, endMillis: Long?): List<LocalDate> {
    val start = startMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
    val end = endMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
    return inclusivePeriodDates(start, end)
}
