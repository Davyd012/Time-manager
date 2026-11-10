package no.timnor.app.shared.features.root.presentation.home.components

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
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.delete
import no.timnor.app.shared.generated.resources.delete_work_confirmation
import no.timnor.app.shared.generated.resources.delete_work_title
import no.timnor.app.shared.generated.resources.edit
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RecentWorkRow(work: StoredWork, projectName: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember(work.id) { mutableStateOf(false) }
    val date = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date
    val minutes = work.time / 60
    val projectSegment = projectName.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "$date$projectSegment · ${minutes / 60}h ${minutes % 60}m" +
                work.description.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
        )
        TextButton(onClick = onEdit) { Text(stringResource(Res.string.edit)) }
        TextButton(onClick = { confirmDelete = true }) {
            Text(stringResource(Res.string.delete))
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.delete_work_title)) },
            text = { Text(stringResource(Res.string.delete_work_confirmation)) },
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
