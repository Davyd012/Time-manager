package no.timnor.app.features.sharedhours

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import no.timnor.app.core.repository.*
import no.timnor.app.shared.data.persistence.*
import no.timnor.app.shared.features.shared_hours.domain.OwnerAllocationLink
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.features.shared_hours.domain.TimeChangeCursor
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.ui.theme.TimnorTheme
import no.timnor.app.R
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class OfflineAllocationScreenTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun localHoursRemainVisibleWhenPlannorCannotBeReached() {
        val client = HttpClient(MockEngine { throw IOException("offline") })
        val viewModel = PlannorConnectionViewModel(
            api = PlannorApi(TestSession(), EmptyRecordStorage(), client),
            persistence = LocalPersistence(),
            createLinkedProject = { false },
            syncSharedWork = { SharedWorkSyncResult(emptyList(), 0) },
            loadSharedWorkReviews = { emptyList() },
            resolveSharedWork = { _, _ -> false },
        )
        composeRule.setContent {
            TimnorTheme(darkTheme = true) { AllocateExistingTimeScreen(viewModel, onConnectRequired = {}) }
        }
        composeRule.waitUntil(10_000) { !viewModel.state.value.loading && viewModel.state.value.entryError }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.entry_network_error)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.entry_local_status)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.entry_prepare)).assertIsDisplayed()
        composeRule.onNodeWithText("SBI").assertIsDisplayed()
        client.close()
    }

    @Test fun projectChoiceIsPersistedOnlyWhenSaveIsPressed() {
        val client = HttpClient(MockEngine { throw IOException("offline") })
        val records = MemoryRecordStorage()
        val project = PlannorProjectSelection(accountId = 7, projectId = 21, name = "SBI", refreshedAtUtc = "now")
        runBlocking { records.put(7, PlannorApi.TYPE_PROJECT, "21", project, PlannorProjectSelection.serializer(), "now") }
        val viewModel = PlannorConnectionViewModel(
            api = PlannorApi(TestSession(), records, client), persistence = LocalPersistence(),
            createLinkedProject = { false }, syncSharedWork = { SharedWorkSyncResult(emptyList(), 0) },
            loadSharedWorkReviews = { emptyList() }, resolveSharedWork = { _, _ -> false },
        )
        composeRule.setContent { TimnorTheme(darkTheme = true) { PlannorProjectsScreen(viewModel, onConnectRequired = {}) } }
        composeRule.waitUntil(10_000) { !viewModel.state.value.loading && viewModel.state.value.projects.isNotEmpty() }
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.plannor_project_accessibility, "SBI",
            composeRule.activity.getString(R.string.plannor_project_context_unknown))).performClick()
        composeRule.runOnIdle { org.junit.Assert.assertEquals(false, viewModel.state.value.projects.single().selected) }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.project_save_selection, 1)).assertIsDisplayed().performClick()
        composeRule.waitUntil(10_000) { !viewModel.state.value.loading && viewModel.state.value.projects.single().selected }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.project_selection_saved)).assertIsDisplayed()
        client.close()
    }

    @Test fun cachedAllocationRemainsReadableAndRemovalRequiresConfirmation() {
        val client = HttpClient(MockEngine { throw IOException("offline") })
        val records = MemoryRecordStorage()
        val project = PlannorProjectSelection(accountId = 7, projectId = 21, name = "SBI", selected = true, refreshedAtUtc = "now")
        val entry = no.timnor.app.shared.features.shared_hours.domain.PersonalTimeEntry(7, "entry-9", "2026-11-14", 3600, 1200, 2)
        val allocation = ProjectTimeAllocation(7, "allocation-1", entry.serverId, 21, null, entry.workDate, 1200, null, 1)
        runBlocking {
            records.put(7, PlannorApi.TYPE_PROJECT, "21", project, PlannorProjectSelection.serializer(), "now")
            records.put(7, PlannorApi.TYPE_ENTRY, entry.serverId, entry, no.timnor.app.shared.features.shared_hours.domain.PersonalTimeEntry.serializer(), "now")
            records.put(7, PlannorApi.TYPE_ALLOCATION, allocation.allocationId, allocation, ProjectTimeAllocation.serializer(), "now")
        }
        val viewModel = PlannorConnectionViewModel(
            api = PlannorApi(TestSession(), records, client), persistence = LocalPersistence(),
            createLinkedProject = { false }, syncSharedWork = { SharedWorkSyncResult(emptyList(), 0) },
            loadSharedWorkReviews = { emptyList() }, resolveSharedWork = { _, _ -> false },
        )
        composeRule.setContent { TimnorTheme(darkTheme = true) { SharedAllocationsScreen(viewModel, onConnectRequired = {}) } }
        composeRule.waitUntil(10_000) { !viewModel.state.value.loading && viewModel.state.value.allocationError }
        composeRule.onNodeWithText("SBI").assertIsDisplayed().performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.plannor_delete_allocation)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.plannor_delete_allocation_title)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.plannor_import_cancel)).performClick()
        composeRule.runOnIdle { org.junit.Assert.assertEquals(1, viewModel.state.value.projectAllocations.size) }
        client.close()
    }
}

private class TestSession : PlannorSessionStorage {
    private var session: PlannorSession? = PlannorSession("access", "refresh", 7, "alex")
    override fun read() = session
    override fun write(session: PlannorSession) { this.session = session }
    override fun clear() { session = null }
}

private class LocalPersistence : TimnorPersistence {
    private val project = StoredProject("", "SBI", 3)
    private val work = StoredWork("private note", 1_794_614_400, 3, 0, 12_600, 42)
    override suspend fun snapshotAll() = TimnorDataSnapshot(listOf(project), listOf(work))
    override fun observeProjects(): Flow<List<StoredProject>> = flowOf(listOf(project))
    override fun observeWorks(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>): Flow<List<StoredWork>> = flowOf(listOf(work))
    override suspend fun hoursForRange(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>) = work.time
    override suspend fun upsertProject(project: StoredProject) = Unit
    override suspend fun deleteProject(project: StoredProject) = Unit
    override suspend fun upsertWork(work: StoredWork) = Unit
    override suspend fun upsertWorks(works: List<StoredWork>) = Unit
    override suspend fun deleteWork(work: StoredWork) = Unit
    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) = Unit
}

private class EmptyRecordStorage : SharedHoursRecordStorage {
    override suspend fun <T> get(accountId: Int, type: String, key: String, serializer: KSerializer<T>): T? = null
    override suspend fun <T> getAll(accountId: Int, type: String, serializer: KSerializer<T>): List<T> = emptyList()
    override suspend fun <T> put(accountId: Int, type: String, key: String, value: T, serializer: KSerializer<T>, updatedAtUtc: String) = Unit
    override suspend fun <T> putAll(accountId: Int, type: String, values: List<Pair<String, T>>, serializer: KSerializer<T>, updatedAtUtc: String) = Unit
    override suspend fun clear(accountId: Int, type: String) = Unit
    override suspend fun delete(accountId: Int, type: String, key: String) = Unit
    override suspend fun commitProjectSnapshot(accountId: Int, ownerLinks: List<OwnerAllocationLink>,
        allocations: List<ProjectTimeAllocation>, cursors: Map<String, TimeChangeCursor>, updatedAtUtc: String) = Unit
}

private class MemoryRecordStorage : SharedHoursRecordStorage {
    private val values = mutableMapOf<Triple<Int, String, String>, String>()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
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
    override suspend fun clear(accountId: Int, type: String) { values.keys.removeAll { it.first == accountId && it.second == type } }
    override suspend fun delete(accountId: Int, type: String, key: String) { values.remove(Triple(accountId, type, key)) }
    override suspend fun commitProjectSnapshot(accountId: Int, ownerLinks: List<OwnerAllocationLink>,
        allocations: List<ProjectTimeAllocation>, cursors: Map<String, TimeChangeCursor>, updatedAtUtc: String) = Unit
}
