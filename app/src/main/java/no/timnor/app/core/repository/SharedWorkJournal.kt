package no.timnor.app.core.repository

import androidx.room.withTransaction
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import no.timnor.app.core.database.Database
import no.timnor.app.core.database.sharedhours.SharedHoursRecord
import no.timnor.app.core.database.work.Work
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.SharedHoursBackupRecord
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.features.shared_hours.domain.PersonalTimeEntry
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOperation
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Clock

data class SharedWorkReviewItem(
    val localWorkId: Int,
    val reason: String,
    val localDate: String,
    val localDurationSeconds: Long,
    val localPrivateNote: String,
    val remoteDate: String? = null,
    val remoteDurationSeconds: Long? = null,
    val remotePrivateNote: String? = null,
    val projectId: Int,
    val remoteProjectId: Int? = null,
    val remoteTaskId: Int? = null,
    val remoteAllocationCount: Int = 0,
    val managedAllocationPresent: Boolean = false,
    val localDeleted: Boolean = false,
    val remoteAllocationsComplete: Boolean = false,
)

/** Called only inside the same Room transaction as the corresponding Work mutation. */
class SharedWorkJournal(private val database: Database, private val sessions: PlannorSessionStorage) {
    private val json = Json { encodeDefaults = true }
    private val dao get() = database.sharedHoursRecordDao()

    suspend fun exportBackupRecords(): List<SharedHoursBackupRecord> = dao.recordsForTypes(BACKUP_RECORD_TYPES)
        .map { SharedHoursBackupRecord(it.accountId, it.recordType, it.recordKey, it.payloadJson) }

    suspend fun restoreBackupRecords(records: List<SharedHoursBackupRecord>) {
        records.forEach { backup ->
            require(backup.type in BACKUP_RECORD_TYPES && backup.accountId > 0 && backup.key.isNotBlank())
            when (backup.type) {
                PlannorApi.TYPE_PROJECT -> {
                    val value = json.decodeFromString(PlannorProjectSelection.serializer(), backup.payloadJson)
                    require(value.accountId == backup.accountId && value.projectId.toString() == backup.key)
                }
                TYPE_WORK_INTENT -> {
                    val value = json.decodeFromString(SharedWorkIntent.serializer(), backup.payloadJson)
                    require(value.accountId == backup.accountId)
                    require(backup.key == value.localWorkId.toString() || backup.key == "detached:${value.sourceId}")
                }
                TYPE_WORK_BASELINE -> {
                    json.decodeFromString(SharedWorkBaseline.serializer(), backup.payloadJson)
                    require(backup.key.toIntOrNull() != null || backup.key.startsWith("detached:"))
                }
                PlannorApi.TYPE_OUTBOX -> {
                    val value = json.decodeFromString(SharedTimeOutboxItem.serializer(), backup.payloadJson)
                    val commandKey = value.atomicIdempotencyKey ?: value.allocationIdempotencyKey ?:
                        value.entryIdempotencyKey ?: value.allocationId ?: value.entryId.orEmpty()
                    require(value.accountId == backup.accountId && commandKey == backup.key)
                }
            }
            val existing = dao.record(backup.accountId, backup.type, backup.key)
            if (existing != null && backup.type == PlannorApi.TYPE_OUTBOX) {
                val existingItem = json.decodeFromString(SharedTimeOutboxItem.serializer(), existing.payloadJson)
                val importedItem = json.decodeFromString(SharedTimeOutboxItem.serializer(), backup.payloadJson)
                check(existingItem.sameImmutableRequest(importedItem)) { "Backup command key collision" }
                if (existingItem.errorCode == "restore_review_required") {
                    dao.upsert(existing.copy(
                        payloadJson = json.encodeToString(
                            SharedTimeOutboxItem.serializer(),
                            existingItem.copy(outcome = SharedTimeOutcome.OUTCOME_UNKNOWN, errorCode = null),
                        ),
                        updatedAtUtc = Clock.System.now().toString(),
                    ))
                }
            } else if (existing != null && backup.type == PlannorApi.TYPE_PROJECT) {
                dao.upsert(SharedHoursRecord(
                    backup.accountId, backup.type, backup.key, backup.payloadJson, Clock.System.now().toString(),
                ))
            } else if (existing != null) {
                check(existing.payloadJson == backup.payloadJson) { "Backup mapping collision" }
            } else {
                dao.upsert(SharedHoursRecord(
                    backup.accountId, backup.type, backup.key, backup.payloadJson, Clock.System.now().toString(),
                ))
            }
        }
        dao.recordsForType(TYPE_WORK_INTENT).forEach { record ->
            if (record.recordKey.startsWith("detached:")) return@forEach
            val intent = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
            val commandRecord = dao.record(intent.accountId, PlannorApi.TYPE_OUTBOX, intent.createKey) ?: return@forEach
            val command = json.decodeFromString(SharedTimeOutboxItem.serializer(), commandRecord.payloadJson)
            if (command.errorCode != "restore_review_required") return@forEach
            if (command.matchesCreate(intent)) {
                dao.upsert(commandRecord.copy(
                    payloadJson = json.encodeToString(
                        SharedTimeOutboxItem.serializer(),
                        command.copy(outcome = SharedTimeOutcome.OUTCOME_UNKNOWN, errorCode = null),
                    ),
                    updatedAtUtc = Clock.System.now().toString(),
                ))
            } else {
                put(intent.copy(reviewReason = intent.reviewReason ?: REVIEW_RESTORE_COMMAND_MISMATCH))
            }
        }
    }

    suspend fun created(work: StoredWork) {
        val accountId = sessions.read()?.accountId ?: return
        val projects = dao.records(accountId, PlannorApi.TYPE_PROJECT).map {
            json.decodeFromString(PlannorProjectSelection.serializer(), it.payloadJson)
        }
        val project = projects.singleOrNull {
            it.accountId == accountId && it.selected && !it.archived && it.available &&
                it.linkedLocalProjectId == work.project
        } ?: return
        val intent = SharedWorkIntent(
            accountId = accountId,
            localWorkId = work.id,
            sourceId = UUID.randomUUID().toString(),
            createKey = UUID.randomUUID().toString(),
            projectId = project.projectId,
            localProjectId = work.project,
            localVersion = 1,
            workDate = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date.toString(),
            durationSeconds = work.time.toLong(),
            privateNote = work.description,
            taskId = work.task.takeIf { it > 0 },
        )
        put(intent)
    }

    fun observeTrackedWorkIds(): Flow<Set<Int>> = dao.observeRecordsForType(TYPE_WORK_INTENT).map { records ->
        records.mapNotNull { it.recordKey.toIntOrNull() }.toSet()
    }

    fun observeWorkIndicators(): Flow<Map<Int, SharedWorkSyncIndicator>> = dao.observeRecordsForTypes(
        listOf(TYPE_WORK_INTENT, TYPE_WORK_BASELINE, PlannorApi.TYPE_OUTBOX, PlannorApi.TYPE_SESSION_STATE),
    ).map { records ->
        val intents = records.filter { it.recordType == TYPE_WORK_INTENT }
            .mapNotNull { record -> runCatching {
                json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
            }.getOrNull() }
        val baselines = records.filter { it.recordType == TYPE_WORK_BASELINE }
            .map { it.accountId to it.recordKey }
            .toSet()
        val outbox = records.filter { it.recordType == PlannorApi.TYPE_OUTBOX }
            .mapNotNull { record -> runCatching {
                (record.accountId to record.recordKey) to json.decodeFromString(
                    SharedTimeOutboxItem.serializer(), record.payloadJson,
                )
            }.getOrNull() }.toMap()

        sharedWorkIndicators(intents, baselines, outbox, sessions.read()?.accountId)
    }

    suspend fun changed(work: StoredWork, deleted: Boolean = false, deletionApproved: Boolean = false) {
        val activeAccountId = sessions.read()?.accountId
        val records = dao.recordsForKey(TYPE_WORK_INTENT, work.id.toString())
        for (record in records) {
            val existing = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
            val projectChanged = work.project != existing.localProjectId
            val newLinkedProject = if (projectChanged) {
                dao.records(existing.accountId, PlannorApi.TYPE_PROJECT).asSequence()
                    .map { json.decodeFromString(PlannorProjectSelection.serializer(), it.payloadJson) }
                    .singleOrNull {
                        it.accountId == existing.accountId && it.linkedLocalProjectId == work.project &&
                            it.selected && !it.archived && it.available
                    }
            } else null
            val otherAccount = record.accountId != activeAccountId
            if (otherAccount && existing.localProjectId == work.project &&
                existing.workDate == utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date.toString() &&
                existing.durationSeconds == work.time.toLong() && existing.privateNote == work.description &&
                existing.taskId == work.task.takeIf { it > 0 } && existing.deleted == deleted
            ) continue
            put(existing.copy(
                localVersion = existing.localVersion + 1,
                localProjectId = work.project,
                projectId = newLinkedProject?.projectId ?: existing.projectId,
                workDate = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date.toString(),
                durationSeconds = work.time.toLong(),
                privateNote = work.description,
                taskId = if (projectChanged) null else work.task.takeIf { it > 0 },
                deleted = deleted,
                deletionApproved = deleted && deletionApproved && !otherAccount,
                reviewReason = if (otherAccount) existing.reviewReason ?: REVIEW_LOCAL_CHANGE_OTHER_ACCOUNT
                    else if (projectChanged && newLinkedProject == null) REVIEW_PROJECT_MOVE_UNAVAILABLE
                    else if (projectChanged) null else existing.reviewReason,
            ))
        }
    }

    /** Freeze a request once; later local edits never change its idempotent payload. */
    suspend fun prepareCreates(): Int {
        val accountId = sessions.read()?.accountId ?: return 0
        return database.withTransaction {
            if (sessions.read()?.accountId != accountId) return@withTransaction 0
            var prepared = 0
            dao.records(accountId, TYPE_WORK_INTENT).forEach { record ->
                if (record.recordKey.startsWith("detached:")) return@forEach
                val intent = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
                if (intent.accountId != accountId || intent.deleted || intent.reviewReason != null || intent.durationSeconds <= 0 ||
                    intent.queuedVersion != null
                ) return@forEach
                val projectRecord = dao.record(accountId, PlannorApi.TYPE_PROJECT, intent.projectId.toString())
                    ?: return@forEach
                val project = json.decodeFromString(PlannorProjectSelection.serializer(), projectRecord.payloadJson)
                if (project.accountId != accountId || project.projectId != intent.projectId || !project.selected ||
                    project.archived || !project.available || project.linkedLocalProjectId != intent.localProjectId
                ) {
                    return@forEach
                }
                if (dao.record(accountId, PlannorApi.TYPE_OUTBOX, intent.createKey) != null) return@forEach
                val request = SharedTimeOutboxItem(
                    accountId = accountId,
                    operation = SharedTimeOperation.CREATE_AND_ALLOCATE,
                    outcome = SharedTimeOutcome.PENDING,
                    projectId = intent.projectId,
                    taskId = intent.taskId,
                    workDate = intent.workDate,
                    durationSeconds = intent.durationSeconds,
                    sourceId = intent.sourceId,
                    privateNote = intent.privateNote.ifBlank { null },
                    sharedNote = null,
                    atomicIdempotencyKey = intent.createKey,
                )
                dao.upsert(SharedHoursRecord(
                    accountId, PlannorApi.TYPE_OUTBOX, intent.createKey,
                    json.encodeToString(SharedTimeOutboxItem.serializer(), request), Clock.System.now().toString(),
                ))
                put(intent.copy(queuedVersion = intent.localVersion))
                prepared++
            }
            check(sessions.read()?.accountId == accountId)
            prepared
        }
    }

    /** Counts local changes that need reconciliation or an explicit decision. */
    suspend fun needsReviewCount(): Int {
        val accountId = sessions.read()?.accountId ?: return 0
        return database.withTransaction {
            dao.records(accountId, TYPE_WORK_INTENT).count { record ->
                val intent = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
                val queued = dao.record(accountId, PlannorApi.TYPE_OUTBOX, intent.createKey)?.let {
                    json.decodeFromString(SharedTimeOutboxItem.serializer(), it.payloadJson)
                }
                val project = dao.record(accountId, PlannorApi.TYPE_PROJECT, intent.projectId.toString())?.let {
                    json.decodeFromString(PlannorProjectSelection.serializer(), it.payloadJson)
                }
                record.recordKey.startsWith("detached:") ||
                    intent.reviewReason != null ||
                    (queued == null && !intent.deleted && (project == null || !project.selected ||
                        project.archived || !project.available || project.linkedLocalProjectId != intent.localProjectId)) ||
                    (queued != null && queued.outcome in setOf(
                        SharedTimeOutcome.CONFLICT, SharedTimeOutcome.REJECTED, SharedTimeOutcome.ACCESS_REVOKED,
                    )) ||
                    (queued?.outcome == SharedTimeOutcome.SUCCEEDED && intent.queuedVersion != intent.localVersion)
            }
        }
    }

    suspend fun reviewItems(
        localWorks: List<StoredWork>,
        entries: List<PersonalTimeEntry>,
        allocations: List<ProjectTimeAllocation>,
    ): List<SharedWorkReviewItem> {
        val accountId = sessions.read()?.accountId ?: return emptyList()
        val worksById = localWorks.associateBy(StoredWork::id)
        return database.withTransaction {
            dao.records(accountId, TYPE_WORK_INTENT).mapNotNull { record ->
                val intent = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
                val reason = intent.reviewReason ?: return@mapNotNull null
                val work = worksById[intent.localWorkId]
                if (work == null && !intent.deleted) return@mapNotNull null
                val baseline = dao.record(accountId, TYPE_WORK_BASELINE, record.recordKey)?.let {
                    json.decodeFromString(SharedWorkBaseline.serializer(), it.payloadJson)
                }
                val entry = baseline?.let { saved ->
                    entries.singleOrNull { it.accountId == accountId && it.serverId == saved.entryId }
                }
                val matching = entry?.let { remote ->
                    allocations.filter { it.accountId == accountId && it.entryId == remote.serverId }
                }.orEmpty()
                val allocationCount = if (entry != null &&
                    entry.allocatedSeconds != matching.sumOf(ProjectTimeAllocation::durationSeconds)
                ) maxOf(2, matching.size) else matching.size
                SharedWorkReviewItem(
                    localWorkId = intent.localWorkId,
                    reason = reason,
                    localDate = work?.let { utcLocalDateTimeFromEpochSeconds(it.dateEpochSeconds).date.toString() }
                        ?: intent.workDate,
                    localDurationSeconds = work?.time?.toLong() ?: intent.durationSeconds,
                    localPrivateNote = work?.description ?: intent.privateNote,
                    remoteDate = entry?.workDate,
                    remoteDurationSeconds = entry?.durationSeconds,
                    remotePrivateNote = entry?.privateNote,
                    projectId = intent.projectId,
                    remoteProjectId = matching.singleOrNull { it.allocationId == baseline?.allocationId }?.projectId,
                    remoteTaskId = matching.singleOrNull { it.allocationId == baseline?.allocationId }?.taskId,
                    remoteAllocationCount = allocationCount,
                    managedAllocationPresent = baseline?.allocationId != null && matching.any {
                        it.allocationId == baseline.allocationId
                    },
                    localDeleted = intent.deleted,
                    remoteAllocationsComplete = entry != null &&
                        entry.allocatedSeconds == matching.sumOf(ProjectTimeAllocation::durationSeconds) &&
                        entry.allocatedSeconds <= entry.durationSeconds,
                )
            }
        }
    }

    /** Resolve only against the latest complete snapshot; ambiguous allocations stay in review. */
    suspend fun resolve(
        localWorkId: Int,
        resolution: SharedWorkResolution,
        entries: List<PersonalTimeEntry>,
        allocations: List<ProjectTimeAllocation>,
    ): Boolean {
        val accountId = sessions.read()?.accountId ?: return false
        return database.withTransaction {
            if (sessions.read()?.accountId != accountId) return@withTransaction false
            val record = dao.record(accountId, TYPE_WORK_INTENT, localWorkId.toString()) ?: return@withTransaction false
            var intent = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
            if (intent.accountId != accountId || intent.reviewReason == null || intent.activeCommandKey != null) {
                return@withTransaction false
            }
            val baselineRecord = dao.record(accountId, TYPE_WORK_BASELINE, record.recordKey)
            val baseline = baselineRecord?.let {
                json.decodeFromString(SharedWorkBaseline.serializer(), it.payloadJson)
            }
            if (baseline == null) {
                val createCommand = dao.record(accountId, PlannorApi.TYPE_OUTBOX, intent.createKey)
                if (createCommand != null) return@withTransaction false
                return@withTransaction when (resolution) {
                    SharedWorkResolution.KEEP_LOCAL_ONLY -> {
                        dao.delete(accountId, TYPE_WORK_INTENT, record.recordKey)
                        true
                    }
                    SharedWorkResolution.KEEP_TIMNOR -> {
                        if (intent.deleted) dao.delete(accountId, TYPE_WORK_INTENT, record.recordKey)
                        else put(intent.copy(queuedVersion = null, reviewReason = null))
                        true
                    }
                    else -> false
                }
            }
            val remoteEntry = entries.singleOrNull {
                it.accountId == accountId && it.serverId == baseline.entryId && it.sourceWorkId == intent.sourceId
            }
            if (remoteEntry == null) {
                if (intent.reviewReason != REVIEW_REMOTE_ENTRY_MISSING) return@withTransaction false
                when (resolution) {
                    SharedWorkResolution.SHARE_AGAIN -> {
                        intent = intent.copy(
                            sourceId = UUID.randomUUID().toString(),
                            createKey = UUID.randomUUID().toString(),
                            localVersion = 1,
                            queuedVersion = null,
                            activeCommandKey = null,
                            reviewReason = null,
                        )
                        dao.delete(accountId, TYPE_WORK_BASELINE, record.recordKey)
                        put(intent)
                        true
                    }
                    SharedWorkResolution.KEEP_LOCAL_ONLY -> {
                        dao.delete(accountId, TYPE_WORK_BASELINE, record.recordKey)
                        dao.delete(accountId, TYPE_WORK_INTENT, record.recordKey)
                        true
                    }
                    else -> false
                }
            } else {
                if (resolution == SharedWorkResolution.SHARE_AGAIN) {
                    return@withTransaction false
                }
                if (resolution == SharedWorkResolution.KEEP_LOCAL_ONLY) {
                    // Detaching preserves the local row and leaves every remote allocation
                    // untouched, including cases where the owner has several allocations.
                    dao.delete(accountId, TYPE_WORK_BASELINE, record.recordKey)
                    dao.delete(accountId, TYPE_WORK_INTENT, record.recordKey)
                    return@withTransaction true
                }
                if (intent.deleted && resolution == SharedWorkResolution.USE_PLANNOR) return@withTransaction false
                val work = database.workDao().getAll().singleOrNull { it.id == localWorkId }
                    ?: if (intent.deleted) null else return@withTransaction false
                val matching = allocations.filter { it.accountId == accountId && it.entryId == remoteEntry.serverId }
                val allocation = matching.singleOrNull { it.allocationId == baseline.allocationId }
                if (remoteEntry.allocatedSeconds != matching.sumOf(ProjectTimeAllocation::durationSeconds) ||
                    remoteEntry.allocatedSeconds > remoteEntry.durationSeconds
                ) {
                    return@withTransaction false
                }
                if (resolution == SharedWorkResolution.USE_PLANNOR && allocation == null) return@withTransaction false
                val project = dao.record(accountId, PlannorApi.TYPE_PROJECT, intent.projectId.toString())?.let {
                    json.decodeFromString(PlannorProjectSelection.serializer(), it.payloadJson)
                } ?: return@withTransaction false
                if (!project.selected || project.archived || project.linkedLocalProjectId != intent.localProjectId) {
                    return@withTransaction false
                }
                val nextBaseline = SharedWorkBaseline(
                    entryId = remoteEntry.serverId,
                    entryRevision = remoteEntry.revision,
                    allocationId = allocation?.allocationId,
                    allocationRevision = allocation?.revision,
                    entryDurationSeconds = remoteEntry.durationSeconds,
                    allocationDurationSeconds = allocation?.durationSeconds ?: baseline.allocationDurationSeconds,
                    allocationTaskId = allocation?.taskId,
                    allocationTracksEntry = allocation?.durationSeconds == remoteEntry.durationSeconds,
                    entryWorkDate = remoteEntry.workDate,
                    entryPrivateNote = remoteEntry.privateNote,
                    allocationProjectId = allocation?.projectId ?: baseline.allocationProjectId,
                )
                when (resolution) {
                    SharedWorkResolution.KEEP_TIMNOR -> {
                        dao.upsert(baselineRecord.copy(payloadJson = json.encodeToString(
                            SharedWorkBaseline.serializer(), nextBaseline,
                        ), updatedAtUtc = Clock.System.now().toString()))
                        put(intent.copy(
                            queuedVersion = intent.localVersion - 1,
                            reviewReason = null,
                            deletionApproved = intent.deleted,
                        ))
                        true
                    }
                    SharedWorkResolution.USE_PLANNOR -> {
                        if (work == null) return@withTransaction false
                        if (remoteEntry.durationSeconds !in 1..Int.MAX_VALUE.toLong()) return@withTransaction false
                        val remoteDate = runCatching { LocalDate.parse(remoteEntry.workDate) }.getOrNull()
                            ?: return@withTransaction false
                        val dateTime = remoteDate.atTime(work.date.toLocalTime())
                        database.workDao().upsert(Work(
                            description = remoteEntry.privateNote.orEmpty(),
                            date = dateTime,
                            project = work.project,
                            task = allocation?.taskId ?: 0,
                            time = remoteEntry.durationSeconds.toInt(),
                            id = work.id,
                        ))
                        val newVersion = intent.localVersion + 1
                        intent = intent.copy(
                            localVersion = newVersion,
                            queuedVersion = newVersion,
                            workDate = remoteEntry.workDate,
                            durationSeconds = remoteEntry.durationSeconds,
                            privateNote = remoteEntry.privateNote.orEmpty(),
                            taskId = allocation?.taskId,
                            reviewReason = null,
                        )
                        dao.upsert(baselineRecord.copy(payloadJson = json.encodeToString(
                            SharedWorkBaseline.serializer(), nextBaseline,
                        ), updatedAtUtc = Clock.System.now().toString()))
                        put(intent)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    /** Capture a baseline only when the server still exactly matches the create we sent. */
    suspend fun recordInitialBaselines(entries: List<PersonalTimeEntry>, allocations: List<ProjectTimeAllocation>) {
        val accountId = sessions.read()?.accountId ?: return
        database.withTransaction {
            if (sessions.read()?.accountId != accountId) return@withTransaction
            dao.records(accountId, TYPE_WORK_INTENT).forEach { record ->
                if (record.recordKey.startsWith("detached:")) return@forEach
                val intent = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
                if (intent.queuedVersion == intent.localVersion ||
                    dao.record(accountId, TYPE_WORK_BASELINE, record.recordKey) != null
                ) return@forEach
                val requestRecord = dao.record(accountId, PlannorApi.TYPE_OUTBOX, intent.createKey) ?: return@forEach
                val request = json.decodeFromString(SharedTimeOutboxItem.serializer(), requestRecord.payloadJson)
                if (request.operation != SharedTimeOperation.CREATE_AND_ALLOCATE ||
                    request.outcome != SharedTimeOutcome.SUCCEEDED ||
                    request.atomicIdempotencyKey != intent.createKey || request.sourceId != intent.sourceId
                ) return@forEach
                val entry = entries.singleOrNull { it.accountId == accountId && it.serverId == request.entryId }
                    ?: return@forEach
                val matching = allocations.filter { it.accountId == accountId && it.entryId == entry.serverId }
                val allocation = selectCreatedAllocation(
                    allocations = matching,
                    allocationId = request.allocationId,
                    projectId = request.projectId,
                    taskId = request.taskId,
                    workDate = request.workDate,
                    durationSeconds = request.durationSeconds,
                    sharedNote = request.sharedNote,
                ) ?: return@forEach
                if (entry.sourceWorkId != intent.sourceId || entry.revision != request.entryRevision ||
                    entry.workDate != request.workDate || entry.durationSeconds != request.durationSeconds ||
                    entry.privateNote != request.privateNote || entry.allocatedSeconds < allocation.durationSeconds ||
                    entry.allocatedSeconds > entry.durationSeconds ||
                    allocation.projectId != request.projectId || allocation.taskId != request.taskId ||
                    allocation.sharedNote != request.sharedNote || allocation.durationSeconds != request.durationSeconds ||
                    allocation.revision != 1
                ) return@forEach
                val baseline = SharedWorkBaseline(
                    entryId = entry.serverId,
                    entryRevision = entry.revision,
                    allocationId = allocation.allocationId,
                    allocationRevision = allocation.revision,
                    entryDurationSeconds = entry.durationSeconds,
                    allocationDurationSeconds = allocation.durationSeconds,
                    allocationTaskId = allocation.taskId,
                    allocationTracksEntry = allocation.durationSeconds == entry.durationSeconds,
                    entryWorkDate = entry.workDate,
                    entryPrivateNote = entry.privateNote,
                    allocationProjectId = allocation.projectId,
                    allocationSnapshots = matching.toSnapshots(),
                )
                dao.upsert(SharedHoursRecord(accountId, TYPE_WORK_BASELINE, record.recordKey,
                    json.encodeToString(SharedWorkBaseline.serializer(), baseline), Clock.System.now().toString()))
            }
        }
    }

    /** Queue at most one immutable correction per entry after checking the latest server revisions. */
    suspend fun prepareReconciliations(entries: List<PersonalTimeEntry>, allocations: List<ProjectTimeAllocation>): Int {
        val accountId = sessions.read()?.accountId ?: return 0
        return database.withTransaction {
            var prepared = 0
            dao.records(accountId, TYPE_WORK_INTENT).forEach { record ->
                if (record.recordKey.startsWith("detached:")) return@forEach
                var intent = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
                val baselineRecord = dao.record(accountId, TYPE_WORK_BASELINE, record.recordKey) ?: return@forEach
                var baseline = json.decodeFromString(SharedWorkBaseline.serializer(), baselineRecord.payloadJson)
                val entry = entries.singleOrNull { it.accountId == accountId && it.serverId == baseline.entryId }
                val matching = allocations.filter { it.accountId == accountId && it.entryId == baseline.entryId }

                intent.activeCommandKey?.let { activeKey ->
                    val row = dao.record(accountId, PlannorApi.TYPE_OUTBOX, activeKey) ?: return@forEach
                    val command = json.decodeFromString(SharedTimeOutboxItem.serializer(), row.payloadJson)
                    if (command.outcome != SharedTimeOutcome.SUCCEEDED) return@forEach
                    if (command.operation == SharedTimeOperation.DELETE_ENTRY) {
                        if (entry != null || !intent.deleted) return@forEach
                        put(intent.copy(queuedVersion = intent.localVersion, activeCommandKey = null))
                        dao.delete(accountId, TYPE_WORK_BASELINE, record.recordKey)
                        return@forEach
                    }
                    val observed = entry
                    if (observed == null) {
                        if (intent.deleted && command.operation == SharedTimeOperation.DELETE_ALLOCATION) {
                            put(intent.copy(queuedVersion = intent.localVersion, activeCommandKey = null))
                            dao.delete(accountId, TYPE_WORK_BASELINE, record.recordKey)
                        } else {
                            put(intent.copy(reviewReason = REVIEW_REMOTE_ENTRY_MISSING))
                        }
                        return@forEach
                    }
                    // Each supported mutation advances the parent exactly once. A larger revision
                    // means another writer intervened and must be reviewed instead of overwritten.
                    if (observed.revision != baseline.entryRevision + 1 ||
                        command.entryRevision != observed.revision || observed.sourceWorkId != intent.sourceId
                    ) return@forEach
                    val allocation = matching.singleOrNull { it.allocationId == baseline.allocationId }
                    val next = when (command.operation) {
                        SharedTimeOperation.ALLOCATE_EXISTING -> {
                            val created = matching.singleOrNull {
                                it.allocationId == command.allocationId && it.projectId == intent.projectId
                            } ?: return@forEach
                            if (created.revision != 1 || created.durationSeconds != command.durationSeconds ||
                                created.taskId != command.taskId || observed.allocatedSeconds !=
                                matching.sumOf(ProjectTimeAllocation::durationSeconds)
                            ) return@forEach
                            baseline.copy(
                                entryRevision = observed.revision,
                                allocationId = created.allocationId,
                                allocationRevision = created.revision,
                                allocationDurationSeconds = created.durationSeconds,
                                allocationTaskId = created.taskId,
                                allocationProjectId = created.projectId,
                                allocationTracksEntry = created.durationSeconds == observed.durationSeconds,
                                entryDurationSeconds = observed.durationSeconds,
                                entryWorkDate = observed.workDate,
                                entryPrivateNote = observed.privateNote,
                            )
                        }
                        SharedTimeOperation.UPDATE_ENTRY -> {
                            if (allocation?.allocationId != baseline.allocationId ||
                                allocation?.revision != baseline.allocationRevision ||
                                observed.durationSeconds != command.durationSeconds || observed.workDate != command.workDate ||
                                observed.privateNote != command.privateNote
                            ) return@forEach
                            baseline.copy(
                                entryRevision = observed.revision,
                                entryDurationSeconds = observed.durationSeconds,
                                entryWorkDate = observed.workDate,
                                entryPrivateNote = observed.privateNote,
                            )
                        }
                        SharedTimeOperation.UPDATE_ALLOCATION -> {
                            if (allocation == null || allocation.allocationId != baseline.allocationId ||
                                allocation.revision != (baseline.allocationRevision ?: return@forEach) + 1 ||
                                allocation.durationSeconds != command.durationSeconds || allocation.taskId != command.taskId
                            ) return@forEach
                            baseline.copy(
                                entryRevision = observed.revision,
                                allocationRevision = allocation.revision,
                                allocationDurationSeconds = allocation.durationSeconds,
                                allocationTaskId = allocation.taskId,
                            )
                        }
                        SharedTimeOperation.DELETE_ALLOCATION -> {
                            if (matching.any { it.allocationId == baseline.allocationId } ||
                                observed.allocatedSeconds != matching.sumOf(ProjectTimeAllocation::durationSeconds)
                            ) return@forEach
                            if (intent.deleted && matching.isNotEmpty()) {
                                // This row owns one allocation. Keep the owner's other project
                                // allocations and remove only the allocation the user approved.
                                dao.delete(accountId, TYPE_WORK_BASELINE, record.recordKey)
                                dao.delete(accountId, TYPE_WORK_INTENT, record.recordKey)
                                return@forEach
                            }
                            baseline.copy(
                                entryRevision = observed.revision,
                                allocationId = null,
                                allocationRevision = null,
                                allocationSnapshots = matching.filterNot { it.allocationId == baseline.allocationId }.toSnapshots(),
                            )
                        }
                        else -> return@forEach
                    }
                    baseline = next.copy(allocationSnapshots = matching.toSnapshots())
                    dao.upsert(baselineRecord.copy(payloadJson = json.encodeToString(SharedWorkBaseline.serializer(), next)))
                    intent = intent.copy(activeCommandKey = null)
                    put(intent)
                }

                val observed = entry
                if (observed == null) {
                    // The owner list is fetched as a full snapshot. Preserve the local row and
                    // surface the missing remote identity for explicit review.
                    if (intent.deleted && intent.activeCommandKey == null) {
                        dao.delete(accountId, TYPE_WORK_BASELINE, record.recordKey)
                        dao.delete(accountId, TYPE_WORK_INTENT, record.recordKey)
                        return@forEach
                    }
                    if (intent.reviewReason != REVIEW_REMOTE_ENTRY_MISSING) {
                        put(intent.copy(reviewReason = REVIEW_REMOTE_ENTRY_MISSING))
                    }
                    return@forEach
                }
                if (observed.sourceWorkId != intent.sourceId) return@forEach
                val currentProject = dao.record(accountId, PlannorApi.TYPE_PROJECT, intent.projectId.toString())?.let {
                    json.decodeFromString(PlannorProjectSelection.serializer(), it.payloadJson)
                }
                if (currentProject?.linkedLocalProjectId != intent.localProjectId ||
                    currentProject.archived || !currentProject.available || !currentProject.selected
                ) {
                    val replacement = dao.records(accountId, PlannorApi.TYPE_PROJECT).asSequence()
                        .map { json.decodeFromString(PlannorProjectSelection.serializer(), it.payloadJson) }
                        .singleOrNull {
                            it.accountId == accountId && it.linkedLocalProjectId == intent.localProjectId &&
                                it.selected && !it.archived && it.available
                        }
                    if (replacement != null && !intent.deleted) {
                        intent = intent.copy(projectId = replacement.projectId, taskId = null, reviewReason = null)
                        put(intent)
                    }
                }
                val allocation = matching.singleOrNull()
                val remote = SharedWorkRemoteState(
                    observed.serverId, observed.revision, observed.durationSeconds, observed.workDate,
                    observed.privateNote, observed.allocatedSeconds,
                    matching.singleOrNull { it.allocationId == baseline.allocationId }?.allocationId,
                    matching.singleOrNull { it.allocationId == baseline.allocationId }?.revision,
                    matching.singleOrNull { it.allocationId == baseline.allocationId }?.projectId,
                    matching.singleOrNull { it.allocationId == baseline.allocationId }?.taskId,
                    matching.singleOrNull { it.allocationId == baseline.allocationId }?.durationSeconds,
                    matching.size,
                    matching.sumOf(ProjectTimeAllocation::durationSeconds),
                    matching.toSnapshots(),
                )
                val remoteChangeReason = sharedWorkRemoteChangeReason(baseline, remote, baseline.allocationProjectId ?: intent.projectId)
                val project = dao.record(accountId, PlannorApi.TYPE_PROJECT, intent.projectId.toString())?.let {
                    json.decodeFromString(PlannorProjectSelection.serializer(), it.payloadJson)
                }
                if (project != null && !project.available) {
                    if (intent.reviewReason != REVIEW_ACCESS_REMOVED) {
                        put(intent.copy(reviewReason = REVIEW_ACCESS_REMOVED))
                    }
                    return@forEach
                }
                if (intent.queuedVersion == intent.localVersion) {
                    val adoptedBaseline = if (remoteChangeReason == REVIEW_REMOTE_ALLOCATIONS_CHANGED &&
                        project != null && project.available
                    ) adoptRemoteAllocationSnapshot(intent, baseline, remote, project) else null
                    if (adoptedBaseline != null) {
                        val localWork = database.workDao().getAll().singleOrNull { it.id == intent.localWorkId }
                            ?: return@forEach
                        val managed = matching.singleOrNull { it.allocationId == baseline.allocationId }
                        dao.upsert(baselineRecord.copy(payloadJson = json.encodeToString(
                            SharedWorkBaseline.serializer(), adoptedBaseline,
                        ), updatedAtUtc = Clock.System.now().toString()))
                        val resolvedTask = managed?.takeIf {
                            matching.size == 1 && it.durationSeconds == observed.durationSeconds &&
                                observed.allocatedSeconds == observed.durationSeconds
                        }?.taskId
                        val localTaskId = resolvedTask ?: 0
                        if (localWork.task != localTaskId) {
                            database.workDao().upsert(localWork.copy(task = localTaskId))
                            val newVersion = intent.localVersion + 1
                            put(intent.copy(
                                taskId = resolvedTask,
                                localVersion = newVersion,
                                queuedVersion = newVersion,
                                reviewReason = null,
                            ))
                        } else if (intent.taskId != resolvedTask || intent.reviewReason != null) {
                            put(intent.copy(taskId = resolvedTask, reviewReason = null))
                        }
                        return@forEach
                    }
                    val reason = remoteChangeReason ?: when {
                        project == null -> REVIEW_REMOTE_ALLOCATIONS_UNAVAILABLE
                        !project.available -> REVIEW_ACCESS_REMOVED
                        else -> null
                    }
                    if (intent.reviewReason != reason) put(intent.copy(reviewReason = reason))
                    return@forEach
                }
                if (remoteChangeReason != null) {
                    if (intent.reviewReason != remoteChangeReason) put(intent.copy(reviewReason = remoteChangeReason))
                    return@forEach
                }
                val localProject = project?.takeIf {
                    it.accountId == accountId && it.projectId == intent.projectId && it.selected &&
                        !it.archived && it.available
                }?.linkedLocalProjectId
                val action = planSharedWorkReconciliation(intent, baseline, remote, localProject)
                if (action == SharedWorkReconciliation.Current) {
                    put(intent.copy(queuedVersion = intent.localVersion, reviewReason = null))
                    return@forEach
                }
                if (action == SharedWorkReconciliation.Conflict) {
                    val reason = remoteChangeReason ?: REVIEW_RECONCILIATION_CONFLICT
                    if (intent.reviewReason != reason) put(intent.copy(reviewReason = reason))
                    return@forEach
                }
                val key = UUID.randomUUID().toString()
                val operation = when (action) {
                    SharedWorkReconciliation.DeleteAllocation -> SharedTimeOperation.DELETE_ALLOCATION
                    SharedWorkReconciliation.DeleteEntry -> SharedTimeOperation.DELETE_ENTRY
                    is SharedWorkReconciliation.CreateAllocation -> SharedTimeOperation.ALLOCATE_EXISTING
                    is SharedWorkReconciliation.UpdateAllocation -> SharedTimeOperation.UPDATE_ALLOCATION
                    is SharedWorkReconciliation.UpdateEntry -> SharedTimeOperation.UPDATE_ENTRY
                    else -> return@forEach
                }
                val allocationCommand = operation == SharedTimeOperation.ALLOCATE_EXISTING ||
                    operation == SharedTimeOperation.UPDATE_ALLOCATION || operation == SharedTimeOperation.DELETE_ALLOCATION
                val request = SharedTimeOutboxItem(
                    accountId = accountId, operation = operation, outcome = SharedTimeOutcome.PENDING,
                    entryId = baseline.entryId, entryRevision = baseline.entryRevision,
                    allocationId = baseline.allocationId, allocationRevision = baseline.allocationRevision,
                    projectId = intent.projectId,
                    taskId = when (action) {
                        is SharedWorkReconciliation.UpdateAllocation -> action.taskId
                        is SharedWorkReconciliation.CreateAllocation -> action.taskId
                        else -> null
                    },
                    workDate = intent.workDate,
                    durationSeconds = when (action) {
                        is SharedWorkReconciliation.UpdateAllocation -> action.durationSeconds
                        is SharedWorkReconciliation.CreateAllocation -> action.durationSeconds
                        is SharedWorkReconciliation.UpdateEntry -> action.durationSeconds
                        else -> intent.durationSeconds
                    },
                    privateNote = (action as? SharedWorkReconciliation.UpdateEntry)?.privateNote,
                    entryIdempotencyKey = if (allocationCommand) null else key,
                    allocationIdempotencyKey = if (allocationCommand) key else null,
                )
                dao.upsert(SharedHoursRecord(accountId, PlannorApi.TYPE_OUTBOX, key,
                    json.encodeToString(SharedTimeOutboxItem.serializer(), request), Clock.System.now().toString()))
                put(intent.copy(activeCommandKey = key))
                prepared++
            }
            check(sessions.read()?.accountId == accountId)
            prepared
        }
    }

    /** A restore may reuse integer IDs. Keep unsent intents, but detach them from restored rows. */
    suspend fun detachForRestore() {
        dao.recordsForType(TYPE_WORK_INTENT).forEach { record ->
            if (record.recordKey.startsWith("detached:")) return@forEach
            val intent = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
            listOfNotNull(intent.createKey, intent.activeCommandKey).forEach { key ->
                dao.record(intent.accountId, PlannorApi.TYPE_OUTBOX, key)?.let { queued ->
                    val item = json.decodeFromString(SharedTimeOutboxItem.serializer(), queued.payloadJson)
                    if (item.outcome == SharedTimeOutcome.PENDING || item.outcome == SharedTimeOutcome.OUTCOME_UNKNOWN) {
                        dao.upsert(queued.copy(payloadJson = json.encodeToString(
                            SharedTimeOutboxItem.serializer(),
                            item.copy(outcome = SharedTimeOutcome.CONFLICT, errorCode = "restore_review_required"),
                        )))
                    }
                }
            }
            dao.upsert(record.copy(
                recordKey = "detached:${intent.sourceId}",
                payloadJson = json.encodeToString(
                    SharedWorkIntent.serializer(), intent.copy(detachedFromLocalWork = true),
                ),
            ))
            dao.delete(record.accountId, record.recordType, record.recordKey)
            dao.record(record.accountId, TYPE_WORK_BASELINE, record.recordKey)?.let { baseline ->
                dao.upsert(baseline.copy(recordKey = "detached:${intent.sourceId}"))
                dao.delete(baseline.accountId, baseline.recordType, baseline.recordKey)
            }
        }
        dao.recordsForType(PlannorApi.TYPE_PROJECT).forEach { record ->
            val project = json.decodeFromString(PlannorProjectSelection.serializer(), record.payloadJson)
            if (project.linkedLocalProjectId != null) {
                dao.upsert(record.copy(payloadJson = json.encodeToString(
                    PlannorProjectSelection.serializer(), project.copy(linkedLocalProjectId = null),
                )))
            }
        }
    }

    private suspend fun put(intent: SharedWorkIntent) {
        dao.upsert(SharedHoursRecord(
            accountId = intent.accountId,
            recordType = TYPE_WORK_INTENT,
            recordKey = intent.localWorkId.toString(),
            payloadJson = json.encodeToString(SharedWorkIntent.serializer(), intent),
            updatedAtUtc = Clock.System.now().toString(),
        ))
    }

    companion object {
        val BACKUP_RECORD_TYPES = listOf(
            PlannorApi.TYPE_PROJECT,
            TYPE_WORK_INTENT,
            TYPE_WORK_BASELINE,
            PlannorApi.TYPE_OUTBOX,
        )
        const val TYPE_WORK_INTENT = "shared_work_intent"
        const val TYPE_WORK_BASELINE = "shared_work_baseline"
        const val REVIEW_REMOTE_ENTRY_MISSING = "remote_entry_missing"
        const val REVIEW_REMOTE_ENTRY_CHANGED = "remote_entry_changed"
        const val REVIEW_REMOTE_ALLOCATIONS_CHANGED = "remote_allocations_changed"
        const val REVIEW_REMOTE_ALLOCATIONS_UNAVAILABLE = "remote_allocations_unavailable"
        const val REVIEW_RECONCILIATION_CONFLICT = "reconciliation_conflict"
        const val REVIEW_PROJECT_MOVE_UNAVAILABLE = "project_move_unavailable"
        const val REVIEW_LOCAL_CHANGE_OTHER_ACCOUNT = "local_change_other_account"
        const val REVIEW_RESTORE_COMMAND_MISMATCH = "restore_command_mismatch"
        const val REVIEW_ACCESS_REMOVED = "access_removed"
    }
}

private fun List<ProjectTimeAllocation>.toSnapshots(): List<SharedWorkAllocationSnapshot> =
    sortedBy(ProjectTimeAllocation::allocationId).map {
        SharedWorkAllocationSnapshot(
            allocationId = it.allocationId,
            revision = it.revision,
            projectId = it.projectId,
            taskId = it.taskId,
            durationSeconds = it.durationSeconds,
            sharedNote = it.sharedNote,
        )
    }

internal fun adoptRemoteAllocationSnapshot(
    intent: SharedWorkIntent,
    baseline: SharedWorkBaseline,
    remote: SharedWorkRemoteState,
    project: PlannorProjectSelection,
): SharedWorkBaseline? {
    if (intent.deleted || intent.queuedVersion != intent.localVersion ||
        remote.entryId != baseline.entryId || remote.durationSeconds != baseline.entryDurationSeconds ||
        remote.workDate != baseline.entryWorkDate || remote.privateNote != baseline.entryPrivateNote ||
        remote.entryRevision <= baseline.entryRevision || baseline.allocationId == null ||
        remote.allocationRowsTotalSeconds != remote.allocatedSeconds ||
        remote.allocationSnapshots.size != remote.allocationCount ||
        remote.allocationSnapshots.map(SharedWorkAllocationSnapshot::allocationId).distinct().size != remote.allocationCount ||
        remote.allocationSnapshots.any { it.revision <= 0 || it.durationSeconds <= 0 } ||
        remote.allocationSnapshots.sumOf(SharedWorkAllocationSnapshot::durationSeconds) != remote.allocatedSeconds ||
        remote.allocatedSeconds > remote.durationSeconds || project.projectId != intent.projectId ||
        project.linkedLocalProjectId != intent.localProjectId || !project.available || !project.selected || project.archived
    ) return null
    if (baseline.allocationId != null && remote.allocationSnapshots.none {
            it.allocationId == baseline.allocationId && it.projectId == baseline.allocationProjectId
        }) return null
    val managed = remote.allocationSnapshots.singleOrNull { it.allocationId == baseline.allocationId }
    return baseline.copy(
        entryRevision = remote.entryRevision,
        allocationRevision = managed?.revision,
        allocationDurationSeconds = managed?.durationSeconds ?: baseline.allocationDurationSeconds,
        allocationTaskId = managed?.taskId,
        allocationProjectId = managed?.projectId ?: baseline.allocationProjectId,
        allocationTracksEntry = managed?.durationSeconds == remote.durationSeconds,
        allocationSnapshots = remote.allocationSnapshots,
    )
}

private fun SharedTimeOutboxItem.sameImmutableRequest(other: SharedTimeOutboxItem): Boolean =
    accountId == other.accountId && operation == other.operation && projectId == other.projectId &&
        taskId == other.taskId && workDate == other.workDate && durationSeconds == other.durationSeconds &&
        sourceId == other.sourceId && privateNote == other.privateNote && sharedNote == other.sharedNote &&
        atomicIdempotencyKey == other.atomicIdempotencyKey && entryIdempotencyKey == other.entryIdempotencyKey &&
        allocationIdempotencyKey == other.allocationIdempotencyKey

private fun SharedTimeOutboxItem.matchesCreate(intent: SharedWorkIntent): Boolean =
    operation == SharedTimeOperation.CREATE_AND_ALLOCATE && accountId == intent.accountId &&
        atomicIdempotencyKey == intent.createKey && sourceId == intent.sourceId &&
        projectId == intent.projectId && taskId == intent.taskId && workDate == intent.workDate &&
        durationSeconds == intent.durationSeconds && privateNote == intent.privateNote.ifBlank { null } &&
        sharedNote == null
