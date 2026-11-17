package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import no.timnor.app.shared.domain.work.manualWorkDurationSeconds
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.duration_custom
import no.timnor.app.shared.generated.resources.duration_decrease
import no.timnor.app.shared.generated.resources.duration_increase
import no.timnor.app.shared.generated.resources.duration_use
import no.timnor.app.shared.generated.resources.work_hours_hint
import no.timnor.app.shared.generated.resources.work_minutes_hint
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WorkDurationDialog(
    initialHours: String,
    initialMinutes: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var hours by remember { mutableStateOf(initialHours) }
    var minutes by remember { mutableStateOf(initialMinutes) }
    val valid = manualWorkDurationSeconds(hours.toIntOrNull(), minutes.toIntOrNull()) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.duration_custom)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium)) {
                DurationUnit(hours, { hours = it }, stringResource(Res.string.work_hours_hint), 596523, Modifier.weight(1f))
                DurationUnit(minutes, { minutes = it }, stringResource(Res.string.work_minutes_hint), 59, Modifier.weight(1f))
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(hours.toInt(), minutes.toInt()) }, enabled = valid) {
                Text(stringResource(Res.string.duration_use))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

@Composable
private fun DurationUnit(value: String, onChange: (String) -> Unit, label: String, maximum: Int, modifier: Modifier = Modifier) {
    val increase = stringResource(Res.string.duration_increase, label)
    val decrease = stringResource(Res.string.duration_decrease, label)
    OutlinedCard(modifier = modifier, shape = MaterialTheme.shapes.medium) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.timnorSpacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextButton(
                onClick = { onChange(((value.toIntOrNull() ?: 0) + 1).coerceAtMost(maximum).toString()) },
                enabled = (value.toIntOrNull() ?: 0) < maximum,
                modifier = Modifier.semantics { contentDescription = increase },
            ) { Text("⌃", style = MaterialTheme.typography.titleLarge) }
            OutlinedTextField(
                value = value,
                onValueChange = { input ->
                    if (input.length <= 6 && input.all { it in '0'..'9' }) onChange(input)
                },
                label = { Text(label) },
                singleLine = true,
                isError = value.isNotEmpty() && (value.toIntOrNull() ?: Int.MAX_VALUE) > maximum,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = { onChange(((value.toIntOrNull() ?: 0) - 1).coerceAtLeast(0).coerceAtMost(maximum).toString()) },
                enabled = (value.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.semantics { contentDescription = decrease },
            ) { Text("⌄", style = MaterialTheme.typography.titleLarge) }
        }
    }
}
