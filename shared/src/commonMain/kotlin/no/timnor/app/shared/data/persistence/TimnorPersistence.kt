package no.timnor.app.shared.data.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import no.timnor.app.core.repository.SharedWorkSyncIndicator

/** Persistence-facing project row. IDs and fields match the existing Room table. */
@Serializable
data class StoredProject(
    val description: String,
    val name: String,
    val id: Int,
    val hourlyRateOre: Int? = null,
)

/** Work date is stored as UTC epoch seconds, matching the existing Room converter. */
@Serializable
data class StoredWork(
    val description: String,
    val dateEpochSeconds: Long,
    val project: Int,
    val task: Int,
    val time: Int,
    val id: Int,
)

data class TimnorDataSnapshot(
    val projects: List<StoredProject>,
    val works: List<StoredWork>,
)

/** Account-scoped shared-work metadata exported without authentication credentials. */
@Serializable
data class SharedHoursBackupRecord(
    val accountId: Int,
    val type: String,
    val key: String,
    val payloadJson: String,
)

data class TimnorBackupSnapshot(
    val projects: List<StoredProject>,
    val works: List<StoredWork>,
    val sharedHoursRecords: List<SharedHoursBackupRecord>,
)

/**
 * Platform storage boundary. Implementations must keep `replaceAll` atomic and preserve IDs.
 * The date range is inclusive and uses the database's UTC epoch-seconds representation.
 */
interface TimnorPersistence {
    /** Returns projects and works from one consistent platform snapshot. */
    suspend fun snapshotAll(): TimnorDataSnapshot

    fun observeProjects(): Flow<List<StoredProject>>

    /** Local work rows with a durable shared-hours journal identity. */
    fun observeSharedWorkIds(): Flow<Set<Int>> = flowOf(emptySet())

    /** Pending or actionable shared-work state, keyed by local work ID. */
    fun observeSharedWorkIndicators(): Flow<Map<Int, SharedWorkSyncIndicator>> =
        observeSharedWorkIds().map { ids -> ids.associateWith { SharedWorkSyncIndicator.PENDING } }

    fun observeWorks(
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        projectIds: List<Int> = emptyList(),
    ): Flow<List<StoredWork>>

    suspend fun hoursForRange(
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        projectIds: List<Int> = emptyList(),
    ): Int

    suspend fun upsertProject(project: StoredProject)

    /** Creates one project and returns its stable local ID without name-based matching. */
    suspend fun createProject(project: StoredProject): StoredProject {
        require(project.id == 0)
        upsertProject(project)
        return snapshotAll().projects.maxByOrNull(StoredProject::id)
            ?: error("Project creation did not persist a project")
    }

    suspend fun deleteProject(project: StoredProject)

    suspend fun upsertWork(work: StoredWork)

    /** Inserts or updates a related set of work entries as one atomic storage mutation. */
    suspend fun upsertWorks(works: List<StoredWork>)

    suspend fun deleteWork(work: StoredWork)

    /** True only after the user confirms that shared allocations will also be removed. */
    suspend fun deleteWork(work: StoredWork, sharedDeletionConfirmed: Boolean) = deleteWork(work)

    suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>)

    suspend fun sharedHoursBackupRecords(): List<SharedHoursBackupRecord> = emptyList()

    suspend fun snapshotBackup(): TimnorBackupSnapshot {
        val data = snapshotAll()
        return TimnorBackupSnapshot(data.projects, data.works, sharedHoursBackupRecords())
    }

    suspend fun replaceAll(
        projects: List<StoredProject>,
        works: List<StoredWork>,
        sharedHoursRecords: List<SharedHoursBackupRecord>,
    ) = replaceAll(projects, works)
}
