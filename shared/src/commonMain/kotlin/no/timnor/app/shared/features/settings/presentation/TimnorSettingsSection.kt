package no.timnor.app.shared.features.settings.presentation

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.back_to_home
import no.timnor.app.shared.generated.resources.arrow_back
import no.timnor.app.shared.generated.resources.cancel
import no.timnor.app.shared.generated.resources.backup_checking
import no.timnor.app.shared.generated.resources.backup_created_at
import no.timnor.app.shared.generated.resources.backup_creating
import no.timnor.app.shared.generated.resources.backup_failure
import no.timnor.app.shared.generated.resources.backup_invalid
import no.timnor.app.shared.generated.resources.backup_restore_description
import no.timnor.app.shared.generated.resources.backup_restore_section
import no.timnor.app.shared.generated.resources.backup_project_count_one
import no.timnor.app.shared.generated.resources.backup_project_count_other
import no.timnor.app.shared.generated.resources.backup_restoring
import no.timnor.app.shared.generated.resources.backup_shared
import no.timnor.app.shared.generated.resources.backup_storage_note
import no.timnor.app.shared.generated.resources.backup_success
import no.timnor.app.shared.generated.resources.backup_unsupported
import no.timnor.app.shared.generated.resources.backup_unreadable
import no.timnor.app.shared.generated.resources.backup_work_count_one
import no.timnor.app.shared.generated.resources.backup_work_count_other
import no.timnor.app.shared.generated.resources.create_backup
import no.timnor.app.shared.generated.resources.document_failure
import no.timnor.app.shared.generated.resources.go_to_timer
import no.timnor.app.shared.generated.resources.replace_data
import no.timnor.app.shared.generated.resources.restore_backup
import no.timnor.app.shared.generated.resources.restore_confirm_counts
import no.timnor.app.shared.generated.resources.restore_confirm_message
import no.timnor.app.shared.generated.resources.restore_confirm_title
import no.timnor.app.shared.generated.resources.restore_failure
import no.timnor.app.shared.generated.resources.restore_success
import no.timnor.app.shared.generated.resources.settings_title
import no.timnor.app.shared.generated.resources.spreadsheet_creating
import no.timnor.app.shared.generated.resources.spreadsheet_success
import no.timnor.app.shared.generated.resources.timer_restore_block_message
import no.timnor.app.shared.generated.resources.timer_restore_block_title
import no.timnor.app.shared.ui.theme.timnorSpacing
import no.timnor.app.shared.features.settings.domain.TimnorSettingsMessage
import no.timnor.app.shared.features.settings.domain.TimnorSettingsProgress
import no.timnor.app.shared.features.settings.domain.TimnorSettingsUiState
import no.timnor.app.shared.platform.locale.formatLocalizedDateTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimnorSettingsSection(
    state: TimnorSettingsUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
    onGoToStopwatch: () -> Unit,
) {
    val backLabel = stringResource(Res.string.back_to_home)
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
            title = { Text(stringResource(Res.string.settings_title)) },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.semantics { contentDescription = backLabel },
                ) {
                    androidx.compose.material3.Icon(
                        painter = org.jetbrains.compose.resources.painterResource(Res.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
        )
            Column(
            modifier = Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.timnorSpacing.medium, vertical = MaterialTheme.timnorSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
            ) {
        Text(stringResource(Res.string.backup_restore_section), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.backup_restore_description), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onCreateBackup, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.create_backup))
        }
        Button(onClick = onRestoreBackup, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.restore_backup))
        }
        Text(stringResource(Res.string.backup_storage_note), style = MaterialTheme.typography.bodySmall)
        state.progress?.let { progress ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(MaterialTheme.timnorSpacing.large))
                Text(
                    stringResource(
                        when (progress) {
                            TimnorSettingsProgress.CreatingBackup -> Res.string.backup_creating
                            TimnorSettingsProgress.CheckingBackup -> Res.string.backup_checking
                            TimnorSettingsProgress.RestoringBackup -> Res.string.backup_restoring
                            TimnorSettingsProgress.CreatingSpreadsheet -> Res.string.spreadsheet_creating
                        },
                    ),
                )
            }
        }
        state.message?.let { message ->
            Text(
                    stringResource(
                        when (message) {
                        TimnorSettingsMessage.BackupSaved -> Res.string.backup_success
                        TimnorSettingsMessage.BackupShared -> Res.string.backup_shared
                        TimnorSettingsMessage.SpreadsheetSaved -> Res.string.spreadsheet_success
                        TimnorSettingsMessage.InvalidBackup -> Res.string.backup_invalid
                        TimnorSettingsMessage.UnsupportedBackupVersion -> Res.string.backup_unsupported
                        TimnorSettingsMessage.BackupUnreadable -> Res.string.backup_unreadable
                        TimnorSettingsMessage.RestoreSucceeded -> Res.string.restore_success
                        TimnorSettingsMessage.RestoreFailed -> Res.string.restore_failure
                        TimnorSettingsMessage.DocumentFailed -> Res.string.document_failure
                        TimnorSettingsMessage.TimerActive -> Res.string.timer_restore_block_message
                        },
                    ),
                    color = if (message == TimnorSettingsMessage.DocumentFailed ||
                        message == TimnorSettingsMessage.InvalidBackup ||
                        message == TimnorSettingsMessage.UnsupportedBackupVersion ||
                        message == TimnorSettingsMessage.BackupUnreadable ||
                        message == TimnorSettingsMessage.RestoreFailed
                    ) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
    }

    state.preview?.takeUnless { state.timerBlocked }?.let { preview ->
        AlertDialog(
            onDismissRequest = { if (!state.busy) onCancelRestore() },
            title = { Text(stringResource(Res.string.restore_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                    Text(stringResource(Res.string.restore_confirm_message))
                    val projectCount = if (preview.projectCount == 1) {
                        stringResource(Res.string.backup_project_count_one)
                    } else {
                        stringResource(Res.string.backup_project_count_other, preview.projectCount)
                    }
                    val workCount = if (preview.workCount == 1) {
                        stringResource(Res.string.backup_work_count_one)
                    } else {
                        stringResource(Res.string.backup_work_count_other, preview.workCount)
                    }
                    Text(stringResource(Res.string.restore_confirm_counts, projectCount, workCount))
                    Text(
                        stringResource(
                            Res.string.backup_created_at,
                            formatLocalizedDateTime(Instant.parse(preview.createdAt)),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmRestore, enabled = !state.busy) {
                    Text(stringResource(Res.string.replace_data))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelRestore, enabled = !state.busy) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (state.timerBlocked) {
        AlertDialog(
            onDismissRequest = onCancelRestore,
            title = { Text(stringResource(Res.string.timer_restore_block_title)) },
            text = { Text(stringResource(Res.string.timer_restore_block_message)) },
            confirmButton = {
                TextButton(onClick = { onCancelRestore(); onGoToStopwatch() }) {
                    Text(stringResource(Res.string.go_to_timer))
                }
            },
            dismissButton = { TextButton(onClick = onCancelRestore) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}
