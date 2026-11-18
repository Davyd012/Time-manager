package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import no.timnor.app.shared.domain.work.manualWorkDurationSeconds
import no.timnor.app.shared.generated.resources.*
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

private const val MAX_HOURS = 24

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WorkDurationDialog(
    initialHours: String,
    initialMinutes: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var hours by rememberSaveable { mutableStateOf((initialHours.toIntOrNull() ?: 0).coerceIn(0, MAX_HOURS).toString()) }
    var minutes by rememberSaveable {
        mutableStateOf(if ((initialHours.toIntOrNull() ?: 0) >= MAX_HOURS) "0" else (initialMinutes.toIntOrNull() ?: 0).coerceIn(0, 59).toString())
    }
    var manual by rememberSaveable { mutableStateOf(false) }
    val h = hours.toIntOrNull()
    val m = minutes.toIntOrNull()
    val representable = h != null && h in 0..MAX_HOURS && m != null && m in 0..59 &&
        (h < MAX_HOURS || m == 0)
    val valid = representable && manualWorkDurationSeconds(h, m) != null

    Column(
        modifier = Modifier.fillMaxWidth().testTag("duration-picker-content").imePadding().navigationBarsPadding()
            .padding(horizontal = MaterialTheme.timnorSpacing.medium),
    ) {
      Column(
        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
            .padding(top = MaterialTheme.timnorSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
      ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.duration_custom), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = {
                if (manual && !representable) {
                    val adjustedHours = (h ?: 0).coerceIn(0, MAX_HOURS)
                    hours = adjustedHours.toString()
                    minutes = (if (adjustedHours == MAX_HOURS) 0 else (m ?: 0).coerceIn(0, 59)).toString()
                }
                manual = !manual
            },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.testTag("duration-manual-toggle")) {
                Text(stringResource(if (manual) Res.string.duration_wheels else Res.string.duration_manual))
            }
        }
        if (manual) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                DurationInput(hours, { hours = it }, stringResource(Res.string.work_hours_hint), Modifier.weight(1f).testTag("duration-hours-input"), !representable)
                DurationInput(minutes, { minutes = it }, stringResource(Res.string.work_minutes_hint), Modifier.weight(1f).testTag("duration-minutes-input"), !representable)
            }
        } else {
            DurationWheels(
                hours = h ?: 0, minutes = m ?: 0,
                onHoursChange = { hours = it.toString(); if (it == MAX_HOURS) minutes = "0" },
                onMinutesChange = { minutes = it.toString() },
            )
        }
        if (!valid) Text(stringResource(Res.string.duration_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            listOf(0 to 30, 1 to 0, 2 to 0, 4 to 0).forEach { (presetHours, presetMinutes) ->
                val label = if (presetHours == 0) stringResource(Res.string.duration_minutes_short, presetMinutes)
                    else stringResource(Res.string.duration_hours_short, presetHours)
                FilterChip(
                    selected = h == presetHours && m == presetMinutes,
                    onClick = { hours = presetHours.toString(); minutes = presetMinutes.toString() },
                    modifier = Modifier.testTag("duration-preset-${presetHours * 60 + presetMinutes}"),
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
      }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text(stringResource(Res.string.cancel)) }
            Spacer(Modifier.width(MaterialTheme.timnorSpacing.small))
            Button(onClick = { onConfirm(h!!, m!!) }, enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.testTag("duration-confirm")) { Text(stringResource(Res.string.duration_use)) }
        }
        Spacer(Modifier.height(MaterialTheme.timnorSpacing.small))
    }
}

@Composable
private fun DurationWheels(hours: Int, minutes: Int, onHoursChange: (Int) -> Unit, onMinutesChange: (Int) -> Unit) {
    val hoursLabel = stringResource(Res.string.work_hours_hint)
    val minutesLabel = stringResource(Res.string.work_minutes_hint)
    val minuteMaximum = if (hours == MAX_HOURS) 0 else 59
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) {
            Text(hoursLabel, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
            Text(minutesLabel, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
        }
        Row(Modifier.fillMaxWidth()) {
            DurationStepButton(hoursLabel, false, hours > 0, { onHoursChange((hours - 1).coerceAtLeast(0)) }, Modifier.weight(1f))
            DurationStepButton(minutesLabel, false, minutes > 0, { onMinutesChange((minutes - 1).coerceAtLeast(0)) }, Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                Box(Modifier.weight(1f).height(48.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium))
                Box(Modifier.weight(1f).height(48.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                DurationWheel(hours, MAX_HOURS, onHoursChange, Modifier.weight(1f), "duration-hours-wheel")
                key(minuteMaximum) {
                    DurationWheel(minutes, minuteMaximum, onMinutesChange, Modifier.weight(1f), "duration-minutes-wheel")
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            DurationStepButton(hoursLabel, true, hours < MAX_HOURS, { onHoursChange((hours + 1).coerceAtMost(MAX_HOURS)) }, Modifier.weight(1f))
            DurationStepButton(minutesLabel, true, minutes < minuteMaximum, { onMinutesChange((minutes + 1).coerceAtMost(minuteMaximum)) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DurationStepButton(label: String, increase: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val description = stringResource(if (increase) Res.string.duration_increase else Res.string.duration_decrease, label)
    TextButton(onClick = onClick, enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        modifier = modifier.semantics { contentDescription = description }) {
        Text(if (increase) "⌄" else "⌃", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun DurationInput(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier, error: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 6 && it.all { char -> char in '0'..'9' }) onChange(it) },
        label = { Text(label) }, singleLine = true, isError = error,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.onSurface,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DurationWheel(value: Int, maximum: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, testTag: String) {
    val safeValue = value.coerceIn(0, maximum)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = safeValue)
    val currentOnChange by rememberUpdatedState(onChange)
    val currentValue by rememberUpdatedState(safeValue)
    val haptics = LocalHapticFeedback.current
    val centeredValue by remember(state, maximum) {
        derivedStateOf {
            val layout = state.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - center) }
                ?.index?.minus(1)?.coerceIn(0, maximum)
        }
    }
    LaunchedEffect(safeValue) {
        if (!state.isScrollInProgress && centeredValue != safeValue) state.scrollToItem(safeValue)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.distinctUntilChanged().filter { !it }.collect {
            val selected = centeredValue ?: return@collect
            if (selected != currentValue) {
                currentOnChange(selected)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }
        LazyColumn(
            state = state, flingBehavior = rememberSnapFlingBehavior(state),
            modifier = modifier.height(144.dp).testTag(testTag), horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(maximum + 3) { index ->
                val number = index - 1
                if (number !in 0..maximum) Spacer(Modifier.height(48.dp))
                else {
                    val selected = number == (if (state.isScrollInProgress) centeredValue else safeValue)
                    Box(Modifier.fillMaxWidth().height(48.dp).clickable { onChange(number) }, contentAlignment = Alignment.Center) {
                        Text(number.toString(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
}
