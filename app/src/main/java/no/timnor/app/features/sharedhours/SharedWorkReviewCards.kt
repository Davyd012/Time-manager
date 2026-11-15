package no.timnor.app.features.sharedhours

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import no.timnor.app.R
import no.timnor.app.core.repository.SharedWorkResolution
import no.timnor.app.core.repository.SharedWorkJournal
import no.timnor.app.core.repository.SharedWorkReviewItem
import no.timnor.app.shared.ui.theme.timnorSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SharedWorkReviewCards(
    reviews: List<SharedWorkReviewItem>,
    loading: Boolean,
    onResolve: (Int, SharedWorkResolution) -> Unit,
) {
    if (reviews.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
        reviews.forEach { review ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(MaterialTheme.timnorSpacing.medium),
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
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
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

