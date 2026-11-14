package no.timnor.app.core.repository

import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation

data class SharedWorkRemoteState(
    val entryId: String,
    val entryRevision: Int,
    val durationSeconds: Long,
    val workDate: String,
    val privateNote: String?,
    val allocatedSeconds: Long,
    val allocationId: String?,
    val allocationRevision: Int?,
    val allocationProjectId: Int?,
    val allocationTaskId: Int?,
    val allocationSeconds: Long?,
    val allocationCount: Int,
    val allocationRowsTotalSeconds: Long = allocatedSeconds,
    val allocationSnapshots: List<SharedWorkAllocationSnapshot> = emptyList(),
)

object SharedWorkReviewReasons {
    const val REMOTE_ENTRY_CHANGED = "remote_entry_changed"
    const val REMOTE_ALLOCATIONS_CHANGED = "remote_allocations_changed"
    const val REMOTE_ALLOCATIONS_UNAVAILABLE = "remote_allocations_unavailable"
}

/** Selects the allocation created by one outbox command, even when its entry has other allocations. */
fun selectCreatedAllocation(
    allocations: List<ProjectTimeAllocation>,
    allocationId: String?,
    projectId: Int,
    taskId: Int?,
    workDate: String,
    durationSeconds: Long,
    sharedNote: String?,
): ProjectTimeAllocation? {
    fun matches(item: ProjectTimeAllocation) = item.projectId == projectId && item.taskId == taskId &&
        item.workDate == workDate && item.durationSeconds == durationSeconds && item.sharedNote == sharedNote
    return if (allocationId != null) {
        allocations.singleOrNull { it.allocationId == allocationId }?.takeIf(::matches)
    } else {
        allocations.filter(::matches).singleOrNull()
    }
}

fun sharedWorkRemoteChangeReason(
    baseline: SharedWorkBaseline,
    remote: SharedWorkRemoteState,
    expectedProjectId: Int,
): String? {
    val legacyBaseline = baseline.entryDurationSeconds == 0L
    return when {
        !legacyBaseline && baseline.entryWorkDate != null && remote.workDate != baseline.entryWorkDate ->
            SharedWorkReviewReasons.REMOTE_ENTRY_CHANGED
        !legacyBaseline && remote.durationSeconds != baseline.entryDurationSeconds ->
            SharedWorkReviewReasons.REMOTE_ENTRY_CHANGED
        !legacyBaseline && remote.privateNote != baseline.entryPrivateNote ->
            SharedWorkReviewReasons.REMOTE_ENTRY_CHANGED
        remote.entryRevision != baseline.entryRevision ->
            SharedWorkReviewReasons.REMOTE_ALLOCATIONS_CHANGED
        remote.allocationRowsTotalSeconds != remote.allocatedSeconds ->
            SharedWorkReviewReasons.REMOTE_ALLOCATIONS_UNAVAILABLE
        baseline.allocationSnapshots.isNotEmpty() &&
            remote.allocationSnapshots.sortedBy { it.allocationId } != baseline.allocationSnapshots.sortedBy { it.allocationId } ->
            SharedWorkReviewReasons.REMOTE_ALLOCATIONS_CHANGED
        baseline.allocationId != null && remote.allocationId == null ->
            SharedWorkReviewReasons.REMOTE_ALLOCATIONS_CHANGED
        remote.allocationId != baseline.allocationId -> SharedWorkReviewReasons.REMOTE_ALLOCATIONS_CHANGED
        remote.allocationRevision != baseline.allocationRevision -> SharedWorkReviewReasons.REMOTE_ALLOCATIONS_CHANGED
        remote.allocationId != null && remote.allocationProjectId != expectedProjectId ->
            SharedWorkReviewReasons.REMOTE_ALLOCATIONS_CHANGED
        remote.allocationId != null && !legacyBaseline && remote.allocationTaskId != baseline.allocationTaskId ->
            SharedWorkReviewReasons.REMOTE_ALLOCATIONS_CHANGED
        remote.allocationId != null && !legacyBaseline && remote.allocationSeconds != baseline.allocationDurationSeconds ->
            SharedWorkReviewReasons.REMOTE_ALLOCATIONS_CHANGED
        else -> null
    }
}

sealed interface SharedWorkReconciliation {
    data object Current : SharedWorkReconciliation
    data object Conflict : SharedWorkReconciliation
    data object DeleteAllocation : SharedWorkReconciliation
    data object DeleteEntry : SharedWorkReconciliation
    data class CreateAllocation(val durationSeconds: Long, val taskId: Int?) : SharedWorkReconciliation
    data class UpdateAllocation(val durationSeconds: Long, val taskId: Int?) : SharedWorkReconciliation
    data class UpdateEntry(val durationSeconds: Long, val workDate: String, val privateNote: String?) : SharedWorkReconciliation
}

/** Plans one revision-checked write; callers must persist its immutable request before sending. */
fun planSharedWorkReconciliation(
    intent: SharedWorkIntent,
    baseline: SharedWorkBaseline,
    remote: SharedWorkRemoteState,
    currentLinkedLocalProjectId: Int?,
): SharedWorkReconciliation {
    if (intent.localProjectId != currentLinkedLocalProjectId || remote.entryId != baseline.entryId ||
        remote.entryRevision != baseline.entryRevision || remote.allocationId != baseline.allocationId ||
        remote.allocationRevision != baseline.allocationRevision ||
        (remote.allocationId != null && (remote.allocationProjectId != baseline.allocationProjectId ||
            remote.allocationSeconds == null || remote.allocationTaskId != baseline.allocationTaskId))
    ) return SharedWorkReconciliation.Conflict

    if (remote.allocationId != null && remote.allocatedSeconds < (remote.allocationSeconds ?: 0L)) {
        return SharedWorkReconciliation.Conflict
    }

    if (!intent.deleted && baseline.allocationProjectId != null && baseline.allocationProjectId != intent.projectId) {
        if (remote.allocationId != null) return SharedWorkReconciliation.DeleteAllocation
        val previousAllocationSeconds = baseline.allocationDurationSeconds ?: return SharedWorkReconciliation.Conflict
        val desiredSeconds = minOf(previousAllocationSeconds, intent.durationSeconds)
        val availableSeconds = remote.durationSeconds - remote.allocatedSeconds
        if (desiredSeconds <= 0 || desiredSeconds > availableSeconds) return SharedWorkReconciliation.Conflict
        return SharedWorkReconciliation.CreateAllocation(desiredSeconds, intent.taskId)
    }

    if (intent.deleted) {
        if (!intent.deletionApproved) return SharedWorkReconciliation.Conflict
        if (remote.allocationId != null) return SharedWorkReconciliation.DeleteAllocation
        return if (remote.allocatedSeconds == 0L) SharedWorkReconciliation.DeleteEntry
        else SharedWorkReconciliation.Conflict
    }
    val allocationSeconds = remote.allocationSeconds ?: run {
        val previousAllocationSeconds = baseline.allocationDurationSeconds ?: return SharedWorkReconciliation.Conflict
        val desired = minOf(previousAllocationSeconds, intent.durationSeconds)
        val available = remote.durationSeconds - remote.allocatedSeconds
        if (desired <= 0 || desired > available) return SharedWorkReconciliation.Conflict
        return SharedWorkReconciliation.CreateAllocation(desired, intent.taskId)
    }
    if (intent.durationSeconds <= 0) return SharedWorkReconciliation.Conflict
    val otherAllocatedSeconds = remote.allocatedSeconds - allocationSeconds
    val allocationCapacity = intent.durationSeconds - otherAllocatedSeconds
    if (allocationCapacity < 0) return SharedWorkReconciliation.Conflict
    val desiredAllocationSeconds = if (baseline.allocationTracksEntry && otherAllocatedSeconds == 0L) {
        intent.durationSeconds
    } else minOf(allocationSeconds, allocationCapacity)
    if (desiredAllocationSeconds != allocationSeconds || remote.allocationTaskId != intent.taskId) {
        if (desiredAllocationSeconds < allocationSeconds) {
            return SharedWorkReconciliation.UpdateAllocation(desiredAllocationSeconds, intent.taskId)
        }
    }
    val privateNote = intent.privateNote.ifBlank { null }
    if (intent.durationSeconds != remote.durationSeconds || intent.workDate != remote.workDate ||
        privateNote != remote.privateNote
    ) {
        return SharedWorkReconciliation.UpdateEntry(intent.durationSeconds, intent.workDate, privateNote)
    }
    if (desiredAllocationSeconds != allocationSeconds || remote.allocationTaskId != intent.taskId) {
        return SharedWorkReconciliation.UpdateAllocation(desiredAllocationSeconds, intent.taskId)
    }
    return SharedWorkReconciliation.Current
}
