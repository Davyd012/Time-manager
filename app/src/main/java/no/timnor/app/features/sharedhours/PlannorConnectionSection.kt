package no.timnor.app.features.sharedhours

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.timnor.app.R
import no.timnor.app.core.repository.PlannorConnectionFailure
import no.timnor.app.core.repository.SharedWriteStatus
import no.timnor.app.core.repository.SharedWorkResolution
import no.timnor.app.core.repository.SharedWorkJournal
import no.timnor.app.core.repository.SharedWorkReviewItem
import no.timnor.app.shared.ui.theme.timnorSpacing

@Composable
fun PlannorConnectionSection(viewModel: PlannorConnectionViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = state.session
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        Text(stringResource(R.string.plannor_connection_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.plannor_connection_private), style = MaterialTheme.typography.bodyMedium)

        if (session == null) {
            OutlinedTextField(
                value = state.identifier,
                onValueChange = viewModel::setIdentifier,
                label = { Text(stringResource(R.string.plannor_identifier)) },
                enabled = !state.loading,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::setPassword,
                label = { Text(stringResource(R.string.plannor_password)) },
                enabled = !state.loading,
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::signIn,
                enabled = !state.loading && state.identifier.isNotBlank() && state.password.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.plannor_connect))
            }
        } else {
            if (state.sharedWorkNeedsReview > 0) {
                Text(
                    stringResource(R.string.plannor_work_needs_review),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            SharedWorkReviewCards(state.sharedWorkReviews, state.loading, viewModel::resolveSharedWork)
            Text(
                stringResource(R.string.plannor_connected_as, session.username),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = state.projectSearch,
                onValueChange = viewModel::setProjectSearch,
                label = { Text(stringResource(R.string.plannor_search_projects)) },
                enabled = !state.loading,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                TextButton(onClick = viewModel::refreshProjects, enabled = !state.loading) {
                    Text(stringResource(R.string.plannor_refresh_projects))
                }
                TextButton(onClick = viewModel::signOut, enabled = !state.loading) {
                    Text(stringResource(R.string.plannor_sign_out))
                }
            }
            val visibleProjects = state.projects.filter {
                state.projectSearch.isBlank() || it.name.contains(state.projectSearch.trim(), ignoreCase = true)
            }
            if (visibleProjects.isEmpty() && !state.loading && !state.projectError) {
                Text(stringResource(R.string.plannor_no_projects), style = MaterialTheme.typography.bodyMedium)
            }
            visibleProjects.forEach { project ->
                val projectDescription = stringResource(
                    R.string.plannor_project_accessibility,
                    project.name,
                    project.contextLabel?.takeIf(String::isNotBlank)
                        ?: project.organizationName?.takeIf(String::isNotBlank)
                        ?: stringResource(R.string.plannor_project_context_unknown),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                ) {
                    Checkbox(
                        checked = project.selected,
                        onCheckedChange = { viewModel.selectProject(project.projectId, it) },
                        enabled = !project.archived && !state.loading,
                        modifier = Modifier.semantics {
                            contentDescription = projectDescription
                        },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(project.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            project.contextLabel?.takeIf(String::isNotBlank)
                                ?: project.organizationName?.takeIf(String::isNotBlank)
                                ?: stringResource(R.string.plannor_project_context_unknown),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (project.archived) Text(
                        stringResource(R.string.archived_project_label),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                var linkMenuExpanded by remember(project.projectId) { mutableStateOf(false) }
                val linkedPersonalProject = state.personalProjects.firstOrNull { it.id == project.linkedLocalProjectId }
                TextButton(
                    onClick = { linkMenuExpanded = true },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        linkedPersonalProject?.let { stringResource(R.string.plannor_personal_project_linked, it.name) }
                            ?: stringResource(R.string.plannor_link_personal_project),
                    )
                }
                DropdownMenu(
                    expanded = linkMenuExpanded,
                    onDismissRequest = { linkMenuExpanded = false },
                ) {
                    if (linkedPersonalProject == null && !project.archived) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.plannor_add_project_to_timnor)) },
                            onClick = {
                                viewModel.addProjectToTimnor(project.projectId)
                                linkMenuExpanded = false
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.plannor_unlink_personal_project)) },
                        onClick = {
                            viewModel.linkProject(project.projectId, null)
                            linkMenuExpanded = false
                        },
                    )
                    state.personalProjects.forEach { localProject ->
                        DropdownMenuItem(
                            text = { Text(localProject.name) },
                            onClick = {
                                viewModel.linkProject(project.projectId, localProject.id)
                                linkMenuExpanded = false
                            },
                        )
                    }
                }
            }
            val shareableProjects = state.projects.filter { it.selected && !it.archived }
            if (shareableProjects.isNotEmpty()) {
                Text(stringResource(R.string.plannor_share_time_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.plannor_share_time_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                shareableProjects.forEach { project ->
                    TextButton(
                        onClick = { viewModel.selectSharedProject(project.projectId) },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (state.sharedProjectId == project.projectId) R.string.plannor_share_target_selected
                                else R.string.plannor_share_target,
                                project.name,
                            ),
                        )
                    }
                }
                var taskMenuExpanded by remember(state.sharedProjectId) { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { taskMenuExpanded = true },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        state.tasks.firstOrNull { it.id == state.sharedTaskId }?.headline
                            ?: stringResource(R.string.plannor_task_optional),
                    )
                }
                DropdownMenu(
                    expanded = taskMenuExpanded,
                    onDismissRequest = { taskMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.plannor_task_optional)) },
                        onClick = {
                            viewModel.selectSharedTask(null)
                            taskMenuExpanded = false
                        },
                    )
                    state.tasks.forEach { task ->
                        DropdownMenuItem(
                            text = { Text(task.headline) },
                            onClick = {
                                viewModel.selectSharedTask(task.id)
                                taskMenuExpanded = false
                            },
                        )
                    }
                }
                SharedWorkDateControl(state.sharedWorkDate, viewModel::setSharedWorkDate)
                SharedDurationControl(state.sharedMinutes, viewModel::setSharedMinutes)
                OutlinedTextField(
                    value = state.privateNote,
                    onValueChange = viewModel::setPrivateNote,
                    label = { Text(stringResource(R.string.plannor_private_note)) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.sharedNote,
                    onValueChange = viewModel::setSharedNote,
                    label = { Text(stringResource(R.string.plannor_shared_note)) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = viewModel::createSharedTime,
                    enabled = !state.loading && state.sharedMinutes.toLongOrNull()?.let { it > 0 } == true,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.plannor_create_shared_time))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                    Text(stringResource(R.string.plannor_existing_entries_title), style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = viewModel::refreshEntries, enabled = !state.loading) {
                        Text(stringResource(R.string.plannor_refresh_entries))
                    }
                }
                state.personalEntries.forEach { entry ->
                    TextButton(
                        onClick = { viewModel.selectExistingEntry(entry.serverId) },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val availableMinutes = entry.remainingSeconds / 60
                        Text(
                            stringResource(
                                if (entry.serverId == state.existingEntryId) R.string.plannor_existing_entry_selected
                                else R.string.plannor_existing_entry,
                                entry.workDate,
                                availableMinutes,
                            ),
                        )
                    }
                }
                state.personalEntries.firstOrNull { it.serverId == state.existingEntryId }?.let { entry ->
                    OutlinedTextField(
                        value = state.entryCorrectionDate,
                        onValueChange = viewModel::setEntryCorrectionDate,
                        label = { Text(stringResource(R.string.plannor_correct_date)) },
                        enabled = !state.loading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.entryCorrectionSeconds,
                        onValueChange = viewModel::setEntryCorrectionSeconds,
                        label = { Text(stringResource(R.string.plannor_correct_seconds)) },
                        enabled = !state.loading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                        TextButton(
                            onClick = viewModel::correctSelectedEntry,
                            enabled = !state.loading && entryCorrectionIsValid(state.entryCorrectionDate, state.entryCorrectionSeconds),
                        ) {
                            Text(stringResource(R.string.plannor_correct_entry))
                        }
                        TextButton(
                            onClick = viewModel::requestDeleteSelectedEntry,
                            enabled = !state.loading && entry.allocatedSeconds == 0L,
                        ) {
                            Text(stringResource(R.string.plannor_delete_entry))
                        }
                    }
                }
                Button(
                    onClick = viewModel::allocateExistingTime,
                    enabled = !state.loading && state.existingEntryId != null &&
                        state.personalEntries.firstOrNull { it.serverId == state.existingEntryId }?.remainingSeconds?.let {
                            it >= (state.sharedMinutes.toLongOrNull() ?: Long.MAX_VALUE) * 60
                        } == true &&
                        state.sharedMinutes.toLongOrNull()?.let { it > 0 } == true,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.plannor_allocate_existing))
                }
                TextButton(onClick = viewModel::reviewHistoricalImport, enabled = !state.loading) {
                    Text(stringResource(R.string.plannor_import_history))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                    Text(stringResource(R.string.plannor_allocations_title), style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = viewModel::refreshProjectSnapshots, enabled = !state.loading) {
                        Text(stringResource(R.string.plannor_refresh_allocations))
                    }
                }
                state.projectAllocations.forEach { allocation ->
                    val projectName = state.projects.firstOrNull { it.projectId == allocation.projectId }?.name
                        ?: allocation.projectId.toString()
                    TextButton(
                        onClick = { viewModel.selectAllocation(allocation.allocationId) },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (allocation.allocationId == state.selectedAllocationId) R.string.plannor_allocation_selected
                                else R.string.plannor_allocation,
                                projectName,
                                allocation.workDate,
                                allocation.durationSeconds,
                            ),
                        )
                    }
                }
                if (state.projectAllocations.any { it.allocationId == state.selectedAllocationId }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                        TextButton(onClick = viewModel::correctSelectedAllocation, enabled = !state.loading) {
                            Text(stringResource(R.string.plannor_correct_allocation))
                        }
                        TextButton(onClick = viewModel::requestDeleteSelectedAllocation, enabled = !state.loading) {
                            Text(stringResource(R.string.plannor_delete_allocation))
                        }
                    }
                }
                state.sharedWriteResult?.let { result ->
                    val label = when (result.status) {
                        SharedWriteStatus.SYNCED -> stringResource(R.string.plannor_shared_time_synced)
                        SharedWriteStatus.PENDING -> stringResource(R.string.plannor_shared_time_pending)
                        SharedWriteStatus.UNKNOWN -> stringResource(R.string.plannor_shared_time_unknown)
                        SharedWriteStatus.CONFLICT -> stringResource(R.string.plannor_shared_time_conflict)
                        SharedWriteStatus.REJECTED -> stringResource(R.string.plannor_shared_time_rejected)
                        SharedWriteStatus.ACCESS_REVOKED -> stringResource(R.string.plannor_shared_time_access_revoked)
                    }
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    if (result.status == SharedWriteStatus.SYNCED) {
                        Text(
                            stringResource(R.string.plannor_shared_time_balance, state.sharedMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (result.status == SharedWriteStatus.UNKNOWN || result.status == SharedWriteStatus.PENDING) {
                        TextButton(onClick = viewModel::retryPendingWrites, enabled = !state.loading) {
                            Text(stringResource(R.string.plannor_retry_shared_time))
                        }
                    }
                }
            }
        }
        if (state.loading) CircularProgressIndicator()
        state.error?.let { error ->
            Text(
                stringResource(error.toStringResource()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.projectError) Text(
            stringResource(R.string.plannor_error_projects),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        state.historicalImportPreview?.let { preview ->
            AlertDialog(
                onDismissRequest = viewModel::cancelHistoricalImport,
                title = { Text(stringResource(R.string.plannor_import_preview_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.plannor_import_preview_message,
                            preview.eligibleEntries,
                            preview.alreadyImported,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmHistoricalImport, enabled = preview.eligibleEntries > 0) {
                        Text(stringResource(R.string.plannor_import_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelHistoricalImport) {
                        Text(stringResource(R.string.plannor_import_cancel))
                    }
                },
            )
        }
        if (state.confirmEntryDeletion) {
            AlertDialog(
                onDismissRequest = viewModel::cancelDeleteSelectedEntry,
                title = { Text(stringResource(R.string.plannor_delete_entry_title)) },
                text = { Text(stringResource(R.string.plannor_delete_entry_message)) },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteSelectedEntry) {
                        Text(stringResource(R.string.plannor_delete_entry))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDeleteSelectedEntry) {
                        Text(stringResource(R.string.plannor_import_cancel))
                    }
                },
            )
        }
        if (state.confirmAllocationDeletion) {
            AlertDialog(
                onDismissRequest = viewModel::cancelDeleteSelectedAllocation,
                title = { Text(stringResource(R.string.plannor_delete_allocation_title)) },
                text = { Text(stringResource(R.string.plannor_delete_allocation_message)) },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteSelectedAllocation) {
                        Text(stringResource(R.string.plannor_delete_allocation))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDeleteSelectedAllocation) {
                        Text(stringResource(R.string.plannor_import_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun SharedWorkReviewCards(
    reviews: List<SharedWorkReviewItem>,
    loading: Boolean,
    onResolve: (Int, SharedWorkResolution) -> Unit,
) {
    if (reviews.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        Text(stringResource(R.string.plannor_shared_review_title), style = MaterialTheme.typography.titleSmall)
        reviews.forEach { review ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
                ) {
                    Text(
                        stringResource(
                            when (review.reason) {
                                SharedWorkJournal.REVIEW_REMOTE_ENTRY_MISSING -> R.string.plannor_shared_review_missing
                                SharedWorkJournal.REVIEW_REMOTE_ENTRY_CHANGED -> R.string.plannor_shared_review_changed
                                SharedWorkJournal.REVIEW_REMOTE_ALLOCATIONS_CHANGED -> R.string.plannor_shared_review_allocations
                                SharedWorkJournal.REVIEW_REMOTE_ALLOCATIONS_UNAVAILABLE,
                                SharedWorkJournal.REVIEW_ACCESS_REMOVED -> R.string.plannor_shared_review_access
                                else -> R.string.plannor_shared_review_conflict
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.plannor_shared_review_timnor_version,
                            review.localDate, review.localDurationSeconds / 60),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    review.localPrivateNote.takeIf(String::isNotBlank)?.let {
                        Text(stringResource(R.string.plannor_shared_review_timnor_note, it),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (review.remoteDate != null && review.remoteDurationSeconds != null) {
                        Text(
                            stringResource(R.string.plannor_shared_review_plannor_version,
                                review.remoteDate, review.remoteDurationSeconds / 60),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        review.remotePrivateNote?.takeIf(String::isNotBlank)?.let {
                            Text(stringResource(R.string.plannor_shared_review_plannor_note, it),
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            stringResource(R.string.plannor_shared_review_separate_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (review.reason == SharedWorkJournal.REVIEW_ACCESS_REMOVED) {
                        TextButton(onClick = {
                            onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                        }, enabled = !loading) {
                            Text(stringResource(
                                if (review.localDeleted) R.string.plannor_shared_review_leave_remote
                                else R.string.plannor_shared_review_keep_separate,
                            ))
                        }
                    } else if (review.localDeleted) {
                        if (!review.remoteAllocationsComplete && review.remoteDate != null) {
                            TextButton(onClick = {
                                onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                            }, enabled = !loading) {
                                Text(stringResource(R.string.plannor_shared_review_leave_remote))
                            }
                        } else if (review.remoteDate == null) {
                            TextButton(onClick = {
                                onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                            }, enabled = !loading) {
                                Text(stringResource(R.string.plannor_shared_review_dismiss))
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                                TextButton(onClick = {
                                    onResolve(review.localWorkId, SharedWorkResolution.KEEP_TIMNOR)
                                }, enabled = !loading) {
                                    Text(stringResource(R.string.plannor_shared_review_retry_deletion))
                                }
                                TextButton(onClick = {
                                    onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                                }, enabled = !loading) {
                                    Text(stringResource(R.string.plannor_shared_review_leave_remote))
                                }
                            }
                        }
                    } else if (review.reason == SharedWorkJournal.REVIEW_REMOTE_ENTRY_MISSING) {
                        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                            TextButton(onClick = {
                                onResolve(review.localWorkId, SharedWorkResolution.SHARE_AGAIN)
                            }, enabled = !loading) {
                                Text(stringResource(R.string.plannor_shared_review_share_again))
                            }
                            TextButton(onClick = {
                                onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                            }, enabled = !loading) {
                                Text(stringResource(R.string.plannor_shared_review_keep_local))
                            }
                        }
                    } else if (review.reason in setOf(
                            SharedWorkJournal.REVIEW_REMOTE_ALLOCATIONS_UNAVAILABLE,
                            SharedWorkJournal.REVIEW_ACCESS_REMOVED,
                        ) &&
                        review.remoteDate != null
                    ) {
                        TextButton(onClick = {
                            onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                        }, enabled = !loading) {
                            Text(stringResource(R.string.plannor_shared_review_keep_separate))
                        }
                    } else if (review.managedAllocationPresent && review.remoteDate != null) {
                        if (review.remoteAllocationCount > 1) {
                            Text(
                                stringResource(R.string.plannor_shared_review_multi_allocation),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                            TextButton(onClick = {
                                onResolve(review.localWorkId, SharedWorkResolution.KEEP_TIMNOR)
                            }, enabled = !loading) {
                                Text(stringResource(R.string.plannor_shared_review_keep_timnor))
                            }
                            if (review.remoteProjectId == review.projectId) {
                                TextButton(onClick = {
                                    onResolve(review.localWorkId, SharedWorkResolution.USE_PLANNOR)
                                }, enabled = !loading) {
                                    Text(stringResource(R.string.plannor_shared_review_use_plannor))
                                }
                            }
                            TextButton(onClick = {
                                onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                            }, enabled = !loading) {
                                Text(stringResource(R.string.plannor_shared_review_keep_separate))
                            }
                        }
                    } else if (review.remoteDate != null) {
                        Text(stringResource(R.string.plannor_shared_review_multi_allocation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                            TextButton(onClick = {
                                onResolve(review.localWorkId, SharedWorkResolution.KEEP_TIMNOR)
                            }, enabled = !loading) {
                                Text(stringResource(R.string.plannor_shared_review_keep_timnor))
                            }
                            TextButton(onClick = {
                                onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                            }, enabled = !loading) {
                                Text(stringResource(R.string.plannor_shared_review_keep_separate))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun PlannorConnectionFailure.toStringResource(): Int = when (this) {
    PlannorConnectionFailure.CREDENTIALS_OR_ACCOUNT -> R.string.plannor_error_credentials
    PlannorConnectionFailure.RATE_LIMITED -> R.string.plannor_error_rate_limited
    PlannorConnectionFailure.SERVER -> R.string.plannor_error_server
    PlannorConnectionFailure.NETWORK, PlannorConnectionFailure.SESSION_EXPIRED -> R.string.plannor_error_network
}

private fun entryCorrectionIsValid(date: String, seconds: String): Boolean =
    runCatching { java.time.LocalDate.parse(date) }.isSuccess && seconds.toLongOrNull()?.let { it > 0 } == true
