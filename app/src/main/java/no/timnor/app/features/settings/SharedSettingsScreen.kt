package no.timnor.app.features.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.timnor.app.shared.data.backup.TIMNOR_BACKUP_MAX_BYTES
import no.timnor.app.shared.features.settings.domain.TimnorDocumentAction
import no.timnor.app.shared.features.settings.domain.TimnorSettingsEffect
import no.timnor.app.shared.features.settings.domain.TimnorSettingsMessage
import no.timnor.app.shared.features.settings.domain.TimnorSettingsViewModel
import no.timnor.app.shared.features.settings.presentation.TimnorSettingsSection
import no.timnor.app.features.sharedhours.PlannorConnectionSection
import no.timnor.app.features.sharedhours.PlannorConnectionViewModel
import no.timnor.app.features.sharedhours.PlannorConnectionUiState
import no.timnor.app.features.sharedhours.PlannorSettingsDestination
import no.timnor.app.features.sharedhours.AllocateExistingTimeScreen
import no.timnor.app.features.sharedhours.PlannorProjectsScreen
import no.timnor.app.features.sharedhours.SharedAllocationsScreen
import no.timnor.app.R
import no.timnor.app.shared.features.settings.presentation.TimnorSettingsEntry
import no.timnor.app.shared.features.settings.presentation.TimnorSettingsOverview
import no.timnor.app.ui.theme.spacing
import java.io.File

/** Collect document effects at the host root so Home export remains available outside Settings. */
@Composable
fun AndroidTimnorDocumentEffectHandler(viewModel: TimnorSettingsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingBackup by remember { mutableStateOf<Pair<String, TimnorDocumentAction>?>(null) }
    var pendingSpreadsheet by remember { mutableStateOf<ByteArray?>(null) }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val pending = pendingBackup
        pendingBackup = null
        if (uri != null && pending != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) { writeText(context, uri, pending.first) }
                if (saved) viewModel.reportDocumentSuccess(pending.second) else viewModel.reportDocumentFailure()
            }
        }
    }
    val saveSpreadsheetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ) { uri ->
        val pending = pendingSpreadsheet
        pendingSpreadsheet = null
        if (uri != null && pending != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) { writeBytes(context, uri, pending) }
                if (saved) viewModel.reportDocumentSuccess(TimnorDocumentAction.Spreadsheet)
                else viewModel.reportDocumentFailure()
            }
        }
    }
    val openBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                when (val result = withContext(Dispatchers.IO) { readText(context, uri) }) {
                    is BackupReadResult.Success -> viewModel.previewBackup(result.contents)
                    BackupReadResult.Invalid -> viewModel.reportInvalidBackupDocument()
                    BackupReadResult.Unreadable -> viewModel.reportUnreadableBackupDocument()
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TimnorSettingsEffect.SaveBackup -> {
                    pendingBackup = effect.contents to TimnorDocumentAction.Backup
                    saveBackupLauncher.launch(effect.fileName)
                }
                is TimnorSettingsEffect.ShareBackup -> {
                    val shared = shareTextFile(context, effect.contents, effect.fileName)
                    if (shared) viewModel.reportDocumentSuccess(TimnorDocumentAction.Share)
                    else viewModel.reportDocumentFailure()
                }
                is TimnorSettingsEffect.ShareText -> {
                    val shared = shareText(context, effect.subject, effect.contents)
                    viewModel.reportTextShareResult(shared)
                }
                is TimnorSettingsEffect.OpenBackup -> openBackupLauncher.launch(effect.mimeTypes.toTypedArray())
                is TimnorSettingsEffect.SaveSpreadsheet -> {
                    pendingSpreadsheet = effect.contents
                    saveSpreadsheetLauncher.launch(effect.fileName)
                }
            }
        }
    }
}

@Composable
fun SharedSettingsScreen(
    viewModel: TimnorSettingsViewModel,
    onBack: () -> Unit,
    onGoToTimer: () -> Unit,
    onRestored: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) {
        if (state.message == TimnorSettingsMessage.RestoreSucceeded) onRestored()
    }

    TimnorSettingsSection(
        state = state,
        onBack = onBack,
        onCreateBackup = viewModel::createBackup,
        onRestoreBackup = viewModel::requestBackupRestore,
        onConfirmRestore = viewModel::restoreBackup,
        onCancelRestore = viewModel::cancelRestorePreview,
        onGoToStopwatch = onGoToTimer,
        backLabelOverride = stringResource(R.string.settings_back_label),
    )
}

@Composable
fun AndroidSettingsOverview(
    state: PlannorConnectionUiState,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onBack: () -> Unit,
    onOpen: (PlannorSettingsDestination) -> Unit,
    onBackup: () -> Unit,
) {
    val accountSummary = state.session?.let { stringResource(R.string.plannor_connected_as, it.username) }
        ?: stringResource(R.string.settings_plannor_disconnected)
    val selectedCount = state.projects.count { it.selected }
    val projectSummary = if (state.session == null) stringResource(R.string.plannor_connect_required)
        else pluralStringResource(R.plurals.settings_selected_projects, selectedCount, selectedCount)
    val reviewSummary = if (state.sharedWorkNeedsReview > 0) {
        stringResource(R.string.plannor_work_needs_review)
    } else if (state.projectAllocations.isNotEmpty()) stringResource(R.string.allocation_count, state.projectAllocations.size)
    else stringResource(R.string.settings_allocations_summary)
    TimnorSettingsOverview(
        entries = listOf(
            TimnorSettingsEntry("account", stringResource(R.string.plannor_connection_title), accountSummary,
                painterResource(R.drawable.baseline_person_24), connected = state.session != null) { onOpen(PlannorSettingsDestination.Account) },
            TimnorSettingsEntry("projects", stringResource(R.string.settings_projects_title), projectSummary,
                painterResource(R.drawable.baseline_work_outline_24)) { onOpen(PlannorSettingsDestination.Projects) },
            TimnorSettingsEntry("share", stringResource(R.string.settings_share_time_title),
                stringResource(R.string.settings_share_time_summary), painterResource(R.drawable.baseline_share_24)) {
                onOpen(PlannorSettingsDestination.ShareTime)
            },
            TimnorSettingsEntry("allocations", stringResource(R.string.plannor_allocations_title), reviewSummary,
                painterResource(R.drawable.baseline_import_export_24)) { onOpen(PlannorSettingsDestination.AllocationsReview) },
            TimnorSettingsEntry("backup", stringResource(R.string.backup_restore_section),
                stringResource(R.string.settings_backup_summary), painterResource(R.drawable.baseline_import_export_24), onClick = onBackup),
        ),
        modifier = modifier,
        backLabel = stringResource(R.string.settings_overview_back_label),
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannorSettingsScreen(
    viewModel: PlannorConnectionViewModel,
    destination: PlannorSettingsDestination,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onBack: () -> Unit,
    onConnectRequired: () -> Unit,
    onOpenProjects: () -> Unit = {},
    onCreateTime: () -> Unit = {},
) {
    val backLabel = stringResource(R.string.back_cd)
    val title = when (destination) {
        PlannorSettingsDestination.Account -> stringResource(R.string.plannor_connection_title)
        PlannorSettingsDestination.Projects -> stringResource(R.string.settings_choose_projects_title)
        PlannorSettingsDestination.ShareTime -> stringResource(R.string.settings_share_time_title)
        PlannorSettingsDestination.CreateTime -> stringResource(R.string.plannor_share_time_title)
        PlannorSettingsDestination.AllocationsReview -> stringResource(R.string.plannor_allocations_title)
    }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = androidx.compose.ui.Modifier.semantics {
                            contentDescription = backLabel
                        },
                    ) {
                        Icon(painterResource(R.drawable.outline_keyboard_arrow_left_24), null)
                    }
                },
            )
        },
    ) { padding ->
        if (destination == PlannorSettingsDestination.ShareTime) {
            AllocateExistingTimeScreen(
                viewModel = viewModel,
                modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(padding),
                onConnectRequired = onConnectRequired,
            )
        } else if (destination == PlannorSettingsDestination.AllocationsReview) {
            SharedAllocationsScreen(viewModel = viewModel, onConnectRequired = onConnectRequired,
                modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(padding))
        } else if (destination == PlannorSettingsDestination.Projects) {
            PlannorProjectsScreen(
                viewModel = viewModel,
                modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(padding)
                    .padding(MaterialTheme.spacing.medium),
                onConnectRequired = onConnectRequired,
            )
        } else {
            Column(
                modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()).padding(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                PlannorConnectionSection(
                    viewModel = viewModel,
                    destination = destination,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    onConnectRequired = onConnectRequired,
                    onOpenProjects = onOpenProjects,
                    onCreateTime = onCreateTime,
                )
            }
        }
    }
}

private fun writeText(context: Context, uri: Uri, contents: String): Boolean = try {
    val output = context.contentResolver.openOutputStream(uri, "wt") ?: return false
    output.use { it.write(contents.encodeToByteArray()) }
    true
} catch (_: Exception) {
    false
}

private fun writeBytes(context: Context, uri: Uri, contents: ByteArray): Boolean = try {
    val output = context.contentResolver.openOutputStream(uri, "wt") ?: return false
    output.use { it.write(contents) }
    true
} catch (_: Exception) {
    false
}

private sealed interface BackupReadResult {
    data class Success(val contents: String) : BackupReadResult
    data object Invalid : BackupReadResult
    data object Unreadable : BackupReadResult
}

private fun readText(context: Context, uri: Uri): BackupReadResult {
    val bytes = try {
        val input = context.contentResolver.openInputStream(uri) ?: return BackupReadResult.Unreadable
        input.use { stream ->
            val buffer = ByteArray(TIMNOR_BACKUP_MAX_BYTES + 1)
            var total = 0
            while (total < buffer.size) {
                val count = stream.read(buffer, total, buffer.size - total)
                if (count <= 0) break
                total += count
            }
            if (total > TIMNOR_BACKUP_MAX_BYTES) return BackupReadResult.Invalid
            buffer.copyOf(total)
        }
    } catch (_: Exception) {
        return BackupReadResult.Unreadable
    }

    val contents = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: Exception) {
        return BackupReadResult.Invalid
    }
    return BackupReadResult.Success(contents)
}

private fun shareTextFile(context: Context, contents: String, fileName: String): Boolean = try {
    val sharedDirectory = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(sharedDirectory, fileName).apply { writeText(contents, Charsets.UTF_8) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newUri(context.contentResolver, file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
    true
} catch (_: Exception) {
    false
}

private fun shareText(context: Context, subject: String, contents: String): Boolean = try {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, contents)
    }
    context.startActivity(Intent.createChooser(sendIntent, subject))
    true
} catch (_: Exception) {
    false
}
