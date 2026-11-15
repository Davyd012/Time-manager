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

data class LocalTimeCandidate(
    val workId: Int,
    val sourceId: String?,
    val workDate: String,
    val durationSeconds: Long,
    val projectName: String?,
)

data class PlannorConnectionUiState(
    val session: PlannorSession? = null,
    val identifier: String = "",
    val password: String = "",
    val projectSearch: String = "",
    val projectDraftSelection: Set<Int>? = null,
    val projectListIndex: Int = 0,
    val projectListOffset: Int = 0,
    val entryListIndex: Int = 0,
    val entryListOffset: Int = 0,
    val allocationListIndex: Int = 0,
    val allocationListOffset: Int = 0,
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
    val localProjectNamesBySource: Map<String, String> = emptyMap(),
    val localTimeCandidates: List<LocalTimeCandidate> = emptyList(),
    val personalEntries: List<PersonalTimeEntry> = emptyList(),
    val projectAllocations: List<ProjectTimeAllocation> = emptyList(),
    val pendingAllocationEntryIds: Set<String> = emptySet(),
    val pendingAllocationIds: Set<String> = emptySet(),
    val pendingImportSourceIds: Set<String> = emptySet(),
    val pendingDirectCreate: Boolean = false,
    val existingEntryId: String? = null,
    val selectedAllocationId: String? = null,
    val allocationCorrectionSeconds: Long = 0,
    val confirmAllocationDeletion: Boolean = false,
    val entryCorrectionDate: String = "",
    val entryCorrectionSeconds: String = "",
    val confirmEntryDeletion: Boolean = false,
    val historicalImportPreview: LocalEntryImportPreview? = null,
    val historicalImportEntries: List<HistoricalTimeEntry> = emptyList(),
    val loading: Boolean = false,
    val error: PlannorConnectionFailure? = null,
    val projectError: Boolean = false,
    val projectSaveError: Boolean = false,
    val entryError: Boolean = false,
    val allocationError: Boolean = false,
)

class PlannorConnectionViewModel(
    private val api: PlannorApi,
    private val persistence: TimnorPersistence,
    private val createLinkedProject: suspend (Int) -> Boolean,
    private val syncSharedWork: suspend () -> SharedWorkSyncResult,
    private val loadSharedWorkReviews: suspend () -> List<SharedWorkReviewItem>,
    private val resolveSharedWork: suspend (Int, SharedWorkResolution) -> Boolean,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlannorConnectionUiState(loading = true))
    val state: StateFlow<PlannorConnectionUiState> = mutableState.asStateFlow()
    private val syncRequests = Channel<Unit>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            for (request in syncRequests) {
                if (mutableState.value.session != null) retryAtConnection()
            }
        }
        viewModelScope.launch {
            try {
            val session = api.currentSession()
            mutableState.update { it.copy(session = session) }
            loadCachedProjects()
            if (session != null) {
                val cachedEntries = api.cachedPersonalEntries()
                val cachedAllocations = api.cachedProjectAllocations()
                val pending = api.pendingAllocationEntryIds()
                val pendingAllocations = api.pendingAllocationIds()
                val pendingImports = api.pendingImportSourceIds()
                val pendingDirect = api.hasPendingDirectCreate()
                mutableState.update { it.copy(personalEntries = cachedEntries, projectAllocations = cachedAllocations, pendingAllocationEntryIds = pending, pendingAllocationIds = pendingAllocations, pendingImportSourceIds = pendingImports, pendingDirectCreate = pendingDirect, loading = false) }
                requestSync()
                refreshProjects()
            } else {
                mutableState.update { it.copy(loading = false) }
            }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, error = PlannorConnectionFailure.NETWORK) }
            }
        }
    }

    fun setIdentifier(value: String) = mutableState.update { it.copy(identifier = value, error = null) }
    fun setPassword(value: String) = mutableState.update { it.copy(password = value, error = null) }
    fun setProjectSearch(value: String) = mutableState.update { it.copy(projectSearch = value) }
    fun setProjectDraft(projectId: Int, selected: Boolean) = mutableState.update { current ->
        val project = current.projects.firstOrNull { it.projectId == projectId }
        if (current.loading || project == null || project.archived || !project.available) return@update current
        val saved = current.projects.filter { it.selected }.map { it.projectId }.toSet()
        val draft = current.projectDraftSelection ?: saved
        val next = if (selected) draft + projectId else draft - projectId
        current.copy(projectDraftSelection = next.takeUnless { it == saved }, projectSaveError = false)
    }

    fun saveProjectDraft() {
        val current = mutableState.value
        val draft = current.projectDraftSelection ?: return
        if (current.loading) return
        mutableState.update { it.copy(loading = true, projectSaveError = false) }
        viewModelScope.launch {
            try {
                val changed = current.projects.filter { it.selected != (it.projectId in draft) }
                val saved = changed.all { api.setProjectSelected(it.projectId, it.projectId in draft) }
                loadCachedProjects()
                mutableState.update { it.copy(loading = false, projectSaveError = !saved, projectDraftSelection = if (saved) null else draft) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                try {
                    loadCachedProjects()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    // Keep the last visible selection if the local cache cannot be reread.
                }
                mutableState.update { it.copy(loading = false, projectSaveError = true, projectDraftSelection = draft) }
            }
        }
    }
    fun rememberProjectScroll(index: Int, offset: Int) = mutableState.update { it.copy(projectListIndex = index, projectListOffset = offset) }
    fun rememberEntryScroll(index: Int, offset: Int) = mutableState.update { it.copy(entryListIndex = index, entryListOffset = offset) }
    fun rememberAllocationScroll(index: Int, offset: Int) = mutableState.update { it.copy(allocationListIndex = index, allocationListOffset = offset) }
    fun clearSharedWriteResult() = mutableState.update { it.copy(sharedWriteResult = null) }
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
    fun selectAllocation(allocationId: String) {
        val allocation = mutableState.value.projectAllocations.firstOrNull { it.allocationId == allocationId } ?: return
        mutableState.update {
            it.copy(selectedAllocationId = allocationId, sharedProjectId = allocation.projectId,
                sharedTaskId = allocation.taskId, sharedMinutes = (allocation.durationSeconds / 60).toString(),
                allocationCorrectionSeconds = allocation.durationSeconds,
                sharedNote = allocation.sharedNote.orEmpty(), tasks = emptyList(), sharedWriteResult = null)
        }
        refreshTasks(allocation.projectId)
    }
    fun setEntryCorrectionDate(value: String) = mutableState.update { it.copy(entryCorrectionDate = value) }
    fun setEntryCorrectionSeconds(value: String) = mutableState.update {
        it.copy(entryCorrectionSeconds = value.filter(Char::isDigit).take(10))
    }
    fun setSharedMinutes(value: String) = mutableState.update {
        val selected = value.filter(Char::isDigit).take(4).toIntOrNull()?.coerceAtMost(1439)?.toString().orEmpty()
        it.copy(sharedMinutes = selected, sharedWriteResult = null)
    }
    fun setAllocationSeconds(value: Long) = mutableState.update { it.copy(allocationCorrectionSeconds = value, sharedWriteResult = null) }
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
        if (current.loading || current.pendingDirectCreate) return
        mutableState.update { it.copy(loading = true, sharedWriteResult = null) }
        viewModelScope.launch {
            try {
            val result = api.createAndAllocate(
                projectId = projectId,
                taskId = current.sharedTaskId,
                workDate = current.sharedWorkDate,
                durationSeconds = seconds,
                privateNote = current.privateNote.ifBlank { null },
                sharedNote = current.sharedNote.ifBlank { null },
            )
            val pending = api.hasPendingDirectCreate()
            mutableState.update { it.copy(loading = false, sharedWriteResult = result, pendingDirectCreate = pending) }
            refreshEntries()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, pendingDirectCreate = true, entryError = true) }
            }
        }
    }

    fun allocateExistingTime() {
        val current = mutableState.value
        val entry = current.personalEntries.firstOrNull { it.serverId == current.existingEntryId } ?: return
        val projectId = current.sharedProjectId
            ?: current.projects.firstOrNull { it.selected && !it.archived }?.projectId
            ?: return
        val seconds = current.sharedMinutes.toLongOrNull()?.takeIf { it > 0 }?.times(60) ?: return
        if (!entry.canAllocate(seconds) || current.loading || entry.serverId in current.pendingAllocationEntryIds) return
        mutableState.update { it.copy(loading = true, sharedWriteResult = null) }
        viewModelScope.launch {
            try {
            val result = api.allocateExisting(
                entry = entry,
                projectId = projectId,
                taskId = current.sharedTaskId,
                durationSeconds = seconds,
                sharedNote = current.sharedNote.ifBlank { null },
            )
            val pending = api.pendingAllocationEntryIds()
            val cachedEntries = api.cachedPersonalEntries()
            mutableState.update { it.copy(loading = false, sharedWriteResult = result, pendingAllocationEntryIds = pending, personalEntries = cachedEntries) }
            refreshEntries()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, entryError = true, pendingAllocationEntryIds = it.pendingAllocationEntryIds + entry.serverId) }
            }
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
        mutableState.update { it.copy(loading = true, allocationError = false) }
        viewModelScope.launch {
            try {
                val allocations = api.refreshProjectSnapshots()
                val entries = api.cachedPersonalEntries()
                mutableState.update { current ->
                    val selected = current.selectedAllocationId?.takeIf { id -> allocations.any { it.allocationId == id } }
                        ?: allocations.firstOrNull()?.allocationId
                    current.copy(projectAllocations = allocations, personalEntries = entries, selectedAllocationId = selected, loading = false)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, allocationError = true) }
            }
        }
    }

    fun correctSelectedAllocation() {
        val current = mutableState.value
        val allocation = current.projectAllocations.firstOrNull { it.allocationId == current.selectedAllocationId } ?: return
        val entry = current.personalEntries.firstOrNull { it.serverId == allocation.entryId } ?: return
        val seconds = current.allocationCorrectionSeconds.takeIf { it > 0 && it <= entry.remainingSeconds + allocation.durationSeconds } ?: return
        if (current.loading || allocation.allocationId in current.pendingAllocationIds) return
        mutableState.update { it.copy(loading = true, sharedWriteResult = null) }
        viewModelScope.launch {
            try {
            val result = api.correctAllocation(
                allocation = allocation,
                entryRevision = entry.revision,
                durationSeconds = seconds,
                taskId = current.sharedTaskId,
                sharedNote = current.sharedNote.ifBlank { null },
            )
            val pending = api.pendingAllocationIds()
            mutableState.update { it.copy(loading = false, sharedWriteResult = result, pendingAllocationIds = pending) }
            refreshEntries()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, allocationError = true, pendingAllocationIds = it.pendingAllocationIds + allocation.allocationId) }
            }
        }
    }

    fun requestDeleteSelectedAllocation() = mutableState.update { it.copy(confirmAllocationDeletion = true) }
    fun cancelDeleteSelectedAllocation() = mutableState.update { it.copy(confirmAllocationDeletion = false) }

    fun confirmDeleteSelectedAllocation() {
        val current = mutableState.value
        val allocation = current.projectAllocations.firstOrNull { it.allocationId == current.selectedAllocationId } ?: return
        val entry = current.personalEntries.firstOrNull { it.serverId == allocation.entryId } ?: return
        if (current.loading || allocation.allocationId in current.pendingAllocationIds) return
        mutableState.update { it.copy(loading = true, confirmAllocationDeletion = false, sharedWriteResult = null) }
        viewModelScope.launch {
            try {
            val result = api.deleteAllocation(allocation, entry.revision)
            val pending = api.pendingAllocationIds()
            mutableState.update { it.copy(loading = false, sharedWriteResult = result, pendingAllocationIds = pending) }
            refreshEntries()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, allocationError = true, pendingAllocationIds = it.pendingAllocationIds + allocation.allocationId) }
            }
        }
    }

    fun refreshEntries() {
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, entryError = false) }
        viewModelScope.launch {
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
                mutableState.update { it.copy(loading = false, entryError = true, allocationError = true) }
            }
        }
    }

    fun retryPendingWrites() {
        if (mutableState.value.loading || mutableState.value.session == null) return
        mutableState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val accountId = mutableState.value.session?.accountId ?: run {
                mutableState.update { it.copy(loading = false) }
                return@launch
            }
            try {
                val results = api.retryPending(forceRetry = true)
                val pending = api.pendingAllocationEntryIds()
                val pendingAllocations = api.pendingAllocationIds()
                val pendingImports = api.pendingImportSourceIds()
                val pendingDirect = api.hasPendingDirectCreate()
                mutableState.update { current ->
                    val matching = current.sharedWriteResult?.commandId?.let { id -> results.firstOrNull { it.commandId == id } }
                    val result = matching ?: results.firstOrNull { it.status != no.timnor.app.core.repository.SharedWriteStatus.SYNCED }
                        ?: results.lastOrNull() ?: current.sharedWriteResult
                    current.copy(pendingAllocationEntryIds = pending, pendingAllocationIds = pendingAllocations,
                        pendingImportSourceIds = pendingImports, pendingDirectCreate = pendingDirect, sharedWriteResult = result)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(entryError = true, allocationError = true) }
            }
            mutableState.update { it.copy(loading = false) }
            if (mutableState.value.session?.accountId == accountId) requestSync()
            if (mutableState.value.session?.accountId == accountId) refreshEntries()
        }
    }

    fun resolveSharedWork(localWorkId: Int, resolution: SharedWorkResolution) {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true) }
            try {
                val resolved = resolveSharedWork.invoke(localWorkId, resolution)
                mutableState.update { it.copy(loading = false, allocationError = !resolved) }
                if (resolved) {
                    requestSync()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, allocationError = true) }
            }
        }
    }

    fun requestSync() { syncRequests.trySend(Unit) }

    fun refreshLocalEntries() {
        viewModelScope.launch {
            try {
                loadCachedProjects()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(entryError = true) }
            }
        }
    }

    fun prepareLocalEntry(workId: Int) {
        val current = mutableState.value
        val accountId = current.session?.accountId ?: return
        val source = sourceId(accountId, workId)
        if (current.loading || source in current.pendingImportSourceIds) return
        mutableState.update { it.copy(loading = true, sharedWriteResult = null) }
        viewModelScope.launch {
            try {
                val work = persistence.snapshotAll().works.firstOrNull { it.id == workId && it.time in 1..315_360_000 }
                    ?: error("local entry no longer exists")
                val result = api.importHistoricalEntries(listOf(HistoricalTimeEntry(
                    sourceId = source,
                    workDate = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date.toString(),
                    durationSeconds = work.time.toLong(),
                )))
                val pending = api.pendingImportSourceIds()
                mutableState.update { it.copy(loading = false, pendingImportSourceIds = pending, sharedWriteResult = result.lastOrNull()) }
                refreshEntries()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, entryError = true,
                    pendingImportSourceIds = it.pendingImportSourceIds + source) }
            }
        }
    }

    fun reviewHistoricalImport() {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, historicalImportPreview = null) }
            try {
                val session = api.currentSession() ?: error("not connected")
                val localWorks = persistence.snapshotAll().works.filter { it.time in 1..315_360_000 }
                val remoteEntries = api.loadPersonalEntries()
                val sourceIds = localWorks.map { work -> sourceId(session.accountId, work.id) }
                val alreadyImported = remoteEntries.mapNotNull(PersonalTimeEntry::sourceWorkId).toSet() + api.pendingImportSourceIds()
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
                mutableState.update { it.copy(loading = false, entryError = true) }
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
            val pendingImports = api.pendingImportSourceIds()
            mutableState.update {
                it.copy(
                    loading = false,
                    pendingImportSourceIds = pendingImports,
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
                    val pending = api.pendingAllocationEntryIds()
                    val pendingAllocations = api.pendingAllocationIds()
                    val pendingImports = api.pendingImportSourceIds()
                    val pendingDirect = api.hasPendingDirectCreate()
                    mutableState.update { it.copy(session = result.session, password = "", loading = false, pendingAllocationEntryIds = pending, pendingAllocationIds = pendingAllocations, pendingImportSourceIds = pendingImports, pendingDirectCreate = pendingDirect) }
                    loadCachedProjects()
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
        mutableState.update { it.copy(loading = true, projectError = false) }
        viewModelScope.launch {
            try {
                api.loadAuthorizedProjects()
                loadCachedProjects()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(projectError = true) }
            }
            mutableState.update { it.copy(loading = false) }
            refreshEntries()
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
            mutableState.value = PlannorConnectionUiState(loading = false)
            loadCachedProjects()
        }
    }

    private suspend fun loadCachedProjects() {
        val accountId = mutableState.value.session?.accountId
        val rows = api.selectedAndCachedProjects().filter { it.accountId == accountId }
        val snapshot = persistence.snapshotAll()
        val personalProjects = snapshot.projects
        val names = personalProjects.associate { it.id to it.name }
        val projectNamesBySource = if (accountId == null) emptyMap() else snapshot.works.mapNotNull { work ->
            names[work.project]?.let { name -> sourceId(accountId, work.id) to name }
        }.toMap()
        val localCandidates = snapshot.works.filter { it.time in 1..315_360_000 }.map { work ->
            LocalTimeCandidate(work.id, accountId?.let { sourceId(it, work.id) },
                utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date.toString(), work.time.toLong(), names[work.project])
        }
        mutableState.update {
            if (it.session?.accountId != accountId) return@update it
            val availableShared = rows.filter { project -> project.selected && !project.archived }
            val selectedId = it.sharedProjectId?.takeIf { id -> availableShared.any { project -> project.projectId == id } }
                ?: availableShared.firstOrNull()?.projectId
            it.copy(projects = rows, personalProjects = personalProjects, sharedProjectId = selectedId,
                localProjectNamesBySource = projectNamesBySource)
                .copy(localTimeCandidates = localCandidates)
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
            mutableState.update { it.copy(allocationError = true) }
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
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { current ->
                    if (current.sharedProjectId == projectId) current.copy(tasks = emptyList())
                    else current
                }
            }
        }
    }

    private fun sourceId(accountId: Int, localWorkId: Int): String =
        UUID.nameUUIDFromBytes("time-manager:$accountId:work:$localWorkId".toByteArray()).toString()
}
