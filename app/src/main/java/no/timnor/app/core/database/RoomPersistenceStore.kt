package no.timnor.app.core.database

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.toKotlinLocalDateTime
import no.timnor.app.core.database.project.Project
import no.timnor.app.core.database.work.Work
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.SharedHoursBackupRecord
import no.timnor.app.shared.data.persistence.TimnorDataSnapshot
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.data.persistence.TimnorBackupSnapshot
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import kotlinx.datetime.toJavaLocalDateTime
import no.timnor.app.core.repository.SharedWorkJournal

class RoomPersistenceStore(
    private val database: Database,
    private val sharedWorkJournal: SharedWorkJournal? = null,
) : TimnorPersistence {
    override suspend fun snapshotAll(): TimnorDataSnapshot = database.withTransaction {
        TimnorDataSnapshot(
            projects = database.projectDao().getAll().map(Project::toStored),
            works = database.workDao().getAll().map(Work::toStored),
        )
    }

    override fun observeProjects(): Flow<List<StoredProject>> =
        database.projectDao().getAllProjects().map { it.map(Project::toStored) }

    override fun observeSharedWorkIds(): Flow<Set<Int>> =
        sharedWorkJournal?.observeTrackedWorkIds() ?: kotlinx.coroutines.flow.flowOf(emptySet())

    override fun observeSharedWorkIndicators(): Flow<Map<Int, no.timnor.app.core.repository.SharedWorkSyncIndicator>> =
        sharedWorkJournal?.observeWorkIndicators() ?: kotlinx.coroutines.flow.flowOf(emptyMap())

    override fun observeWorks(
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        projectIds: List<Int>,
    ): Flow<List<StoredWork>> {
        val works = if (projectIds.isEmpty()) {
            database.workDao().getAllWorks(startEpochSeconds, endEpochSeconds)
        } else {
            database.workDao().getAllWorksForProjects(startEpochSeconds, endEpochSeconds, projectIds)
        }
        return works.map { rows -> rows.map(Work::toStored) }
    }

    override suspend fun hoursForRange(
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        projectIds: List<Int>,
    ): Int = if (projectIds.isEmpty()) {
        database.workDao().getHoursByDay(startEpochSeconds, endEpochSeconds)
    } else {
        database.workDao().getHoursByDayAndProject(startEpochSeconds, endEpochSeconds, projectIds)
    }

    override suspend fun upsertProject(project: StoredProject) {
        database.projectDao().upsert(project.toEntity())
    }

    override suspend fun createProject(project: StoredProject): StoredProject = database.withTransaction {
        require(project.id == 0)
        val id = database.projectDao().insert(project.toEntity())
        check(id in 1..Int.MAX_VALUE.toLong())
        project.copy(id = id.toInt())
    }

    override suspend fun deleteProject(project: StoredProject) {
        database.projectDao().delete(project.toEntity())
    }

    override suspend fun upsertWork(work: StoredWork) {
        database.withTransaction { saveWork(work) }
    }

    override suspend fun upsertWorks(works: List<StoredWork>) {
        database.withTransaction {
            works.forEach { saveWork(it) }
        }
    }

    override suspend fun deleteWork(work: StoredWork) = deleteWork(work, false)

    override suspend fun deleteWork(work: StoredWork, sharedDeletionConfirmed: Boolean) {
        database.withTransaction {
            sharedWorkJournal?.changed(work, deleted = true, deletionApproved = sharedDeletionConfirmed)
            database.workDao().delete(work.toEntity())
        }
    }

    private suspend fun saveWork(work: StoredWork) {
        if (work.id == 0) {
            val id = database.workDao().insert(work.toEntity())
            check(id in 1..Int.MAX_VALUE.toLong())
            sharedWorkJournal?.created(work.copy(id = id.toInt()))
        } else {
            database.workDao().upsert(work.toEntity())
            sharedWorkJournal?.changed(work)
        }
    }

    override suspend fun sharedHoursBackupRecords(): List<SharedHoursBackupRecord> =
        sharedWorkJournal?.exportBackupRecords().orEmpty()

    override suspend fun snapshotBackup(): TimnorBackupSnapshot = database.withTransaction {
        TimnorBackupSnapshot(
            projects = database.projectDao().getAll().map(Project::toStored),
            works = database.workDao().getAll().map(Work::toStored),
            sharedHoursRecords = sharedWorkJournal?.exportBackupRecords().orEmpty(),
        )
    }

    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) =
        replaceAll(projects, works, emptyList())

    override suspend fun replaceAll(
        projects: List<StoredProject>,
        works: List<StoredWork>,
        sharedHoursRecords: List<SharedHoursBackupRecord>,
    ) {
        database.withTransaction {
            sharedWorkJournal?.detachForRestore()
            database.workDao().deleteAll()
            database.projectDao().deleteAll()
            database.projectDao().upsertAll(projects.map(StoredProject::toEntity))
            database.workDao().upsertAll(works.map(StoredWork::toEntity))
            sharedWorkJournal?.restoreBackupRecords(sharedHoursRecords)
        }
    }
}

private fun Project.toStored() = StoredProject(description, name, id, hourlyRateOre)

private fun Work.toStored() = StoredWork(
    description = description,
    dateEpochSeconds = utcEpochSecondsFromLocalDateTime(date.toKotlinLocalDateTime()),
    project = project,
    task = task,
    time = time,
    id = id,
)

private fun StoredProject.toEntity() = Project(description, name, id, hourlyRateOre)

private fun StoredWork.toEntity() = Work(
    description = description,
    date = utcLocalDateTimeFromEpochSeconds(dateEpochSeconds).toJavaLocalDateTime(),
    project = project,
    task = task,
    time = time,
    id = id,
)
