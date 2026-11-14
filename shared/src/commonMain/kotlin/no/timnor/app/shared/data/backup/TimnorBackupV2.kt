package no.timnor.app.shared.data.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.SharedHoursBackupRecord
import no.timnor.app.core.repository.SharedWorkBaseline
import no.timnor.app.core.repository.SharedWorkIntent
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem

const val TIMNOR_BACKUP_V2_VERSION = 2
private const val MAX_SHARED_HOURS_RECORDS = 400_000
private const val MAX_SHARED_HOURS_PAYLOAD_BYTES = 1_000_000
private val BACKUP_SHARED_HOURS_TYPES = setOf(
    "plannor_project",
    "shared_work_intent",
    "shared_work_baseline",
    "shared_time_outbox",
)

@Serializable
data class TimnorBackupV2(
    val format: String,
    val version: Int,
    val createdAt: String,
    val projects: List<TimnorBackupProject>,
    val works: List<TimnorBackupWork>,
    val sharedHoursRecords: List<SharedHoursBackupRecord> = emptyList(),
)

data class TimnorBackupImport(
    val projects: List<TimnorBackupProject>,
    val works: List<TimnorBackupWork>,
    val sharedHoursRecords: List<SharedHoursBackupRecord>,
    val createdAt: String,
    val version: Int,
)

private val backupV2Json = Json { ignoreUnknownKeys = false; isLenient = false }

fun encodeTimnorBackupV2(
    projects: List<StoredProject>,
    works: List<StoredWork>,
    sharedHoursRecords: List<SharedHoursBackupRecord>,
    createdAt: String = Clock.System.now().toString(),
): String {
    val backup = TimnorBackupV2(
        format = TIMNOR_BACKUP_FORMAT,
        version = TIMNOR_BACKUP_V2_VERSION,
        createdAt = createdAt,
        projects = projects.map { TimnorBackupProject(it.id, it.description, it.name) },
        works = works.map { work ->
            TimnorBackupWork(
                id = work.id,
                description = work.description,
                date = no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).toString(),
                project = work.project,
                task = work.task,
                time = work.time,
            )
        },
        sharedHoursRecords = sharedHoursRecords,
    )
    validateTimnorBackupV2(backup)
    return backupV2Json.encodeToString(backup).also { encoded ->
        require(encoded.encodeToByteArray().size <= TIMNOR_BACKUP_MAX_BYTES)
    }
}

/** Reads both existing version-1 files and version-2 files with account-scoped sync metadata. */
fun decodeTimnorBackup(contents: String): TimnorBackupImport {
    require(contents.encodeToByteArray().size <= TIMNOR_BACKUP_MAX_BYTES)
    val root = backupV2Json.parseToJsonElement(contents).jsonObject
    val version = root["version"]?.jsonPrimitive?.intOrNull
        ?: throw IllegalArgumentException("Missing backup version")
    if (version > TIMNOR_BACKUP_V2_VERSION) throw UnsupportedTimnorBackupVersionException(version)
    return when (version) {
        TIMNOR_BACKUP_VERSION -> decodeTimnorBackupV1(contents).let { backup ->
            TimnorBackupImport(backup.projects, backup.works, emptyList(), backup.createdAt, backup.version)
        }
        TIMNOR_BACKUP_V2_VERSION -> backupV2Json.decodeFromString<TimnorBackupV2>(contents).also(::validateTimnorBackupV2).let {
            TimnorBackupImport(it.projects, it.works, it.sharedHoursRecords, it.createdAt, it.version)
        }
        else -> throw IllegalArgumentException("Unsupported backup version")
    }
}

fun TimnorBackupImport.toStoredProjects(): List<StoredProject> =
    projects.map { StoredProject(it.description, it.name, it.id) }

fun TimnorBackupImport.toStoredWorks(): List<StoredWork> = works.map { backupWork ->
    val date = kotlinx.datetime.LocalDateTime.parse(backupWork.date)
    StoredWork(
        description = backupWork.description,
        dateEpochSeconds = no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime(date),
        project = backupWork.project,
        task = backupWork.task,
        time = backupWork.time,
        id = backupWork.id,
    )
}

fun validateTimnorBackupV2(value: TimnorBackupV2) {
    require(value.format == TIMNOR_BACKUP_FORMAT && value.version == TIMNOR_BACKUP_V2_VERSION)
    require(value.projects.size <= 100_000 && value.works.size <= 100_000)
    require(value.projects.map(TimnorBackupProject::id).toSet().size == value.projects.size)
    require(value.works.map(TimnorBackupWork::id).toSet().size == value.works.size)
    require(value.sharedHoursRecords.size <= MAX_SHARED_HOURS_RECORDS)
    require(value.sharedHoursRecords.map { Triple(it.accountId, it.type, it.key) }.toSet().size == value.sharedHoursRecords.size)
    value.works.forEach { kotlinx.datetime.LocalDateTime.parse(it.date) }
    kotlin.time.Instant.parse(value.createdAt)
    value.sharedHoursRecords.forEach { record ->
        require(record.accountId > 0 && record.type in BACKUP_SHARED_HOURS_TYPES && record.key.isNotBlank())
        require(record.payloadJson.encodeToByteArray().size <= MAX_SHARED_HOURS_PAYLOAD_BYTES)
        validateSharedRecord(record)
    }
}

private fun validateSharedRecord(record: SharedHoursBackupRecord) {
    when (record.type) {
        "plannor_project" -> {
            val value = backupV2Json.decodeFromString(PlannorProjectSelection.serializer(), record.payloadJson)
            require(value.accountId == record.accountId && value.projectId.toString() == record.key)
        }
        "shared_work_intent" -> {
            val value = backupV2Json.decodeFromString(SharedWorkIntent.serializer(), record.payloadJson)
            require(value.accountId == record.accountId)
            require(record.key == value.localWorkId.toString() || record.key == "detached:${value.sourceId}")
        }
        "shared_work_baseline" -> {
            backupV2Json.decodeFromString(SharedWorkBaseline.serializer(), record.payloadJson)
            require(record.key.toIntOrNull() != null || record.key.startsWith("detached:"))
        }
        "shared_time_outbox" -> {
            val value = backupV2Json.decodeFromString(SharedTimeOutboxItem.serializer(), record.payloadJson)
            val commandKey = value.atomicIdempotencyKey ?: value.allocationIdempotencyKey ?:
                value.entryIdempotencyKey ?: value.allocationId ?: value.entryId.orEmpty()
            require(value.accountId == record.accountId && commandKey == record.key)
        }
    }
}
