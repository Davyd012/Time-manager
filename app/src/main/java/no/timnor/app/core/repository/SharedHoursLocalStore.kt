package no.timnor.app.core.repository

import androidx.room.withTransaction
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.timnor.app.core.database.Database
import no.timnor.app.core.database.sharedhours.SharedHoursRecord
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem
import no.timnor.app.shared.features.shared_hours.domain.OwnerAllocationLink
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.features.shared_hours.domain.TimeChangeCursor

/** Durable, account-partitioned storage for Plannor snapshots and retryable commands. */
class SharedHoursLocalStore(private val database: Database) : SharedHoursRecordStorage {
    private val dao get() = database.sharedHoursRecordDao()
    private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true }

    override suspend fun <T> get(accountId: Int, type: String, key: String, serializer: KSerializer<T>): T? {
        require(accountId > 0)
        return dao.record(accountId, type, key)?.let { json.decodeFromString(serializer, it.payloadJson) }
    }

    override suspend fun <T> getAll(accountId: Int, type: String, serializer: KSerializer<T>): List<T> {
        require(accountId > 0)
        return dao.records(accountId, type).map { json.decodeFromString(serializer, it.payloadJson) }
    }

    override suspend fun <T> put(accountId: Int, type: String, key: String, value: T, serializer: KSerializer<T>, updatedAtUtc: String) {
        require(accountId > 0 && type.isNotBlank() && key.isNotBlank())
        dao.upsert(SharedHoursRecord(accountId, type, key, json.encodeToString(serializer, value), updatedAtUtc))
    }

    override suspend fun <T> putAll(
        accountId: Int,
        type: String,
        values: List<Pair<String, T>>,
        serializer: KSerializer<T>,
        updatedAtUtc: String,
    ) {
        require(accountId > 0 && type.isNotBlank())
        database.withTransaction {
            dao.upsertAll(values.map { (key, value) ->
                require(key.isNotBlank())
                SharedHoursRecord(accountId, type, key, json.encodeToString(serializer, value), updatedAtUtc)
            })
        }
    }

    override suspend fun <T> replaceAll(
        accountId: Int,
        type: String,
        values: List<Pair<String, T>>,
        serializer: KSerializer<T>,
        updatedAtUtc: String,
    ) {
        require(accountId > 0 && type.isNotBlank())
        database.withTransaction {
            dao.clearType(accountId, type)
            dao.upsertAll(values.map { (key, value) ->
                require(key.isNotBlank())
                SharedHoursRecord(accountId, type, key, json.encodeToString(serializer, value), updatedAtUtc)
            })
        }
    }

    override suspend fun clear(accountId: Int, type: String) {
        require(accountId > 0)
        dao.clearType(accountId, type)
    }

    override suspend fun delete(accountId: Int, type: String, key: String) {
        require(accountId > 0)
        dao.delete(accountId, type, key)
    }

    override suspend fun commitProjectSnapshot(
        accountId: Int,
        ownerLinks: List<OwnerAllocationLink>,
        allocations: List<ProjectTimeAllocation>,
        cursors: Map<String, TimeChangeCursor>,
        updatedAtUtc: String,
    ) {
        require(accountId > 0)
        database.withTransaction {
            dao.clearType(accountId, PlannorApi.TYPE_OWNER_LINK)
            dao.upsertAll(ownerLinks.map { link ->
                SharedHoursRecord(accountId, PlannorApi.TYPE_OWNER_LINK, link.allocationId,
                    json.encodeToString(OwnerAllocationLink.serializer(), link), updatedAtUtc)
            })
            dao.clearType(accountId, PlannorApi.TYPE_ALLOCATION)
            dao.upsertAll(allocations.map { allocation ->
                SharedHoursRecord(accountId, PlannorApi.TYPE_ALLOCATION, allocation.allocationId,
                    json.encodeToString(ProjectTimeAllocation.serializer(), allocation), updatedAtUtc)
            })
            dao.upsertAll(cursors.map { (key, cursor) ->
                SharedHoursRecord(accountId, PlannorApi.TYPE_CURSOR, key,
                    json.encodeToString(TimeChangeCursor.serializer(), cursor), updatedAtUtc)
            })
        }
    }

    override suspend fun persistSharedWorkCreate(
        intent: SharedWorkIntent,
        request: SharedTimeOutboxItem,
    ) {
        require(intent.accountId == request.accountId && intent.createKey == request.atomicIdempotencyKey)
        database.withTransaction {
            val existing = dao.record(intent.accountId, PlannorApi.TYPE_OUTBOX, intent.createKey)?.let {
                json.decodeFromString(SharedTimeOutboxItem.serializer(), it.payloadJson)
            }
            if (existing?.errorCode == "restore_review_required") return@withTransaction
            val timestamp = kotlin.time.Clock.System.now().toString()
            dao.upsert(SharedHoursRecord(
                intent.accountId,
                SharedWorkSyncCoordinator.TYPE_WORK_INTENT,
                intent.localWorkId.toString(),
                json.encodeToString(SharedWorkIntent.serializer(), intent.copy(queuedVersion = intent.localVersion)),
                timestamp,
            ))
            dao.upsert(SharedHoursRecord(
                request.accountId,
                PlannorApi.TYPE_OUTBOX,
                intent.createKey,
                json.encodeToString(SharedTimeOutboxItem.serializer(), request),
                timestamp,
            ))
        }
    }

    override suspend fun putOutboxUnlessRestorePaused(accountId: Int, key: String, value: SharedTimeOutboxItem, updatedAtUtc: String) {
        database.withTransaction {
            val existing = dao.record(accountId, PlannorApi.TYPE_OUTBOX, key)?.let {
                json.decodeFromString(SharedTimeOutboxItem.serializer(), it.payloadJson)
            }
            if (existing?.errorCode != "restore_review_required") {
                dao.upsert(SharedHoursRecord(accountId, PlannorApi.TYPE_OUTBOX, key,
                    json.encodeToString(SharedTimeOutboxItem.serializer(), value), updatedAtUtc))
            }
        }
    }
}
