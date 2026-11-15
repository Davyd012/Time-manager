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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.timnor.app.R
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.ui.theme.timnorSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedAllocationsScreen(viewModel: PlannorConnectionViewModel, onConnectRequired: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState(state.allocationListIndex, state.allocationListOffset)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> viewModel.rememberAllocationScroll(index, offset) }
    }
    var filterId by rememberSaveable { mutableStateOf<Int?>(null) }
    var detailsId by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var filterOpen by remember { mutableStateOf(false) }
    val ids = state.projectAllocations.map { it.projectId }.distinct()
    val items = state.projectAllocations.filter { filterId == null || it.projectId == filterId }.sortedByDescending { it.workDate }
    Column(modifier.padding(horizontal = MaterialTheme.timnorSpacing.medium)) {
        if (state.session == null) {
            Text(stringResource(R.string.plannor_connect_required))
            Button(onClick = onConnectRequired) { Text(stringResource(R.string.plannor_connection_title)) }
            return@Column
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState) {
            item(key = "allocation-filter") {
                Column {
        Text(stringResource(R.string.allocation_count, state.projectAllocations.size), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            Box {
                OutlinedButton(onClick = { filterOpen = true }) {
                    Text(filterId?.let { projectName(state, it) } ?: stringResource(R.string.allocation_all_projects))
                }
                DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.allocation_all_projects)) }, onClick = { filterId = null; filterOpen = false })
                    ids.forEach { id -> DropdownMenuItem(text = { Text(projectName(state, id)) }, onClick = { filterId = id; filterOpen = false }) }
                }
            }
            TextButton(onClick = viewModel::refreshProjectSnapshots, enabled = !state.loading) { Text(stringResource(R.string.entry_retry_refresh)) }
        }
        if (state.allocationError) {
            Text(stringResource(R.string.entry_network_error), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = viewModel::refreshProjectSnapshots, enabled = !state.loading) { Text(stringResource(R.string.entry_retry)) }
        }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        WriteResult(state, viewModel::retryPendingWrites)
                }
            }
            if (state.sharedWorkNeedsReview > 0) item(key = "review-heading") {
                Text(stringResource(R.string.plannor_work_needs_review), color = MaterialTheme.colorScheme.error)
            }
            if (state.sharedWorkReviews.isNotEmpty()) item(key = "review-title") {
                Text(stringResource(R.string.plannor_shared_review_title), style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = MaterialTheme.timnorSpacing.small))
            }
            items(state.sharedWorkReviews, key = { "review:${it.localWorkId}" }) { review ->
                SharedWorkReviewCards(listOf(review), state.loading, viewModel::resolveSharedWork)
            }
            if (items.isEmpty() && !state.loading && !state.allocationError) {
                item(key = "empty") {
                    Text(stringResource(if (state.projectAllocations.isEmpty()) R.string.settings_no_allocations else R.string.allocation_filter_empty),
                        modifier = Modifier.padding(vertical = MaterialTheme.timnorSpacing.medium))
                }
            }
            items(items, key = { "allocation:${it.allocationId}" }) { allocation ->
                val selected = allocation.allocationId == detailsId
                val secondaryContentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                ) {
                Row(Modifier.fillMaxWidth().semantics { this.selected = selected }.clickable {
                    viewModel.selectAllocation(allocation.allocationId)
                    detailsId = allocation.allocationId
                }.padding(MaterialTheme.timnorSpacing.medium), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(projectName(state, allocation.projectId), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.allocation_date_duration, formattedDate(allocation.workDate), durationText(allocation.durationSeconds)),
                            style = MaterialTheme.typography.bodyMedium, color = secondaryContentColor)
                    }
                    Icon(painterResource(R.drawable.outline_keyboard_arrow_right_24), contentDescription = null,
                        tint = secondaryContentColor)
                }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    state.projectAllocations.firstOrNull { it.allocationId == detailsId }?.let { allocation ->
        AllocationDetails(state, allocation, viewModel, editing, onEdit = { editing = it }, onDismiss = { detailsId = null; editing = false })
    }
    if (state.confirmAllocationDeletion) AlertDialog(onDismissRequest = viewModel::cancelDeleteSelectedAllocation,
        title = { Text(stringResource(R.string.plannor_delete_allocation_title)) },
        text = { Text(stringResource(R.string.plannor_delete_allocation_message)) },
        confirmButton = { TextButton(onClick = viewModel::confirmDeleteSelectedAllocation) {
            Text(stringResource(R.string.plannor_delete_allocation), color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(onClick = viewModel::cancelDeleteSelectedAllocation) { Text(stringResource(R.string.plannor_import_cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllocationDetails(state: PlannorConnectionUiState, allocation: ProjectTimeAllocation, viewModel: PlannorConnectionViewModel,
    editing: Boolean, onEdit: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var taskMenu by remember { mutableStateOf(false) }
    val ownerEntry = state.personalEntries.firstOrNull { it.serverId == allocation.entryId }
    val pending = allocation.allocationId in state.pendingAllocationIds
    val canChange = !pending && ownerEntry != null && state.projects.any { it.projectId == allocation.projectId && it.selected && !it.archived }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
            .padding(horizontal = MaterialTheme.timnorSpacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
            Text(projectName(state, allocation.projectId), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.allocation_date_duration, formattedDate(allocation.workDate), durationText(allocation.durationSeconds)))
            allocation.sharedNote?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            allocation.taskId?.let { taskId -> Text(state.tasks.firstOrNull { it.id == taskId }?.headline ?: stringResource(R.string.allocation_task_id, taskId)) }
            WriteResult(state, viewModel::retryPendingWrites)
            if (pending) {
                Text(stringResource(R.string.entry_pending), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = viewModel::retryPendingWrites, enabled = !state.loading) { Text(stringResource(R.string.plannor_retry_shared_time)) }
            } else if (!canChange) Text(stringResource(R.string.allocation_edit_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (editing) {
                ExistingDurationControl(state.allocationCorrectionSeconds, viewModel::setAllocationSeconds)
                Box {
                    OutlinedButton(onClick = { taskMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(state.tasks.firstOrNull { it.id == state.sharedTaskId }?.headline
                            ?: state.sharedTaskId?.let { stringResource(R.string.allocation_task_id, it) }
                            ?: stringResource(R.string.plannor_task_optional))
                    }
                    DropdownMenu(expanded = taskMenu, onDismissRequest = { taskMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.plannor_task_optional)) }, onClick = { viewModel.selectSharedTask(null); taskMenu = false })
                        state.tasks.forEach { task -> DropdownMenuItem(text = { Text(task.headline) }, onClick = { viewModel.selectSharedTask(task.id); taskMenu = false }) }
                    }
                }
                OutlinedTextField(value = state.sharedNote, onValueChange = viewModel::setSharedNote,
                    label = { Text(stringResource(R.string.plannor_shared_note)) }, modifier = Modifier.fillMaxWidth())
                if (ownerEntry != null && state.allocationCorrectionSeconds > ownerEntry.remainingSeconds + allocation.durationSeconds) {
                    Text(stringResource(R.string.entry_duration_exceeds_remaining), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = { viewModel.correctSelectedAllocation(); onEdit(false) }, enabled = canChange && !state.loading &&
                    state.allocationCorrectionSeconds > 0 && state.allocationCorrectionSeconds <=
                    (ownerEntry?.remainingSeconds ?: 0) + allocation.durationSeconds, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.plannor_correct_entry))
                }
            } else {
                OutlinedButton(onClick = { onEdit(true) }, enabled = canChange && !state.loading, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.allocation_edit))
                }
                TextButton(onClick = viewModel::requestDeleteSelectedAllocation, enabled = canChange && !state.loading) {
                    Text(stringResource(R.string.plannor_delete_allocation), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(MaterialTheme.timnorSpacing.medium))
        }
    }
}

@Composable
private fun projectName(state: PlannorConnectionUiState, id: Int): String =
    state.projects.firstOrNull { it.projectId == id }?.name ?: stringResource(R.string.allocation_project_id, id)
