package no.timnor.app.core.repository

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.CancellationException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import no.timnor.app.core.database.Database
import kotlinx.datetime.toKotlinLocalDateTime
import no.timnor.app.shared.data.backup.TIMNOR_BACKUP_FORMAT
import no.timnor.app.shared.data.backup.TIMNOR_BACKUP_MAX_BYTES
import no.timnor.app.shared.data.backup.TIMNOR_BACKUP_VERSION
import no.timnor.app.shared.data.backup.TimnorBackupProject
import no.timnor.app.shared.data.backup.TimnorBackupV1
import no.timnor.app.shared.data.backup.TimnorBackupWork
import no.timnor.app.shared.data.backup.UnsupportedTimnorBackupVersionException
import no.timnor.app.shared.data.backup.decodeTimnorBackupV1
import no.timnor.app.shared.data.backup.encodeTimnorBackupV1
import no.timnor.app.shared.data.backup.toStoredProjects
import no.timnor.app.shared.data.backup.toStoredWorks
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime

typealias BackupFile = TimnorBackupV1
typealias BackupProject = TimnorBackupProject
typealias BackupWork = TimnorBackupWork

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
    private val persistence: TimnorPersistence,
    private val resolver: ContentResolver,
    private val coordinator: DataMutationCoordinator,
) : BackupRepository {
    private val staged = mutableMapOf<String, BackupFile>()

    override suspend fun export(uri: Uri): BackupResult = try {
        val contents = coordinator.snapshot {
            database.withTransaction {
                encodeTimnorBackupV1(
                    projects = database.projectDao().getAll().map {
                        StoredProject(it.description, it.name, it.id, it.hourlyRateOre, it.weekendHourlyRateOre)
                    },
                    works = database.workDao().getAll().map {
                        StoredWork(
                            description = it.description,
                            dateEpochSeconds = utcEpochSecondsFromLocalDateTime(it.date.toKotlinLocalDateTime()),
                            project = it.project,
                            task = it.task,
                            time = it.time,
                            id = it.id,
                            appliedHourlyRateOre = it.appliedHourlyRateOre,
                            enteredEndMinute = it.enteredEndMinute,
                        )
                    },
                    createdAt = java.time.Instant.now().toString(),
                )
            }
        }
        val output = resolver.openOutputStream(uri) ?: return BackupResult.Failure
        output.use { it.write(contents.toByteArray(Charsets.UTF_8)) }
        BackupResult.Success
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { BackupResult.Failure }

    override suspend fun stage(uri: Uri): Pair<RestorePreview, BackupResult?> = try {
        val input = resolver.openInputStream(uri) ?: return RestorePreview("", 0, 0) to BackupResult.Unreadable
        val bytes = input.use { stream ->
            val buffer = ByteArray(TIMNOR_BACKUP_MAX_BYTES + 1)
            var total = 0
            while (total < buffer.size) {
                val read = stream.read(buffer, total, buffer.size - total)
                if (read < 0) break
                total += read
            }
            if (total > TIMNOR_BACKUP_MAX_BYTES) return RestorePreview("", 0, 0) to BackupResult.Invalid
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
        val parsed = try {
            decodeTimnorBackupV1(decodedText)
        } catch (_: UnsupportedTimnorBackupVersionException) {
            return RestorePreview("", 0, 0) to BackupResult.UnsupportedVersion
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
            persistence.replaceAll(
                projects = backup.toStoredProjects(),
                works = backup.toStoredWorks(),
            )
        }
        if (restored == null) return BackupResult.TimerActive
        discard(token)
        BackupResult.Success
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) { discard(token); BackupResult.RestoreFailure }

    override fun discard(token: String) { synchronized(staged) { staged.remove(token) } }

    companion object {
        const val FORMAT = TIMNOR_BACKUP_FORMAT
        const val VERSION = TIMNOR_BACKUP_VERSION
    }
}

internal fun validateBackupV1(value: BackupFile) =
    no.timnor.app.shared.data.backup.validateTimnorBackupV1(value)
