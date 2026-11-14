package no.timnor.app.core.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.uuid.Uuid
import no.timnor.app.shared.features.shared_hours.domain.PersonalTimeEntry
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOperation
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem

data class SharedWorkSyncStatus(
    val writes: List<SharedWriteResult> = emptyList(),
    val needsReview: Int = 0,
    val failed: Boolean = false,
)

data class SharedWorkReviewSummary(
    val localWorkId: Int,
    val reason: String,
    val localWorkDate: String,
    val localDurationSeconds: Long,
    val remoteWorkDate: String? = null,
    val remoteDurationSeconds: Long? = null,
    val remoteAllocationCount: Int = 0,
    val localPrivateNote: String = "",
    val remotePrivateNote: String? = null,
    val managedAllocationPresent: Boolean = false,
    val projectId: Int? = null,
    val remoteProjectId: Int? = null,
    val localDeleted: Boolean = false,
    val remoteAllocationsComplete: Boolean = false,
)

/** Shared create, retry, pull, and conflict detection path used by the iOS host. */
class SharedWorkSyncCoordinator(
    private val sessions: PlannorSessionStorage,
    private val records: SharedHoursRecordStorage,
    private val api: PlannorApi,
    private val applyRemoteVersion: suspend (SharedWorkIntent, PersonalTimeEntry, ProjectTimeAllocation?) -> Boolean =
        { _, _, _ -> false },
) {
    private val mutex = Mutex()
    private val json = Json { encodeDefaults = true }

    suspend fun sync(): SharedWorkSyncStatus = mutex.withLock {
        val accountId = sessions.read()?.accountId ?: return@withLock SharedWorkSyncStatus()
        try {
            prepareCreates(accountId)
            val writes = api.retryPending()
            val allocations = api.refreshProjectSnapshots()
            val entries = api.loadPersonalEntries()
            if (sessions.read()?.accountId != accountId) return@withLock SharedWorkSyncStatus(failed = true)
            captureInitialBaselines(accountId, entries, allocations)
            reconcileLocalChanges(accountId, entries, allocations)
            markRemoteChanges(accountId, entries, allocations)
            SharedWorkSyncStatus(writes, reviewCount(accountId))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            SharedWorkSyncStatus(needsReview = reviewCount(accountId), failed = true)
        }
    }

    suspend fun reviewItems(): List<SharedWorkReviewSummary> {
        val accountId = sessions.read()?.accountId ?: return emptyList()
        val intents = records.getAll(accountId, TYPE_WORK_INTENT, SharedWorkIntent.serializer())
            .filter { it.reviewReason != null && !it.detachedFromLocalWork }
        if (intents.isEmpty()) return emptyList()
        val entries = api.loadPersonalEntries()
        val allocations = api.refreshProjectSnapshots()
        if (sessions.read()?.accountId != accountId) return emptyList()
        return intents.map { intent ->
            val baseline = records.get(accountId, TYPE_WORK_BASELINE, intent.localWorkId.toString(),
                SharedWorkBaseline.serializer())
            val entry = baseline?.let { saved -> entries.singleOrNull {
                it.accountId == accountId && it.serverId == saved.entryId && it.sourceWorkId == intent.sourceId
            } }
            val matching = entry?.let { remote -> allocations.filter {
                it.accountId == accountId && it.entryId == remote.serverId
            } }.orEmpty()
            val count = matching.size
            val managed = baseline?.allocationId?.let { id -> matching.singleOrNull { it.allocationId == id } }
            SharedWorkReviewSummary(
                localWorkId = intent.localWorkId,
                reason = intent.reviewReason.orEmpty(),
                localWorkDate = intent.workDate,
                localDurationSeconds = intent.durationSeconds,
                remoteWorkDate = entry?.workDate,
                remoteDurationSeconds = entry?.durationSeconds,
                remoteAllocationCount = count,
                localPrivateNote = intent.privateNote,
                remotePrivateNote = entry?.privateNote,
                managedAllocationPresent = managed != null,
                projectId = intent.projectId,
                remoteProjectId = managed?.projectId,
                localDeleted = intent.deleted,
                remoteAllocationsComplete = entry != null &&
                    entry.allocatedSeconds == matching.sumOf(ProjectTimeAllocation::durationSeconds) &&
                    entry.allocatedSeconds <= entry.durationSeconds,
            )
        }
    }

    /** Stops future sharing while preserving the user's local row and the Plannor record. */
    suspend fun keepLocalOnly(localWorkId: Int): Boolean {
        val accountId = sessions.read()?.accountId ?: return false
        val intent = records.get(accountId, TYPE_WORK_INTENT, localWorkId.toString(), SharedWorkIntent.serializer())
            ?: return false
        if (intent.reviewReason == null || intent.accountId != accountId || intent.activeCommandKey != null) return false
        val baseline = records.get(accountId, TYPE_WORK_BASELINE, localWorkId.toString(), SharedWorkBaseline.serializer())
        if (baseline == null) {
            if (records.get(accountId, PlannorApi.TYPE_OUTBOX, intent.createKey, SharedTimeOutboxItem.serializer()) != null) return false
            records.delete(accountId, TYPE_WORK_INTENT, localWorkId.toString())
            return sessions.read()?.accountId == accountId
        }
        val entries = api.loadPersonalEntries()
        if (sessions.read()?.accountId != accountId) return false
        val remote = entries.singleOrNull { it.accountId == accountId && it.serverId == baseline.entryId }
        if (remote != null && remote.sourceWorkId != intent.sourceId) return false
        records.delete(accountId, TYPE_WORK_BASELINE, localWorkId.toString())
        records.delete(accountId, TYPE_WORK_INTENT, localWorkId.toString())
        return sessions.read()?.accountId == accountId
    }

    /** Explicit conflict resolution. The newest complete snapshot becomes the next baseline. */
    suspend fun resolve(localWorkId: Int, resolution: SharedWorkResolution): Boolean {
        val accountId = sessions.read()?.accountId ?: return false
        val key = localWorkId.toString()
        var intent = records.get(accountId, TYPE_WORK_INTENT, key, SharedWorkIntent.serializer()) ?: return false
        if (intent.accountId != accountId || intent.reviewReason == null || intent.activeCommandKey != null ||
            intent.detachedFromLocalWork
        ) return false
        val baseline = records.get(accountId, TYPE_WORK_BASELINE, key, SharedWorkBaseline.serializer())
        if (baseline == null) {
            if (records.get(accountId, PlannorApi.TYPE_OUTBOX, intent.createKey, SharedTimeOutboxItem.serializer()) != null) return false
            return when (resolution) {
                SharedWorkResolution.KEEP_LOCAL_ONLY -> keepLocalOnly(localWorkId)
                SharedWorkResolution.KEEP_TIMNOR -> {
                    if (intent.deleted) records.delete(accountId, TYPE_WORK_INTENT, key)
                    else records.put(
                        accountId, TYPE_WORK_INTENT, key,
                        intent.copy(queuedVersion = null, reviewReason = null),
                        SharedWorkIntent.serializer(), Clock.System.now().toString(),
                    )
                    sessions.read()?.accountId == accountId
                }
                else -> false
            }
        }
        val entries = api.loadPersonalEntries()
        val allocations = api.refreshProjectSnapshots()
        if (sessions.read()?.accountId != accountId) return false
        val entry = entries.singleOrNull {
            it.accountId == accountId && it.serverId == baseline.entryId && it.sourceWorkId == intent.sourceId
        }
        if (entry == null) {
            if (intent.reviewReason != REVIEW_REMOTE_ENTRY_MISSING) return false
            return when (resolution) {
                SharedWorkResolution.SHARE_AGAIN -> {
                    val restarted = intent.copy(
                        sourceId = Uuid.random().toString(),
                        createKey = Uuid.random().toString(),
                        localVersion = 1,
                        queuedVersion = null,
                        activeCommandKey = null,
                        reviewReason = null,
                    )
                    records.delete(accountId, TYPE_WORK_BASELINE, key)
                    records.put(accountId, TYPE_WORK_INTENT, key, restarted, SharedWorkIntent.serializer(), Clock.System.now().toString())
                    true
                }
                SharedWorkResolution.KEEP_LOCAL_ONLY -> keepLocalOnly(localWorkId)
                else -> false
            }
        }
        if (resolution == SharedWorkResolution.SHARE_AGAIN) return false
        if (resolution == SharedWorkResolution.KEEP_LOCAL_ONLY) return keepLocalOnly(localWorkId)
        if (intent.deleted && resolution == SharedWorkResolution.USE_PLANNOR) return false

        val matching = allocations.filter { it.accountId == accountId && it.entryId == entry.serverId }
        val allocatedTotal = matching.sumOf(ProjectTimeAllocation::durationSeconds)
        if (allocatedTotal != entry.allocatedSeconds || allocatedTotal > entry.durationSeconds) return false
        val managed = baseline.allocationId?.let { allocationId ->
            matching.singleOrNull { it.allocationId == allocationId }
        }
        if (baseline.allocationId != null && managed == null && resolution == SharedWorkResolution.USE_PLANNOR) return false
        val nextBaseline = SharedWorkBaseline(
            entryId = entry.serverId,
            entryRevision = entry.revision,
            allocationId = managed?.allocationId,
            allocationRevision = managed?.revision,
            entryDurationSeconds = entry.durationSeconds,
            allocationDurationSeconds = managed?.durationSeconds ?: baseline.allocationDurationSeconds,
            allocationTaskId = managed?.taskId,
            allocationTracksEntry = managed?.durationSeconds == entry.durationSeconds,
            entryWorkDate = entry.workDate,
            entryPrivateNote = entry.privateNote,
            allocationProjectId = managed?.projectId ?: baseline.allocationProjectId,
        )
        when (resolution) {
            SharedWorkResolution.KEEP_TIMNOR -> {
                records.put(accountId, TYPE_WORK_BASELINE, key, nextBaseline, SharedWorkBaseline.serializer(), Clock.System.now().toString())
                intent = intent.copy(
                    queuedVersion = intent.localVersion - 1,
                    reviewReason = null,
                    activeCommandKey = null,
                    deletionApproved = intent.deleted,
                )
            }
            SharedWorkResolution.USE_PLANNOR -> {
                if (entry.durationSeconds !in 1..Int.MAX_VALUE.toLong() || !applyRemoteVersion(intent, entry, managed)) return false
                if (sessions.read()?.accountId != accountId) return false
                intent = records.get(accountId, TYPE_WORK_INTENT, key, SharedWorkIntent.serializer()) ?: return false
                intent = intent.copy(
                    queuedVersion = intent.localVersion,
                    workDate = entry.workDate,
                    durationSeconds = entry.durationSeconds,
                    privateNote = entry.privateNote.orEmpty(),
                    taskId = managed?.taskId,
                    reviewReason = null,
                    activeCommandKey = null,
                )
                records.put(accountId, TYPE_WORK_BASELINE, key, nextBaseline, SharedWorkBaseline.serializer(), Clock.System.now().toString())
            }
            else -> return false
        }
        records.put(accountId, TYPE_WORK_INTENT, key, intent, SharedWorkIntent.serializer(), Clock.System.now().toString())
        return sessions.read()?.accountId == accountId
    }

    private suspend fun prepareCreates(accountId: Int) {
        if (sessions.read()?.accountId != accountId) return
        val projects = records.getAll(accountId, PlannorApi.TYPE_PROJECT, PlannorProjectSelection.serializer())
            .associateBy(PlannorProjectSelection::projectId)
        records.getAll(accountId, TYPE_WORK_INTENT, SharedWorkIntent.serializer()).forEach { intent ->
            if (intent.accountId != accountId || intent.deleted || intent.durationSeconds <= 0 ||
                intent.queuedVersion != null || intent.activeCommandKey != null || intent.reviewReason != null ||
                intent.localWorkId < 1
            ) return@forEach
            val project = projects[intent.projectId] ?: return@forEach
            if (!project.selected || project.archived || !project.available ||
                project.linkedLocalProjectId != intent.localProjectId
            ) return@forEach
            val existing = records.get(accountId, PlannorApi.TYPE_OUTBOX, intent.createKey, SharedTimeOutboxItem.serializer())
            if (existing?.errorCode == "restore_review_required") return@forEach
            val request = existing ?: SharedTimeOutboxItem(
                accountId = accountId,
                operation = SharedTimeOperation.CREATE_AND_ALLOCATE,
                outcome = SharedTimeOutcome.PENDING,
                projectId = intent.projectId,
                taskId = intent.taskId,
                workDate = intent.workDate,
                durationSeconds = intent.durationSeconds,
                sourceId = intent.sourceId,
                privateNote = intent.privateNote.ifBlank { null },
                sharedNote = null,
                atomicIdempotencyKey = intent.createKey,
            )
            if (request.operation != SharedTimeOperation.CREATE_AND_ALLOCATE ||
                request.atomicIdempotencyKey != intent.createKey || request.sourceId != intent.sourceId
            ) return@forEach
            if (sessions.read()?.accountId != accountId) return
            records.persistSharedWorkCreate(intent, request)
        }
    }

    private suspend fun captureInitialBaselines(
        accountId: Int,
        entries: List<PersonalTimeEntry>,
        allocations: List<ProjectTimeAllocation>,
    ) {
        records.getAll(accountId, TYPE_WORK_INTENT, SharedWorkIntent.serializer()).forEach { intent ->
            if (intent.queuedVersion == null ||
                records.get(accountId, TYPE_WORK_BASELINE, intent.localWorkId.toString(), SharedWorkBaseline.serializer()) != null
            ) return@forEach
            val request = records.get(accountId, PlannorApi.TYPE_OUTBOX, intent.createKey, SharedTimeOutboxItem.serializer())
                ?.takeIf { it.outcome == SharedTimeOutcome.SUCCEEDED } ?: return@forEach
            val entry = entries.singleOrNull {
                it.accountId == accountId && it.serverId == request.entryId && it.sourceWorkId == intent.sourceId
            } ?: return@forEach
            val matching = allocations.filter { it.accountId == accountId && it.entryId == entry.serverId }
            val allocation = selectCreatedAllocation(
                allocations = matching,
                allocationId = request.allocationId,
                projectId = request.projectId,
                taskId = request.taskId,
                workDate = request.workDate,
                durationSeconds = request.durationSeconds,
                sharedNote = request.sharedNote,
            ) ?: return@forEach
            if (entry.revision != request.entryRevision || entry.workDate != request.workDate ||
                entry.durationSeconds != request.durationSeconds || entry.privateNote != request.privateNote ||
                allocation.projectId != intent.projectId || allocation.taskId != intent.taskId ||
                allocation.durationSeconds != request.durationSeconds || allocation.sharedNote != request.sharedNote ||
                allocation.revision != 1 ||
                matching.sumOf(ProjectTimeAllocation::durationSeconds) != entry.allocatedSeconds ||
                entry.allocatedSeconds > entry.durationSeconds
            ) return@forEach
            val baseline = SharedWorkBaseline(
                entryId = entry.serverId,
                entryRevision = entry.revision,
                allocationId = allocation.allocationId,
                allocationRevision = allocation.revision,
                entryDurationSeconds = entry.durationSeconds,
                allocationDurationSeconds = allocation.durationSeconds,
                allocationTaskId = allocation.taskId,
                allocationTracksEntry = allocation.durationSeconds == entry.durationSeconds,
                entryWorkDate = entry.workDate,
                entryPrivateNote = entry.privateNote,
                allocationProjectId = allocation.projectId,
            )
            records.put(
                accountId, TYPE_WORK_BASELINE, intent.localWorkId.toString(), baseline,
                SharedWorkBaseline.serializer(), Clock.System.now().toString(),
            )
        }
    }

    private suspend fun markRemoteChanges(
        accountId: Int,
        entries: List<PersonalTimeEntry>,
        allocations: List<ProjectTimeAllocation>,
    ) {
        records.getAll(accountId, TYPE_WORK_INTENT, SharedWorkIntent.serializer()).forEach { intent ->
            if (intent.detachedFromLocalWork || intent.activeCommandKey != null ||
                intent.queuedVersion != intent.localVersion
            ) return@forEach
            val key = intent.localWorkId.toString()
            val baseline = records.get(accountId, TYPE_WORK_BASELINE, key, SharedWorkBaseline.serializer())
                ?: return@forEach
            val entry = entries.singleOrNull { it.accountId == accountId && it.serverId == baseline.entryId }
            if (entry == null) {
                updateReviewReason(accountId, intent, REVIEW_REMOTE_ENTRY_MISSING)
                return@forEach
            }
            if (entry.sourceWorkId != intent.sourceId) return@forEach
            val project = records.get(accountId, PlannorApi.TYPE_PROJECT, intent.projectId.toString(),
                PlannorProjectSelection.serializer())
            if (project == null || !project.available) {
                updateReviewReason(accountId, intent, REVIEW_ACCESS_REMOVED)
                return@forEach
            }
            val matching = allocations.filter { it.accountId == accountId && it.entryId == entry.serverId }
            val managed = matching.singleOrNull { it.allocationId == baseline.allocationId }
            val complete = matching.sumOf(ProjectTimeAllocation::durationSeconds) == entry.allocatedSeconds &&
                entry.allocatedSeconds <= entry.durationSeconds
            val reason = when {
                entry.workDate != baseline.entryWorkDate || entry.durationSeconds != baseline.entryDurationSeconds ||
                    entry.privateNote != baseline.entryPrivateNote ->
                    REVIEW_REMOTE_ENTRY_CHANGED
                !complete || managed == null || managed.revision != baseline.allocationRevision ||
                    managed.durationSeconds != baseline.allocationDurationSeconds ||
                    managed.taskId != baseline.allocationTaskId || managed.projectId != baseline.allocationProjectId ->
                    REVIEW_REMOTE_ALLOCATIONS_CHANGED
                entry.revision != baseline.entryRevision -> REVIEW_REMOTE_ALLOCATIONS_CHANGED
                else -> null
            }
            updateReviewReason(accountId, intent, reason)
        }
    }

    /** Reconciles iOS local edits through the same persisted, revision-checked API outbox. */
    private suspend fun reconcileLocalChanges(
        accountId: Int,
        entries: List<PersonalTimeEntry>,
        allocations: List<ProjectTimeAllocation>,
    ) {
        records.getAll(accountId, TYPE_WORK_INTENT, SharedWorkIntent.serializer()).forEach { original ->
            if (original.detachedFromLocalWork) return@forEach
            var intent = records.get(accountId, TYPE_WORK_INTENT, original.localWorkId.toString(), SharedWorkIntent.serializer())
                ?: return@forEach
            val key = intent.localWorkId.toString()
            val baseline = records.get(accountId, TYPE_WORK_BASELINE, key, SharedWorkBaseline.serializer()) ?: return@forEach
            val entry = entries.singleOrNull { it.accountId == accountId && it.serverId == baseline.entryId }
            val matching = allocations.filter { it.accountId == accountId && it.entryId == baseline.entryId }

            intent.activeCommandKey?.let { activeKey ->
                val command = records.get(accountId, PlannorApi.TYPE_OUTBOX, activeKey, SharedTimeOutboxItem.serializer())
                    ?: return@forEach
                if (command.outcome != SharedTimeOutcome.SUCCEEDED) {
                    if (command.outcome in setOf(SharedTimeOutcome.CONFLICT, SharedTimeOutcome.REJECTED,
                            SharedTimeOutcome.ACCESS_REVOKED)
                    ) {
                        intent = intent.copy(activeCommandKey = null)
                        records.put(accountId, TYPE_WORK_INTENT, key, intent, SharedWorkIntent.serializer(),
                            Clock.System.now().toString())
                        updateReviewReason(accountId, intent, when (command.outcome) {
                            SharedTimeOutcome.ACCESS_REVOKED -> REVIEW_ACCESS_REMOVED
                            SharedTimeOutcome.CONFLICT -> REVIEW_RECONCILIATION_CONFLICT
                            else -> command.errorCode ?: REVIEW_RECONCILIATION_CONFLICT
                        })
                    }
                    return@forEach
                }
                if (command.operation == SharedTimeOperation.DELETE_ENTRY) {
                    if (entry == null && intent.deleted) {
                        records.delete(accountId, TYPE_WORK_BASELINE, key)
                        records.delete(accountId, TYPE_WORK_INTENT, key)
                    } else updateReviewReason(accountId, intent, REVIEW_REMOTE_ENTRY_MISSING)
                    return@forEach
                }
                val observed = entry ?: run {
                    updateReviewReason(accountId, intent, REVIEW_REMOTE_ENTRY_MISSING)
                    return@forEach
                }
                if (observed.revision != baseline.entryRevision + 1 || observed.sourceWorkId != intent.sourceId ||
                    matching.sumOf(ProjectTimeAllocation::durationSeconds) != observed.allocatedSeconds
                ) {
                    updateReviewReason(accountId, intent, REVIEW_REMOTE_ALLOCATIONS_CHANGED)
                    return@forEach
                }
                if (command.operation == SharedTimeOperation.DELETE_ALLOCATION) {
                    if (matching.any { it.allocationId == baseline.allocationId }) {
                        updateReviewReason(accountId, intent, REVIEW_REMOTE_ALLOCATIONS_CHANGED)
                        return@forEach
                    }
                    if (intent.deleted && matching.isNotEmpty()) {
                        records.delete(accountId, TYPE_WORK_BASELINE, key)
                        records.delete(accountId, TYPE_WORK_INTENT, key)
                        return@forEach
                    }
                    records.put(accountId, TYPE_WORK_BASELINE, key, baseline.copy(
                        entryRevision = observed.revision,
                        allocationId = null,
                        allocationRevision = null,
                    ), SharedWorkBaseline.serializer(), Clock.System.now().toString())
                } else {
                    val allocationId = if (command.operation == SharedTimeOperation.ALLOCATE_EXISTING) {
                        command.allocationId
                    } else baseline.allocationId
                    val managed = matching.singleOrNull { it.allocationId == allocationId }
                    if (managed == null || managed.revision != when (command.operation) {
                            SharedTimeOperation.ALLOCATE_EXISTING -> 1
                            SharedTimeOperation.UPDATE_ALLOCATION -> (baseline.allocationRevision ?: 0) + 1
                            else -> baseline.allocationRevision
                        } || observed.allocatedSeconds > observed.durationSeconds
                    ) {
                        updateReviewReason(accountId, intent, REVIEW_REMOTE_ALLOCATIONS_CHANGED)
                        return@forEach
                    }
                    val next = baseline.copy(
                        entryRevision = observed.revision,
                        allocationId = managed.allocationId,
                        allocationRevision = managed.revision,
                        allocationDurationSeconds = managed.durationSeconds,
                        allocationTaskId = managed.taskId,
                        allocationProjectId = managed.projectId,
                        allocationTracksEntry = managed.durationSeconds == observed.durationSeconds,
                        entryDurationSeconds = observed.durationSeconds,
                        entryWorkDate = observed.workDate,
                        entryPrivateNote = observed.privateNote,
                    )
                    records.put(accountId, TYPE_WORK_BASELINE, key, next, SharedWorkBaseline.serializer(), Clock.System.now().toString())
                }
                intent = intent.copy(activeCommandKey = null)
                records.put(accountId, TYPE_WORK_INTENT, key, intent, SharedWorkIntent.serializer(), Clock.System.now().toString())
            }

            intent = records.get(accountId, TYPE_WORK_INTENT, key, SharedWorkIntent.serializer()) ?: return@forEach
            if (intent.queuedVersion == intent.localVersion || intent.activeCommandKey != null || intent.reviewReason != null) return@forEach
            val currentBaseline = records.get(accountId, TYPE_WORK_BASELINE, key, SharedWorkBaseline.serializer()) ?: return@forEach
            val currentEntry = entries.singleOrNull { it.accountId == accountId && it.serverId == currentBaseline.entryId }
                ?: run {
                    updateReviewReason(accountId, intent, REVIEW_REMOTE_ENTRY_MISSING)
                    return@forEach
                }
            if (currentEntry.sourceWorkId != intent.sourceId) return@forEach
            val currentProject = records.get(accountId, PlannorApi.TYPE_PROJECT, intent.projectId.toString(),
                PlannorProjectSelection.serializer())
            if (currentProject == null || !currentProject.available) {
                updateReviewReason(accountId, intent, REVIEW_ACCESS_REMOVED)
                return@forEach
            }
            val currentAllocations = allocations.filter { it.accountId == accountId && it.entryId == currentEntry.serverId }
            val managed = currentAllocations.singleOrNull { it.allocationId == currentBaseline.allocationId }
            if (currentAllocations.sumOf(ProjectTimeAllocation::durationSeconds) != currentEntry.allocatedSeconds) {
                updateReviewReason(accountId, intent, SharedWorkReviewReasons.REMOTE_ALLOCATIONS_UNAVAILABLE)
                return@forEach
            }
            val remote = SharedWorkRemoteState(
                currentEntry.serverId, currentEntry.revision, currentEntry.durationSeconds, currentEntry.workDate,
                currentEntry.privateNote, currentEntry.allocatedSeconds, managed?.allocationId, managed?.revision,
                managed?.projectId, managed?.taskId, managed?.durationSeconds, currentAllocations.size,
                currentAllocations.sumOf(ProjectTimeAllocation::durationSeconds),
            )
            val remoteReason = sharedWorkRemoteChangeReason(
                currentBaseline, remote, currentBaseline.allocationProjectId ?: intent.projectId,
            )
            if (remoteReason != null) {
                updateReviewReason(accountId, intent, remoteReason)
                return@forEach
            }
            val replacement = if (currentProject?.linkedLocalProjectId == intent.localProjectId && currentProject.selected &&
                !currentProject.archived && currentProject.available
            ) currentProject else records.getAll(accountId, PlannorApi.TYPE_PROJECT, PlannorProjectSelection.serializer())
                .singleOrNull { it.linkedLocalProjectId == intent.localProjectId && it.selected && !it.archived && it.available }
            if (!intent.deleted && replacement == null) {
                updateReviewReason(accountId, intent, REVIEW_PROJECT_MOVE_UNAVAILABLE)
                return@forEach
            }
            if (replacement != null && replacement.projectId != intent.projectId) {
                intent = intent.copy(projectId = replacement.projectId, taskId = null)
                records.put(accountId, TYPE_WORK_INTENT, key, intent, SharedWorkIntent.serializer(), Clock.System.now().toString())
            }
            val localProjectId = replacement?.linkedLocalProjectId
            val action = planSharedWorkReconciliation(intent, currentBaseline, remote, localProjectId)
            if (action == SharedWorkReconciliation.Current) {
                records.put(accountId, TYPE_WORK_INTENT, key, intent.copy(
                    queuedVersion = intent.localVersion,
                    reviewReason = null,
                ), SharedWorkIntent.serializer(), Clock.System.now().toString())
                return@forEach
            }
            if (action == SharedWorkReconciliation.Conflict) {
                updateReviewReason(accountId, intent, REVIEW_RECONCILIATION_CONFLICT)
                return@forEach
            }
            val result = when (action) {
                SharedWorkReconciliation.DeleteEntry -> api.deleteEntry(currentEntry)
                SharedWorkReconciliation.DeleteAllocation -> {
                    val target = managed ?: run {
                        updateReviewReason(accountId, intent, REVIEW_REMOTE_ALLOCATIONS_CHANGED)
                        return@forEach
                    }
                    api.deleteAllocation(target, currentEntry.revision)
                }
                is SharedWorkReconciliation.CreateAllocation -> api.allocateExisting(
                    currentEntry, intent.projectId, action.taskId, action.durationSeconds, null,
                )
                is SharedWorkReconciliation.UpdateAllocation -> {
                    val target = managed ?: run {
                        updateReviewReason(accountId, intent, REVIEW_REMOTE_ALLOCATIONS_CHANGED)
                        return@forEach
                    }
                    api.correctAllocation(target, currentEntry.revision, action.durationSeconds, action.taskId, target.sharedNote)
                }
                is SharedWorkReconciliation.UpdateEntry -> api.correctEntry(
                    currentEntry, action.workDate, action.durationSeconds, action.privateNote,
                )
                else -> return@forEach
            }
            if (result.commandId.isNotBlank() && result.status in setOf(
                    SharedWriteStatus.SYNCED, SharedWriteStatus.PENDING, SharedWriteStatus.UNKNOWN,
                )
            ) {
                records.put(accountId, TYPE_WORK_INTENT, key, intent.copy(activeCommandKey = result.commandId),
                    SharedWorkIntent.serializer(), Clock.System.now().toString())
            } else {
                updateReviewReason(accountId, intent, when (result.status) {
                    SharedWriteStatus.ACCESS_REVOKED -> REVIEW_ACCESS_REMOVED
                    SharedWriteStatus.CONFLICT -> REVIEW_RECONCILIATION_CONFLICT
                    else -> result.errorCode ?: REVIEW_RECONCILIATION_CONFLICT
                })
            }
        }
    }

    private suspend fun updateReviewReason(accountId: Int, intent: SharedWorkIntent, reason: String?) {
        if (intent.reviewReason == reason) return
        records.put(
            accountId, TYPE_WORK_INTENT, intent.localWorkId.toString(), intent.copy(reviewReason = reason),
            SharedWorkIntent.serializer(), Clock.System.now().toString(),
        )
    }

    private suspend fun reviewCount(accountId: Int): Int = records.getAll(
        accountId, TYPE_WORK_INTENT, SharedWorkIntent.serializer(),
    ).count { it.reviewReason != null }

    companion object {
        const val TYPE_WORK_INTENT = "shared_work_intent"
        const val TYPE_WORK_BASELINE = "shared_work_baseline"
        const val REVIEW_REMOTE_ENTRY_MISSING = "remote_entry_missing"
        const val REVIEW_REMOTE_ENTRY_CHANGED = "remote_entry_changed"
        const val REVIEW_REMOTE_ALLOCATIONS_CHANGED = "remote_allocations_changed"
        const val REVIEW_ACCESS_REMOVED = "access_removed"
        const val REVIEW_RECONCILIATION_CONFLICT = "reconciliation_conflict"
        const val REVIEW_PROJECT_MOVE_UNAVAILABLE = "project_move_unavailable"
        const val REVIEW_LOCAL_CHANGE_OTHER_ACCOUNT = "local_change_other_account"
        const val REVIEW_RESTORE_COMMAND_MISMATCH = "restore_command_mismatch"
    }
}
