package no.timnor.app.shared.features.root.presentation.newProject.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import no.timnor.app.shared.generated.resources.project_custom_weekend_rate
import no.timnor.app.shared.generated.resources.project_weekend_rate_days
import no.timnor.app.shared.generated.resources.project_weekend_rate_help
import no.timnor.app.shared.generated.resources.project_rate_changes_help
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.invalid_hourly_rate
import no.timnor.app.shared.generated.resources.project_hourly_rate
import no.timnor.app.shared.generated.resources.project_hourly_rate_unit
import no.timnor.app.shared.generated.resources.project_weekend_rate
import no.timnor.app.shared.domain.project.parseHourlyRateOre
import no.timnor.app.shared.generated.resources.project_payment_optional
import no.timnor.app.shared.generated.resources.project_payment_help
import no.timnor.app.shared.platform.locale.localizedDecimalSeparator
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProjectHourlyRateField(
    rate: String?,
    weekendRate: String?,
    isError: Boolean,
    onRateChange: (String?) -> Unit,
    onWeekendRateChange: (String?) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.timnorSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
        ) {
            RateSwitch(
                title = stringResource(Res.string.project_payment_optional),
                subtitle = stringResource(Res.string.project_payment_help),
                checked = rate != null,
                onCheckedChange = { onRateChange(if (it) "" else null) },
            )
            if (rate != null) {
                RateInput(
                    rate = rate,
                    label = stringResource(Res.string.project_hourly_rate),
                    showError = isError,
                    onRateChange = onRateChange,
                )
                HorizontalDivider()
                RateSwitch(
                    title = stringResource(Res.string.project_custom_weekend_rate),
                    subtitle = stringResource(Res.string.project_weekend_rate_days),
                    checked = weekendRate != null,
                    onCheckedChange = { onWeekendRateChange(if (it) "" else null) },
                )
                if (weekendRate != null) {
                    RateInput(
                        rate = weekendRate,
                        label = stringResource(Res.string.project_weekend_rate),
                        showError = isError,
                        onRateChange = onWeekendRateChange,
                    )
                    RateHelp(stringResource(Res.string.project_weekend_rate_help))
                }
                RateHelp(stringResource(Res.string.project_rate_changes_help))
            }
        }
    }
}

@Composable
private fun RateSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            RateHelp(subtitle)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun RateHelp(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RateInput(rate: String, label: String, showError: Boolean, onRateChange: (String) -> Unit) {
    val decimalSeparator = localizedDecimalSeparator()
    var input by remember { mutableStateOf(TextFieldValue(rate.replace('.', decimalSeparator))) }
    val focusManager = LocalFocusManager.current
    val invalid = showError && parseHourlyRateOre(rate) == null
    OutlinedTextField(
        value = input,
        onValueChange = {
            input = it
            onRateChange(it.text)
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = { Text(stringResource(Res.string.project_hourly_rate_unit)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        singleLine = true,
        isError = invalid,
        supportingText = if (invalid) {
            { Text(stringResource(Res.string.invalid_hourly_rate)) }
        } else null,
    )
}
