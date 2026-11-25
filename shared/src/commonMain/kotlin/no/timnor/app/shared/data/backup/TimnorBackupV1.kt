package no.timnor.app.shared.data.backup

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.Instant
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.domain.project.MAX_HOURLY_RATE_ORE
import no.timnor.app.shared.data.persistence.StoredWork

const val TIMNOR_BACKUP_FORMAT = "org.examples.time_manager.backup"
const val TIMNOR_BACKUP_VERSION = 1
const val TIMNOR_BACKUP_MAX_BYTES = 25 * 1024 * 1024
private const val MAX_RECORDS_PER_TABLE = 100_000

@Serializable
data class TimnorBackupV1(
    val format: String,
    val version: Int,
    val createdAt: String,
    val projects: List<TimnorBackupProject>,
    val works: List<TimnorBackupWork>,
)

@Serializable
data class TimnorBackupProject(
    val id: Int,
    val description: String,
    val name: String,
    val hourlyRateOre: Int? = null,
    val weekendHourlyRateOre: Int? = null,
)

@Serializable
data class TimnorBackupWork(
    val id: Int,
    val description: String,
    val date: String,
    val project: Int,
    val task: Int,
    val time: Int,
    val appliedHourlyRateOre: Int? = null,
)

class UnsupportedTimnorBackupVersionException(val backupVersion: Int) : IllegalArgumentException()

private val backupJson = Json { ignoreUnknownKeys = false; isLenient = false }

/** Encodes the existing Android version-1 interchange envelope for any platform. */
fun encodeTimnorBackupV1(
    projects: List<StoredProject>,
    works: List<StoredWork>,
    createdAt: String = Clock.System.now().toString(),
): String {
    val backup = TimnorBackupV1(
        format = TIMNOR_BACKUP_FORMAT,
        version = TIMNOR_BACKUP_VERSION,
        createdAt = createdAt,
        projects = projects.map { TimnorBackupProject(it.id, it.description, it.name, it.hourlyRateOre, it.weekendHourlyRateOre) },
        works = works.map {
            TimnorBackupWork(
                id = it.id,
                description = it.description,
                date = no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds(it.dateEpochSeconds).toString(),
                project = it.project,
                task = it.task,
                time = it.time,
                appliedHourlyRateOre = it.appliedHourlyRateOre,
            )
        },
    )
    validateTimnorBackupV1(backup)
    return backupJson.encodeToString(backup).also { encoded ->
        require(encoded.encodeToByteArray().size <= TIMNOR_BACKUP_MAX_BYTES)
    }
}

/** Strictly decodes and validates the existing version-1 backup contract. */
fun decodeTimnorBackupV1(contents: String): TimnorBackupV1 {
    require(contents.encodeToByteArray().size <= TIMNOR_BACKUP_MAX_BYTES)
    val root = backupJson.parseToJsonElement(contents).jsonObject
    val version = root["version"]?.jsonPrimitive?.intOrNull
        ?: throw IllegalArgumentException("Missing backup version")
    if (version > TIMNOR_BACKUP_VERSION) throw UnsupportedTimnorBackupVersionException(version)
    require(version == TIMNOR_BACKUP_VERSION)
    return backupJson.decodeFromString<TimnorBackupV1>(root.toString()).also(::validateTimnorBackupV1)
}

fun validateTimnorBackupV1(value: TimnorBackupV1) {
    require(value.format == TIMNOR_BACKUP_FORMAT && value.version == TIMNOR_BACKUP_VERSION)
    require(value.projects.size <= MAX_RECORDS_PER_TABLE && value.works.size <= MAX_RECORDS_PER_TABLE)
    require(value.projects.map(TimnorBackupProject::id).toSet().size == value.projects.size)
    require(value.projects.all { it.hourlyRateOre == null || it.hourlyRateOre in 0..MAX_HOURLY_RATE_ORE })
    require(value.projects.all { it.weekendHourlyRateOre == null || it.weekendHourlyRateOre in 0..MAX_HOURLY_RATE_ORE })
    require(value.works.map(TimnorBackupWork::id).toSet().size == value.works.size)
    value.works.forEach {
        LocalDateTime.parse(it.date)
        require(it.appliedHourlyRateOre == null || it.appliedHourlyRateOre in 0..MAX_HOURLY_RATE_ORE)
    }
    Instant.parse(value.createdAt)
}

fun TimnorBackupV1.toStoredProjects(): List<StoredProject> =
    projects.map { StoredProject(it.description, it.name, it.id, it.hourlyRateOre, it.weekendHourlyRateOre) }

fun TimnorBackupV1.toStoredWorks(): List<StoredWork> = works.map { backupWork ->
    val date = LocalDateTime.parse(backupWork.date)
    StoredWork(
        description = backupWork.description,
        dateEpochSeconds = no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime(date),
        project = backupWork.project,
        task = backupWork.task,
        time = backupWork.time,
        id = backupWork.id,
        appliedHourlyRateOre = backupWork.appliedHourlyRateOre,
    )
}
