package no.timnor.app.core.repository

import kotlinx.serialization.Serializable

/** Complete, owner-visible allocation projection captured with an entry revision. */
@Serializable
data class SharedWorkAllocationSnapshot(
    val allocationId: String,
    val revision: Int,
    val projectId: Int,
    val taskId: Int?,
    val durationSeconds: Long,
    val sharedNote: String?,
)

/** Last server revisions confirmed after one of our own writes. */
@Serializable
data class SharedWorkBaseline(
    val entryId: String,
    val entryRevision: Int,
    val allocationId: String?,
    val allocationRevision: Int?,
    val entryDurationSeconds: Long = 0,
    val allocationDurationSeconds: Long? = null,
    val allocationTaskId: Int? = null,
    val allocationTracksEntry: Boolean = false,
    val entryWorkDate: String? = null,
    val entryPrivateNote: String? = null,
    val allocationProjectId: Int? = null,
    val allocationSnapshots: List<SharedWorkAllocationSnapshot> = emptyList(),
)
