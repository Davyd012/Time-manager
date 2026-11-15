package no.timnor.app.features.sharedhours

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.timnor.app.R
import no.timnor.app.core.repository.SharedWriteStatus
import no.timnor.app.shared.features.shared_hours.domain.PersonalTimeEntry
import no.timnor.app.shared.ui.theme.timnorSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class EntryFilter { Available, Allocated, All }
private sealed interface EntryListItem {
    val date: String
    data class Remote(val entry: PersonalTimeEntry) : EntryListItem { override val date = entry.workDate }
    data class Local(val entry: LocalTimeCandidate) : EntryListItem { override val date = entry.workDate }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllocateExistingTimeScreen(
    viewModel: PlannorConnectionViewModel,
    onConnectRequired: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.refreshLocalEntries() }
    val listState = rememberLazyListState(state.entryListIndex, state.entryListOffset)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> viewModel.rememberEntryScroll(index, offset) }
    }
    var filter by rememberSaveable { mutableStateOf(EntryFilter.Available) }
    var month by rememberSaveable { mutableStateOf("") }
    var openEntry by rememberSaveable { mutableStateOf<String?>(null) }
    var openLocalEntry by rememberSaveable { mutableStateOf<Int?>(null) }
    var editEntry by rememberSaveable { mutableStateOf(false) }
    var monthMenu by remember { mutableStateOf(false) }
    val entries = state.personalEntries.sortedByDescending { it.workDate }
    val remoteSources = entries.mapNotNull { it.sourceWorkId }.toSet()
    val local = state.localTimeCandidates.filter { it.sourceId == null || it.sourceId !in remoteSources }
    val months = (entries.map { it.workDate } + local.map { it.workDate }).mapNotNull {
        runCatching { LocalDate.parse(it).withDayOfMonth(1).toString() }.getOrNull()
    }.distinct().sortedDescending()
    val filteredRemote = entries.filter {
        (month.isEmpty() || it.workDate.startsWith(month.take(7))) && when (filter) {
            EntryFilter.Available -> it.remainingSeconds > 0
            EntryFilter.Allocated -> it.remainingSeconds == 0L
            EntryFilter.All -> true
        }
    }
    val filteredLocal = local.filter { (month.isEmpty() || it.workDate.startsWith(month.take(7))) && filter != EntryFilter.Allocated }
    val filtered = (filteredRemote.map { EntryListItem.Remote(it) } + filteredLocal.map { EntryListItem.Local(it) })
        .sortedByDescending { it.date }
    Column(modifier = modifier.padding(horizontal = MaterialTheme.timnorSpacing.medium, vertical = MaterialTheme.timnorSpacing.small)) {
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), state = listState) {
            item(key = "filters") {
                Column {
        if (state.session == null) {
            Text(stringResource(R.string.plannor_connect_required))
            Button(onClick = onConnectRequired) { Text(stringResource(R.string.plannor_connection_title)) }
        }
        Text(stringResource(R.string.allocation_choose_entry), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            EntryFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    label = { Text(stringResource(when (option) {
                        EntryFilter.Available -> R.string.entry_filter_available
                        EntryFilter.Allocated -> R.string.entry_filter_allocated
                        EntryFilter.All -> R.string.entry_filter_all
                    })) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            Box {
                OutlinedButton(onClick = { monthMenu = true }) {
                    Text(if (month.isEmpty()) stringResource(R.string.entry_all_periods) else formattedMonth(month))
                }
                DropdownMenu(expanded = monthMenu, onDismissRequest = { monthMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.entry_all_periods)) }, onClick = { month = ""; monthMenu = false })
                    months.forEach { value ->
                        DropdownMenuItem(text = { Text(formattedMonth(value)) }, onClick = { month = value; monthMenu = false })
                    }
                }
            }
            if (state.session != null) TextButton(onClick = viewModel::refreshEntries, enabled = !state.loading) {
                Text(stringResource(R.string.plannor_refresh_entries))
            }
        }
        if (state.entryError) {
            Text(stringResource(R.string.entry_network_error), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = viewModel::refreshEntries, enabled = !state.loading) { Text(stringResource(R.string.entry_retry)) }
        }
        WriteResult(state, viewModel::retryPendingWrites)
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (filtered.isEmpty() && !state.loading && (entries.isNotEmpty() || local.isNotEmpty() || !state.entryError)) {
            Text(stringResource(if (entries.isEmpty() && local.isEmpty()) R.string.entry_empty else R.string.entry_filter_empty),
                modifier = Modifier.padding(vertical = MaterialTheme.timnorSpacing.medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
                }
            }
            items(filtered, key = { when (it) {
                is EntryListItem.Remote -> "remote:${it.entry.serverId}"
                is EntryListItem.Local -> "local:${it.entry.workId}"
            } }) { item ->
                when (item) {
                    is EntryListItem.Remote -> EntryRow(item.entry, selected = item.entry.serverId == openEntry,
                        localProjectName = item.entry.sourceWorkId?.let(state.localProjectNamesBySource::get), onClick = {
                            viewModel.selectExistingEntry(item.entry.serverId)
                            val minutes = (item.entry.remainingSeconds / 60).coerceAtMost(1439)
                            if (minutes > 0) viewModel.setSharedMinutes(minutes.toString())
                            openEntry = item.entry.serverId
                        })
                    is EntryListItem.Local -> LocalEntryRow(item.entry, selected = item.entry.workId == openLocalEntry,
                        pending = item.entry.sourceId in state.pendingImportSourceIds, onClick = {
                        viewModel.clearSharedWriteResult()
                        openLocalEntry = item.entry.workId
                    })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        if (state.session != null) TextButton(onClick = viewModel::reviewHistoricalImport, enabled = !state.loading) {
            Text(stringResource(R.string.plannor_import_history))
        }
    }
    val selected = state.personalEntries.firstOrNull { it.serverId == openEntry }
    if (selected != null) {
        EntryDetails(state, selected, viewModel, editEntry, onEdit = { editEntry = it }, onDismiss = { openEntry = null; editEntry = false })
    }
    state.localTimeCandidates.firstOrNull { it.workId == openLocalEntry }?.let { localEntry ->
        val imported = localEntry.sourceId?.let { source -> entries.firstOrNull { it.sourceWorkId == source } }
        LaunchedEffect(imported?.serverId) {
            if (imported != null) {
                viewModel.selectExistingEntry(imported.serverId)
                val minutes = (imported.remainingSeconds / 60).coerceAtMost(1439)
                if (minutes > 0) viewModel.setSharedMinutes(minutes.toString())
                openLocalEntry = null
                openEntry = imported.serverId
            }
        }
        LocalEntryDetails(localEntry, state, viewModel, onConnectRequired, onDismiss = { openLocalEntry = null })
    }
    EntryDialogs(state, viewModel)
}

@Composable
private fun LocalEntryRow(entry: LocalTimeCandidate, selected: Boolean, pending: Boolean, onClick: () -> Unit) {
    val secondaryContentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
    ) {
    Row(Modifier.fillMaxWidth().semantics { this.selected = selected }.clickable(onClick = onClick).padding(MaterialTheme.timnorSpacing.medium),
        verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
        Text(formattedDate(entry.workDate), style = MaterialTheme.typography.titleSmall)
        val duration = durationText(entry.durationSeconds)
        Text(entry.projectName?.let { stringResource(R.string.entry_duration_project, duration, it) } ?: duration,
            style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(if (pending) R.string.entry_local_pending else R.string.entry_local_status), style = MaterialTheme.typography.bodySmall,
            color = secondaryContentColor)
    }
    Icon(painterResource(R.drawable.outline_keyboard_arrow_right_24), contentDescription = null,
        tint = secondaryContentColor)
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalEntryDetails(entry: LocalTimeCandidate, state: PlannorConnectionUiState,
    viewModel: PlannorConnectionViewModel, onConnectRequired: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(MaterialTheme.timnorSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
        ) {
            Text(formattedDate(entry.workDate), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.entry_total, durationText(entry.durationSeconds)))
            entry.projectName?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            when {
                state.session == null -> Button(onClick = { onDismiss(); onConnectRequired() },
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.plannor_connection_title))
                }
                entry.sourceId in state.pendingImportSourceIds -> TextButton(onClick = viewModel::retryPendingWrites,
                    enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.plannor_retry_shared_time))
                }
                else -> Button(onClick = { viewModel.prepareLocalEntry(entry.workId) }, enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.entry_prepare))
                }
            }
            Text(stringResource(R.string.entry_local_prepare_explanation), style = MaterialTheme.typography.bodyMedium)
            if (state.entryError) Text(stringResource(R.string.entry_network_error), color = MaterialTheme.colorScheme.error)
            WriteResult(state, viewModel::retryPendingWrites)
            Spacer(Modifier.height(MaterialTheme.timnorSpacing.medium))
        }
    }
}

@Composable
internal fun EntryRow(entry: PersonalTimeEntry, selected: Boolean, localProjectName: String? = null, onClick: () -> Unit) {
    val secondaryContentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
    ) {
    Row(Modifier.fillMaxWidth().semantics { this.selected = selected }.clickable(onClick = onClick).padding(MaterialTheme.timnorSpacing.medium),
        verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
        Text(formattedDate(entry.workDate), style = MaterialTheme.typography.titleSmall)
        val duration = durationText(entry.durationSeconds)
        Text(if (localProjectName == null) duration else stringResource(R.string.entry_duration_project, duration, localProjectName),
            style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(when {
            entry.remainingSeconds == 0L -> R.string.entry_status_complete
            entry.remainingSeconds < 60 -> R.string.entry_status_under_minute
            entry.allocatedSeconds > 0 -> R.string.entry_status_partial
            else -> R.string.entry_status_available
        }), style = MaterialTheme.typography.bodySmall, color = secondaryContentColor)
    }
    Icon(painterResource(R.drawable.outline_keyboard_arrow_right_24), contentDescription = null,
        tint = secondaryContentColor)
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDetails(
    state: PlannorConnectionUiState,
    entry: PersonalTimeEntry,
    viewModel: PlannorConnectionViewModel,
    editing: Boolean,
    onEdit: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.timnorSpacing.medium)
            .navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            Text(formattedDate(entry.workDate), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.entry_total, durationText(entry.durationSeconds)))
            Text(stringResource(R.string.entry_allocated, durationText(entry.allocatedSeconds)))
            Text(stringResource(R.string.entry_remaining, durationText(entry.remainingSeconds)))
            WriteResult(state, viewModel::retryPendingWrites)
            if (editing) {
                SharedWorkDateControl(state.entryCorrectionDate, viewModel::setEntryCorrectionDate)
                ExistingDurationControl(state.entryCorrectionSeconds.toLongOrNull() ?: 0) {
                    viewModel.setEntryCorrectionSeconds(it.toString())
                }
                Button(onClick = { viewModel.correctSelectedEntry(); onEdit(false) }, enabled = !state.loading &&
                    (state.entryCorrectionSeconds.toLongOrNull() ?: 0) >= entry.allocatedSeconds &&
                    (state.entryCorrectionSeconds.toLongOrNull() ?: 0) > 0, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.plannor_correct_entry))
                }
                TextButton(onClick = viewModel::requestDeleteSelectedEntry, enabled = !state.loading && entry.allocatedSeconds == 0L) {
                    Text(stringResource(R.string.plannor_delete_entry), color = MaterialTheme.colorScheme.error)
                }
            } else {
                if (entry.remainingSeconds >= 60) AllocationFields(state, entry, viewModel)
                else if (entry.remainingSeconds > 0) Text(stringResource(R.string.entry_minimum_allocation),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { onEdit(true) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.entry_edit))
                }
            }
            Spacer(Modifier.height(MaterialTheme.timnorSpacing.medium))
        }
    }
}

@Composable
private fun AllocationFields(state: PlannorConnectionUiState, entry: PersonalTimeEntry, viewModel: PlannorConnectionViewModel) {
    var projectMenu by remember { mutableStateOf(false) }
    var taskMenu by remember { mutableStateOf(false) }
    val projects = state.projects.filter { it.selected && !it.archived }
    Box {
        OutlinedButton(onClick = { projectMenu = true }, modifier = Modifier.fillMaxWidth()) {
            Text(projects.firstOrNull { it.projectId == state.sharedProjectId }?.name ?: stringResource(R.string.entry_choose_project))
        }
        DropdownMenu(expanded = projectMenu, onDismissRequest = { projectMenu = false }) {
            projects.forEach { project ->
                DropdownMenuItem(text = { Text(project.name) }, onClick = { viewModel.selectSharedProject(project.projectId); projectMenu = false })
            }
        }
    }
    if (projects.isEmpty()) Text(stringResource(R.string.entry_no_selected_project), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Box {
        OutlinedButton(onClick = { taskMenu = true }, modifier = Modifier.fillMaxWidth()) {
            Text(state.tasks.firstOrNull { it.id == state.sharedTaskId }?.headline ?: stringResource(R.string.plannor_task_optional))
        }
        DropdownMenu(expanded = taskMenu, onDismissRequest = { taskMenu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.plannor_task_optional)) }, onClick = { viewModel.selectSharedTask(null); taskMenu = false })
            state.tasks.forEach { task ->
                DropdownMenuItem(text = { Text(task.headline) }, onClick = { viewModel.selectSharedTask(task.id); taskMenu = false })
            }
        }
    }
    SharedDurationControl(state.sharedMinutes, viewModel::setSharedMinutes)
    OutlinedTextField(value = state.sharedNote, onValueChange = viewModel::setSharedNote,
        label = { Text(stringResource(R.string.plannor_shared_note)) }, modifier = Modifier.fillMaxWidth())
    val seconds = (state.sharedMinutes.toLongOrNull() ?: 0) * 60
    if (seconds > entry.remainingSeconds) {
        Text(stringResource(R.string.entry_duration_exceeds_remaining), color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium)
    }
    if (entry.serverId in state.pendingAllocationEntryIds) {
        Text(stringResource(R.string.entry_pending), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = viewModel::retryPendingWrites, enabled = !state.loading) { Text(stringResource(R.string.plannor_retry_shared_time)) }
    }
    Button(onClick = viewModel::allocateExistingTime,
        enabled = !state.loading && entry.serverId !in state.pendingAllocationEntryIds && state.sharedProjectId != null && entry.canAllocate(seconds),
        colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.entry_allocate_duration, durationText(seconds)))
    }
}

@Composable
private fun EntryDialogs(state: PlannorConnectionUiState, viewModel: PlannorConnectionViewModel) {
    state.historicalImportPreview?.let { preview ->
        AlertDialog(onDismissRequest = viewModel::cancelHistoricalImport,
            title = { Text(stringResource(R.string.plannor_import_preview_title)) },
            text = { Text(stringResource(R.string.plannor_import_preview_message, preview.eligibleEntries, preview.alreadyImported)) },
            confirmButton = { TextButton(onClick = viewModel::confirmHistoricalImport, enabled = preview.eligibleEntries > 0) { Text(stringResource(R.string.plannor_import_confirm)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelHistoricalImport) { Text(stringResource(R.string.plannor_import_cancel)) } })
    }
    if (state.confirmEntryDeletion) AlertDialog(onDismissRequest = viewModel::cancelDeleteSelectedEntry,
        title = { Text(stringResource(R.string.plannor_delete_entry_title)) },
        text = { Text(stringResource(R.string.plannor_delete_entry_message)) },
        confirmButton = { TextButton(onClick = viewModel::confirmDeleteSelectedEntry) {
            Text(stringResource(R.string.plannor_delete_entry), color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(onClick = viewModel::cancelDeleteSelectedEntry) { Text(stringResource(R.string.plannor_import_cancel)) } })
}

@Composable
internal fun WriteResult(state: PlannorConnectionUiState, onRetry: () -> Unit) {
    state.sharedWriteResult?.let { result ->
        Text(stringResource(when (result.status) {
            SharedWriteStatus.SYNCED -> R.string.entry_write_success
            SharedWriteStatus.PENDING -> R.string.plannor_shared_time_pending
            SharedWriteStatus.UNKNOWN -> R.string.plannor_shared_time_unknown
            SharedWriteStatus.CONFLICT -> R.string.plannor_shared_time_conflict
            SharedWriteStatus.REJECTED -> R.string.plannor_shared_time_rejected
            SharedWriteStatus.ACCESS_REVOKED -> R.string.plannor_shared_time_access_revoked
        }), style = MaterialTheme.typography.bodyMedium)
        if (result.status == SharedWriteStatus.PENDING || result.status == SharedWriteStatus.UNKNOWN) {
            TextButton(onClick = onRetry, enabled = !state.loading) { Text(stringResource(R.string.plannor_retry_shared_time)) }
        }
    }
}

@Composable
internal fun formattedDate(value: String): String {
    val locale = LocalContext.current.resources.configuration.locales[0]
    return remember(value, locale) { runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) }.getOrDefault(value) }
}

@Composable
private fun formattedMonth(value: String): String {
    val locale = LocalContext.current.resources.configuration.locales[0]
    return remember(value, locale) { runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)) }.getOrDefault(value) }
}

@Composable
internal fun durationText(seconds: Long): String {
    val minutes = seconds / 60
    return if (seconds in 1..59) stringResource(R.string.duration_under_minute)
    else if (minutes >= 60) stringResource(R.string.plannor_shared_time_duration_hours, (minutes / 60).toInt(), (minutes % 60).toInt())
    else stringResource(R.string.plannor_shared_time_duration_minutes, minutes.toInt())
}
