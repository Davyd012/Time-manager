package no.timnor.app.shared.data.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Persistence-facing project row. IDs and fields match the existing Room table. */
@Serializable
data class StoredProject(
    val description: String,
    val name: String,
    val id: Int,
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

/**
 * Platform storage boundary. Implementations must keep `replaceAll` atomic and preserve IDs.
 * The date range is inclusive and uses the database's UTC epoch-seconds representation.
 */
interface TimnorPersistence {
    fun observeProjects(): Flow<List<StoredProject>>

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

    suspend fun deleteProject(project: StoredProject)

    suspend fun upsertWork(work: StoredWork)

    suspend fun deleteWork(work: StoredWork)

    suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>)
}
