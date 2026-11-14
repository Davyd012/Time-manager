package no.timnor.app.shared.features.shared_hours.domain

import kotlinx.serialization.Serializable

/** Local-only identifiers remain independent from Plannor's numeric project IDs. */
@Serializable
data class PlannorProjectSelection(
    val accountId: Int,
    val projectId: Int,
    val name: String,
    val organizationName: String? = null,
    val contextLabel: String? = null,
    val archived: Boolean = false,
    val selected: Boolean = false,
    val refreshedAtUtc: String,
    val linkedLocalProjectId: Int? = null,
    val available: Boolean = true,
) {
    init {
        require(accountId > 0)
        require(projectId > 0)
        require(name.isNotBlank())
        require(linkedLocalProjectId == null || linkedLocalProjectId > 0)
    }
}

/** Task choices loaded only for the selected linked project. */
data class PlannorTaskOption(
    val projectId: Int,
    val taskId: Int,
    val headline: String,
)

/** Owner-private ledger data. The private note is never part of a project projection. */
@Serializable
data class PersonalTimeEntry(
    val accountId: Int,
    val serverId: String,
    val workDate: String,
    val durationSeconds: Long,
    val allocatedSeconds: Long,
    val revision: Int,
    val privateNote: String? = null,
    val sourceWorkId: String? = null,
) {
    val remainingSeconds: Long get() = durationSeconds - allocatedSeconds

    init {
        require(accountId > 0)
        require(serverId.isNotBlank())
        require(durationSeconds >= 0)
        require(allocatedSeconds in 0..durationSeconds)
        require(revision >= 1)
    }

    fun canAllocate(seconds: Long): Boolean = seconds > 0 && seconds <= remainingSeconds
}

/** Only fields authorized for project members belong in this projection. */
@Serializable
data class ProjectTimeAllocation(
    val accountId: Int,
    val allocationId: String,
    val entryId: String,
    val projectId: Int,
    val taskId: Int?,
    val workDate: String,
    val durationSeconds: Long,
    val sharedNote: String? = null,
    val revision: Int,
)

@Serializable
data class OwnerAllocationLink(
    val accountId: Int,
    val allocationId: String,
    val entryId: String,
)

@Serializable
data class TimeChangeCursor(val cursor: Long)

enum class SharedTimeOperation { CREATE_AND_ALLOCATE, ALLOCATE_EXISTING, IMPORT_ENTRY, UPDATE_ENTRY, DELETE_ENTRY, UPDATE_ALLOCATION, DELETE_ALLOCATION }
enum class SharedTimeOutcome { PENDING, OUTCOME_UNKNOWN, SUCCEEDED, CONFLICT, REJECTED, ACCESS_REVOKED }

/** Immutable request intent. Retries must reuse these keys and payload values. */
@Serializable
data class SharedTimeOutboxItem(
    val accountId: Int,
    val operation: SharedTimeOperation,
    val outcome: SharedTimeOutcome,
    val entryId: String? = null,
    val entryRevision: Int? = null,
    val allocationId: String? = null,
    val allocationRevision: Int? = null,
    val projectId: Int,
    val taskId: Int? = null,
    val workDate: String,
    val durationSeconds: Long,
    val sourceId: String? = null,
    val privateNote: String? = null,
    val sharedNote: String? = null,
    val atomicIdempotencyKey: String? = null,
    val entryIdempotencyKey: String? = null,
    val allocationIdempotencyKey: String? = null,
    val errorCode: String? = null,
    /** Retry metadata is local scheduling state; command payload and keys stay immutable. */
    val retryCount: Int = 0,
    val retryAfterEpochSeconds: Long? = null,
) {
    init {
        require(accountId > 0)
        require(projectId >= 0)
        require(durationSeconds >= 0)
        require(retryCount >= 0)
        require(retryAfterEpochSeconds == null || retryAfterEpochSeconds >= 0)
        when (operation) {
            SharedTimeOperation.CREATE_AND_ALLOCATE -> {
                require(projectId > 0)
                require(!atomicIdempotencyKey.isNullOrBlank())
            }
            SharedTimeOperation.ALLOCATE_EXISTING -> {
                require(projectId > 0)
                require(!entryId.isNullOrBlank() && (entryRevision ?: 0) > 0)
                require(!allocationIdempotencyKey.isNullOrBlank())
            }
            SharedTimeOperation.IMPORT_ENTRY -> {
                require(projectId == 0 && !sourceId.isNullOrBlank())
                require(!entryIdempotencyKey.isNullOrBlank())
            }
            SharedTimeOperation.UPDATE_ENTRY, SharedTimeOperation.DELETE_ENTRY -> {
                require(!entryId.isNullOrBlank() && (entryRevision ?: 0) > 0)
                require(!entryIdempotencyKey.isNullOrBlank())
            }
            SharedTimeOperation.UPDATE_ALLOCATION, SharedTimeOperation.DELETE_ALLOCATION -> {
                require(projectId > 0)
                require(!entryId.isNullOrBlank() && (entryRevision ?: 0) > 0)
                require(!allocationId.isNullOrBlank() && (allocationRevision ?: 0) > 0)
                require(!allocationIdempotencyKey.isNullOrBlank())
            }
        }
    }
}

data class LocalEntryImportPreview(
    val totalEntries: Int,
    val alreadyImported: Int,
    val eligibleEntries: Int,
    val privateNotesIncluded: Boolean = false,
) {
    init {
        require(totalEntries >= 0)
        require(alreadyImported in 0..totalEntries)
        require(eligibleEntries == totalEntries - alreadyImported)
        require(!privateNotesIncluded)
    }
}

fun belongsToAccount(accountId: Int, itemAccountId: Int): Boolean = accountId > 0 && accountId == itemAccountId

fun <T> accountScoped(accountId: Int, items: Iterable<T>, owner: (T) -> Int): List<T> =
    items.filter { belongsToAccount(accountId, owner(it)) }

/** Personal totals count entries once; allocations only classify their existing time. */
fun personalTimeTotal(entries: Iterable<PersonalTimeEntry>): Long =
    entries.sumOf(PersonalTimeEntry::durationSeconds)

fun allocationBalance(entry: PersonalTimeEntry, allocations: Iterable<ProjectTimeAllocation>): Long? {
    if (allocations.any { it.entryId == entry.serverId && it.accountId != entry.accountId }) return null
    val allocated = allocations.filter { it.entryId == entry.serverId }.sumOf(ProjectTimeAllocation::durationSeconds)
    return (entry.durationSeconds - allocated).takeIf { allocated in 0..entry.durationSeconds }
}

/** Candidate matching is deliberately based on durable source IDs, never names/dates/durations. */
fun eligibleForImport(sourceIds: Iterable<String>, importedSourceIds: Set<String>): List<String> =
    sourceIds.filter { it.isNotBlank() && it !in importedSourceIds }.distinct()

/** Projection for sharing intentionally omits local description/private note. */
fun toProjectAllocation(
    entry: PersonalTimeEntry,
    allocationId: String,
    projectId: Int,
    taskId: Int?,
    durationSeconds: Long,
    sharedNote: String?,
): ProjectTimeAllocation? {
    if (!entry.canAllocate(durationSeconds) || allocationId.isBlank() || projectId <= 0) return null
    return ProjectTimeAllocation(
        accountId = entry.accountId,
        allocationId = allocationId,
        entryId = entry.serverId,
        projectId = projectId,
        taskId = taskId,
        workDate = entry.workDate,
        durationSeconds = durationSeconds,
        sharedNote = sharedNote,
        revision = 1,
    )
}
