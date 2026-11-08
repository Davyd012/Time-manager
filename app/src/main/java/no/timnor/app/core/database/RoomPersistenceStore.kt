package no.timnor.app.core.database

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.toKotlinLocalDateTime
import no.timnor.app.core.database.project.Project
import no.timnor.app.core.database.work.Work
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import kotlinx.datetime.toJavaLocalDateTime

class RoomPersistenceStore(private val database: Database) : TimnorPersistence {
    override fun observeProjects(): Flow<List<StoredProject>> =
        database.projectDao().getAllProjects().map { it.map(Project::toStored) }

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

    override suspend fun deleteProject(project: StoredProject) {
        database.projectDao().delete(project.toEntity())
    }

    override suspend fun upsertWork(work: StoredWork) {
        database.workDao().upsert(work.toEntity())
    }

    override suspend fun deleteWork(work: StoredWork) {
        database.workDao().delete(work.toEntity())
    }

    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) {
        database.withTransaction {
            database.workDao().deleteAll()
            database.projectDao().deleteAll()
            database.projectDao().upsertAll(projects.map(StoredProject::toEntity))
            database.workDao().upsertAll(works.map(StoredWork::toEntity))
        }
    }
}

private fun Project.toStored() = StoredProject(description, name, id)

private fun Work.toStored() = StoredWork(
    description = description,
    dateEpochSeconds = utcEpochSecondsFromLocalDateTime(date.toKotlinLocalDateTime()),
    project = project,
    task = task,
    time = time,
    id = id,
)

private fun StoredProject.toEntity() = Project(description, name, id)

private fun StoredWork.toEntity() = Work(
    description = description,
    date = utcLocalDateTimeFromEpochSeconds(dateEpochSeconds).toJavaLocalDateTime(),
    project = project,
    task = task,
    time = time,
    id = id,
)
