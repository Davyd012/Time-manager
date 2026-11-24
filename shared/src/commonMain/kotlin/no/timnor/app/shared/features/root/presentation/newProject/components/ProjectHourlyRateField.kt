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
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.invalid_hourly_rate
import no.timnor.app.shared.generated.resources.project_hourly_rate
import no.timnor.app.shared.generated.resources.project_hourly_rate_unit
import no.timnor.app.shared.generated.resources.project_payment_optional
import no.timnor.app.shared.generated.resources.project_payment_help
import no.timnor.app.shared.platform.locale.localizedDecimalSeparator
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProjectHourlyRateField(
    rate: String?,
    isError: Boolean,
    onRateChange: (String?) -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth().toggleable(
                    value = rate != null,
                    role = Role.Switch,
                    onValueChange = { enabled -> onRateChange(if (enabled) "" else null) },
                ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.project_payment_optional),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(checked = rate != null, onCheckedChange = null)
            }
            if (rate != null) {
                val decimalSeparator = localizedDecimalSeparator()
                var input by remember { mutableStateOf(TextFieldValue(rate.replace('.', decimalSeparator))) }
                val focusManager = LocalFocusManager.current
                Text(
                    stringResource(Res.string.project_payment_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { updated ->
                        input = updated
                        onRateChange(updated.text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.project_hourly_rate)) },
                    suffix = { Text(stringResource(Res.string.project_hourly_rate_unit)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(stringResource(Res.string.invalid_hourly_rate)) }
                    } else null,
                )
            }
        }
    }
}
