package no.timnor.app.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.timnor.app.R
import no.timnor.app.core.repository.BackupResult
import no.timnor.app.ui.theme.spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onGoToTimer: () -> Unit,
    onRestored: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    LaunchedEffect(state.message) {
        if (state.message == BackupResult.Success) onRestored()
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        viewModel.createBackup(it)
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.readBackup(it)
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(ImageVector.vectorResource(id = R.drawable.outline_keyboard_arrow_left_24), stringResource(R.string.back_cd))
                }
            },
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            Text(stringResource(R.string.backup_restore_section), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.backup_restore_description), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = { saveLauncher.launch("time-manager-backup-${java.time.LocalDate.now()}.json") },
                enabled = !state.busy,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.create_backup))
            }
            Button(
                onClick = { openLauncher.launch(arrayOf("*/*")) },
                enabled = !state.busy,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.restore_backup))
            }
            Text(stringResource(R.string.backup_storage_note), style = MaterialTheme.typography.bodySmall)
            if (state.busy) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(stringResource((state.progress ?: Progress.CheckingBackup).toStringResource()))
            }
            state.message?.let { Text(stringResource(it.toStringResource()), color = MaterialTheme.colorScheme.onSurface) }
        }
    }
    if (state.preview != null && !state.timerBlocked) {
        val preview = state.preview!!
        AlertDialog(
            onDismissRequest = { if (!state.busy) viewModel.cancelPreview() },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Text(stringResource(R.string.restore_confirm_message))
                    Text(
                        stringResource(
                            R.string.backup_created_at,
                            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                .withLocale(locale)
                                .withZone(ZoneId.systemDefault())
                                .format(Instant.parse(preview.createdAt)),
                        ),
                    )
                    Text(
                        stringResource(
                            R.string.restore_count,
                            pluralStringResource(R.plurals.backup_projects_count, preview.projectCount, preview.projectCount),
                            pluralStringResource(R.plurals.backup_works_count, preview.workCount, preview.workCount),
                        ),
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::restore, enabled = !state.busy) { Text(stringResource(R.string.replace_data)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelPreview, enabled = !state.busy) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (state.timerBlocked) {
        AlertDialog(
            onDismissRequest = viewModel::cancelPreview,
            title = { Text(stringResource(R.string.timer_restore_block_title)) },
            text = { Text(stringResource(R.string.timer_restore_block_message)) },
            confirmButton = { TextButton(onClick = { viewModel.cancelPreview(); onGoToTimer() }) { Text(stringResource(R.string.go_to_timer)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelPreview) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun BackupResult.toStringResource(): Int = when (this) {
    BackupResult.Success -> R.string.backup_success
    BackupResult.Invalid -> R.string.backup_invalid
    BackupResult.UnsupportedVersion -> R.string.backup_unsupported
    BackupResult.Unreadable -> R.string.backup_unreadable
    BackupResult.Failure -> R.string.backup_failure
    BackupResult.RestoreFailure -> R.string.restore_failure
    BackupResult.TimerActive -> R.string.timer_restore_block_message
}

private fun Progress.toStringResource(): Int = when (this) {
    Progress.CreatingBackup -> R.string.backup_creating
    Progress.CheckingBackup -> R.string.backup_checking
    Progress.RestoringBackup -> R.string.backup_restoring
}
