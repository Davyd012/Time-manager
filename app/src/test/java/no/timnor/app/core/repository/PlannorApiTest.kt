package no.timnor.app.core.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.features.shared_hours.domain.PersonalTimeEntry
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.features.shared_hours.domain.OwnerAllocationLink
import no.timnor.app.shared.features.shared_hours.domain.TimeChangeCursor
import no.timnor.app.core.repository.HistoricalTimeEntry
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOperation
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannorApiTest {
    @Test
    fun uncertainAllocationRetriesWithTheSameKeyAndUpdatesRemainingTime() = runTest {
        val store = FakeRecordStorage()
        val project = PlannorProjectSelection(7, 21, "SBI", selected = true, refreshedAtUtc = "now")
        store.put(7, PlannorApi.TYPE_PROJECT, "21", project, PlannorProjectSelection.serializer(), "now")
        val entry = PersonalTimeEntry(7, "entry-9", "2026-11-12", 3600, 0, 1)
        store.put(7, PlannorApi.TYPE_ENTRY, entry.serverId, entry, PersonalTimeEntry.serializer(), "now")
        val keys = mutableListOf<String>()
        val http = HttpClient(MockEngine { request ->
            when (request.method) {
                HttpMethod.Post -> {
                    val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    keys += body.getValue("idempotencyKey").jsonPrimitive.content
                    if (keys.size == 1) respond("{}", HttpStatusCode.InternalServerError)
                    else respond("""{"data":{"id":"allocation-1","entryId":"entry-9","projectId":21,"workDate":"2026-11-12","durationSeconds":1200,"revision":1}}""",
                        HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                }
                HttpMethod.Get -> respond("""{"data":{"items":[{"id":"entry-9","ownerId":7,"workDate":"2026-11-12","durationSeconds":3600,"allocatedSeconds":1200,"revision":2}]}}""",
                    HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                else -> error("Unexpected request")
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val api = PlannorApi(FakeSessions(PlannorSession("access", "refresh", 7, "alex")), store, http)

        assertEquals(SharedWriteStatus.UNKNOWN, api.allocateExisting(entry, 21, null, 1200, null).status)
        assertEquals(setOf("entry-9"), api.pendingAllocationEntryIds())
        assertEquals(SharedWriteStatus.SYNCED, api.retryPending(forceRetry = true).single().status)
        assertEquals(2, keys.size)
        assertEquals(keys.first(), keys.last())
        assertTrue(api.pendingAllocationEntryIds().isEmpty())
        assertEquals(2400L, api.cachedPersonalEntries().single().remainingSeconds)
        http.close()
    }

    @Test
    fun unresolvedLocalImportIsNotSubmittedAgainWithANewKey() = runTest {
        val store = FakeRecordStorage()
        val pending = SharedTimeOutboxItem(accountId = 7, operation = SharedTimeOperation.IMPORT_ENTRY,
            outcome = SharedTimeOutcome.OUTCOME_UNKNOWN, projectId = 0, workDate = "2026-11-12",
            durationSeconds = 600, sourceId = "local-source", entryIdempotencyKey = "original-key")
        store.put(7, PlannorApi.TYPE_OUTBOX, "original-key", pending, SharedTimeOutboxItem.serializer(), "now")
        var writes = 0
        val http = HttpClient(MockEngine { request ->
            if (request.method != HttpMethod.Get) writes++
            respond("""{"data":{"items":[]}}""", HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val api = PlannorApi(FakeSessions(PlannorSession("access", "refresh", 7, "alex")), store, http)

        val results = api.importHistoricalEntries(listOf(HistoricalTimeEntry("local-source", "2026-11-12", 600)))

        assertTrue(results.isEmpty())
        assertEquals(0, writes)
        assertEquals(setOf("local-source"), api.pendingImportSourceIds())
        http.close()
    }

    @Test
    fun cachedEntriesAndUnresolvedWritesAreScopedToTheConnectedAccount() = runTest {
        val store = FakeRecordStorage()
        val entry = PersonalTimeEntry(7, "entry-7", "2026-11-12", 3600, 0, 1)
        val other = PersonalTimeEntry(8, "entry-8", "2026-11-12", 1800, 0, 1)
        store.put(7, PlannorApi.TYPE_ENTRY, entry.serverId, entry, PersonalTimeEntry.serializer(), "now")
        store.put(8, PlannorApi.TYPE_ENTRY, other.serverId, other, PersonalTimeEntry.serializer(), "now")
        val pending = SharedTimeOutboxItem(accountId = 7, operation = SharedTimeOperation.ALLOCATE_EXISTING,
            outcome = SharedTimeOutcome.OUTCOME_UNKNOWN, entryId = "entry-7", entryRevision = 1,
            projectId = 21, workDate = entry.workDate, durationSeconds = 600, allocationIdempotencyKey = "key-1")
        val resolved = pending.copy(outcome = SharedTimeOutcome.SUCCEEDED, entryId = "completed", allocationIdempotencyKey = "key-2")
        val edit = SharedTimeOutboxItem(accountId = 7, operation = SharedTimeOperation.UPDATE_ALLOCATION,
            outcome = SharedTimeOutcome.PENDING, entryId = "entry-7", entryRevision = 1, allocationId = "allocation-7",
            allocationRevision = 1, projectId = 21, workDate = entry.workDate, durationSeconds = 600, allocationIdempotencyKey = "key-3")
        val importing = SharedTimeOutboxItem(accountId = 7, operation = SharedTimeOperation.IMPORT_ENTRY,
            outcome = SharedTimeOutcome.OUTCOME_UNKNOWN, projectId = 0, workDate = entry.workDate,
            durationSeconds = 600, sourceId = "local-source", entryIdempotencyKey = "key-4")
        val direct = SharedTimeOutboxItem(accountId = 7, operation = SharedTimeOperation.CREATE_AND_ALLOCATE,
            outcome = SharedTimeOutcome.PENDING, projectId = 21, workDate = entry.workDate,
            durationSeconds = 600, atomicIdempotencyKey = "key-5")
        listOf(pending, resolved, edit, importing, direct).forEachIndexed { index, item ->
            store.put(7, PlannorApi.TYPE_OUTBOX, index.toString(), item, SharedTimeOutboxItem.serializer(), "now")
        }
        val http = HttpClient(MockEngine { error("Cache reads must not use the network") })
        val sessions = FakeSessions(PlannorSession("access", "refresh", 7, "alex"))
        val api = PlannorApi(sessions, store, http)

        assertEquals(listOf(entry), api.cachedPersonalEntries())
        assertEquals(setOf("entry-7"), api.pendingAllocationEntryIds())
        assertEquals(setOf("allocation-7"), api.pendingAllocationIds())
        assertEquals(setOf("local-source"), api.pendingImportSourceIds())
        assertTrue(api.hasPendingDirectCreate())
        sessions.write(PlannorSession("access", "refresh", 8, "sam"))
        assertEquals(listOf(other), api.cachedPersonalEntries())
        assertTrue(api.pendingAllocationEntryIds().isEmpty())
        assertTrue(api.pendingAllocationIds().isEmpty())
        assertTrue(api.pendingImportSourceIds().isEmpty())
        assertEquals(false, api.hasPendingDirectCreate())
        http.close()
    }

    @Test
    fun missingOwnerSnapshotFallsBackToAllAccessibleProjectsIncludingUnselectedOnes() = runTest {
        val store = FakeRecordStorage()
        listOf(
            PlannorProjectSelection(7, 21, "Selected", selected = true, refreshedAtUtc = "now"),
            PlannorProjectSelection(7, 22, "Other accessible project", selected = false, refreshedAtUtc = "now"),
        ).forEach { project ->
            store.put(7, PlannorApi.TYPE_PROJECT, project.projectId.toString(), project,
                PlannorProjectSelection.serializer(), "now")
        }
        val requestedPaths = mutableListOf<String>()
        val http = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            requestedPaths += path
            when (path) {
                "/v1/me/time-changes" -> respond(
                    """{"data":{"items":[{"cursor":1,"kind":"allocation_created","allocationId":"allocation-22","entryId":"entry-9","revision":1}],"nextCursor":1}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-entries" -> respond(
                    """{"data":{"items":[{"id":"entry-9","ownerId":7,"workDate":"2026-11-12","durationSeconds":900,"allocatedSeconds":900,"revision":2}]}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-allocations" -> respond(
                    """{"error":{"code":"not_found"}}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/projects/21/time-changes", "/v1/projects/22/time-changes" -> respond(
                    """{"data":{"items":[],"nextCursor":0}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/projects/21/time-allocations" -> respond(
                    """{"data":{"items":[],"page":1,"pageSize":100,"hasMore":false}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/projects/22/time-allocations" -> respond(
                    """{"data":{"items":[{"id":"allocation-22","projectId":22,"taskId":null,"workDate":"2026-11-12","durationSeconds":900,"sharedNote":null,"revision":1}],"page":1,"pageSize":100,"hasMore":false}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                else -> error("Unexpected request: $path")
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val api = PlannorApi(FakeSessions(PlannorSession("access", "refresh", 7, "alex")), store, http)

        val allocations = api.refreshProjectSnapshots()

        assertEquals(listOf("allocation-22"), allocations.map { it.allocationId })
        assertTrue("/v1/projects/22/time-allocations" in requestedPaths)
        assertEquals(listOf("allocation-22"), api.cachedProjectAllocations().map { it.allocationId })
        http.close()
    }

    @Test
    fun failedSnapshotDoesNotAdvanceOwnerCursorOrReplaceOwnerLinks() = runTest {
        val store = FakeRecordStorage()
        val existingLink = OwnerAllocationLink(7, "old-allocation", "old-entry")
        store.put(7, PlannorApi.TYPE_OWNER_LINK, existingLink.allocationId, existingLink,
            OwnerAllocationLink.serializer(), "old")
        store.put(7, PlannorApi.TYPE_CURSOR, PlannorApi.OWNER_CURSOR_KEY, TimeChangeCursor(0),
            TimeChangeCursor.serializer(), "old")
        val http = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/me/time-changes" -> respond(
                    """{"data":{"items":[{"cursor":4,"kind":"allocation_created","allocationId":"new-allocation","entryId":"new-entry","revision":1}],"nextCursor":4}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-entries" -> respond("{}", status = HttpStatusCode.InternalServerError)
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val api = PlannorApi(FakeSessions(PlannorSession("access", "refresh", 7, "alex")), store, http)

        assertTrue(runCatching { api.refreshProjectSnapshots() }.isFailure)
        assertEquals(TimeChangeCursor(0), store.get(7, PlannorApi.TYPE_CURSOR, PlannorApi.OWNER_CURSOR_KEY,
            TimeChangeCursor.serializer()))
        assertEquals(listOf(existingLink), store.getAll(7, PlannorApi.TYPE_OWNER_LINK, OwnerAllocationLink.serializer()))
        http.close()
    }

    @Test
    fun laterProjectFailureDoesNotCommitEarlierProjectCursor() = runTest {
        val store = FakeRecordStorage()
        listOf(21, 22).forEach { projectId ->
            val project = PlannorProjectSelection(
                accountId = 7,
                projectId = projectId,
                name = "Project $projectId",
                selected = true,
                refreshedAtUtc = "now",
            )
            store.put(7, PlannorApi.TYPE_PROJECT, projectId.toString(), project,
                PlannorProjectSelection.serializer(), "now")
            store.put(7, PlannorApi.TYPE_CURSOR, "project:$projectId", TimeChangeCursor(0),
                TimeChangeCursor.serializer(), "old")
        }
        var project21SnapshotRead = false
        val http = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path == "/v1/me/time-changes" -> respond(
                    """{"data":{"items":[],"nextCursor":0}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                path == "/v1/me/time-entries" -> respond(
                    """{"data":{"items":[]}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                path == "/v1/me/time-allocations" -> respond(
                    """{"data":{"items":[],"page":1,"pageSize":100,"hasMore":false}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                path == "/v1/projects/21/time-changes" -> respond(
                    """{"data":{"items":[],"nextCursor":5}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                path == "/v1/projects/21/time-allocations" -> {
                    project21SnapshotRead = true
                    respond(
                        """{"data":{"items":[],"page":1,"pageSize":100,"hasMore":false}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
                path == "/v1/projects/22/time-changes" -> respond("{}", status = HttpStatusCode.InternalServerError)
                else -> error("Unexpected request: $path")
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val api = PlannorApi(FakeSessions(PlannorSession("access", "refresh", 7, "alex")), store, http)

        assertTrue(runCatching { api.refreshProjectSnapshots() }.isFailure)
        assertTrue(project21SnapshotRead)
        assertEquals(TimeChangeCursor(0), store.get(7, PlannorApi.TYPE_CURSOR, "project:21", TimeChangeCursor.serializer()))
        assertEquals(TimeChangeCursor(0), store.get(7, PlannorApi.TYPE_CURSOR, "project:22", TimeChangeCursor.serializer()))
        http.close()
    }

    @Test
    fun clearingPrivateNoteSendsExplicitNull() = runTest {
        val store = FakeRecordStorage()
        var body: JsonObject? = null
        val http = HttpClient(MockEngine { request ->
            body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            respond(
                """{"data":{"id":"entry-9","ownerId":7,"workDate":"2026-11-12","durationSeconds":1800,"allocatedSeconds":0,"revision":2,"privateNote":null}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val api = PlannorApi(FakeSessions(PlannorSession("a", "r", 7, "alex")), store, http)
        val result = api.correctEntry(PersonalTimeEntry(7, "entry-9", "2026-11-12", 1800, 0, 1, "old"),
            "2026-11-12", 1800, null)
        assertEquals(SharedWriteStatus.SYNCED, result.status)
        assertTrue(body!!.containsKey("privateNote"))
        assertEquals(JsonNull, body!!["privateNote"])
        http.close()
    }
    @Test
    fun reviewedHistoricalImportRetriesWithSameKeyAndOmitsPrivateNotes() = runTest {
        val sessions = FakeSessions(PlannorSession("access", "refresh", 7, "alex"))
        val store = FakeRecordStorage()
        val postBodies = mutableListOf<String>()
        val http = HttpClient(MockEngine { request ->
            if (request.url.encodedPath == "/v1/me/time-entries" && request.method == HttpMethod.Get) {
                respond(
                    content = """{"data":{"items":[]}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                val body = (request.body as TextContent).text
                postBodies += body
                if (postBodies.size == 1) {
                    respond(
                        content = "{}",
                        status = HttpStatusCode.ServiceUnavailable,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                } else {
                    respond(
                        content = """{"data":{"id":"entry-13","ownerId":7,"sourceId":"5b2e21ba-f7b7-3e45-9e1d-b2272ed1c3ec","workDate":"2026-11-12","durationSeconds":300,"allocatedSeconds":0,"revision":1,"privateNote":null}}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            }
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
        }
        val api = PlannorApi(sessions, store, http)

        val first = api.importHistoricalEntries(
            listOf(HistoricalTimeEntry("5b2e21ba-f7b7-3e45-9e1d-b2272ed1c3ec", "2026-11-12", 300)),
        ).single()
        assertTrue(api.retryPending().isEmpty())
        val retry = api.retryPending(forceRetry = true).single()

        assertEquals(SharedWriteStatus.UNKNOWN, first.status)
        assertEquals(SharedWriteStatus.SYNCED, retry.status)
        val firstBody = Json.parseToJsonElement(postBodies[0]).jsonObject
        val retryBody = Json.parseToJsonElement(postBodies[1]).jsonObject
        assertEquals(firstBody, retryBody)
        assertNull(firstBody["privateNote"])
        assertEquals("5b2e21ba-f7b7-3e45-9e1d-b2272ed1c3ec", firstBody["sourceId"]?.jsonPrimitive?.content)
        http.close()
    }

    @Test
    fun unknownAtomicWriteRetriesWithSameKeyAndNeverCopiesPrivateNoteIntoAllocation() = runTest {
        val sessions = FakeSessions(PlannorSession("access", "refresh", 7, "alex"))
        val store = FakeRecordStorage()
        store.put(7, PlannorApi.TYPE_PROJECT, "21", PlannorProjectSelection.serializer().let {
            PlannorProjectSelection(7, 21, "Site", selected = true, refreshedAtUtc = "now")
        }, PlannorProjectSelection.serializer(), "now")
        val requests = mutableListOf<String>()
        val http = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/v1/me/time-entries/allocate", request.url.encodedPath)
            val body = (request.body as TextContent).text
            requests += body
            if (requests.size == 1) {
                respond(
                    content = """{"error":{"code":"temporarily_unavailable","message":"retry"}}""",
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respond(
                        content = """{"data":{"id":"entry-9","allocationId":"allocation-9","ownerId":7,"workDate":"2026-11-12","durationSeconds":1800,"allocatedSeconds":1800,"unallocatedSeconds":0,"revision":1,"privateNote":"Personal note"}}""",
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
        }
        val api = PlannorApi(sessions, store, http)

        val first = api.createAndAllocate(
            projectId = 21,
            taskId = 3,
            workDate = "2026-11-12",
            durationSeconds = 1800,
            privateNote = "Personal note",
            sharedNote = "Review foundations",
            sourceId = "local-work-uuid",
        )
        assertTrue(api.retryPending().isEmpty())
        assertEquals(1, requests.size)
        val savedUnknown = store.get(7, PlannorApi.TYPE_OUTBOX, first.commandId, SharedTimeOutboxItem.serializer())!!
        assertEquals(1, savedUnknown.retryCount)
        assertTrue(savedUnknown.retryAfterEpochSeconds != null)
        val retried = api.retryPending(forceRetry = true).single()

        assertEquals(SharedWriteStatus.UNKNOWN, first.status)
        assertEquals(SharedWriteStatus.SYNCED, retried.status)
        assertEquals(2, requests.size)
        val firstBody = Json.parseToJsonElement(requests[0]).jsonObject
        val retryBody = Json.parseToJsonElement(requests[1]).jsonObject
        assertEquals(firstBody["idempotencyKey"], retryBody["idempotencyKey"])
        assertEquals(firstBody, retryBody)
        val entry = firstBody.getValue("entry").jsonObject
        val allocation = firstBody.getValue("allocation").jsonObject
        assertEquals("Personal note", entry.getValue("privateNote").jsonPrimitive.content)
        assertEquals("Review foundations", allocation.getValue("sharedNote").jsonPrimitive.content)
        assertNull(allocation["privateNote"])
        assertEquals("entry-9", retried.entryId)
        assertEquals("allocation-9", store.get(7, PlannorApi.TYPE_OUTBOX, retried.commandId, SharedTimeOutboxItem.serializer())?.allocationId)
        http.close()
    }

    @Test
    fun ownerSnapshotPaginatesAndKeepsPartialAndProjectOnlyAllocations() = runTest {
        val pages = mutableListOf<String>()
        val store = FakeRecordStorage()
        val http = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/me/time-changes" -> respond(
                    """{"data":{"items":[],"nextCursor":0}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-entries" -> respond(
                    """{"data":{"items":[{"id":"entry-9","ownerId":7,"workDate":"2026-11-12","durationSeconds":3600,"allocatedSeconds":3600,"revision":2}]}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-allocations" -> {
                    val page = request.url.parameters["page"]!!
                    pages += page
                    val content = if (page == "1") {
                        """{"data":{"items":[{"id":"allocation-1","entryId":"entry-9","projectId":21,"taskId":3,"workDate":"2026-11-12","durationSeconds":900,"sharedNote":"task","revision":2}],"page":1,"pageSize":100,"hasMore":true}}"""
                    } else {
                        """{"data":{"items":[{"id":"allocation-2","entryId":"entry-9","projectId":22,"taskId":null,"workDate":"2026-11-12","durationSeconds":2700,"sharedNote":null,"revision":1}],"page":2,"pageSize":100,"hasMore":false}}"""
                    }
                    respond(content, status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                }
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val api = PlannorApi(FakeSessions(PlannorSession("access", "refresh", 7, "alex")), store, http)

        val snapshot = api.refreshProjectSnapshots()

        assertEquals(listOf("1", "2"), pages)
        assertEquals(listOf("allocation-1", "allocation-2"), snapshot.map { it.allocationId })
        assertEquals(3600L, snapshot.sumOf { it.durationSeconds })
        assertEquals(900L, snapshot.single { it.allocationId == "allocation-1" }.durationSeconds)
        assertNull(snapshot.single { it.allocationId == "allocation-2" }.taskId)
        http.close()
    }

    @Test
    fun ownerSnapshotReadsHistoriesBeyondOneThousandPages() = runTest {
        val store = FakeRecordStorage()
        var allocationPageCount = 0
        val http = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/me/time-changes" -> respond(
                    """{"data":{"items":[],"nextCursor":0}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-entries" -> respond(
                    """{"data":{"items":[]}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-allocations" -> {
                    val page = request.url.parameters["page"]!!.toInt()
                    allocationPageCount++
                    respond(
                        """{"data":{"items":[{"id":"allocation-$page","entryId":"entry-$page","projectId":21,"workDate":"2026-11-12","durationSeconds":1,"revision":1}],"page":$page,"pageSize":100,"hasMore":${page < 1001}}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val api = PlannorApi(FakeSessions(PlannorSession("access", "refresh", 7, "alex")), store, http)

        val snapshot = api.refreshProjectSnapshots()

        assertEquals(1001, allocationPageCount)
        assertEquals(1001, snapshot.size)
        assertEquals("allocation-1001", snapshot.last().allocationId)
        http.close()
    }

    @Test
    fun incompleteOwnerSnapshotDoesNotReplaceTheAuthorizedAllocationCache() = runTest {
        val store = FakeRecordStorage()
        val cached = ProjectTimeAllocation(
            accountId = 7, allocationId = "cached", entryId = "entry-old", projectId = 21,
            taskId = 3, workDate = "2026-11-11", durationSeconds = 600, sharedNote = null, revision = 1,
        )
        store.put(7, PlannorApi.TYPE_ALLOCATION, cached.allocationId, cached,
            ProjectTimeAllocation.serializer(), "old")
        val http = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/me/time-changes" -> respond(
                    """{"data":{"items":[],"nextCursor":0}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-entries" -> respond(
                    """{"data":{"items":[]}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-allocations" -> respond(
                    """{"data":{"items":[],"page":2,"pageSize":100,"hasMore":false}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val api = PlannorApi(FakeSessions(PlannorSession("access", "refresh", 7, "alex")), store, http)

        assertTrue(runCatching { api.refreshProjectSnapshots() }.isFailure)
        assertEquals(listOf(cached), api.cachedProjectAllocations())
        http.close()
    }

    @Test
    fun retryReadsOnlyTheAuthenticatedAccountsOutbox() = runTest {
        val store = FakeRecordStorage()
        store.put(
            99,
            PlannorApi.TYPE_OUTBOX,
            "foreign-key",
            SharedTimeOutboxItem(
                accountId = 99,
                operation = SharedTimeOperation.CREATE_AND_ALLOCATE,
                outcome = SharedTimeOutcome.OUTCOME_UNKNOWN,
                projectId = 5,
                workDate = "2026-11-12",
                durationSeconds = 10,
                atomicIdempotencyKey = "foreign-key",
            ),
            SharedTimeOutboxItem.serializer(),
            "now",
        )
        var requestCount = 0
        val http = HttpClient(MockEngine { request ->
            requestCount++
            respond("{}", status = HttpStatusCode.OK)
        })
        val api = PlannorApi(FakeSessions(PlannorSession("a", "r", 7, "alex")), store, http)

        assertTrue(api.retryPending().isEmpty())
        assertEquals(0, requestCount)
        http.close()
    }

    @Test
    fun revokedProjectAccessDropsCachedProjectAndAllocationDetailsButKeepsOwnerJournal() = runTest {
        val store = FakeRecordStorage()
        store.put(7, PlannorApi.TYPE_PROJECT, "21",
            PlannorProjectSelection(7, 21, "Private site", selected = true,
                refreshedAtUtc = "now", linkedLocalProjectId = 3),
            PlannorProjectSelection.serializer(), "now")
        var projectSnapshotRequested = false
        val http = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/me/time-changes" -> respond(
                    """{"data":{"items":[],"nextCursor":0}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-entries" -> respond(
                    """{"data":{"items":[]}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-allocations" -> respond(
                    """{"data":{"items":[{"id":"allocation-revoked","entryId":"entry-9","projectId":21,"taskId":3,"workDate":"2026-11-12","durationSeconds":900,"sharedNote":"owner note","revision":2}],"page":1,"pageSize":100,"hasMore":false}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/projects/21/time-changes" -> {
                    projectSnapshotRequested = true
                    respond("{}", status = HttpStatusCode.Forbidden)
                }
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val api = PlannorApi(FakeSessions(PlannorSession("access", "refresh", 7, "alex")), store, http)

        val reconciliationSnapshot = api.refreshProjectSnapshots()
        assertEquals(listOf("allocation-revoked"), reconciliationSnapshot.map { it.allocationId })
        assertTrue(projectSnapshotRequested)
        val restricted = store.get(7, PlannorApi.TYPE_PROJECT, "21", PlannorProjectSelection.serializer())!!
        assertEquals("Unavailable project", restricted.name)
        assertNull(restricted.contextLabel)
        assertEquals(3, restricted.linkedLocalProjectId)
        assertTrue(restricted.selected)
        assertTrue(restricted.archived)
        assertTrue(!restricted.available)
        assertTrue(api.cachedProjectAllocations().isEmpty())
        assertTrue(!api.setProjectSelected(21, true))
        assertTrue(!api.setProjectLink(21, 4))
        assertEquals(3, restricted.linkedLocalProjectId)
        val blockedWrite = api.createAndAllocate(
            projectId = 21,
            taskId = null,
            workDate = "2026-11-12",
            durationSeconds = 900,
            privateNote = "private",
            sharedNote = null,
        )
        assertEquals(SharedWriteStatus.REJECTED, blockedWrite.status)
        assertEquals("invalid_or_unselected_project", blockedWrite.errorCode)
        http.close()
    }

    @Test
    fun remoteAllocationDeletionBecomesReviewWithoutClearingLocalWork() = runTest {
        val store = FakeRecordStorage()
        val project = PlannorProjectSelection(
            accountId = 7, projectId = 21, name = "Shared project", selected = true,
            refreshedAtUtc = "now", linkedLocalProjectId = 3,
        )
        val intent = SharedWorkIntent(
            accountId = 7, localWorkId = 12, sourceId = "source-12", createKey = "create-12",
            projectId = 21, localProjectId = 3, localVersion = 2, queuedVersion = 2,
            workDate = "2026-11-12", durationSeconds = 3_600, privateNote = "private work",
        )
        val baseline = SharedWorkBaseline(
            entryId = "entry-12", entryRevision = 1, allocationId = "allocation-12", allocationRevision = 1,
            entryDurationSeconds = 3_600, allocationDurationSeconds = 3_600,
            allocationTracksEntry = true, entryWorkDate = "2026-11-12",
            entryPrivateNote = "private work", allocationProjectId = 21,
        )
        store.put(7, PlannorApi.TYPE_PROJECT, "21", project, PlannorProjectSelection.serializer(), "now")
        store.put(7, SharedWorkSyncCoordinator.TYPE_WORK_INTENT, "12", intent,
            SharedWorkIntent.serializer(), "now")
        store.put(7, SharedWorkSyncCoordinator.TYPE_WORK_BASELINE, "12", baseline,
            SharedWorkBaseline.serializer(), "now")
        val http = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/me/time-changes", "/v1/projects/21/time-changes" -> respond(
                    """{"data":{"items":[],"nextCursor":0}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-entries" -> respond(
                    """{"data":{"items":[{"id":"entry-12","ownerId":7,"sourceId":"source-12","workDate":"2026-11-12","durationSeconds":3600,"allocatedSeconds":0,"revision":2,"privateNote":"private work"}]}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-allocations" -> respond(
                    """{"data":{"items":[],"page":1,"pageSize":100,"hasMore":false}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/projects/21/time-allocations" -> respond(
                    """{"data":{"items":[],"page":1,"pageSize":100,"hasMore":false}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val sessions = FakeSessions(PlannorSession("access", "refresh", 7, "alex"))
        val coordinator = SharedWorkSyncCoordinator(sessions, store, PlannorApi(sessions, store, http))

        val status = coordinator.sync()

        val preserved = store.get(7, SharedWorkSyncCoordinator.TYPE_WORK_INTENT, "12", SharedWorkIntent.serializer())!!
        assertEquals(1, status.needsReview)
        assertEquals(SharedWorkSyncCoordinator.REVIEW_REMOTE_ALLOCATIONS_CHANGED, preserved.reviewReason)
        assertEquals(2L, preserved.localVersion)
        assertEquals(2L, preserved.queuedVersion)
        assertEquals("private work", preserved.privateNote)
        http.close()
    }

    @Test
    fun accountSwitchDuringPullStopsSyncBeforeUsingTheNewAccount() = runTest {
        val store = FakeRecordStorage()
        val originalIntent = SharedWorkIntent(
            accountId = 7, localWorkId = 12, sourceId = "source-12", createKey = "create-12",
            projectId = 21, localProjectId = 3, localVersion = 2, queuedVersion = 1,
            workDate = "2026-11-12", durationSeconds = 3_600, privateNote = "account one work",
        )
        store.put(7, SharedWorkSyncCoordinator.TYPE_WORK_INTENT, "12", originalIntent,
            SharedWorkIntent.serializer(), "now")
        val sessions = FakeSessions(PlannorSession("account-one-token", "refresh-one", 7, "alex"))
        val observedAuthorization = mutableListOf<String?>()
        val http = HttpClient(MockEngine { request ->
            observedAuthorization += request.headers[HttpHeaders.Authorization]
            check(request.url.encodedPath == "/v1/me/time-changes")
            sessions.write(PlannorSession("account-two-token", "refresh-two", 8, "sam"))
            respond(
                """{"data":{"items":[],"nextCursor":0}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val coordinator = SharedWorkSyncCoordinator(sessions, store, PlannorApi(sessions, store, http))

        val status = coordinator.sync()

        assertEquals(true, status.failed)
        assertEquals(listOf("Bearer account-one-token"), observedAuthorization)
        assertEquals(8, sessions.read()?.accountId)
        assertEquals(originalIntent, store.get(7, SharedWorkSyncCoordinator.TYPE_WORK_INTENT, "12",
            SharedWorkIntent.serializer()))
        assertEquals(emptyList<SharedWorkIntent>(), store.getAll(8,
            SharedWorkSyncCoordinator.TYPE_WORK_INTENT, SharedWorkIntent.serializer()))
        http.close()
    }

    @Test
    fun simultaneousRemoteEditStopsLocalWriteAndPreservesBothVersionsForReview() = runTest {
        val store = FakeRecordStorage()
        val project = PlannorProjectSelection(
            accountId = 7, projectId = 21, name = "Shared project", selected = true,
            refreshedAtUtc = "now", linkedLocalProjectId = 3,
        )
        val intent = SharedWorkIntent(
            accountId = 7, localWorkId = 12, sourceId = "source-12", createKey = "create-12",
            projectId = 21, localProjectId = 3, localVersion = 2, queuedVersion = 1,
            workDate = "2026-11-12", durationSeconds = 5_400, privateNote = "Timnor edit",
        )
        val baseline = SharedWorkBaseline(
            entryId = "entry-12", entryRevision = 1, allocationId = "allocation-12", allocationRevision = 1,
            entryDurationSeconds = 3_600, allocationDurationSeconds = 3_600,
            allocationTracksEntry = true, entryWorkDate = "2026-11-12",
            entryPrivateNote = "private work", allocationProjectId = 21,
        )
        store.put(7, PlannorApi.TYPE_PROJECT, "21", project, PlannorProjectSelection.serializer(), "now")
        store.put(7, SharedWorkSyncCoordinator.TYPE_WORK_INTENT, "12", intent,
            SharedWorkIntent.serializer(), "now")
        store.put(7, SharedWorkSyncCoordinator.TYPE_WORK_BASELINE, "12", baseline,
            SharedWorkBaseline.serializer(), "now")
        var mutationRequests = 0
        val http = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/me/time-changes", "/v1/projects/21/time-changes" -> respond(
                    """{"data":{"items":[],"nextCursor":0}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-entries" -> respond(
                    """{"data":{"items":[{"id":"entry-12","ownerId":7,"sourceId":"source-12","workDate":"2026-11-12","durationSeconds":4200,"allocatedSeconds":3600,"revision":2,"privateNote":"Plannor edit"}]}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-allocations" -> respond(
                    """{"data":{"items":[{"id":"allocation-12","entryId":"entry-12","projectId":21,"taskId":null,"workDate":"2026-11-12","durationSeconds":3600,"sharedNote":null,"revision":1}],"page":1,"pageSize":100,"hasMore":false}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/projects/21/time-allocations" -> respond(
                    """{"data":{"items":[{"id":"allocation-12","entryId":"entry-12","projectId":21,"taskId":null,"workDate":"2026-11-12","durationSeconds":3600,"sharedNote":null,"revision":1}],"page":1,"pageSize":100,"hasMore":false}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                else -> {
                    mutationRequests++
                    error("Unexpected request: ${request.url.encodedPath}")
                }
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val sessions = FakeSessions(PlannorSession("access", "refresh", 7, "alex"))
        val coordinator = SharedWorkSyncCoordinator(sessions, store, PlannorApi(sessions, store, http))

        val status = coordinator.sync()

        val preserved = store.get(7, SharedWorkSyncCoordinator.TYPE_WORK_INTENT, "12", SharedWorkIntent.serializer())!!
        assertEquals(false, status.failed)
        assertEquals(1, status.needsReview)
        assertEquals(SharedWorkSyncCoordinator.REVIEW_REMOTE_ENTRY_CHANGED, preserved.reviewReason)
        assertEquals(5_400L, preserved.durationSeconds)
        assertEquals("Timnor edit", preserved.privateNote)
        assertEquals(0, mutationRequests)
        http.close()
    }

    @Test
    fun inaccessibleProjectWithMissingOwnerSnapshotPreservesLocalEditForReview() = runTest {
        val store = FakeRecordStorage()
        val project = PlannorProjectSelection(
            accountId = 7, projectId = 21, name = "Unavailable project", selected = true,
            archived = true, refreshedAtUtc = "now", linkedLocalProjectId = 3, available = false,
        )
        val intent = SharedWorkIntent(
            accountId = 7, localWorkId = 12, sourceId = "source-12", createKey = "create-12",
            projectId = 21, localProjectId = 3, localVersion = 2, queuedVersion = 1,
            workDate = "2026-11-12", durationSeconds = 3_600, privateNote = "private edit",
        )
        val baseline = SharedWorkBaseline(
            entryId = "entry-12", entryRevision = 1, allocationId = "allocation-12", allocationRevision = 1,
            entryDurationSeconds = 3_600, allocationDurationSeconds = 3_600,
            entryWorkDate = "2026-11-12", entryPrivateNote = "private edit", allocationProjectId = 21,
        )
        store.put(7, PlannorApi.TYPE_PROJECT, "21", project, PlannorProjectSelection.serializer(), "now")
        store.put(7, SharedWorkSyncCoordinator.TYPE_WORK_INTENT, "12", intent,
            SharedWorkIntent.serializer(), "now")
        store.put(7, SharedWorkSyncCoordinator.TYPE_WORK_BASELINE, "12", baseline,
            SharedWorkBaseline.serializer(), "now")
        val http = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/me/time-changes" -> respond(
                    """{"data":{"items":[],"nextCursor":0}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-entries" -> respond(
                    """{"data":{"items":[{"id":"entry-12","ownerId":7,"sourceId":"source-12","workDate":"2026-11-12","durationSeconds":3600,"allocatedSeconds":3600,"revision":2}]}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/v1/me/time-allocations" -> respond(
                    """{"error":{"code":"not_found"}}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        val sessions = FakeSessions(PlannorSession("access", "refresh", 7, "alex"))
        val api = PlannorApi(sessions, store, http)
        val coordinator = SharedWorkSyncCoordinator(sessions, store, api)

        val status = coordinator.sync()

        val updated = store.get(7, SharedWorkSyncCoordinator.TYPE_WORK_INTENT, "12", SharedWorkIntent.serializer())!!
        assertEquals(1, status.needsReview)
        assertEquals(SharedWorkSyncCoordinator.REVIEW_ACCESS_REMOVED, updated.reviewReason)
        assertEquals(2L, updated.localVersion)
        assertEquals(1L, updated.queuedVersion)
        assertEquals("private edit", updated.privateNote)
        http.close()
    }

    private class FakeSessions(initial: PlannorSession?) : PlannorSessionStorage {
        private var current = initial
        override fun read() = current
        override fun write(session: PlannorSession) { current = session }
        override fun clear() { current = null }
    }

    private class FakeRecordStorage : SharedHoursRecordStorage {
        private val values = mutableMapOf<Triple<Int, String, String>, String>()
        private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

        override suspend fun <T> get(accountId: Int, type: String, key: String, serializer: KSerializer<T>): T? =
            values[Triple(accountId, type, key)]?.let { json.decodeFromString(serializer, it) }

        override suspend fun <T> getAll(accountId: Int, type: String, serializer: KSerializer<T>): List<T> =
            values.filterKeys { it.first == accountId && it.second == type }.values.map { json.decodeFromString(serializer, it) }

        override suspend fun <T> put(accountId: Int, type: String, key: String, value: T, serializer: KSerializer<T>, updatedAtUtc: String) {
            values[Triple(accountId, type, key)] = json.encodeToString(serializer, value)
        }

        override suspend fun <T> putAll(accountId: Int, type: String, values: List<Pair<String, T>>, serializer: KSerializer<T>, updatedAtUtc: String) {
            values.forEach { (key, value) -> put(accountId, type, key, value, serializer, updatedAtUtc) }
        }

        override suspend fun clear(accountId: Int, type: String) {
            values.keys.removeAll { it.first == accountId && it.second == type }
        }

        override suspend fun delete(accountId: Int, type: String, key: String) {
            values.remove(Triple(accountId, type, key))
        }

        override suspend fun commitProjectSnapshot(
            accountId: Int,
            ownerLinks: List<OwnerAllocationLink>,
            allocations: List<ProjectTimeAllocation>,
            cursors: Map<String, TimeChangeCursor>,
            updatedAtUtc: String,
        ) {
            clear(accountId, PlannorApi.TYPE_OWNER_LINK)
            putAll(accountId, PlannorApi.TYPE_OWNER_LINK, ownerLinks.map { it.allocationId to it },
                OwnerAllocationLink.serializer(), updatedAtUtc)
            clear(accountId, PlannorApi.TYPE_ALLOCATION)
            putAll(accountId, PlannorApi.TYPE_ALLOCATION, allocations.map { it.allocationId to it },
                ProjectTimeAllocation.serializer(), updatedAtUtc)
            putAll(accountId, PlannorApi.TYPE_CURSOR, cursors.toList(), TimeChangeCursor.serializer(), updatedAtUtc)
        }
    }
}
