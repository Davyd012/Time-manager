package no.timnor.app.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.invalid_work
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.work_add
import no.timnor.app.shared.generated.resources.work_date_hint
import no.timnor.app.shared.generated.resources.work_description_hint
import no.timnor.app.shared.generated.resources.work_hours_hint
import no.timnor.app.shared.generated.resources.work_minutes_hint
import no.timnor.app.shared.generated.resources.work_project_hint
import no.timnor.app.shared.generated.resources.work_title
import no.timnor.app.shared.generated.resources.work_update
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WorkEntrySection(
    projects: List<StoredProject>,
    selectedProjectId: Int?,
    projectMenuExpanded: Boolean,
    onProjectMenuExpandedChange: (Boolean) -> Unit,
    onProjectSelected: (Int) -> Unit,
    date: String,
    onDateChange: (String) -> Unit,
    hours: String,
    onHoursChange: (String) -> Unit,
    minutes: String,
    onMinutesChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    isEditing: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        Text(stringResource(Res.string.work_title), style = MaterialTheme.typography.titleLarge)
        ProjectSelector(
            projects = projects,
            selectedProjectId = selectedProjectId,
            onProjectSelected = onProjectSelected,
            expanded = projectMenuExpanded,
            onExpandedChange = onProjectMenuExpandedChange,
            placeholder = stringResource(Res.string.work_project_hint),
        )
        OutlinedTextField(
            value = date,
            onValueChange = onDateChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(Res.string.work_date_hint)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            OutlinedTextField(
                value = hours,
                onValueChange = onHoursChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(Res.string.work_hours_hint)) },
            )
            OutlinedTextField(
                value = minutes,
                onValueChange = onMinutesChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(Res.string.work_minutes_hint)) },
            )
        }
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(Res.string.work_description_hint)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            Button(onClick = onSave, enabled = canSave) {
                Text(stringResource(if (isEditing) Res.string.work_update else Res.string.work_add))
            }
            if (isEditing) {
                TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
            }
        }
        if (projects.isNotEmpty() && !canSave) {
            Text(stringResource(Res.string.invalid_work), color = MaterialTheme.colorScheme.error)
        }
    }
}
