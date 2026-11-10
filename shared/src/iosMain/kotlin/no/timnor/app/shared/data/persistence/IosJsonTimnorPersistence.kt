package no.timnor.app.shared.data.persistence

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import no.timnor.app.shared.data.backup.decodeTimnorBackupV1
import no.timnor.app.shared.data.backup.encodeTimnorBackupV1
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
class IosJsonTimnorPersistence : TimnorPersistence {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = false }
    private val filePath = dataFilePath()
    private val initialData = readData()
    private val projects = MutableStateFlow(initialData.projects)
    private val works = MutableStateFlow(initialData.works)

    override fun observeProjects(): Flow<List<StoredProject>> = projects

    override suspend fun snapshotAll(): TimnorDataSnapshot = mutex.withLock {
        TimnorDataSnapshot(projects.value.toList(), works.value.toList())
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

    override suspend fun deleteProject(project: StoredProject) = mutex.withLock {
        persist(projects.value.filterNot { it.id == project.id }, works.value)
    }

    override suspend fun upsertWork(work: StoredWork) = mutex.withLock {
        val id = work.id.takeIf { it != 0 } ?: ((works.value.maxOfOrNull(StoredWork::id) ?: 0) + 1)
        val updated = works.value.filterNot { it.id == id } + work.copy(id = id)
        persist(projects.value, updated)
    }

    override suspend fun upsertWorks(works: List<StoredWork>) = mutex.withLock {
        var nextId = this.works.value.maxOfOrNull(StoredWork::id) ?: 0
        val updated = works.fold(this.works.value) { current, work ->
            val id = work.id.takeIf { it != 0 } ?: ++nextId
            current.filterNot { it.id == id } + work.copy(id = id)
        }
        persist(projects.value, updated)
    }

    override suspend fun deleteWork(work: StoredWork) = mutex.withLock {
        persist(projects.value, works.value.filterNot { it.id == work.id })
    }

    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) = mutex.withLock {
        persist(projects, works)
    }

    /** Creates the Android-compatible version-1 backup payload for platform file/share UI. */
    suspend fun exportBackupV1(): String = mutex.withLock {
        encodeTimnorBackupV1(projects.value, works.value)
    }

    fun snapshotWorks(): List<StoredWork> = works.value.toList()

    /** Validates the shared interchange format before atomically replacing this local store. */
    suspend fun restoreBackupV1(contents: String) {
        val backup = decodeTimnorBackupV1(contents)
        replaceAll(backup.toStoredProjects(), backup.toStoredWorks())
    }

    private fun persist(projectValues: List<StoredProject>, workValues: List<StoredWork>) {
        val data = TimnorStoreFile(projectValues.sortedBy(StoredProject::id), workValues.sortedBy(StoredWork::id))
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
)

object IosTimnorPlatform {
    private val store by lazy { IosJsonTimnorPersistence() }

    fun createPersistence(): TimnorPersistence = store

    suspend fun exportBackupV1(): String = store.exportBackupV1()

    suspend fun previewBackupV1(contents: String): String {
        val backup = decodeTimnorBackupV1(contents)
        return "${backup.projects.size}|${backup.works.size}"
    }

    suspend fun restoreBackupV1(contents: String) = store.restoreBackupV1(contents)

    @OptIn(ExperimentalEncodingApi::class)
    fun exportCurrentMonthXlsxBase64(): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return Base64.encode(createTimnorXlsx(store.snapshotWorks(), today.year, today.month.ordinal + 1))
    }
}

fun createTimnorPersistence(): TimnorPersistence = IosTimnorPlatform.createPersistence()
