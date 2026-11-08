package org.examples.time_manager.core.repository

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import org.examples.time_manager.core.database.Database
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.work.Work
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    val createdAt: String,
    val projects: List<BackupProject>,
    val works: List<BackupWork>,
)

@Serializable
data class BackupProject(val id: Int, val description: String, val name: String)

@Serializable
data class BackupWork(
    val id: Int,
    val description: String,
    val date: String,
    val project: Int,
    val task: Int,
    val time: Int,
)

data class RestorePreview(
    val token: String,
    val projectCount: Int,
    val workCount: Int,
    val createdAt: String = "",
)

sealed interface BackupResult {
    data object Success : BackupResult
    data object Invalid : BackupResult
    data object UnsupportedVersion : BackupResult
    data object Unreadable : BackupResult
    data object Failure : BackupResult
    data object RestoreFailure : BackupResult
    data object TimerActive : BackupResult
}

interface BackupRepository {
    suspend fun export(uri: Uri): BackupResult
    suspend fun stage(uri: Uri): Pair<RestorePreview, BackupResult?>
    suspend fun restore(token: String): BackupResult
    fun discard(token: String)
}

/** Android document I/O and strict format handling live outside presentation. */
class RoomBackupRepository(
    private val database: Database,
    private val resolver: ContentResolver,
    private val coordinator: DataMutationCoordinator,
) : BackupRepository {
    private val json = Json { ignoreUnknownKeys = false; isLenient = false }
    private val staged = mutableMapOf<String, BackupFile>()

    override suspend fun export(uri: Uri): BackupResult = try {
        val contents = coordinator.snapshot {
            database.withTransaction {
                BackupFile(
                    format = FORMAT,
                    version = VERSION,
                    createdAt = java.time.Instant.now().toString(),
                    projects = database.projectDao().getAll().map { BackupProject(it.id, it.description, it.name) },
                    works = database.workDao().getAll().map {
                        BackupWork(it.id, it.description, it.date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), it.project, it.task, it.time)
                    },
                )
            }
        }
        val output = resolver.openOutputStream(uri) ?: return BackupResult.Failure
        output.use { it.write(json.encodeToString(BackupFile.serializer(), contents).toByteArray(Charsets.UTF_8)) }
        BackupResult.Success
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { BackupResult.Failure }

    override suspend fun stage(uri: Uri): Pair<RestorePreview, BackupResult?> = try {
        val input = resolver.openInputStream(uri) ?: return RestorePreview("", 0, 0) to BackupResult.Unreadable
        val bytes = input.use { stream ->
            val buffer = ByteArray(MAX_BYTES + 1)
            var total = 0
            while (total < buffer.size) {
                val read = stream.read(buffer, total, buffer.size - total)
                if (read < 0) break
                total += read
            }
            if (total > MAX_BYTES) return RestorePreview("", 0, 0) to BackupResult.Invalid
            buffer.copyOf(total)
        }
        val decodedText = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            return RestorePreview("", 0, 0) to BackupResult.Invalid
        }
        val root = json.parseToJsonElement(decodedText)
        val version = root as? kotlinx.serialization.json.JsonObject
        val versionNumber = version?.get("version")?.toString()?.toIntOrNull()
            ?: return RestorePreview("", 0, 0) to BackupResult.Invalid
        if (versionNumber > VERSION) return RestorePreview("", 0, 0) to BackupResult.UnsupportedVersion
        if (versionNumber != VERSION) return RestorePreview("", 0, 0) to BackupResult.Invalid
        val parsed = json.decodeFromString(BackupFile.serializer(), root.toString())
        try {
            validateBackupV1(parsed)
        } catch (_: Exception) {
            return RestorePreview("", 0, 0) to BackupResult.Invalid
        }
        val token = java.util.UUID.randomUUID().toString()
        synchronized(staged) { staged[token] = parsed }
            RestorePreview(token, parsed.projects.size, parsed.works.size, parsed.createdAt) to null
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SerializationException) {
        RestorePreview("", 0, 0) to BackupResult.Invalid
    } catch (_: Exception) {
        RestorePreview("", 0, 0) to BackupResult.Unreadable
    }

    override suspend fun restore(token: String): BackupResult = try {
        val backup = synchronized(staged) { staged[token] } ?: return BackupResult.Invalid
        val restored = coordinator.restoreIfTimerClear {
            database.withTransaction {
                database.workDao().deleteAll()
                database.projectDao().deleteAll()
                database.projectDao().upsertAll(backup.projects.map { Project(it.description, it.name, it.id) })
                database.workDao().upsertAll(backup.works.map {
                    Work(it.description, LocalDateTime.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE_TIME), it.project, it.task, it.time, it.id)
                })
            }
        }
        if (restored == null) return BackupResult.TimerActive
        discard(token)
        BackupResult.Success
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) { discard(token); BackupResult.RestoreFailure }

    override fun discard(token: String) { synchronized(staged) { staged.remove(token) } }

    companion object {
        const val FORMAT = "org.examples.time_manager.backup"
        const val VERSION = 1
        private const val MAX_BYTES = 25 * 1024 * 1024
    }
}

internal fun validateBackupV1(value: BackupFile) {
    require(value.format == RoomBackupRepository.FORMAT && value.version == RoomBackupRepository.VERSION)
    require(value.projects.size <= 100_000 && value.works.size <= 100_000)
    require(value.projects.map { it.id }.toSet().size == value.projects.size)
    require(value.works.map { it.id }.toSet().size == value.works.size)
    value.works.forEach { LocalDateTime.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
    java.time.Instant.parse(value.createdAt)
}
