package no.timnor.app.core.repository

import androidx.room.withTransaction
import kotlinx.serialization.json.Json
import no.timnor.app.core.database.Database
import no.timnor.app.core.database.project.Project
import no.timnor.app.core.database.sharedhours.SharedHoursRecord
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection

/** Creates the local project and its account-scoped link in the same transaction. */
class PlannorProjectCreator(
    private val database: Database,
    private val sessions: PlannorSessionStorage,
    private val coordinator: DataMutationCoordinator,
) {
    private val json = Json { encodeDefaults = true }

    suspend fun create(projectId: Int): Boolean = coordinator.mutate {
        val accountId = sessions.read()?.accountId ?: return@mutate false
        database.withTransaction {
            val dao = database.sharedHoursRecordDao()
            val record = dao.record(accountId, PlannorApi.TYPE_PROJECT, projectId.toString())
                ?: return@withTransaction false
            val selection = json.decodeFromString(PlannorProjectSelection.serializer(), record.payloadJson)
            if (selection.accountId != accountId || selection.projectId != projectId || selection.archived ||
                selection.linkedLocalProjectId != null || sessions.read()?.accountId != accountId
            ) return@withTransaction false
            val id = database.projectDao().insert(Project(name = selection.name))
            check(id in 1..Int.MAX_VALUE.toLong())
            val updated = selection.copy(selected = true, linkedLocalProjectId = id.toInt())
            dao.upsert(SharedHoursRecord(accountId, record.recordType, record.recordKey,
                json.encodeToString(PlannorProjectSelection.serializer(), updated), record.updatedAtUtc))
            check(sessions.read()?.accountId == accountId)
            true
        }
    }
}
