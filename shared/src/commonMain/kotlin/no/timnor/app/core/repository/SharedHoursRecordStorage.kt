package no.timnor.app.core.repository

import kotlinx.serialization.KSerializer
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem
import no.timnor.app.shared.features.shared_hours.domain.OwnerAllocationLink
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.features.shared_hours.domain.TimeChangeCursor
import kotlin.time.Clock

/** Account scoped durable records used by the API cache, cursors, and immutable command outbox. */
interface SharedHoursRecordStorage {
    suspend fun <T> get(accountId: Int, type: String, key: String, serializer: KSerializer<T>): T?
    suspend fun <T> getAll(accountId: Int, type: String, serializer: KSerializer<T>): List<T>
    suspend fun <T> put(
        accountId: Int,
        type: String,
        key: String,
        value: T,
        serializer: KSerializer<T>,
        updatedAtUtc: String,
    )
    suspend fun <T> putAll(
        accountId: Int,
        type: String,
        values: List<Pair<String, T>>,
        serializer: KSerializer<T>,
        updatedAtUtc: String,
    )
    suspend fun <T> replaceAll(
        accountId: Int,
        type: String,
        values: List<Pair<String, T>>,
        serializer: KSerializer<T>,
        updatedAtUtc: String,
    ) {
        clear(accountId, type)
        putAll(accountId, type, values, serializer, updatedAtUtc)
    }
    suspend fun clear(accountId: Int, type: String)
    suspend fun delete(accountId: Int, type: String, key: String)
    /** Atomically applies one fully fetched owner/project snapshot and its corresponding cursors. */
    suspend fun commitProjectSnapshot(
        accountId: Int,
        ownerLinks: List<OwnerAllocationLink>,
        allocations: List<ProjectTimeAllocation>,
        cursors: Map<String, TimeChangeCursor>,
        updatedAtUtc: String,
    )
    /** Stores an immutable create payload and its queued intent together before network delivery. */
    suspend fun persistSharedWorkCreate(intent: SharedWorkIntent, request: SharedTimeOutboxItem) {
        require(intent.accountId == request.accountId && intent.createKey == request.atomicIdempotencyKey)
        put(
            intent.accountId,
            SharedWorkSyncCoordinator.TYPE_WORK_INTENT,
            intent.localWorkId.toString(),
            intent.copy(queuedVersion = intent.localVersion),
            SharedWorkIntent.serializer(),
            Clock.System.now().toString(),
        )
        putOutboxUnlessRestorePaused(intent.accountId, intent.createKey, request, Clock.System.now().toString())
    }
    suspend fun putOutboxUnlessRestorePaused(
        accountId: Int,
        key: String,
        value: SharedTimeOutboxItem,
        updatedAtUtc: String,
    ) {
        val existing = get(accountId, PlannorApi.TYPE_OUTBOX, key, SharedTimeOutboxItem.serializer())
        if (existing?.errorCode != "restore_review_required") {
            put(accountId, PlannorApi.TYPE_OUTBOX, key, value, SharedTimeOutboxItem.serializer(), updatedAtUtc)
        }
    }
}
