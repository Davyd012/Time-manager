package no.timnor.app.shared.features.shared_hours.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import no.timnor.app.shared.features.shared_hours.domain.PlannorAccountUiState
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.plannor_account_connect
import no.timnor.app.shared.generated.resources.plannor_account_connected
import no.timnor.app.shared.generated.resources.plannor_account_context
import no.timnor.app.shared.generated.resources.plannor_account_error
import no.timnor.app.shared.generated.resources.plannor_account_identifier
import no.timnor.app.shared.generated.resources.plannor_account_link_local
import no.timnor.app.shared.generated.resources.plannor_account_no_link
import no.timnor.app.shared.generated.resources.plannor_account_password
import no.timnor.app.shared.generated.resources.plannor_account_private_hours
import no.timnor.app.shared.generated.resources.plannor_account_projects
import no.timnor.app.shared.generated.resources.plannor_account_add_project
import no.timnor.app.shared.generated.resources.plannor_account_refresh
import no.timnor.app.shared.generated.resources.plannor_account_sign_out
import no.timnor.app.shared.generated.resources.plannor_account_title
import no.timnor.app.shared.generated.resources.plannor_account_error_credentials
import no.timnor.app.shared.generated.resources.plannor_account_error_network
import no.timnor.app.shared.generated.resources.plannor_account_error_server
import no.timnor.app.shared.generated.resources.plannor_account_error_storage
import no.timnor.app.shared.generated.resources.plannor_account_error_rate_limited
import no.timnor.app.shared.generated.resources.plannor_account_project_checked
import no.timnor.app.shared.generated.resources.plannor_account_project_unchecked
import no.timnor.app.shared.generated.resources.plannor_account_project_unavailable
import no.timnor.app.shared.generated.resources.plannor_account_sync_error
import no.timnor.app.shared.generated.resources.plannor_account_sync_pending
import no.timnor.app.shared.generated.resources.plannor_account_sync_review
import no.timnor.app.shared.generated.resources.plannor_account_sync_retry
import no.timnor.app.shared.generated.resources.plannor_account_sync_success
import no.timnor.app.shared.generated.resources.plannor_account_syncing
import no.timnor.app.core.repository.SharedWriteStatus
import no.timnor.app.core.repository.SharedWorkReviewSummary
import no.timnor.app.core.repository.SharedWorkResolution
import no.timnor.app.core.repository.SharedWorkSyncCoordinator
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.domain.time.formatStopwatchTime
import no.timnor.app.shared.generated.resources.plannor_account_review_title
import no.timnor.app.shared.generated.resources.plannor_account_review_local
import no.timnor.app.shared.generated.resources.plannor_account_review_remote
import no.timnor.app.shared.generated.resources.plannor_account_review_missing
import no.timnor.app.shared.generated.resources.plannor_account_review_changed
import no.timnor.app.shared.generated.resources.plannor_account_review_allocations_changed
import no.timnor.app.shared.generated.resources.plannor_account_review_access_removed
import no.timnor.app.shared.generated.resources.plannor_account_review_conflict
import no.timnor.app.shared.generated.resources.plannor_account_review_keep_local
import no.timnor.app.shared.generated.resources.plannor_account_review_keep_timnor
import no.timnor.app.shared.generated.resources.plannor_account_review_use_plannor
import no.timnor.app.shared.generated.resources.plannor_account_review_share_again
import no.timnor.app.shared.generated.resources.plannor_account_review_local_note
import no.timnor.app.shared.generated.resources.plannor_account_review_remote_note
import no.timnor.app.shared.generated.resources.plannor_account_review_separate_note
import no.timnor.app.shared.generated.resources.plannor_account_review_retry_deletion
import no.timnor.app.shared.generated.resources.plannor_account_review_leave_remote
import no.timnor.app.shared.generated.resources.plannor_account_review_dismiss
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlannorAccountSection(
    state: PlannorAccountUiState,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onRetrySync: () -> Unit,
    onProjectSelected: (Int, Boolean) -> Unit,
    onProjectLinked: (Int, Int?) -> Unit,
    onAddProjectToTimnor: (Int) -> Unit,
    onKeepLocalOnly: (Int) -> Unit,
    onResolveSharedWork: (Int, SharedWorkResolution) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small),
    ) {
        Text(stringResource(Res.string.plannor_account_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.plannor_account_private_hours),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.session == null) {
            OutlinedTextField(
                value = state.identifier,
                onValueChange = onIdentifierChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.plannor_account_identifier)) },
                singleLine = true,
                enabled = !state.loading,
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.plannor_account_password)) },
                singleLine = true,
                enabled = !state.loading,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            )
            Button(onClick = onSignIn, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.plannor_account_connect))
            }
        } else {
            Text(
                stringResource(Res.string.plannor_account_connected, state.session.username),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                OutlinedButton(onClick = onRefresh, enabled = !state.loading) {
                    Text(stringResource(Res.string.plannor_account_refresh))
                }
                TextButton(onClick = onSignOut, enabled = !state.loading) {
                    Text(stringResource(Res.string.plannor_account_sign_out))
                }
            }
            state.syncStatus?.let { sync ->
                val pending = sync.writes.any { it.status == SharedWriteStatus.PENDING ||
                    it.status == SharedWriteStatus.UNKNOWN || it.status == SharedWriteStatus.ACCESS_REVOKED ||
                    it.status == SharedWriteStatus.CONFLICT || it.status == SharedWriteStatus.REJECTED }
                val statusText = when {
                    state.syncing -> stringResource(Res.string.plannor_account_syncing)
                    sync.failed -> stringResource(Res.string.plannor_account_sync_error)
                    sync.needsReview > 0 -> stringResource(Res.string.plannor_account_sync_review, sync.needsReview)
                    pending -> stringResource(Res.string.plannor_account_sync_pending)
                    else -> stringResource(Res.string.plannor_account_sync_success)
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sync.failed || sync.needsReview > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.syncing) CircularProgressIndicator()
                else if (sync.failed || sync.needsReview > 0 || pending) {
                    TextButton(onClick = onRetrySync) {
                        Text(stringResource(Res.string.plannor_account_sync_retry))
                    }
                }
            }
            if (state.reviewItems.isNotEmpty()) {
                Text(stringResource(Res.string.plannor_account_review_title), style = MaterialTheme.typography.titleSmall)
                state.reviewItems.forEach { review ->
                    SharedWorkReviewCard(review, !state.loading, onKeepLocalOnly, onResolveSharedWork)
                }
            }
            Text(stringResource(Res.string.plannor_account_projects), style = MaterialTheme.typography.titleSmall)
            state.projects.forEach { project ->
                PlannorAccountProjectCard(
                    project = project,
                    localProjectNames = state.localProjects.associate { it.id to it.name },
                    enabled = !state.loading,
                    onSelected = { onProjectSelected(project.projectId, it) },
                    onLinked = { onProjectLinked(project.projectId, it) },
                    onAddToTimnor = { onAddProjectToTimnor(project.projectId) },
                )
            }
        }
        state.error?.let { error ->
            val errorLabel = stringResource(Res.string.plannor_account_error)
            Text(
                stringResource(
                    when (error) {
                        no.timnor.app.core.repository.PlannorConnectionFailure.CREDENTIALS_OR_ACCOUNT,
                        no.timnor.app.core.repository.PlannorConnectionFailure.SESSION_EXPIRED -> Res.string.plannor_account_error_credentials
                        no.timnor.app.core.repository.PlannorConnectionFailure.RATE_LIMITED -> Res.string.plannor_account_error_rate_limited
                        no.timnor.app.core.repository.PlannorConnectionFailure.SERVER -> Res.string.plannor_account_error_server
                        no.timnor.app.core.repository.PlannorConnectionFailure.STORAGE -> Res.string.plannor_account_error_storage
                        no.timnor.app.core.repository.PlannorConnectionFailure.NETWORK -> Res.string.plannor_account_error_network
                    },
                ),
                modifier = Modifier.semantics { contentDescription = errorLabel },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SharedWorkReviewCard(
    review: SharedWorkReviewSummary,
    enabled: Boolean,
    onKeepLocalOnly: (Int) -> Unit,
    onResolve: (Int, SharedWorkResolution) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.timnorSpacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.extraSmall),
        ) {
            Text(review.localWorkDate, style = MaterialTheme.typography.titleSmall)
            val reviewMessage = when (review.reason) {
                SharedWorkSyncCoordinator.REVIEW_REMOTE_ENTRY_MISSING -> null
                SharedWorkSyncCoordinator.REVIEW_REMOTE_ENTRY_CHANGED -> Res.string.plannor_account_review_changed
                SharedWorkSyncCoordinator.REVIEW_REMOTE_ALLOCATIONS_CHANGED ->
                    Res.string.plannor_account_review_allocations_changed
                SharedWorkSyncCoordinator.REVIEW_ACCESS_REMOVED -> Res.string.plannor_account_review_access_removed
                else -> Res.string.plannor_account_review_conflict
            }
            reviewMessage?.let {
                Text(
                    stringResource(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (review.reason == SharedWorkSyncCoordinator.REVIEW_ACCESS_REMOVED)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(
                    Res.string.plannor_account_review_local,
                    formatStopwatchTime(review.localDurationSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (review.remoteDurationSeconds == null || review.remoteWorkDate == null) {
                Text(stringResource(Res.string.plannor_account_review_missing), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    stringResource(
                        Res.string.plannor_account_review_remote,
                        review.remoteWorkDate,
                        formatStopwatchTime(review.remoteDurationSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
                        review.remoteAllocationCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            review.localPrivateNote.takeIf(String::isNotBlank)?.let {
                Text(stringResource(Res.string.plannor_account_review_local_note, it), style = MaterialTheme.typography.bodySmall)
            }
            review.remotePrivateNote?.takeIf(String::isNotBlank)?.let {
                Text(stringResource(Res.string.plannor_account_review_remote_note, it), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                stringResource(Res.string.plannor_account_review_separate_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (review.reason == SharedWorkSyncCoordinator.REVIEW_ACCESS_REMOVED) {
                TextButton(
                    onClick = {
                        if (review.localDeleted) onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                        else onKeepLocalOnly(review.localWorkId)
                    },
                    enabled = enabled,
                ) {
                    Text(
                        stringResource(
                            if (review.localDeleted) Res.string.plannor_account_review_leave_remote
                            else Res.string.plannor_account_review_keep_local,
                        ),
                    )
                }
            } else if (review.localDeleted) {
                if (!review.remoteAllocationsComplete && review.remoteDurationSeconds != null) {
                    TextButton(onClick = {
                        onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                    }, enabled = enabled) {
                        Text(stringResource(Res.string.plannor_account_review_leave_remote))
                    }
                } else if (review.remoteDurationSeconds == null || review.remoteWorkDate == null) {
                    TextButton(onClick = {
                        onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                    }, enabled = enabled) {
                        Text(stringResource(Res.string.plannor_account_review_dismiss))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                        TextButton(onClick = {
                            onResolve(review.localWorkId, SharedWorkResolution.KEEP_TIMNOR)
                        }, enabled = enabled) {
                            Text(stringResource(Res.string.plannor_account_review_retry_deletion))
                        }
                        TextButton(onClick = {
                            onResolve(review.localWorkId, SharedWorkResolution.KEEP_LOCAL_ONLY)
                        }, enabled = enabled) {
                            Text(stringResource(Res.string.plannor_account_review_leave_remote))
                        }
                    }
                }
            } else if (review.remoteDurationSeconds == null || review.remoteWorkDate == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                    TextButton(onClick = {
                        onResolve(review.localWorkId, SharedWorkResolution.SHARE_AGAIN)
                    }, enabled = enabled) {
                        Text(stringResource(Res.string.plannor_account_review_share_again))
                    }
                    TextButton(onClick = { onKeepLocalOnly(review.localWorkId) }, enabled = enabled) {
                        Text(stringResource(Res.string.plannor_account_review_keep_local))
                    }
                }
            } else if (!review.remoteAllocationsComplete) {
                TextButton(onClick = { onKeepLocalOnly(review.localWorkId) }, enabled = enabled) {
                    Text(stringResource(Res.string.plannor_account_review_keep_local))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                    TextButton(onClick = {
                        onResolve(review.localWorkId, SharedWorkResolution.KEEP_TIMNOR)
                    }, enabled = enabled) {
                        Text(stringResource(Res.string.plannor_account_review_keep_timnor))
                    }
                    if (review.managedAllocationPresent && review.projectId == review.remoteProjectId) {
                        TextButton(onClick = {
                            onResolve(review.localWorkId, SharedWorkResolution.USE_PLANNOR)
                        }, enabled = enabled) {
                            Text(stringResource(Res.string.plannor_account_review_use_plannor))
                        }
                    }
                    TextButton(onClick = { onKeepLocalOnly(review.localWorkId) }, enabled = enabled) {
                        Text(stringResource(Res.string.plannor_account_review_keep_local))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannorAccountProjectCard(
    project: PlannorProjectSelection,
    localProjectNames: Map<Int, String>,
    enabled: Boolean,
    onSelected: (Boolean) -> Unit,
    onLinked: (Int?) -> Unit,
    onAddToTimnor: () -> Unit,
) {
    var expanded by remember(project.projectId) { mutableStateOf(false) }
    val linkedName = project.linkedLocalProjectId?.let(localProjectNames::get)
    val displayName = if (project.available) project.name else stringResource(Res.string.plannor_account_project_unavailable)
    val selectionLabel = stringResource(
        if (project.selected) Res.string.plannor_account_project_checked else Res.string.plannor_account_project_unchecked,
        displayName,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.timnorSpacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.extraSmall),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = project.selected,
                    onCheckedChange = onSelected,
                    enabled = enabled && !project.archived,
                    modifier = Modifier.semantics { contentDescription = selectionLabel },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, style = MaterialTheme.typography.titleSmall)
                    val context = if (project.available) listOfNotNull(project.organizationName, project.contextLabel)
                        .filter(String::isNotBlank)
                        .joinToString(" · ") else ""
                    if (context.isNotBlank()) {
                        Text(
                            stringResource(Res.string.plannor_account_context, context),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (linkedName == null && project.selected && !project.archived) {
                    TextButton(onClick = onAddToTimnor, enabled = enabled) {
                        Text(stringResource(Res.string.plannor_account_add_project))
                    }
                }
                OutlinedButton(onClick = { expanded = true }, enabled = enabled) {
                    Text(linkedName ?: stringResource(Res.string.plannor_account_no_link))
                }
                Text(
                    stringResource(Res.string.plannor_account_link_local),
                    modifier = Modifier.padding(start = MaterialTheme.timnorSpacing.small),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.plannor_account_no_link)) },
                        onClick = { expanded = false; onLinked(null) },
                    )
                    localProjectNames.toList().sortedBy { it.second.lowercase() }.forEach { (localId, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = { expanded = false; onLinked(localId) },
                        )
                    }
                }
            }
        }
    }
}
