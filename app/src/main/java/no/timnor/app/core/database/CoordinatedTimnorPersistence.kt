package no.timnor.app.core.database

import kotlinx.coroutines.flow.Flow
import no.timnor.app.core.repository.DataMutationCoordinator
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorDataSnapshot
import no.timnor.app.shared.data.persistence.TimnorPersistence

/** Applies the app's existing mutation lock to common-screen writes. */
class CoordinatedTimnorPersistence(
    private val delegate: TimnorPersistence,
    private val coordinator: DataMutationCoordinator,
) : TimnorPersistence {
    override suspend fun snapshotAll(): TimnorDataSnapshot = coordinator.snapshot { delegate.snapshotAll() }

    override fun observeProjects(): Flow<List<StoredProject>> = delegate.observeProjects()

    override fun observeWorks(
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        projectIds: List<Int>,
    ): Flow<List<StoredWork>> = delegate.observeWorks(startEpochSeconds, endEpochSeconds, projectIds)

    override suspend fun hoursForRange(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>): Int =
        delegate.hoursForRange(startEpochSeconds, endEpochSeconds, projectIds)

    override suspend fun upsertProject(project: StoredProject) =
        coordinator.mutate { delegate.upsertProject(project) }

    override suspend fun deleteProject(project: StoredProject) =
        coordinator.mutate { delegate.deleteProject(project) }

    override suspend fun upsertWork(work: StoredWork) =
        coordinator.mutate { delegate.upsertWork(work) }

    override suspend fun upsertWorks(works: List<StoredWork>) =
        coordinator.mutate { delegate.upsertWorks(works) }

    override suspend fun deleteWork(work: StoredWork) =
        coordinator.mutate { delegate.deleteWork(work) }

    /** Restore replaces records inside the separate timer-clear gate and DB transaction. */
    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) =
        delegate.replaceAll(projects, works)
}
