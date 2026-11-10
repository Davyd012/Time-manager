package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.domain.work.plannedWeekdayWork
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.delete
import no.timnor.app.shared.generated.resources.done
import no.timnor.app.shared.generated.resources.invalid_work
import no.timnor.app.shared.generated.resources.work_period_submit
import no.timnor.app.shared.generated.resources.work_save_changes
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WorkEntryActions(
    isEditing: Boolean,
    periodRegistration: Boolean,
    periodDates: List<LocalDate>,
    projectsExist: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onSubmitPeriod: (List<LocalDate>) -> Unit,
    onCancel: () -> Unit,
) {
    if (periodRegistration) {
        Button(
            onClick = { onSubmitPeriod(periodDates) },
            enabled = plannedWeekdayWork(periodDates).isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) { Text(stringResource(Res.string.work_period_submit)) }
    } else {
        val saveColors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
            ) {
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.delete),
                        contentDescription = stringResource(Res.string.delete),
                    )
                }
                Button(
                    onClick = onSave,
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = saveColors,
                ) {
                    Icon(painter = painterResource(Res.drawable.done), contentDescription = null)
                    Spacer(Modifier.width(MaterialTheme.timnorSpacing.small))
                    Text(stringResource(Res.string.work_save_changes))
                }
                TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
            }
        } else {
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = saveColors,
            ) {
                Icon(painter = painterResource(Res.drawable.done), contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.timnorSpacing.small))
                Text(stringResource(Res.string.done))
            }
        }
    }

    if (!periodRegistration && projectsExist && !canSave) {
        Text(stringResource(Res.string.invalid_work), color = MaterialTheme.colorScheme.error)
    }
}
