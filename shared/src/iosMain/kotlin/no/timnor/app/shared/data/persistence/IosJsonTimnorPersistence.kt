package no.timnor.app.shared.data.persistence

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid
import no.timnor.app.core.repository.PlannorApi
import no.timnor.app.core.repository.PlannorSessionStorage
import no.timnor.app.core.repository.SharedWorkIntent
import no.timnor.app.core.repository.SharedWorkSyncIndicator
import no.timnor.app.core.repository.sharedWorkIndicators
import no.timnor.app.core.repository.SharedHoursRecordStorage
import no.timnor.app.core.repository.SharedWorkSyncCoordinator
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem
import no.timnor.app.shared.features.shared_hours.domain.OwnerAllocationLink
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.features.shared_hours.domain.TimeChangeCursor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import no.timnor.app.shared.data.backup.decodeTimnorBackup
import no.timnor.app.shared.data.backup.encodeTimnorBackupV2
import no.timnor.app.shared.data.backup.toStoredProjects
import no.timnor.app.shared.data.backup.toStoredWorks
import no.timnor.app.shared.data.export.createTimnorXlsx
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/** iOS file-backed implementation of the common persistence contract. */
@OptIn(ExperimentalForeignApi::class)
class IosJsonTimnorPersistence : TimnorPersistence, SharedHoursRecordStorage {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = false }
    private val filePath = dataFilePath()
    private val initialData = readData()
    private val projects = MutableStateFlow(initialData.projects)
    private val works = MutableStateFlow(initialData.works)
    private val sharedHoursRecords = MutableStateFlow(initialData.sharedHoursRecords)
    private var sessionStorage: PlannorSessionStorage? = null

    fun configureSessionStorage(storage: PlannorSessionStorage) {
        sessionStorage = storage
    }

    override fun observeProjects(): Flow<List<StoredProject>> = projects

    override fun observeSharedWorkIds(): Flow<Set<Int>> = sharedHoursRecords.map { records ->
        records.asSequence()
            .filter { it.type == IOS_WORK_INTENT && !it.key.startsWith("detached:") }
            .mapNotNull { it.key.toIntOrNull() }
            .toSet()
    }

    override fun observeSharedWorkIndicators(): Flow<Map<Int, SharedWorkSyncIndicator>> = sharedHoursRecords.map { records ->
        val intents = records.asSequence()
            .filter { it.type == IOS_WORK_INTENT }
            .mapNotNull { runCatching { json.decodeFromString(SharedWorkIntent.serializer(), it.payloadJson) }.getOrNull() }
            .toList()
        val baselines = records.asSequence()
            .filter { it.type == IOS_WORK_BASELINE }
            .map { it.accountId to it.key }
            .toSet()
        val outbox = records.asSequence()
            .filter { it.type == PlannorApi.TYPE_OUTBOX }
            .mapNotNull { record -> runCatching {
                (record.accountId to record.key) to json.decodeFromString(
                    SharedTimeOutboxItem.serializer(), record.payloadJson,
                )
            }.getOrNull() }
            .toMap()
        sharedWorkIndicators(intents, baselines, outbox, sessionStorage?.read()?.accountId)
    }

    override suspend fun snapshotAll(): TimnorDataSnapshot = mutex.withLock {
        TimnorDataSnapshot(projects.value.toList(), works.value.toList())
    }

    override suspend fun snapshotBackup(): TimnorBackupSnapshot = mutex.withLock {
        TimnorBackupSnapshot(
            projects = projects.value.toList(),
            works = works.value.toList(),
            sharedHoursRecords = sharedHoursRecords.value.filter { it.type in IOS_BACKUP_RECORD_TYPES }
                .map { SharedHoursBackupRecord(it.accountId, it.type, it.key, it.payloadJson) },
        )
    }

    override fun observeWorks(
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        projectIds: List<Int>,
    ): Flow<List<StoredWork>> = works.map { values ->
        values.filter { work ->
            work.dateEpochSeconds in startEpochSeconds..endEpochSeconds &&
                (projectIds.isEmpty() || work.project in projectIds)
        }
    }

    override suspend fun hoursForRange(
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        projectIds: List<Int>,
    ): Int = works.value.asSequence()
        .filter { it.dateEpochSeconds in startEpochSeconds..endEpochSeconds }
        .filter { projectIds.isEmpty() || it.project in projectIds }
        .sumOf(StoredWork::time)

    override suspend fun upsertProject(project: StoredProject) = mutex.withLock {
        val id = project.id.takeIf { it != 0 } ?: ((projects.value.maxOfOrNull(StoredProject::id) ?: 0) + 1)
        val updated = projects.value.filterNot { it.id == id } + project.copy(id = id)
        persist(updated, works.value)
    }

    override suspend fun createProject(project: StoredProject): StoredProject = mutex.withLock {
        require(project.id == 0)
        val id = (projects.value.maxOfOrNull(StoredProject::id) ?: 0) + 1
        check(id > 0) { "Project ID space exhausted" }
        val created = project.copy(id = id)
        persist(projects.value + created, works.value)
        created
    }

    override suspend fun deleteProject(project: StoredProject) = mutex.withLock {
        persist(projects.value.filterNot { it.id == project.id }, works.value)
    }

    override suspend fun upsertWork(work: StoredWork) = mutex.withLock {
        val isNew = work.id == 0
        val id = work.id.takeIf { it != 0 } ?: ((works.value.maxOfOrNull(StoredWork::id) ?: 0) + 1)
        val savedWork = work.copy(id = id)
        val updated = works.value.filterNot { it.id == id } + savedWork
        val records = updateIntentForWork(savedWork, sharedHoursRecords.value, created = isNew)
        persist(projects.value, updated, records)
    }

    override suspend fun upsertWorks(works: List<StoredWork>) = mutex.withLock {
        var nextId = this.works.value.maxOfOrNull(StoredWork::id) ?: 0
        var records = sharedHoursRecords.value
        val updated = works.fold(this.works.value) { current, work ->
            val isNew = work.id == 0
            val id = work.id.takeIf { it != 0 } ?: ++nextId
            val savedWork = work.copy(id = id)
            records = updateIntentForWork(savedWork, records, created = isNew)
            current.filterNot { it.id == id } + savedWork
        }
        persist(projects.value, updated, records)
    }

    override suspend fun deleteWork(work: StoredWork) = mutex.withLock {
        persist(projects.value, works.value.filterNot { it.id == work.id })
    }

    override suspend fun deleteWork(work: StoredWork, sharedDeletionConfirmed: Boolean) = mutex.withLock {
        val records = updateIntentForWork(
            work,
            sharedHoursRecords.value,
            created = false,
            deleted = true,
            deletionApproved = sharedDeletionConfirmed,
        )
        persist(projects.value, works.value.filterNot { it.id == work.id }, records)
    }

    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) = mutex.withLock {
        persist(projects, works, detachIntentsForRestore(sharedHoursRecords.value))
    }

    override suspend fun sharedHoursBackupRecords(): List<SharedHoursBackupRecord> = mutex.withLock {
        sharedHoursRecords.value.filter { it.type in IOS_BACKUP_RECORD_TYPES }
            .map { SharedHoursBackupRecord(it.accountId, it.type, it.key, it.payloadJson) }
    }

    override suspend fun replaceAll(
        projects: List<StoredProject>,
        works: List<StoredWork>,
        sharedHoursRecords: List<SharedHoursBackupRecord>,
    ) = mutex.withLock {
        val detached = detachIntentsForRestore(this.sharedHoursRecords.value).toMutableList()
        sharedHoursRecords.forEach { backup ->
            require(backup.accountId > 0 && backup.type in IOS_BACKUP_RECORD_TYPES && backup.key.isNotBlank())
            when (backup.type) {
                PlannorApi.TYPE_PROJECT -> {
                    val value = json.decodeFromString(PlannorProjectSelection.serializer(), backup.payloadJson)
                    require(value.accountId == backup.accountId && value.projectId.toString() == backup.key)
                }
                IOS_WORK_INTENT -> {
                    val value = json.decodeFromString(SharedWorkIntent.serializer(), backup.payloadJson)
                    require(value.accountId == backup.accountId)
                    require(backup.key == value.localWorkId.toString() || backup.key == "detached:${value.sourceId}")
                }
                IOS_WORK_BASELINE -> {
                    json.decodeFromString(no.timnor.app.core.repository.SharedWorkBaseline.serializer(), backup.payloadJson)
                    require(backup.key.toIntOrNull() != null || backup.key.startsWith("detached:"))
                }
                PlannorApi.TYPE_OUTBOX -> {
                    val value = json.decodeFromString(SharedTimeOutboxItem.serializer(), backup.payloadJson)
                    val commandKey = value.atomicIdempotencyKey ?: value.allocationIdempotencyKey ?:
                        value.entryIdempotencyKey ?: value.allocationId ?: value.entryId.orEmpty()
                    require(value.accountId == backup.accountId && commandKey == backup.key)
                }
            }
            val index = detached.indexOfFirst {
                it.accountId == backup.accountId && it.type == backup.type && it.key == backup.key
            }
            if (index < 0) {
                detached += IosSharedHoursRecord(
                    backup.accountId, backup.type, backup.key, backup.payloadJson, Clock.System.now().toString(),
                )
            } else if (backup.type == PlannorApi.TYPE_OUTBOX) {
                val existing = json.decodeFromString(SharedTimeOutboxItem.serializer(), detached[index].payloadJson)
                val imported = json.decodeFromString(SharedTimeOutboxItem.serializer(), backup.payloadJson)
                check(existing.sameImmutableRequest(imported)) { "Backup command key collision" }
                if (existing.errorCode == "restore_review_required") {
                    detached[index] = detached[index].copy(
                        payloadJson = json.encodeToString(
                            SharedTimeOutboxItem.serializer(),
                            existing.copy(
                                outcome = no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome.OUTCOME_UNKNOWN,
                                errorCode = null,
                            ),
                        ),
                        updatedAtUtc = Clock.System.now().toString(),
                    )
                }
            } else if (backup.type == PlannorApi.TYPE_PROJECT) {
                detached[index] = IosSharedHoursRecord(
                    backup.accountId, backup.type, backup.key, backup.payloadJson, Clock.System.now().toString(),
                )
            } else {
                check(detached[index].payloadJson == backup.payloadJson) { "Backup mapping collision" }
            }
        }
        detached.filter { it.type == IOS_WORK_INTENT && !it.key.startsWith("detached:") }.forEach { record ->
            val intent = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
            val commandIndex = detached.indexOfFirst {
                it.accountId == intent.accountId && it.type == PlannorApi.TYPE_OUTBOX && it.key == intent.createKey
            }
            if (commandIndex < 0) return@forEach
            val commandRecord = detached[commandIndex]
            val command = json.decodeFromString(SharedTimeOutboxItem.serializer(), commandRecord.payloadJson)
            if (command.errorCode != "restore_review_required") return@forEach
            if (command.matchesCreate(intent)) {
                detached[commandIndex] = commandRecord.copy(
                    payloadJson = json.encodeToString(
                        SharedTimeOutboxItem.serializer(),
                        command.copy(
                            outcome = no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome.OUTCOME_UNKNOWN,
                            errorCode = null,
                        ),
                    ),
                    updatedAtUtc = Clock.System.now().toString(),
                )
            } else {
                val intentIndex = detached.indexOfFirst {
                    it.accountId == record.accountId && it.type == record.type && it.key == record.key
                }
                if (intentIndex >= 0) detached[intentIndex] = record.copy(
                    payloadJson = json.encodeToString(
                        SharedWorkIntent.serializer(),
                        intent.copy(reviewReason = intent.reviewReason ?: SharedWorkSyncCoordinator.REVIEW_RESTORE_COMMAND_MISMATCH),
                    ),
                    updatedAtUtc = Clock.System.now().toString(),
                )
            }
        }
        persist(projects, works, detached)
    }

    /** Creates the Android-compatible version-1 backup payload for platform file/share UI. */
    suspend fun exportBackupV1(): String = mutex.withLock {
        val records = sharedHoursRecords.value.filter { it.type in IOS_BACKUP_RECORD_TYPES }
            .map { SharedHoursBackupRecord(it.accountId, it.type, it.key, it.payloadJson) }
        encodeTimnorBackupV2(projects.value, works.value, records)
    }

    fun snapshotWorks(): List<StoredWork> = works.value.toList()

    /** Validates the shared interchange format before atomically replacing this local store. */
    suspend fun restoreBackupV1(contents: String) {
        val backup = decodeTimnorBackup(contents)
        replaceAll(backup.toStoredProjects(), backup.toStoredWorks(), backup.sharedHoursRecords)
    }

    override suspend fun <T> get(
        accountId: Int,
        type: String,
        key: String,
        serializer: KSerializer<T>,
    ): T? = mutex.withLock {
        require(accountId > 0)
        sharedHoursRecords.value.singleOrNull { it.accountId == accountId && it.type == type && it.key == key }
            ?.let { json.decodeFromString(serializer, it.payloadJson) }
    }

    override suspend fun <T> getAll(
        accountId: Int,
        type: String,
        serializer: KSerializer<T>,
    ): List<T> = mutex.withLock {
        require(accountId > 0)
        sharedHoursRecords.value.filter { it.accountId == accountId && it.type == type }
            .sortedBy(IosSharedHoursRecord::updatedAtUtc)
            .map { json.decodeFromString(serializer, it.payloadJson) }
    }

    override suspend fun <T> put(
        accountId: Int,
        type: String,
        key: String,
        value: T,
        serializer: KSerializer<T>,
        updatedAtUtc: String,
    ) = mutex.withLock {
        require(accountId > 0 && type.isNotBlank() && key.isNotBlank())
        val record = IosSharedHoursRecord(accountId, type, key, json.encodeToString(serializer, value), updatedAtUtc)
        persist(projects.value, works.value, sharedHoursRecords.value.filterNot {
            it.accountId == accountId && it.type == type && it.key == key
        } + record)
    }

    override suspend fun <T> putAll(
        accountId: Int,
        type: String,
        values: List<Pair<String, T>>,
        serializer: KSerializer<T>,
        updatedAtUtc: String,
    ) = mutex.withLock {
        require(accountId > 0 && type.isNotBlank())
        val records = values.map { (key, value) ->
            require(key.isNotBlank())
            IosSharedHoursRecord(accountId, type, key, json.encodeToString(serializer, value), updatedAtUtc)
        }
        val merged = sharedHoursRecords.value.associateBy { Triple(it.accountId, it.type, it.key) }.toMutableMap()
        records.forEach { merged[Triple(it.accountId, it.type, it.key)] = it }
        persist(projects.value, works.value, merged.values.toList())
    }

    override suspend fun <T> replaceAll(
        accountId: Int,
        type: String,
        values: List<Pair<String, T>>,
        serializer: KSerializer<T>,
        updatedAtUtc: String,
    ) = mutex.withLock {
        require(accountId > 0 && type.isNotBlank())
        val records = values.map { (key, value) ->
            require(key.isNotBlank())
            IosSharedHoursRecord(accountId, type, key, json.encodeToString(serializer, value), updatedAtUtc)
        }
        persist(projects.value, works.value, sharedHoursRecords.value.filterNot {
            it.accountId == accountId && it.type == type
        } + records)
    }

    override suspend fun commitProjectSnapshot(
        accountId: Int,
        ownerLinks: List<OwnerAllocationLink>,
        allocations: List<ProjectTimeAllocation>,
        cursors: Map<String, TimeChangeCursor>,
        updatedAtUtc: String,
    ) = mutex.withLock {
        require(accountId > 0)
        val replacementTypes = setOf(PlannorApi.TYPE_OWNER_LINK, PlannorApi.TYPE_ALLOCATION)
        val replacementRecords = ownerLinks.map { link ->
            IosSharedHoursRecord(accountId, PlannorApi.TYPE_OWNER_LINK, link.allocationId,
                json.encodeToString(OwnerAllocationLink.serializer(), link), updatedAtUtc)
        } + allocations.map { allocation ->
            IosSharedHoursRecord(accountId, PlannorApi.TYPE_ALLOCATION, allocation.allocationId,
                json.encodeToString(ProjectTimeAllocation.serializer(), allocation), updatedAtUtc)
        } + cursors.map { (key, cursor) ->
            IosSharedHoursRecord(accountId, PlannorApi.TYPE_CURSOR, key,
                json.encodeToString(TimeChangeCursor.serializer(), cursor), updatedAtUtc)
        }
        persist(projects.value, works.value, sharedHoursRecords.value.filterNot {
            it.accountId == accountId && it.type in replacementTypes ||
                it.accountId == accountId && it.type == PlannorApi.TYPE_CURSOR && it.key in cursors.keys
        } + replacementRecords)
    }

    override suspend fun clear(accountId: Int, type: String) = mutex.withLock {
        require(accountId > 0)
        persist(projects.value, works.value, sharedHoursRecords.value.filterNot {
            it.accountId == accountId && it.type == type
        })
    }

    override suspend fun delete(accountId: Int, type: String, key: String) = mutex.withLock {
        require(accountId > 0)
        persist(projects.value, works.value, sharedHoursRecords.value.filterNot {
            it.accountId == accountId && it.type == type && it.key == key
        })
    }

    override suspend fun persistSharedWorkCreate(
        intent: SharedWorkIntent,
        request: SharedTimeOutboxItem,
    ) = mutex.withLock {
        require(intent.accountId == request.accountId && intent.createKey == request.atomicIdempotencyKey)
        val current = sharedHoursRecords.value
        val existing = current.singleOrNull {
            it.accountId == intent.accountId && it.type == PlannorApi.TYPE_OUTBOX && it.key == intent.createKey
        }?.let { json.decodeFromString(SharedTimeOutboxItem.serializer(), it.payloadJson) }
        if (existing?.errorCode == "restore_review_required") return@withLock
        val timestamp = Clock.System.now().toString()
        val intentRecord = IosSharedHoursRecord(
            intent.accountId,
            SharedWorkSyncCoordinator.TYPE_WORK_INTENT,
            intent.localWorkId.toString(),
            json.encodeToString(SharedWorkIntent.serializer(), intent.copy(queuedVersion = intent.localVersion)),
            timestamp,
        )
        val outboxRecord = IosSharedHoursRecord(
            request.accountId,
            PlannorApi.TYPE_OUTBOX,
            intent.createKey,
            json.encodeToString(SharedTimeOutboxItem.serializer(), request),
            timestamp,
        )
        persist(
            projects.value,
            works.value,
            current.filterNot {
                it.accountId == intent.accountId && it.type == SharedWorkSyncCoordinator.TYPE_WORK_INTENT &&
                    it.key == intent.localWorkId.toString() ||
                    it.accountId == request.accountId && it.type == PlannorApi.TYPE_OUTBOX && it.key == intent.createKey
            } + intentRecord + outboxRecord,
        )
    }

    override suspend fun putOutboxUnlessRestorePaused(
        accountId: Int,
        key: String,
        value: SharedTimeOutboxItem,
        updatedAtUtc: String,
    ) = mutex.withLock {
        require(accountId > 0 && key.isNotBlank())
        val current = sharedHoursRecords.value
        val existing = current.singleOrNull {
            it.accountId == accountId && it.type == PlannorApi.TYPE_OUTBOX && it.key == key
        }?.let { json.decodeFromString(SharedTimeOutboxItem.serializer(), it.payloadJson) }
        if (existing?.errorCode != "restore_review_required") {
            val record = IosSharedHoursRecord(
                accountId, PlannorApi.TYPE_OUTBOX, key,
                json.encodeToString(SharedTimeOutboxItem.serializer(), value), updatedAtUtc,
            )
            persist(projects.value, works.value, current.filterNot {
                it.accountId == accountId && it.type == PlannorApi.TYPE_OUTBOX && it.key == key
            } + record)
        }
    }

    private fun persist(
        projectValues: List<StoredProject>,
        workValues: List<StoredWork>,
        recordValues: List<IosSharedHoursRecord> = sharedHoursRecords.value,
    ) {
        val data = TimnorStoreFile(
            projectValues.sortedBy(StoredProject::id),
            workValues.sortedBy(StoredWork::id),
            recordValues.sortedWith(compareBy(
                IosSharedHoursRecord::accountId, IosSharedHoursRecord::type, IosSharedHoursRecord::key,
            )),
        )
        @Suppress("CAST_NEVER_SUCCEEDS")
        val nativeContents = json.encodeToString(data) as NSString
        val written = nativeContents.writeToFile(
            filePath,
            true,
            NSUTF8StringEncoding,
            null,
        )
        check(written) { "Unable to save Timnor data file" }
        projects.value = data.projects
        works.value = data.works
        sharedHoursRecords.value = data.sharedHoursRecords
    }

    private fun updateIntentForWork(
        work: StoredWork,
        records: List<IosSharedHoursRecord>,
        created: Boolean,
        deleted: Boolean = false,
        deletionApproved: Boolean = false,
    ): List<IosSharedHoursRecord> {
        val accountId = sessionStorage?.read()?.accountId
        val recordKey = work.id.toString()
        val workDate = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date.toString()
        var scopedRecords = records
        records.filter { it.type == IOS_WORK_INTENT && it.key == recordKey && it.accountId != accountId }
            .forEach { record ->
                val existing = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
                val projectChanged = work.project != existing.localProjectId
                val replacementProject = if (projectChanged) scopedRecords.asSequence()
                    .filter { it.accountId == record.accountId && it.type == PlannorApi.TYPE_PROJECT }
                    .map { json.decodeFromString(PlannorProjectSelection.serializer(), it.payloadJson) }
                    .singleOrNull {
                        it.linkedLocalProjectId == work.project && it.selected && !it.archived && it.available
                    }
                else null
                val changed = existing.localProjectId != work.project || existing.workDate != workDate ||
                    existing.durationSeconds != work.time.toLong() || existing.privateNote != work.description ||
                    existing.taskId != work.task.takeIf { it > 0 } || existing.deleted != deleted
                if (!changed) return@forEach
                val updated = existing.copy(
                    localVersion = existing.localVersion + 1,
                    localProjectId = work.project,
                    projectId = replacementProject?.projectId ?: existing.projectId,
                    workDate = workDate,
                    durationSeconds = work.time.toLong(),
                    privateNote = work.description,
                    taskId = if (projectChanged) null else work.task.takeIf { it > 0 },
                    deleted = deleted,
                    deletionApproved = false,
                    reviewReason = existing.reviewReason ?: SharedWorkSyncCoordinator.REVIEW_LOCAL_CHANGE_OTHER_ACCOUNT,
                )
                scopedRecords = scopedRecords.filterNot {
                    it.accountId == record.accountId && it.type == record.type && it.key == record.key
                } + record.copy(
                    payloadJson = json.encodeToString(SharedWorkIntent.serializer(), updated),
                    updatedAtUtc = Clock.System.now().toString(),
                )
            }
        if (accountId == null) return scopedRecords
        val existingRecord = records.singleOrNull {
            it.accountId == accountId && it.type == IOS_WORK_INTENT && it.key == recordKey
        }
        val intent = if (existingRecord != null) {
            val existing = json.decodeFromString(SharedWorkIntent.serializer(), existingRecord.payloadJson)
            val projectChanged = work.project != existing.localProjectId
            val replacementProject = if (projectChanged) scopedRecords.asSequence()
                .filter { it.accountId == accountId && it.type == PlannorApi.TYPE_PROJECT }
                .map { json.decodeFromString(PlannorProjectSelection.serializer(), it.payloadJson) }
                .singleOrNull {
                    it.accountId == accountId && it.linkedLocalProjectId == work.project &&
                        it.selected && !it.archived && it.available
                }
            else null
            existing.copy(
                localProjectId = work.project,
                projectId = replacementProject?.projectId ?: existing.projectId,
                localVersion = existing.localVersion + 1,
                workDate = workDate,
                durationSeconds = work.time.toLong(),
                privateNote = work.description,
                taskId = if (projectChanged) null else work.task.takeIf { it > 0 },
                deleted = deleted,
                deletionApproved = deleted && deletionApproved,
                reviewReason = when {
                    deleted -> existing.reviewReason
                    projectChanged && replacementProject == null -> SharedWorkSyncCoordinator.REVIEW_PROJECT_MOVE_UNAVAILABLE
                    else -> null
                },
            )
        } else {
            if (!created || deleted) return records
            val project = scopedRecords.asSequence()
                .filter { it.accountId == accountId && it.type == PlannorApi.TYPE_PROJECT }
                .map { json.decodeFromString(PlannorProjectSelection.serializer(), it.payloadJson) }
                .singleOrNull {
                    it.accountId == accountId && it.selected && !it.archived && it.available &&
                        it.linkedLocalProjectId == work.project
                } ?: return records
            SharedWorkIntent(
                accountId = accountId,
                localWorkId = work.id,
                sourceId = Uuid.random().toString(),
                createKey = Uuid.random().toString(),
                projectId = project.projectId,
                localProjectId = work.project,
                localVersion = 1,
                workDate = workDate,
                durationSeconds = work.time.toLong(),
                privateNote = work.description,
                taskId = work.task.takeIf { it > 0 },
            )
        }
        val updated = IosSharedHoursRecord(
            accountId,
            IOS_WORK_INTENT,
            recordKey,
            json.encodeToString(SharedWorkIntent.serializer(), intent),
            Clock.System.now().toString(),
        )
        return scopedRecords.filterNot { it.accountId == accountId && it.type == IOS_WORK_INTENT && it.key == recordKey } + updated
    }

    private fun detachIntentsForRestore(records: List<IosSharedHoursRecord>): List<IosSharedHoursRecord> {
        val updated = records.toMutableList()
        records.filter { it.type == IOS_WORK_INTENT && !it.key.startsWith("detached:") }.forEach { record ->
            val intent = json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
            listOfNotNull(intent.createKey, intent.activeCommandKey).forEach { commandKey ->
                val index = updated.indexOfFirst {
                    it.accountId == intent.accountId && it.type == PlannorApi.TYPE_OUTBOX && it.key == commandKey
                }
                if (index < 0) return@forEach
                val outbox = updated[index]
                val command = json.decodeFromString(SharedTimeOutboxItem.serializer(), outbox.payloadJson)
                if (command.outcome == no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome.PENDING ||
                    command.outcome == no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome.OUTCOME_UNKNOWN
                ) {
                    val paused = command.copy(
                        outcome = no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome.CONFLICT,
                        errorCode = "restore_review_required",
                    )
                    updated[index] = outbox.copy(
                        payloadJson = json.encodeToString(SharedTimeOutboxItem.serializer(), paused),
                    )
                }
            }
            val detachedKey = "detached:${intent.sourceId}"
            val recordIndex = updated.indexOfFirst {
                it.accountId == record.accountId && it.type == record.type && it.key == record.key
            }
            if (recordIndex >= 0) updated.removeAt(recordIndex)
            updated += record.copy(
                key = detachedKey,
                payloadJson = json.encodeToString(SharedWorkIntent.serializer(), intent.copy(detachedFromLocalWork = true)),
                updatedAtUtc = Clock.System.now().toString(),
            )
            val baselineIndex = updated.indexOfFirst {
                it.accountId == intent.accountId && it.type == IOS_WORK_BASELINE && it.key == record.key
            }
            if (baselineIndex >= 0) updated[baselineIndex] = updated[baselineIndex].copy(key = detachedKey)
        }
        return updated.map { record ->
            if (record.type != PlannorApi.TYPE_PROJECT) return@map record
            val project = json.decodeFromString(PlannorProjectSelection.serializer(), record.payloadJson)
            if (project.linkedLocalProjectId == null) record else record.copy(
                payloadJson = json.encodeToString(
                    PlannorProjectSelection.serializer(), project.copy(linkedLocalProjectId = null),
                ),
            )
        }
    }

    private fun readData(): TimnorStoreFile {
        val contents = NSString.stringWithContentsOfFile(
            path = filePath,
            encoding = NSUTF8StringEncoding,
            error = null,
        ) ?: return TimnorStoreFile()
        return json.decodeFromString(contents)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun dataFilePath(): String {
        val documents: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        return requireNotNull(documents?.path) + "/timnor-data.json"
    }
}

@Serializable
private data class TimnorStoreFile(
    val projects: List<StoredProject> = emptyList(),
    val works: List<StoredWork> = emptyList(),
    val sharedHoursRecords: List<IosSharedHoursRecord> = emptyList(),
)

@Serializable
private data class IosSharedHoursRecord(
    val accountId: Int,
    val type: String,
    val key: String,
    val payloadJson: String,
    val updatedAtUtc: String,
)

private const val IOS_WORK_INTENT = "shared_work_intent"
private const val IOS_WORK_BASELINE = "shared_work_baseline"
private val IOS_BACKUP_RECORD_TYPES = setOf(
    PlannorApi.TYPE_PROJECT,
    IOS_WORK_INTENT,
    IOS_WORK_BASELINE,
    PlannorApi.TYPE_OUTBOX,
)

private fun SharedTimeOutboxItem.sameImmutableRequest(other: SharedTimeOutboxItem): Boolean =
    accountId == other.accountId && operation == other.operation && projectId == other.projectId &&
        taskId == other.taskId && workDate == other.workDate && durationSeconds == other.durationSeconds &&
        sourceId == other.sourceId && privateNote == other.privateNote && sharedNote == other.sharedNote &&
        atomicIdempotencyKey == other.atomicIdempotencyKey && entryIdempotencyKey == other.entryIdempotencyKey &&
        allocationIdempotencyKey == other.allocationIdempotencyKey

private fun SharedTimeOutboxItem.matchesCreate(intent: SharedWorkIntent): Boolean =
    operation == no.timnor.app.shared.features.shared_hours.domain.SharedTimeOperation.CREATE_AND_ALLOCATE &&
        accountId == intent.accountId && atomicIdempotencyKey == intent.createKey && sourceId == intent.sourceId &&
        projectId == intent.projectId && taskId == intent.taskId && workDate == intent.workDate &&
        durationSeconds == intent.durationSeconds && privateNote == intent.privateNote.ifBlank { null } && sharedNote == null

object IosTimnorPlatform {
    private val store by lazy { IosJsonTimnorPersistence() }

    fun createPersistence(sessionStorage: PlannorSessionStorage): TimnorPersistence = store.also {
        it.configureSessionStorage(sessionStorage)
    }

    suspend fun exportBackupV1(): String = store.exportBackupV1()

    suspend fun previewBackupV1(contents: String): String {
        val backup = decodeTimnorBackup(contents)
        return "${backup.projects.size}|${backup.works.size}|${backup.sharedHoursRecords.size}"
    }

    suspend fun restoreBackupV1(contents: String) = store.restoreBackupV1(contents)

    @OptIn(ExperimentalEncodingApi::class)
    fun exportCurrentMonthXlsxBase64(): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return Base64.encode(createTimnorXlsx(store.snapshotWorks(), today.year, today.month.ordinal + 1))
    }
}

fun createTimnorPersistence(sessionStorage: PlannorSessionStorage): TimnorPersistence =
    IosTimnorPlatform.createPersistence(sessionStorage)
