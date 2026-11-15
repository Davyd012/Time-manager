package no.timnor.app.core.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.encodeToString
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.features.shared_hours.domain.PlannorTaskOption
import no.timnor.app.shared.features.shared_hours.domain.PersonalTimeEntry
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOperation
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.features.shared_hours.domain.OwnerAllocationLink
import no.timnor.app.shared.features.shared_hours.domain.TimeChangeCursor
import kotlin.time.Clock
import kotlin.uuid.Uuid

private const val PLANNOR_API = "https://api.plannor.no/v1/"

expect fun createPlannorHttpClient(): HttpClient

@Serializable
private data class ApiEnvelope<T>(val data: T)

@Serializable
private data class SessionLoginRequest(val identifier: String, val password: String)

@Serializable
private data class RefreshRequest(val refreshToken: String)

@Serializable
private data class SessionTokens(
    val id: Int,
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
private data class MeDto(val id: Int, val username: String)

@Serializable
data class AuthorizedPlannorProject(
    val id: Int,
    val name: String,
    val headline: String = "",
    val description: String = "",
    val archived: Boolean = false,
    val ownerId: Int = 0,
)

@Serializable
private data class ProjectPageMeta(val page: Int = 1, val hasMore: Boolean = false)

@Serializable
private data class ProjectEnvelope(val data: List<AuthorizedPlannorProject>, val meta: ProjectPageMeta? = null)

@Serializable
data class AuthorizedPlannorTask(val id: Int, val projectId: Int, val headline: String, val status: Int)

@Serializable
private data class ProjectTaskPage(val data: List<AuthorizedPlannorTask>, val meta: ProjectPageMeta? = null)

@Serializable
private data class RemoteTimeEntry(
    val id: String,
    val allocationId: String? = null,
    val ownerId: Int,
    val sourceId: String? = null,
    val timeProjectId: String? = null,
    val workDate: String,
    val durationSeconds: Long,
    val allocatedSeconds: Long,
    val revision: Int,
    val privateNote: String? = null,
)

@Serializable
private data class TimeEntryList(val items: List<RemoteTimeEntry>)

@Serializable
private data class RemoteAllocation(
    val id: String,
    val entryId: String,
    val projectId: Int = 0,
    val taskId: Int? = null,
    val workDate: String = "",
    val durationSeconds: Long = 0,
    val sharedNote: String? = null,
    val revision: Int,
)

@Serializable
private data class RemoteProjectAllocation(
    val id: String,
    val projectId: Int,
    val taskId: Int? = null,
    val workDate: String,
    val durationSeconds: Long,
    val sharedNote: String? = null,
    val revision: Int,
)

@Serializable
private data class RemoteAllocationPage(
    val items: List<RemoteProjectAllocation>,
    val page: Int = 1,
    val pageSize: Int = 100,
    val hasMore: Boolean = false,
)

@Serializable
private data class RemoteOwnerAllocation(
    val id: String,
    val entryId: String,
    val projectId: Int,
    val taskId: Int? = null,
    val workDate: String,
    val durationSeconds: Long,
    val sharedNote: String? = null,
    val revision: Int,
)

@Serializable
private data class RemoteOwnerAllocationPage(
    val items: List<RemoteOwnerAllocation>,
    val page: Int = 1,
    val pageSize: Int = 100,
    val hasMore: Boolean = false,
)

@Serializable
private data class RemoteTimeChange(
    val cursor: Long,
    val kind: String,
    val entryId: String? = null,
    val allocationId: String? = null,
    val revision: Int,
)

@Serializable
private data class RemoteTimeChangePage(val items: List<RemoteTimeChange>, val nextCursor: Long)

@Serializable
private data class RemoteProjectTimeChange(
    val cursor: Long,
    val kind: String,
    val allocationId: String? = null,
    val revision: Int,
)

@Serializable
private data class RemoteProjectTimeChangePage(val items: List<RemoteProjectTimeChange>, val nextCursor: Long)

@Serializable
private data class AtomicCreateRequest(
    val idempotencyKey: String,
    val entry: EntryDraft,
    val allocation: AllocationDraft,
)

@Serializable
private data class EntryDraft(
    val sourceId: String?,
    val timeProjectId: String? = null,
    val workDate: String,
    val durationSeconds: Long,
    val privateNote: String?,
)

@Serializable
private data class AllocationDraft(
    val projectId: Int,
    val taskId: Int?,
    val durationSeconds: Long,
    val sharedNote: String?,
)

@Serializable
private data class ExistingAllocationRequest(
    val idempotencyKey: String,
    val expectedRevision: Int,
    val projectId: Int,
    val taskId: Int?,
    val durationSeconds: Long,
    val sharedNote: String?,
)

@Serializable
private data class TimeEntryCreateRequest(
    val idempotencyKey: String,
    val sourceId: String?,
    val timeProjectId: String? = null,
    val workDate: String,
    val durationSeconds: Long,
    val privateNote: String? = null,
)

@Serializable
private data class TimeDeleteRequest(val idempotencyKey: String, val expectedRevision: Int)

enum class SharedWriteStatus { SYNCED, PENDING, UNKNOWN, CONFLICT, REJECTED, ACCESS_REVOKED }

data class SharedWriteResult(
    val commandId: String,
    val status: SharedWriteStatus,
    val entryId: String? = null,
    val errorCode: String? = null,
)

data class HistoricalTimeEntry(
    val sourceId: String,
    val workDate: String,
    val durationSeconds: Long,
)

sealed interface PlannorConnectionResult {
    data class Connected(val session: PlannorSession) : PlannorConnectionResult
    data class Failed(val reason: PlannorConnectionFailure) : PlannorConnectionResult
}

enum class PlannorConnectionFailure { NETWORK, CREDENTIALS_OR_ACCOUNT, RATE_LIMITED, SESSION_EXPIRED, SERVER, STORAGE }

/** Uses only the first-party session and project-discovery routes registered by Plannor. */
class PlannorApi(
    private val sessions: PlannorSessionStorage,
    private val localStore: SharedHoursRecordStorage,
    private val client: HttpClient = createPlannorHttpClient(),
) {
    private val refreshMutex = Mutex()
    private val retryMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

    suspend fun signIn(identifier: String, password: String): PlannorConnectionResult = try {
        require(identifier.isNotBlank() && password.isNotEmpty())
        val response = client.post(PLANNOR_API + "auth/session") {
            contentType(ContentType.Application.Json)
            setBody(SessionLoginRequest(identifier.trim(), password))
        }
        if (response.status != HttpStatusCode.OK) return PlannorConnectionResult.Failed(response.status.toFailure())
        val tokens = response.body<ApiEnvelope<SessionTokens>>().data
        val provisional = PlannorSession(tokens.accessToken, tokens.refreshToken, tokens.id, identifier.trim())
        val account = me(provisional)
        if (account.id != tokens.id || account.username.isBlank()) {
            return PlannorConnectionResult.Failed(PlannorConnectionFailure.SERVER)
        }
        val session = provisional.copy(username = account.username)
        if (!sessions.saveSession(session)) return PlannorConnectionResult.Failed(PlannorConnectionFailure.STORAGE)
        recordSessionStateChange(session.accountId)
        PlannorConnectionResult.Connected(session)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        PlannorConnectionResult.Failed(PlannorConnectionFailure.NETWORK)
    }

    suspend fun currentSession(): PlannorSession? {
        val current = sessions.read() ?: return null
        return try {
            val account = authorized(current) { token -> me(current.copy(accessToken = token)) }
            if (account.id != current.accountId) {
                clearSessionIfCurrent(current)
                null
            } else current.copy(username = account.username).also { sessions.saveSession(it) }
        } catch (_: UnauthorizedException) {
            null
        } catch (_: Throwable) {
            current
        }
    }

    suspend fun refresh(staleSession: PlannorSession? = null): Boolean = refreshMutex.withLock {
        val current = sessions.read() ?: return false
        if (staleSession != null && current != staleSession) return current.accountId == staleSession.accountId
        try {
            val response = client.patch(PLANNOR_API + "auth/session") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(current.refreshToken))
            }
            if (response.status != HttpStatusCode.OK) {
                if (response.status == HttpStatusCode.Unauthorized) clearSessionIfCurrent(current)
                return false
            }
            val next = response.body<ApiEnvelope<SessionTokens>>().data
            if (sessions.read() != current) return false
            if (next.id != current.accountId) {
                clearSessionIfCurrent(current)
                return false
            }
            sessions.saveSession(current.copy(accessToken = next.accessToken, refreshToken = next.refreshToken))
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun signOut(): Boolean {
        val current = sessions.read() ?: return true
        return try {
            var session = current
            var response = deleteSession(session)
            if (response.status == HttpStatusCode.Unauthorized && refresh(current)) {
                session = sessions.read() ?: return false
                if (session.accountId != current.accountId) return false
                response = deleteSession(session)
            }
            if (response.status != HttpStatusCode.NoContent && response.status != HttpStatusCode.OK) return false
            clearSessionIfCurrent(session)
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun loadAuthorizedProjects(): List<AuthorizedPlannorProject> {
        val current = sessions.read() ?: throw IllegalStateException("not connected")
        return authorized(current) { token ->
            val all = mutableListOf<AuthorizedPlannorProject>()
            var page = 1
            while (true) {
                val response = client.get(PLANNOR_API + "projects") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("page", page)
                    parameter("pageSize", 100)
                }
                ensureSuccess(response.status)
                val envelope = response.body<ProjectEnvelope>()
                all += envelope.data
                val meta = envelope.meta
                if (meta?.hasMore != true) break
                check(meta.page == page && envelope.data.isNotEmpty() && page < Int.MAX_VALUE) {
                    "project pagination did not advance"
                }
                page++
            }
            all.distinctBy(AuthorizedPlannorProject::id).also { projects ->
                if (sessions.read() != current) throw IllegalStateException("account changed")
                val old = localStore.getAll(current.accountId, TYPE_PROJECT, PlannorProjectSelection.serializer())
                    .associateBy(PlannorProjectSelection::projectId)
                val refreshedAt = Clock.System.now().toString()
                val refreshed = projects.map { project ->
                    project.id.toString() to PlannorProjectSelection(
                        accountId = current.accountId,
                        projectId = project.id,
                        name = project.name,
                        organizationName = null,
                        contextLabel = project.headline.ifBlank { null },
                        archived = project.archived,
                        selected = old[project.id]?.selected == true,
                        refreshedAtUtc = refreshedAt,
                        linkedLocalProjectId = old[project.id]?.linkedLocalProjectId,
                        available = true,
                    )
                }
                val authorizedIds = projects.mapTo(mutableSetOf(), AuthorizedPlannorProject::id)
                val unavailable = old.values.filterNot { it.projectId in authorizedIds }.map { previous ->
                    previous.copy(
                        name = "Unavailable project",
                        organizationName = null,
                        contextLabel = null,
                        archived = true,
                        refreshedAtUtc = refreshedAt,
                        available = false,
                    ).let { previous.projectId.toString() to it }
                }
                localStore.replaceAll(
                    current.accountId,
                    TYPE_PROJECT,
                    refreshed + unavailable,
                    PlannorProjectSelection.serializer(),
                    refreshedAt,
                )
            }
        }
    }

    suspend fun loadProjectTasks(projectId: Int): List<AuthorizedPlannorTask> {
        val current = sessions.read() ?: return emptyList()
        if (!isSelectedProject(current.accountId, projectId)) return emptyList()
        return authorized(current) { token ->
            val all = mutableListOf<AuthorizedPlannorTask>()
            var page = 1
            while (true) {
                val response = client.get(PLANNOR_API + "projects/$projectId/tasks") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("status", "open")
                    parameter("page", page)
                    parameter("pageSize", 100)
                }
                ensureSuccess(response.status)
                val result = response.body<ProjectTaskPage>()
                all += result.data
                val meta = result.meta
                if (meta?.hasMore != true) break
                check(meta.page == page && result.data.isNotEmpty() && page < Int.MAX_VALUE) {
                    "task pagination did not advance"
                }
                page++
            }
            if (sessions.read()?.accountId != current.accountId) throw IllegalStateException("account changed")
            all.filter { it.projectId == projectId }.distinctBy(AuthorizedPlannorTask::id)
        }
    }

    suspend fun loadTasksForLinkedLocalProject(localProjectId: Int): List<PlannorTaskOption> {
        if (localProjectId <= 0) return emptyList()
        val accountId = sessions.read()?.accountId ?: return emptyList()
        val project = selectedAndCachedProjects().singleOrNull {
            it.accountId == accountId && it.selected && !it.archived && it.linkedLocalProjectId == localProjectId
        } ?: return emptyList()
        return loadProjectTasks(project.projectId).map { task ->
            PlannorTaskOption(task.projectId, task.id, task.headline)
        }
    }

    /** Applies durable owner/project change cursors, then replaces visible allocation snapshots. */
    suspend fun refreshProjectSnapshots(): List<ProjectTimeAllocation> {
        val current = sessions.read() ?: return emptyList()
        val ownerLinks = localStore.getAll(current.accountId, TYPE_OWNER_LINK, OwnerAllocationLink.serializer())
            .associateBy(OwnerAllocationLink::allocationId)
            .toMutableMap()
        var ownerCursor = localStore.get(current.accountId, TYPE_CURSOR, OWNER_CURSOR_KEY, TimeChangeCursor.serializer())?.cursor ?: 0L
        authorized(current) { token ->
            var nextCursor = ownerCursor
            do {
                val response = client.get(PLANNOR_API + "me/time-changes") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("cursor", nextCursor)
                    parameter("limit", 200)
                }
                ensureSuccess(response.status)
                val page = response.body<ApiEnvelope<RemoteTimeChangePage>>().data
                page.items.forEach { change ->
                    val allocationId = change.allocationId
                    val entryId = change.entryId
                    if (allocationId != null && entryId != null) {
                        ownerLinks[allocationId] = OwnerAllocationLink(current.accountId, allocationId, entryId)
                    }
                }
                ownerCursor = maxOf(ownerCursor, page.nextCursor)
                if (page.items.isEmpty() || page.nextCursor <= nextCursor) break
                nextCursor = page.nextCursor
            } while (true)
        }
        if (sessions.read()?.accountId != current.accountId) throw IllegalStateException("account changed")
        val cachedProjects = selectedAndCachedProjects().filter { it.available && !it.archived }
        val entriesById = loadPersonalEntries().associateBy(PersonalTimeEntry::serverId)
        val snapshots = mutableListOf<ProjectTimeAllocation>()
        val cursorsToCommit = mutableMapOf<String, TimeChangeCursor>()
        cursorsToCommit[OWNER_CURSOR_KEY] = TimeChangeCursor(ownerCursor)
        val ownerSnapshot = try {
            authorized(current) { token ->
                val rows = mutableListOf<ProjectTimeAllocation>()
                var pageNumber = 1
                while (true) {
                    val response = client.get(PLANNOR_API + "me/time-allocations") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        parameter("page", pageNumber)
                        parameter("pageSize", 100)
                    }
                    ensureSuccess(response.status)
                    val result = response.body<ApiEnvelope<RemoteOwnerAllocationPage>>().data
                    check(result.page == pageNumber && result.pageSize in 1..100) {
                        "owner allocation pagination response does not match request"
                    }
                    result.items.forEach { allocation ->
                        rows += ProjectTimeAllocation(
                            accountId = current.accountId,
                            allocationId = allocation.id,
                            entryId = allocation.entryId,
                            projectId = allocation.projectId,
                            taskId = allocation.taskId,
                            workDate = allocation.workDate,
                            durationSeconds = allocation.durationSeconds,
                            sharedNote = allocation.sharedNote,
                            revision = allocation.revision,
                        )
                    }
                    if (!result.hasMore) break
                    check(result.items.isNotEmpty() && pageNumber < Int.MAX_VALUE) {
                        "owner allocation pagination did not advance"
                    }
                    pageNumber++
                }
                rows
            }
        } catch (error: ApiStatusException) {
            if (error.status == 404) null else throw error
        }
        // Older deployments may not have the owner-wide allocation snapshot yet.
        // In that case, reconstruct the owner's allocations from every accessible
        // cached project; restricting this fallback to selected projects would make
        // allocations in another project look like missing/partial remote work.
        val projects = if (ownerSnapshot == null) cachedProjects else cachedProjects.filter { it.selected }
        authorized(current) { token ->
            for (project in projects) {
                try {
                var projectCursor = localStore.get(
                    current.accountId,
                    TYPE_CURSOR,
                    "project:${project.projectId}",
                    TimeChangeCursor.serializer(),
                )?.cursor ?: 0L
                var nextCursor = projectCursor
                do {
                    val response = client.get(PLANNOR_API + "projects/${project.projectId}/time-changes") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        parameter("cursor", nextCursor)
                        parameter("limit", 200)
                    }
                    ensureSuccess(response.status)
                    val page = response.body<ApiEnvelope<RemoteProjectTimeChangePage>>().data
                    projectCursor = maxOf(projectCursor, page.nextCursor)
                    if (page.items.isEmpty() || page.nextCursor <= nextCursor) break
                    nextCursor = page.nextCursor
                } while (true)
                var pageNumber = 1
                while (true) {
                    val response = client.get(PLANNOR_API + "projects/${project.projectId}/time-allocations") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        parameter("page", pageNumber)
                        parameter("pageSize", 100)
                    }
                    ensureSuccess(response.status)
                    val result = response.body<ApiEnvelope<RemoteAllocationPage>>().data
                    check(result.page == pageNumber && result.pageSize in 1..100) {
                        "project allocation pagination response does not match request"
                    }
                    result.items.forEach { allocation ->
                        val entryId = ownerLinks[allocation.id]?.entryId ?: return@forEach
                        val entry = entriesById[entryId] ?: return@forEach
                        snapshots += ProjectTimeAllocation(
                            accountId = current.accountId,
                            allocationId = allocation.id,
                            entryId = entryId,
                            projectId = allocation.projectId,
                            taskId = allocation.taskId,
                            workDate = allocation.workDate,
                            durationSeconds = allocation.durationSeconds,
                            sharedNote = allocation.sharedNote,
                            revision = allocation.revision,
                        )
                    }
                    if (!result.hasMore) break
                    check(result.items.isNotEmpty() && pageNumber < Int.MAX_VALUE) {
                        "project allocation pagination did not advance"
                    }
                    pageNumber++
                }
                cursorsToCommit["project:${project.projectId}"] = TimeChangeCursor(projectCursor)
                } catch (error: ApiStatusException) {
                    if (error.status !in setOf(403, 404)) throw error
                    // Access is checked on each feed request. Drop restricted project detail and
                    // retain only the stable project/local link needed to review existing work.
                    val cached = localStore.get(
                        current.accountId, TYPE_PROJECT, project.projectId.toString(),
                        PlannorProjectSelection.serializer(),
                    )
                    if (cached != null) {
                        localStore.put(
                            current.accountId, TYPE_PROJECT, project.projectId.toString(),
                            cached.copy(
                                name = "Unavailable project",
                                organizationName = null,
                                contextLabel = null,
                                archived = true,
                                available = false,
                                refreshedAtUtc = now(),
                            ),
                            PlannorProjectSelection.serializer(), now(),
                        )
                    }
                }
            }
        }
        if (sessions.read()?.accountId != current.accountId) throw IllegalStateException("account changed")
        val completeSnapshot = (ownerSnapshot.orEmpty() + snapshots).distinctBy(ProjectTimeAllocation::allocationId)
        localStore.commitProjectSnapshot(
            current.accountId,
            ownerLinks.values.toList(),
            // Persist only project-authorized detail. The owner snapshot is used
            // transiently for full balance reconciliation, including inaccessible
            // projects, and must not repopulate their restricted local cache.
            snapshots,
            cursorsToCommit,
            now(),
        )
        return completeSnapshot
    }

    suspend fun cachedProjectAllocations(): List<ProjectTimeAllocation> {
        val accountId = sessions.read()?.accountId ?: return emptyList()
        return localStore.getAll(accountId, TYPE_ALLOCATION, ProjectTimeAllocation.serializer())
    }

    suspend fun setProjectSelected(projectId: Int, selected: Boolean): Boolean {
        val current = sessions.read() ?: return false
        val row = localStore.get(current.accountId, TYPE_PROJECT, projectId.toString(), PlannorProjectSelection.serializer())
            ?: return false
        if (selected && (!row.available || row.archived)) return false
        localStore.put(
            current.accountId,
            TYPE_PROJECT,
            projectId.toString(),
            row.copy(selected = selected),
            PlannorProjectSelection.serializer(),
            row.refreshedAtUtc,
        )
        return true
    }

    suspend fun setProjectLink(projectId: Int, localProjectId: Int?): Boolean {
        val accountId = sessions.read()?.accountId ?: return false
        if (localProjectId != null && localProjectId <= 0) return false
        val rows = localStore.getAll(accountId, TYPE_PROJECT, PlannorProjectSelection.serializer())
        val target = rows.firstOrNull { it.projectId == projectId } ?: return false
        if (localProjectId != null && (!target.available || target.archived)) return false
        if (localProjectId != null) {
            rows.filter { it.projectId != projectId && it.linkedLocalProjectId == localProjectId }
                .forEach { previous ->
                    localStore.put(
                        accountId,
                        TYPE_PROJECT,
                        previous.projectId.toString(),
                        previous.copy(linkedLocalProjectId = null),
                        PlannorProjectSelection.serializer(),
                        previous.refreshedAtUtc,
                    )
                }
        }
        localStore.put(
            accountId,
            TYPE_PROJECT,
            projectId.toString(),
            target.copy(linkedLocalProjectId = localProjectId),
            PlannorProjectSelection.serializer(),
            target.refreshedAtUtc,
        )
        return true
    }

    suspend fun linkedLocalProjects(): Map<Int, String> = selectedAndCachedProjects()
        .mapNotNull { project ->
            project.linkedLocalProjectId?.let { it to if (project.available) project.name else "" }
        }
        .toMap()

    suspend fun selectedAndCachedProjects(): List<PlannorProjectSelection> {
        val accountId = sessions.read()?.accountId ?: return emptyList()
        return localStore.getAll(accountId, TYPE_PROJECT, PlannorProjectSelection.serializer())
    }

    /** Loads the signed-in owner's entries and replaces only that account's cached snapshot. */
    suspend fun loadPersonalEntries(): List<PersonalTimeEntry> {
        val current = sessions.read() ?: throw IllegalStateException("not connected")
        return authorized(current) { token ->
            val response = client.get(PLANNOR_API + "me/time-entries") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            requireSuccessOrThrow(response)
            val entries = response.body<ApiEnvelope<TimeEntryList>>().data.items
                .filter { it.ownerId == current.accountId }
                .map { it.toDomain() }
            if (sessions.read()?.accountId != current.accountId) throw IllegalStateException("account changed")
            localStore.clear(current.accountId, TYPE_ENTRY)
            localStore.putAll(
                current.accountId,
                TYPE_ENTRY,
                entries.map { it.serverId to it },
                PersonalTimeEntry.serializer(),
                now(),
            )
            entries
        }
    }

    /** Last successfully loaded owner entries, available while the service is offline. */
    suspend fun cachedPersonalEntries(): List<PersonalTimeEntry> {
        val accountId = sessions.read()?.accountId ?: return emptyList()
        return localStore.getAll(accountId, TYPE_ENTRY, PersonalTimeEntry.serializer())
    }

    /** Entries with an allocation request whose result still needs reconciliation. */
    suspend fun pendingAllocationEntryIds(): Set<String> {
        val accountId = sessions.read()?.accountId ?: return emptySet()
        return localStore.getAll(accountId, TYPE_OUTBOX, SharedTimeOutboxItem.serializer())
            .filter { it.operation == SharedTimeOperation.ALLOCATE_EXISTING &&
                it.outcome in setOf(SharedTimeOutcome.PENDING, SharedTimeOutcome.OUTCOME_UNKNOWN) }
            .mapNotNull { it.entryId }.toSet()
    }

    suspend fun pendingAllocationIds(): Set<String> {
        val accountId = sessions.read()?.accountId ?: return emptySet()
        return localStore.getAll(accountId, TYPE_OUTBOX, SharedTimeOutboxItem.serializer())
            .filter { it.operation in setOf(SharedTimeOperation.UPDATE_ALLOCATION, SharedTimeOperation.DELETE_ALLOCATION) &&
                it.outcome in setOf(SharedTimeOutcome.PENDING, SharedTimeOutcome.OUTCOME_UNKNOWN) }
            .mapNotNull { it.allocationId }.toSet()
    }

    suspend fun pendingImportSourceIds(): Set<String> {
        val accountId = sessions.read()?.accountId ?: return emptySet()
        return localStore.getAll(accountId, TYPE_OUTBOX, SharedTimeOutboxItem.serializer())
            .filter { it.operation == SharedTimeOperation.IMPORT_ENTRY &&
                it.outcome in setOf(SharedTimeOutcome.PENDING, SharedTimeOutcome.OUTCOME_UNKNOWN) }
            .mapNotNull { it.sourceId }.toSet()
    }

    suspend fun hasPendingDirectCreate(): Boolean {
        val accountId = sessions.read()?.accountId ?: return false
        return localStore.getAll(accountId, TYPE_OUTBOX, SharedTimeOutboxItem.serializer()).any {
            it.operation == SharedTimeOperation.CREATE_AND_ALLOCATE && it.sourceId == null &&
                it.outcome in setOf(SharedTimeOutcome.PENDING, SharedTimeOutcome.OUTCOME_UNKNOWN)
        }
    }

    suspend fun clearAccountCache(accountId: Int) {
        localStore.clear(accountId, TYPE_PROJECT)
    }

    /** Persists the complete request and idempotency key before the network write. */
    suspend fun createAndAllocate(
        projectId: Int,
        taskId: Int?,
        workDate: String,
        durationSeconds: Long,
        privateNote: String?,
        sharedNote: String?,
        sourceId: String? = null,
    ): SharedWriteResult {
        val session = sessions.read() ?: return SharedWriteResult("", SharedWriteStatus.ACCESS_REVOKED)
        if (!isSelectedProject(session.accountId, projectId) || durationSeconds <= 0) {
            return SharedWriteResult("", SharedWriteStatus.REJECTED, errorCode = "invalid_or_unselected_project")
        }
        val item = SharedTimeOutboxItem(
            accountId = session.accountId,
            operation = SharedTimeOperation.CREATE_AND_ALLOCATE,
            outcome = SharedTimeOutcome.PENDING,
            projectId = projectId,
            taskId = taskId,
            workDate = workDate,
            durationSeconds = durationSeconds,
            sourceId = sourceId,
            privateNote = privateNote,
            sharedNote = sharedNote,
            atomicIdempotencyKey = newKey(),
        )
        persistOutbox(item)
        return deliver(item)
    }

    /** Allocates time from an existing owner-private entry without creating a second entry. */
    suspend fun allocateExisting(
        entry: PersonalTimeEntry,
        projectId: Int,
        taskId: Int?,
        durationSeconds: Long,
        sharedNote: String?,
    ): SharedWriteResult {
        val session = sessions.read() ?: return SharedWriteResult("", SharedWriteStatus.ACCESS_REVOKED)
        if (entry.accountId != session.accountId || !entry.canAllocate(durationSeconds) ||
            !isSelectedProject(session.accountId, projectId)
        ) return SharedWriteResult("", SharedWriteStatus.REJECTED, errorCode = "invalid_or_unselected_project")
        val item = SharedTimeOutboxItem(
            accountId = session.accountId,
            operation = SharedTimeOperation.ALLOCATE_EXISTING,
            outcome = SharedTimeOutcome.PENDING,
            entryId = entry.serverId,
            entryRevision = entry.revision,
            projectId = projectId,
            taskId = taskId,
            workDate = entry.workDate,
            durationSeconds = durationSeconds,
            sharedNote = sharedNote,
            allocationIdempotencyKey = newKey(),
        )
        persistOutbox(item)
        return deliver(item)
    }

    suspend fun correctEntry(
        entry: PersonalTimeEntry,
        workDate: String,
        durationSeconds: Long,
        privateNote: String?,
    ): SharedWriteResult {
        val session = sessions.read() ?: return SharedWriteResult("", SharedWriteStatus.ACCESS_REVOKED)
        if (entry.accountId != session.accountId || durationSeconds < entry.allocatedSeconds || durationSeconds <= 0) {
            return SharedWriteResult("", SharedWriteStatus.REJECTED, entry.serverId, "invalid_entry_correction")
        }
        val item = SharedTimeOutboxItem(
            accountId = session.accountId,
            operation = SharedTimeOperation.UPDATE_ENTRY,
            outcome = SharedTimeOutcome.PENDING,
            entryId = entry.serverId,
            entryRevision = entry.revision,
            projectId = 0,
            workDate = workDate,
            durationSeconds = durationSeconds,
            privateNote = privateNote,
            entryIdempotencyKey = newKey(),
        )
        persistOutbox(item)
        return deliver(item)
    }

    suspend fun deleteEntry(entry: PersonalTimeEntry): SharedWriteResult {
        val session = sessions.read() ?: return SharedWriteResult("", SharedWriteStatus.ACCESS_REVOKED)
        if (entry.accountId != session.accountId || entry.allocatedSeconds != 0L) {
            return SharedWriteResult("", SharedWriteStatus.REJECTED, entry.serverId, "entry_has_allocations")
        }
        val item = SharedTimeOutboxItem(
            accountId = session.accountId,
            operation = SharedTimeOperation.DELETE_ENTRY,
            outcome = SharedTimeOutcome.PENDING,
            entryId = entry.serverId,
            entryRevision = entry.revision,
            projectId = 0,
            workDate = entry.workDate,
            durationSeconds = entry.durationSeconds,
            entryIdempotencyKey = newKey(),
        )
        persistOutbox(item)
        return deliver(item)
    }

    /** Imports already-reviewed local history; private notes are never included. */
    suspend fun importHistoricalEntries(entries: List<HistoricalTimeEntry>): List<SharedWriteResult> {
        val session = sessions.read() ?: return emptyList()
        val existingSourceIds = loadPersonalEntries().mapNotNull(PersonalTimeEntry::sourceWorkId).toSet() + pendingImportSourceIds()
        val eligibleIds = no.timnor.app.shared.features.shared_hours.domain.eligibleForImport(
            entries.map(HistoricalTimeEntry::sourceId),
            existingSourceIds,
        ).toSet()
        return entries.filter { it.sourceId in eligibleIds }.map { entry ->
            if (sessions.read()?.accountId != session.accountId) {
                SharedWriteResult("", SharedWriteStatus.ACCESS_REVOKED)
            } else {
                val item = SharedTimeOutboxItem(
                    accountId = session.accountId,
                    operation = SharedTimeOperation.IMPORT_ENTRY,
                    outcome = SharedTimeOutcome.PENDING,
                    projectId = 0,
                    workDate = entry.workDate,
                    durationSeconds = entry.durationSeconds,
                    sourceId = entry.sourceId,
                    entryIdempotencyKey = newKey(),
                )
                persistOutbox(item)
                deliver(item)
            }
        }
    }

    suspend fun correctAllocation(
        allocation: ProjectTimeAllocation,
        entryRevision: Int,
        durationSeconds: Long,
        taskId: Int?,
        sharedNote: String?,
    ): SharedWriteResult {
        val session = sessions.read() ?: return SharedWriteResult("", SharedWriteStatus.ACCESS_REVOKED)
        val entry = localStore.get(session.accountId, TYPE_ENTRY, allocation.entryId, PersonalTimeEntry.serializer())
        if (allocation.accountId != session.accountId || entry?.revision != entryRevision ||
            durationSeconds <= 0 || !isSelectedProject(session.accountId, allocation.projectId)
        ) return SharedWriteResult("", SharedWriteStatus.REJECTED, allocation.entryId, "invalid_allocation_correction")
        val item = SharedTimeOutboxItem(
            accountId = session.accountId,
            operation = SharedTimeOperation.UPDATE_ALLOCATION,
            outcome = SharedTimeOutcome.PENDING,
            entryId = allocation.entryId,
            entryRevision = entryRevision,
            allocationId = allocation.allocationId,
            allocationRevision = allocation.revision,
            projectId = allocation.projectId,
            taskId = taskId,
            workDate = allocation.workDate,
            durationSeconds = durationSeconds,
            sharedNote = sharedNote,
            allocationIdempotencyKey = newKey(),
        )
        persistOutbox(item)
        return deliver(item)
    }

    suspend fun deleteAllocation(allocation: ProjectTimeAllocation, entryRevision: Int): SharedWriteResult {
        val session = sessions.read() ?: return SharedWriteResult("", SharedWriteStatus.ACCESS_REVOKED)
        val entry = localStore.get(session.accountId, TYPE_ENTRY, allocation.entryId, PersonalTimeEntry.serializer())
        if (allocation.accountId != session.accountId || entry?.revision != entryRevision ||
            !isSelectedProject(session.accountId, allocation.projectId)
        ) return SharedWriteResult("", SharedWriteStatus.REJECTED, allocation.entryId, "invalid_allocation")
        val item = SharedTimeOutboxItem(
            accountId = session.accountId,
            operation = SharedTimeOperation.DELETE_ALLOCATION,
            outcome = SharedTimeOutcome.PENDING,
            entryId = allocation.entryId,
            entryRevision = entryRevision,
            allocationId = allocation.allocationId,
            allocationRevision = allocation.revision,
            projectId = allocation.projectId,
            workDate = allocation.workDate,
            durationSeconds = allocation.durationSeconds,
            allocationIdempotencyKey = newKey(),
        )
        persistOutbox(item)
        return deliver(item)
    }

    /** Retries only the active account's supported creates, with their original bodies and keys. */
    suspend fun retryPending(forceRetry: Boolean = false): List<SharedWriteResult> = retryMutex.withLock {
        val session = sessions.read() ?: return emptyList()
        val nowEpochSeconds = Clock.System.now().epochSeconds
        val pending = localStore.getAll(session.accountId, TYPE_OUTBOX, SharedTimeOutboxItem.serializer())
            .filter {
                (it.outcome == SharedTimeOutcome.PENDING || it.outcome == SharedTimeOutcome.OUTCOME_UNKNOWN) &&
                    (forceRetry || it.retryAfterEpochSeconds == null || it.retryAfterEpochSeconds <= nowEpochSeconds)
            }
        return pending.map { item ->
            if (sessions.read()?.accountId != item.accountId) {
                SharedWriteResult(item.atomicIdempotencyKey ?: item.allocationIdempotencyKey.orEmpty(), SharedWriteStatus.ACCESS_REVOKED)
            } else deliver(item)
        }
    }

    private suspend fun deliver(item: SharedTimeOutboxItem): SharedWriteResult {
        val session = sessions.read()?.takeIf { it.accountId == item.accountId }
            ?: return updateOutcome(item, SharedTimeOutcome.ACCESS_REVOKED, "account_changed")
        var createdAllocationId = item.allocationId
        return try {
            val entry = authorized(session) { token ->
                when (item.operation) {
                    SharedTimeOperation.CREATE_AND_ALLOCATE -> {
                        val response = client.post(PLANNOR_API + "me/time-entries/allocate") {
                            header(HttpHeaders.Authorization, "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(
                                AtomicCreateRequest(
                                    idempotencyKey = requireNotNull(item.atomicIdempotencyKey),
                                    entry = EntryDraft(
                                        sourceId = item.sourceId,
                                        workDate = item.workDate,
                                        durationSeconds = item.durationSeconds,
                                        privateNote = item.privateNote,
                                    ),
                                    allocation = AllocationDraft(
                                        projectId = item.projectId,
                                        taskId = item.taskId,
                                        durationSeconds = item.durationSeconds,
                                        sharedNote = item.sharedNote,
                                    ),
                                ),
                            )
                        }
                        requireSuccessOrThrow(response)
                        response.body<ApiEnvelope<RemoteTimeEntry>>().data.also {
                            createdAllocationId = it.allocationId ?: createdAllocationId
                        }
                    }
                    SharedTimeOperation.IMPORT_ENTRY -> {
                        val response = client.post(PLANNOR_API + "me/time-entries") {
                            header(HttpHeaders.Authorization, "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(
                                TimeEntryCreateRequest(
                                    idempotencyKey = requireNotNull(item.entryIdempotencyKey),
                                    sourceId = item.sourceId,
                                    workDate = item.workDate,
                                    durationSeconds = item.durationSeconds,
                                    privateNote = null,
                                ),
                            )
                        }
                        requireSuccessOrThrow(response)
                        response.body<ApiEnvelope<RemoteTimeEntry>>().data
                    }
                    SharedTimeOperation.ALLOCATE_EXISTING -> {
                        val response = client.post(PLANNOR_API + "me/time-entries/${item.entryId}/allocations") {
                            header(HttpHeaders.Authorization, "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(
                                ExistingAllocationRequest(
                                    idempotencyKey = requireNotNull(item.allocationIdempotencyKey),
                                    expectedRevision = requireNotNull(item.entryRevision),
                                    projectId = item.projectId,
                                    taskId = item.taskId,
                                    durationSeconds = item.durationSeconds,
                                    sharedNote = item.sharedNote,
                                ),
                            )
                        }
                        requireSuccessOrThrow(response)
                        val allocation = response.body<ApiEnvelope<RemoteAllocation>>().data
                        createdAllocationId = allocation.id
                        localStore.put(
                            item.accountId,
                            TYPE_ALLOCATION,
                            allocation.id,
                            ProjectTimeAllocation(
                                accountId = item.accountId,
                                allocationId = allocation.id,
                                entryId = allocation.entryId,
                                projectId = allocation.projectId,
                                taskId = allocation.taskId,
                                workDate = allocation.workDate.ifBlank { item.workDate },
                                durationSeconds = allocation.durationSeconds,
                                sharedNote = allocation.sharedNote,
                                revision = allocation.revision,
                            ),
                            ProjectTimeAllocation.serializer(),
                            now(),
                        )
                        localStore.put(
                            item.accountId,
                            TYPE_OWNER_LINK,
                            allocation.id,
                            OwnerAllocationLink(item.accountId, allocation.id, allocation.entryId),
                            OwnerAllocationLink.serializer(),
                            now(),
                        )
                        fetchEntry(token, requireNotNull(item.entryId))
                    }
                    SharedTimeOperation.UPDATE_ENTRY -> {
                        val response = client.patch(PLANNOR_API + "me/time-entries/${item.entryId}") {
                            header(HttpHeaders.Authorization, "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(
                                buildJsonObject {
                                    put("idempotencyKey", JsonPrimitive(requireNotNull(item.entryIdempotencyKey)))
                                    put("expectedRevision", JsonPrimitive(requireNotNull(item.entryRevision)))
                                    put("workDate", JsonPrimitive(item.workDate))
                                    put("durationSeconds", JsonPrimitive(item.durationSeconds))
                                    put("privateNote", item.privateNote?.let(::JsonPrimitive) ?: JsonNull)
                                },
                            )
                        }
                        requireSuccessOrThrow(response)
                        response.body<ApiEnvelope<RemoteTimeEntry>>().data
                    }
                    SharedTimeOperation.DELETE_ENTRY -> {
                        val response = client.delete(PLANNOR_API + "me/time-entries/${item.entryId}") {
                            header(HttpHeaders.Authorization, "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(TimeDeleteRequest(requireNotNull(item.entryIdempotencyKey), requireNotNull(item.entryRevision)))
                        }
                        requireSuccessOrThrow(response)
                        null
                    }
                    SharedTimeOperation.UPDATE_ALLOCATION -> {
                        val response = client.patch(
                            PLANNOR_API + "me/time-entries/${item.entryId}/allocations/${item.allocationId}",
                        ) {
                            header(HttpHeaders.Authorization, "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(
                                buildJsonObject {
                                    put("idempotencyKey", JsonPrimitive(requireNotNull(item.allocationIdempotencyKey)))
                                    put("expectedRevision", JsonPrimitive(requireNotNull(item.allocationRevision)))
                                    put("durationSeconds", JsonPrimitive(item.durationSeconds))
                                    put("taskId", item.taskId?.let(::JsonPrimitive) ?: JsonNull)
                                    put("sharedNote", item.sharedNote?.let(::JsonPrimitive) ?: JsonNull)
                                },
                            )
                        }
                        requireSuccessOrThrow(response)
                        val updated = response.body<ApiEnvelope<RemoteAllocation>>().data
                        localStore.put(
                            item.accountId,
                            TYPE_ALLOCATION,
                            updated.id,
                            ProjectTimeAllocation(
                                accountId = item.accountId,
                                allocationId = updated.id,
                                entryId = updated.entryId,
                                projectId = updated.projectId,
                                taskId = updated.taskId,
                                workDate = item.workDate,
                                durationSeconds = updated.durationSeconds,
                                sharedNote = updated.sharedNote,
                                revision = updated.revision,
                            ),
                            ProjectTimeAllocation.serializer(),
                            now(),
                        )
                        fetchEntry(token, requireNotNull(item.entryId))
                    }
                    SharedTimeOperation.DELETE_ALLOCATION -> {
                        val response = client.delete(
                            PLANNOR_API + "me/time-entries/${item.entryId}/allocations/${item.allocationId}",
                        ) {
                            header(HttpHeaders.Authorization, "Bearer $token")
                            contentType(ContentType.Application.Json)
                            setBody(TimeDeleteRequest(requireNotNull(item.allocationIdempotencyKey), requireNotNull(item.allocationRevision)))
                        }
                        requireSuccessOrThrow(response)
                        localStore.delete(item.accountId, TYPE_ALLOCATION, requireNotNull(item.allocationId))
                        fetchEntry(token, requireNotNull(item.entryId))
                    }
                    else -> error("unsupported outbox operation")
                }
            }
            if (entry != null && entry.ownerId != item.accountId) {
                return updateOutcome(item, SharedTimeOutcome.REJECTED, "owner_mismatch")
            }
            val updated = item.copy(
                entryId = entry?.id ?: item.entryId,
                entryRevision = entry?.revision ?: item.entryRevision,
                allocationId = createdAllocationId,
                outcome = SharedTimeOutcome.SUCCEEDED,
                errorCode = null,
            )
            if (entry == null) localStore.delete(item.accountId, TYPE_ENTRY, requireNotNull(item.entryId))
            else localStore.put(item.accountId, TYPE_ENTRY, entry.id, entry.toDomain(), PersonalTimeEntry.serializer(), now())
            persistOutbox(updated)
            SharedWriteResult(commandKey(item), SharedWriteStatus.SYNCED, entry?.id ?: item.entryId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: ApiStatusException) {
            val outcome = when (error.status) {
                403 -> SharedTimeOutcome.ACCESS_REVOKED
                409 -> SharedTimeOutcome.CONFLICT
                408, 425, 429 -> SharedTimeOutcome.OUTCOME_UNKNOWN
                in 400..499 -> SharedTimeOutcome.REJECTED
                else -> SharedTimeOutcome.OUTCOME_UNKNOWN
            }
            updateOutcome(item, outcome, error.code)
        } catch (_: Throwable) {
            updateOutcome(item, SharedTimeOutcome.OUTCOME_UNKNOWN, "outcome_unknown")
        }
    }

    private suspend fun requireSuccessOrThrow(response: HttpResponse) {
        if (response.status == HttpStatusCode.Unauthorized) throw UnauthorizedException()
        if (response.status.isSuccess()) return
        val code = runCatching { response.body<JsonObject>()["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content }.getOrNull()
        throw ApiStatusException(response.status.value, code ?: "request_rejected")
    }

    private suspend fun fetchEntry(token: String, entryId: String): RemoteTimeEntry {
        val response = client.get(PLANNOR_API + "me/time-entries") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        requireSuccessOrThrow(response)
        return response.body<ApiEnvelope<TimeEntryList>>().data.items.firstOrNull { it.id == entryId }
            ?: throw ApiStatusException(404, "entry_missing_after_allocation")
    }

    private suspend fun persistOutbox(item: SharedTimeOutboxItem) {
        val key = commandKey(item)
        localStore.putOutboxUnlessRestorePaused(item.accountId, key, item, now())
    }

    private suspend fun clearSessionIfCurrent(session: PlannorSession): Boolean {
        if (sessions.read() != session) return false
        if (!sessions.clearSession()) return false
        recordSessionStateChange(session.accountId)
        return true
    }

    private suspend fun recordSessionStateChange(accountId: Int) {
        runCatching {
            localStore.put(
                accountId,
                TYPE_SESSION_STATE,
                "changed",
                now(),
                String.serializer(),
                now(),
            )
        }
    }

    private suspend fun updateOutcome(item: SharedTimeOutboxItem, outcome: SharedTimeOutcome, errorCode: String?): SharedWriteResult {
        val retryCount = if (outcome == SharedTimeOutcome.OUTCOME_UNKNOWN) item.retryCount + 1 else item.retryCount
        val retryAfter = if (outcome == SharedTimeOutcome.OUTCOME_UNKNOWN) {
            val exponent = (retryCount - 1).coerceIn(0, 9)
            val delaySeconds = minOf(MAX_RETRY_DELAY_SECONDS, BASE_RETRY_DELAY_SECONDS * (1L shl exponent))
            Clock.System.now().epochSeconds + delaySeconds
        } else null
        val updated = item.copy(
            outcome = outcome,
            errorCode = errorCode,
            retryCount = retryCount,
            retryAfterEpochSeconds = retryAfter,
        )
        persistOutbox(updated)
        val status = when (outcome) {
            SharedTimeOutcome.PENDING -> SharedWriteStatus.PENDING
            SharedTimeOutcome.OUTCOME_UNKNOWN -> SharedWriteStatus.UNKNOWN
            SharedTimeOutcome.SUCCEEDED -> SharedWriteStatus.SYNCED
            SharedTimeOutcome.CONFLICT -> SharedWriteStatus.CONFLICT
            SharedTimeOutcome.ACCESS_REVOKED -> SharedWriteStatus.ACCESS_REVOKED
            SharedTimeOutcome.REJECTED -> SharedWriteStatus.REJECTED
        }
        return SharedWriteResult(commandKey(item), status, item.entryId, errorCode)
    }

    private suspend fun isSelectedProject(accountId: Int, projectId: Int): Boolean =
        localStore.get(accountId, TYPE_PROJECT, projectId.toString(), PlannorProjectSelection.serializer())
            ?.let { it.selected && it.available && !it.archived } == true

    private fun RemoteTimeEntry.toDomain() = PersonalTimeEntry(
        accountId = ownerId,
        serverId = id,
        workDate = workDate,
        durationSeconds = durationSeconds,
        allocatedSeconds = allocatedSeconds,
        revision = revision,
        privateNote = privateNote,
        sourceWorkId = sourceId,
    )

    private fun commandKey(item: SharedTimeOutboxItem) =
        item.atomicIdempotencyKey ?: item.allocationIdempotencyKey ?: item.entryIdempotencyKey ?: item.allocationId ?: item.entryId.orEmpty()

    private fun newKey() = Uuid.random().toString()
    private fun now() = Clock.System.now().toString()

    private class ApiStatusException(val status: Int, val code: String) : IllegalStateException()

    private suspend fun <T> authorized(session: PlannorSession, call: suspend (String) -> T): T {
        try {
            return call(session.accessToken)
        } catch (error: UnauthorizedException) {
            if (!refresh(session)) throw error
            val refreshed = sessions.read() ?: throw error
            if (refreshed.accountId != session.accountId) throw error
            return call(refreshed.accessToken)
        }
    }

    private suspend fun me(session: PlannorSession): MeDto {
        val response = client.get(PLANNOR_API + "me") {
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
        }
        ensureSuccess(response.status)
        return response.body<ApiEnvelope<MeDto>>().data
    }

    private suspend fun deleteSession(session: PlannorSession) = client.delete(PLANNOR_API + "auth/session") {
        header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
        contentType(ContentType.Application.Json)
        setBody(RefreshRequest(session.refreshToken))
    }

    private fun ensureSuccess(status: HttpStatusCode) {
        when {
            status == HttpStatusCode.Unauthorized -> throw UnauthorizedException()
            status.isSuccess() -> Unit
            else -> throw ApiStatusException(status.value, "request_rejected")
        }
    }

    private fun HttpStatusCode.toFailure() = when (this) {
        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> PlannorConnectionFailure.CREDENTIALS_OR_ACCOUNT
        HttpStatusCode.TooManyRequests -> PlannorConnectionFailure.RATE_LIMITED
        else -> if (value >= 500) PlannorConnectionFailure.SERVER else PlannorConnectionFailure.CREDENTIALS_OR_ACCOUNT
    }

    private class UnauthorizedException : IllegalStateException()

    companion object {
        const val TYPE_PROJECT = "plannor_project"
        const val TYPE_OUTBOX = "shared_time_outbox"
        const val TYPE_ENTRY = "personal_time_entry"
        const val TYPE_OWNER_LINK = "owner_allocation_link"
        const val TYPE_CURSOR = "time_change_cursor"
        const val TYPE_ALLOCATION = "project_time_allocation"
        const val TYPE_SESSION_STATE = "plannor_session_state"
        const val OWNER_CURSOR_KEY = "owner"
        private const val BASE_RETRY_DELAY_SECONDS = 2L
        private const val MAX_RETRY_DELAY_SECONDS = 15 * 60L
    }
}
