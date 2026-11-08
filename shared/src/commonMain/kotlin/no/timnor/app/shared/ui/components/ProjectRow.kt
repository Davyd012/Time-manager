package no.timnor.app.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.delete
import no.timnor.app.shared.generated.resources.delete_project_confirmation
import no.timnor.app.shared.generated.resources.delete_project_title
import no.timnor.app.shared.generated.resources.edit
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProjectRow(project: StoredProject, onEdit: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember(project.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = project.name,
            style = MaterialTheme.typography.bodyLarge,
        )
        TextButton(onClick = onEdit) { Text(stringResource(Res.string.edit)) }
        TextButton(onClick = { confirmDelete = true }) { Text(stringResource(Res.string.delete)) }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.delete_project_title)) },
            text = { Text(stringResource(Res.string.delete_project_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(Res.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}
