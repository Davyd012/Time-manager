package no.timnor.app.features.sharedhours

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import no.timnor.app.core.repository.PlannorApi
import no.timnor.app.core.repository.PlannorConnectionFailure
import no.timnor.app.core.repository.PlannorConnectionResult
import no.timnor.app.core.repository.PlannorSession
import no.timnor.app.core.repository.SharedWriteResult
import no.timnor.app.core.repository.SharedWorkSyncResult
import no.timnor.app.core.repository.SharedWorkResolution
import no.timnor.app.core.repository.SharedWorkReviewItem
import no.timnor.app.core.repository.HistoricalTimeEntry
import no.timnor.app.core.repository.AuthorizedPlannorTask
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.features.shared_hours.domain.LocalEntryImportPreview
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.features.shared_hours.domain.PersonalTimeEntry
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.features.shared_hours.domain.eligibleForImport
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import java.time.LocalDate
import java.util.UUID

data class PlannorConnectionUiState(
    val session: PlannorSession? = null,
    val identifier: String = "",
    val password: String = "",
    val projectSearch: String = "",
    val sharedProjectId: Int? = null,
    val sharedTaskId: Int? = null,
    val tasks: List<AuthorizedPlannorTask> = emptyList(),
    val sharedMinutes: String = "30",
    val sharedWorkDate: String = LocalDate.now().toString(),
    val privateNote: String = "",
    val sharedNote: String = "",
    val sharedWriteResult: SharedWriteResult? = null,
    val sharedWorkNeedsReview: Int = 0,
    val sharedWorkReviews: List<SharedWorkReviewItem> = emptyList(),
    val projects: List<PlannorProjectSelection> = emptyList(),
    val personalProjects: List<StoredProject> = emptyList(),
    val personalEntries: List<PersonalTimeEntry> = emptyList(),
    val projectAllocations: List<ProjectTimeAllocation> = emptyList(),
    val existingEntryId: String? = null,
    val selectedAllocationId: String? = null,
    val confirmAllocationDeletion: Boolean = false,
    val entryCorrectionDate: String = "",
    val entryCorrectionSeconds: String = "",
    val confirmEntryDeletion: Boolean = false,
    val historicalImportPreview: LocalEntryImportPreview? = null,
    val historicalImportEntries: List<HistoricalTimeEntry> = emptyList(),
    val loading: Boolean = false,
    val error: PlannorConnectionFailure? = null,
    val projectError: Boolean = false,
)

class PlannorConnectionViewModel(
    private val api: PlannorApi,
    private val persistence: TimnorPersistence,
    private val createLinkedProject: suspend (Int) -> Boolean,
    private val syncSharedWork: suspend () -> SharedWorkSyncResult,
    private val loadSharedWorkReviews: suspend () -> List<SharedWorkReviewItem>,
    private val resolveSharedWork: suspend (Int, SharedWorkResolution) -> Boolean,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlannorConnectionUiState())
    val state: StateFlow<PlannorConnectionUiState> = mutableState.asStateFlow()
    private val syncRequests = Channel<Unit>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            for (request in syncRequests) {
                if (mutableState.value.session != null) retryAtConnection()
            }
        }
        viewModelScope.launch {
            val personalProjects = persistence.snapshotAll().projects
            mutableState.update { it.copy(personalProjects = personalProjects) }
            val session = api.currentSession()
            mutableState.update { it.copy(session = session) }
            if (session != null) {
                requestSync()
                refreshProjects()
            }
        }
    }

    fun setIdentifier(value: String) = mutableState.update { it.copy(identifier = value, error = null) }
    fun setPassword(value: String) = mutableState.update { it.copy(password = value, error = null) }
    fun setProjectSearch(value: String) = mutableState.update { it.copy(projectSearch = value) }
    fun selectSharedProject(projectId: Int) {
        mutableState.update { it.copy(sharedProjectId = projectId, sharedTaskId = null, tasks = emptyList(), sharedWriteResult = null) }
        refreshTasks(projectId)
    }
    fun selectSharedTask(taskId: Int?) = mutableState.update { it.copy(sharedTaskId = taskId) }
    fun selectExistingEntry(entryId: String) = mutableState.update { current ->
        val entry = current.personalEntries.firstOrNull { it.serverId == entryId }
        current.copy(
            existingEntryId = entryId,
            entryCorrectionDate = entry?.workDate.orEmpty(),
            entryCorrectionSeconds = entry?.durationSeconds?.toString().orEmpty(),
            sharedWriteResult = null,
        )
    }
    fun selectAllocation(allocationId: String) = mutableState.update {
        it.copy(selectedAllocationId = allocationId, sharedWriteResult = null)
    }
    fun setEntryCorrectionDate(value: String) = mutableState.update { it.copy(entryCorrectionDate = value) }
    fun setEntryCorrectionSeconds(value: String) = mutableState.update {
        it.copy(entryCorrectionSeconds = value.filter(Char::isDigit).take(10))
    }
    fun setSharedMinutes(value: String) = mutableState.update {
        val selected = value.filter(Char::isDigit).take(4).toIntOrNull()?.coerceAtMost(1439)?.toString().orEmpty()
        it.copy(sharedMinutes = selected, sharedWriteResult = null)
    }
    fun setSharedWorkDate(value: String) {
        if (runCatching { LocalDate.parse(value) }.isSuccess) {
            mutableState.update { it.copy(sharedWorkDate = value, sharedWriteResult = null) }
        }
    }
    fun setPrivateNote(value: String) = mutableState.update { it.copy(privateNote = value, sharedWriteResult = null) }
    fun setSharedNote(value: String) = mutableState.update { it.copy(sharedNote = value, sharedWriteResult = null) }

    fun createSharedTime() {
        val current = mutableState.value
        val projectId = current.sharedProjectId ?: current.projects.firstOrNull { it.selected && !it.archived }?.projectId
            ?: return
        val seconds = current.sharedMinutes.toLongOrNull()?.takeIf { it in 1L..1439L }?.times(60) ?: return
        if (current.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, sharedWriteResult = null) }
            val result = api.createAndAllocate(
                projectId = projectId,
                taskId = current.sharedTaskId,
                workDate = current.sharedWorkDate,
                durationSeconds = seconds,
                privateNote = current.privateNote.ifBlank { null },
                sharedNote = current.sharedNote.ifBlank { null },
            )
            mutableState.update { it.copy(loading = false, sharedWriteResult = result) }
            refreshEntries()
        }
    }

    fun allocateExistingTime() {
        val current = mutableState.value
        val entry = current.personalEntries.firstOrNull { it.serverId == current.existingEntryId } ?: return
        val projectId = current.sharedProjectId
            ?: current.projects.firstOrNull { it.selected && !it.archived }?.projectId
            ?: return
        val seconds = current.sharedMinutes.toLongOrNull()?.takeIf { it > 0 }?.times(60) ?: return
        if (!entry.canAllocate(seconds) || current.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, sharedWriteResult = null) }
            val result = api.allocateExisting(
                entry = entry,
                projectId = projectId,
                taskId = current.sharedTaskId,
                durationSeconds = seconds,
                sharedNote = current.sharedNote.ifBlank { null },
            )
            mutableState.update { it.copy(loading = false, sharedWriteResult = result) }
            refreshEntries()
        }
    }

    fun correctSelectedEntry() {
        val current = mutableState.value
        val entry = current.personalEntries.firstOrNull { it.serverId == current.existingEntryId } ?: return
        val seconds = current.entryCorrectionSeconds.toLongOrNull() ?: return
        if (seconds <= 0 || current.loading || runCatching { LocalDate.parse(current.entryCorrectionDate) }.isFailure) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, sharedWriteResult = null) }
            val result = api.correctEntry(
                entry = entry,
                workDate = current.entryCorrectionDate,
                durationSeconds = seconds,
                privateNote = entry.privateNote,
            )
            mutableState.update { it.copy(loading = false, sharedWriteResult = result) }
            refreshEntries()
        }
    }

    fun requestDeleteSelectedEntry() = mutableState.update { it.copy(confirmEntryDeletion = true) }
    fun cancelDeleteSelectedEntry() = mutableState.update { it.copy(confirmEntryDeletion = false) }

    fun confirmDeleteSelectedEntry() {
        val entry = mutableState.value.personalEntries.firstOrNull { it.serverId == mutableState.value.existingEntryId } ?: return
        if (entry.allocatedSeconds > 0 || mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, confirmEntryDeletion = false, sharedWriteResult = null) }
            val result = api.deleteEntry(entry)
            mutableState.update { it.copy(loading = false, sharedWriteResult = result) }
            refreshEntries()
        }
    }

    fun refreshProjectSnapshots() {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, projectError = false) }
            try {
                val allocations = api.refreshProjectSnapshots()
                mutableState.update { current ->
                    val selected = current.selectedAllocationId?.takeIf { id -> allocations.any { it.allocationId == id } }
                        ?: allocations.firstOrNull()?.allocationId
                    current.copy(projectAllocations = allocations, selectedAllocationId = selected, loading = false)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, projectError = true) }
            }
        }
    }

    fun correctSelectedAllocation() {
        val current = mutableState.value
        val allocation = current.projectAllocations.firstOrNull { it.allocationId == current.selectedAllocationId } ?: return
        val entry = current.personalEntries.firstOrNull { it.serverId == allocation.entryId } ?: return
        val seconds = current.sharedMinutes.toLongOrNull()?.takeIf { it > 0 }?.times(60) ?: return
        if (current.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, sharedWriteResult = null) }
            val result = api.correctAllocation(
                allocation = allocation,
                entryRevision = entry.revision,
                durationSeconds = seconds,
                taskId = current.sharedTaskId,
                sharedNote = current.sharedNote.ifBlank { null },
            )
            mutableState.update { it.copy(loading = false, sharedWriteResult = result) }
            refreshProjectSnapshots()
        }
    }

    fun requestDeleteSelectedAllocation() = mutableState.update { it.copy(confirmAllocationDeletion = true) }
    fun cancelDeleteSelectedAllocation() = mutableState.update { it.copy(confirmAllocationDeletion = false) }

    fun confirmDeleteSelectedAllocation() {
        val current = mutableState.value
        val allocation = current.projectAllocations.firstOrNull { it.allocationId == current.selectedAllocationId } ?: return
        val entry = current.personalEntries.firstOrNull { it.serverId == allocation.entryId } ?: return
        if (current.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, confirmAllocationDeletion = false, sharedWriteResult = null) }
            val result = api.deleteAllocation(allocation, entry.revision)
            mutableState.update { it.copy(loading = false, sharedWriteResult = result) }
            refreshProjectSnapshots()
        }
    }

    fun refreshEntries() {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, projectError = false) }
            try {
                val entries = api.loadPersonalEntries()
                mutableState.update { current ->
                    val selectedId = current.existingEntryId?.takeIf { id -> entries.any { it.serverId == id } }
                        ?: entries.firstOrNull { it.remainingSeconds > 0 }?.serverId
                    current.copy(personalEntries = entries, existingEntryId = selectedId, loading = false)
                }
                mutableState.value.sharedProjectId?.let(::refreshTasks)
                refreshProjectSnapshots()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, projectError = true) }
            }
        }
    }

    fun retryPendingWrites() {
        viewModelScope.launch {
            val accountId = mutableState.value.session?.accountId ?: return@launch
            try {
                api.retryPending(forceRetry = true)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(projectError = true) }
            }
            if (mutableState.value.session?.accountId == accountId) requestSync()
        }
    }

    fun resolveSharedWork(localWorkId: Int, resolution: SharedWorkResolution) {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true) }
            try {
                val resolved = resolveSharedWork.invoke(localWorkId, resolution)
                mutableState.update { it.copy(loading = false, projectError = !resolved) }
                if (resolved) {
                    requestSync()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, projectError = true) }
            }
        }
    }

    fun requestSync() { syncRequests.trySend(Unit) }

    fun reviewHistoricalImport() {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, historicalImportPreview = null) }
            try {
                val session = api.currentSession() ?: error("not connected")
                val localWorks = persistence.snapshotAll().works.filter { it.time in 1..315_360_000 }
                val remoteEntries = api.loadPersonalEntries()
                val sourceIds = localWorks.map { work -> sourceId(session.accountId, work.id) }
                val alreadyImported = remoteEntries.mapNotNull(PersonalTimeEntry::sourceWorkId).toSet()
                val eligibleIds = eligibleForImport(sourceIds, alreadyImported).toSet()
                val candidates = localWorks.filter { sourceId(session.accountId, it.id) in eligibleIds }.map { work ->
                    HistoricalTimeEntry(
                        sourceId = sourceId(session.accountId, work.id),
                        workDate = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date.toString(),
                        durationSeconds = work.time.toLong(),
                    )
                }
                mutableState.update {
                    it.copy(
                        historicalImportPreview = LocalEntryImportPreview(
                            totalEntries = localWorks.size,
                            alreadyImported = localWorks.size - candidates.size,
                            eligibleEntries = candidates.size,
                        ),
                        historicalImportEntries = candidates,
                        loading = false,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, projectError = true) }
            }
        }
    }

    fun cancelHistoricalImport() = mutableState.update {
        it.copy(historicalImportPreview = null, historicalImportEntries = emptyList())
    }

    fun confirmHistoricalImport() {
        val current = mutableState.value
        if (current.loading || current.historicalImportPreview == null) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true) }
            val results = api.importHistoricalEntries(current.historicalImportEntries)
            mutableState.update {
                it.copy(
                    loading = false,
                    sharedWriteResult = results.firstOrNull {
                        it.status == no.timnor.app.core.repository.SharedWriteStatus.UNKNOWN ||
                            it.status == no.timnor.app.core.repository.SharedWriteStatus.PENDING
                    } ?: results.lastOrNull() ?: it.sharedWriteResult,
                    historicalImportPreview = null,
                    historicalImportEntries = emptyList(),
                )
            }
            refreshEntries()
        }
    }

    fun signIn() {
        val current = mutableState.value
        if (current.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null, projectError = false) }
            when (val result = api.signIn(current.identifier, current.password)) {
                is PlannorConnectionResult.Connected -> {
                    mutableState.update { it.copy(session = result.session, password = "", loading = false) }
                    requestSync()
                    refreshProjects()
                }
                is PlannorConnectionResult.Failed -> mutableState.update {
                    it.copy(error = result.reason, loading = false)
                }
            }
        }
    }

    fun refreshProjects() {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, projectError = false) }
            try {
                api.loadAuthorizedProjects()
                val entries = api.loadPersonalEntries()
                loadCachedProjects()
                mutableState.update { current ->
                    val selectedEntryId = current.existingEntryId?.takeIf { id -> entries.any { it.serverId == id } }
                        ?: entries.firstOrNull { it.remainingSeconds > 0 }?.serverId
                    current.copy(personalEntries = entries, existingEntryId = selectedEntryId, loading = false)
                }
                mutableState.value.sharedProjectId?.let(::refreshTasks)
                refreshProjectSnapshots()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, projectError = true) }
            }
        }
    }

    fun selectProject(projectId: Int, selected: Boolean) {
        viewModelScope.launch {
            val updated = api.setProjectSelected(projectId, selected)
            if (updated) loadCachedProjects() else mutableState.update { it.copy(projectError = true) }
        }
    }

    fun linkProject(projectId: Int, localProjectId: Int?) {
        if (localProjectId != null && mutableState.value.personalProjects.none { it.id == localProjectId }) return
        viewModelScope.launch {
            if (!api.setProjectLink(projectId, localProjectId)) {
                mutableState.update { it.copy(projectError = true) }
                return@launch
            }
            loadCachedProjects()
        }
    }

    fun addProjectToTimnor(projectId: Int) {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, projectError = false) }
            try {
                val created = createLinkedProject(projectId)
                loadCachedProjects()
                mutableState.update { it.copy(loading = false, projectError = !created) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, projectError = true) }
            }
        }
    }

    fun signOut() {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            if (!api.signOut()) {
                mutableState.update { it.copy(loading = false, error = PlannorConnectionFailure.NETWORK) }
                return@launch
            }
            mutableState.value = PlannorConnectionUiState()
        }
    }

    private suspend fun loadCachedProjects() {
        val rows = api.selectedAndCachedProjects()
        val personalProjects = persistence.snapshotAll().projects
        mutableState.update {
            val availableShared = rows.filter { project -> project.selected && !project.archived }
            val selectedId = it.sharedProjectId?.takeIf { id -> availableShared.any { project -> project.projectId == id } }
                ?: availableShared.firstOrNull()?.projectId
            it.copy(projects = rows, personalProjects = personalProjects, sharedProjectId = selectedId)
        }
    }

    private suspend fun retryAtConnection() {
        try {
            val accountId = mutableState.value.session?.accountId ?: return
            val result = syncSharedWork()
            mutableState.update { current ->
                if (current.session?.accountId != accountId) current
                else current.copy(
                    sharedWriteResult = result.writes.firstOrNull {
                        it.status != no.timnor.app.core.repository.SharedWriteStatus.SYNCED
                    } ?: result.writes.lastOrNull() ?: current.sharedWriteResult,
                    sharedWorkNeedsReview = result.needsReview,
                )
            }
            refreshSharedWorkReviews()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            mutableState.update { it.copy(projectError = true) }
        }
    }

    private suspend fun refreshSharedWorkReviews() {
        val reviews = loadSharedWorkReviews()
        mutableState.update { current ->
            if (current.session == null) current else current.copy(sharedWorkReviews = reviews)
        }
    }

    private fun refreshTasks(projectId: Int) {
        viewModelScope.launch {
            try {
                val tasks = api.loadProjectTasks(projectId)
                mutableState.update { current ->
                    if (current.sharedProjectId != projectId) current
                    else current.copy(
                        tasks = tasks,
                        sharedTaskId = current.sharedTaskId?.takeIf { taskId -> tasks.any { it.id == taskId } },
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { current ->
                    if (current.sharedProjectId == projectId) current.copy(tasks = emptyList(), sharedTaskId = null)
                    else current
                }
            }
        }
    }

    private fun sourceId(accountId: Int, localWorkId: Int): String =
        UUID.nameUUIDFromBytes("time-manager:$accountId:work:$localWorkId".toByteArray()).toString()
}
