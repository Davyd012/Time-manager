package no.timnor.app.shared.features.shared_hours.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.timnor.app.core.repository.PlannorApi
import no.timnor.app.core.repository.PlannorConnectionFailure
import no.timnor.app.core.repository.PlannorConnectionResult
import no.timnor.app.core.repository.PlannorSession
import no.timnor.app.core.repository.SharedWorkSyncCoordinator
import no.timnor.app.core.repository.SharedWorkReviewSummary
import no.timnor.app.core.repository.SharedWorkSyncStatus
import no.timnor.app.core.repository.SharedWorkResolution
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.TimnorPersistence

data class PlannorAccountUiState(
    val session: PlannorSession? = null,
    val identifier: String = "",
    val password: String = "",
    val projects: List<PlannorProjectSelection> = emptyList(),
    val localProjects: List<StoredProject> = emptyList(),
    val syncStatus: SharedWorkSyncStatus? = null,
    val reviewItems: List<SharedWorkReviewSummary> = emptyList(),
    val syncing: Boolean = false,
    val lastSyncUtc: String? = null,
    val loading: Boolean = false,
    val error: PlannorConnectionFailure? = null,
)

/** Shared account and project-linking flow used by the native iOS host. */
class PlannorAccountViewModel(
    private val api: PlannorApi,
    private val persistence: TimnorPersistence,
    private val syncSharedWork: suspend () -> SharedWorkSyncStatus = { SharedWorkSyncStatus() },
    private val loadSharedWorkReviews: suspend () -> List<SharedWorkReviewSummary> = { emptyList() },
    private val keepSharedWorkLocalOnly: suspend (Int) -> Boolean = { false },
    private val resolveSharedWorkIntent: suspend (Int, SharedWorkResolution) -> Boolean = { _, _ -> false },
    private val onProjectsChanged: () -> Unit = {},
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlannorAccountUiState())
    val state: StateFlow<PlannorAccountUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val projects = persistence.snapshotAll().projects
            val session = api.currentSession()
            mutableState.update { it.copy(localProjects = projects, session = session) }
            if (session != null) {
                refreshProjects()
                retrySync()
            }
        }
    }

    fun setIdentifier(value: String) = mutableState.update { it.copy(identifier = value, error = null) }
    fun setPassword(value: String) = mutableState.update { it.copy(password = value, error = null) }

    fun signIn() {
        val current = mutableState.value
        if (current.loading || current.identifier.isBlank() || current.password.isEmpty()) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            when (val result = api.signIn(current.identifier, current.password)) {
                is PlannorConnectionResult.Connected -> {
                    mutableState.update { it.copy(session = result.session, password = "", loading = false) }
                    refreshProjects()
                    retrySync()
                }
                is PlannorConnectionResult.Failed -> mutableState.update {
                    it.copy(loading = false, error = result.reason)
                }
            }
        }
    }

    fun refreshProjects() {
        if (mutableState.value.loading) return
        val accountId = mutableState.value.session?.accountId ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                api.loadAuthorizedProjects()
                val projects = api.selectedAndCachedProjects().filter { it.accountId == accountId }
                val localProjects = persistence.snapshotAll().projects
                mutableState.update { current ->
                    if (current.session?.accountId != accountId) current
                    else current.copy(projects = projects, localProjects = localProjects, loading = false)
                }
                onProjectsChanged()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { current ->
                    if (current.session?.accountId != accountId) current
                    else current.copy(loading = false, error = PlannorConnectionFailure.NETWORK)
                }
            }
        }
    }

    fun retrySync() = retrySync(forcePendingWrites = false)

    fun retrySyncNow() = retrySync(forcePendingWrites = true)

    private fun retrySync(forcePendingWrites: Boolean) {
        val accountId = mutableState.value.session?.accountId ?: return
        if (mutableState.value.syncing) return
        viewModelScope.launch {
            mutableState.update { it.copy(syncing = true, syncStatus = null) }
            val result = try {
                if (forcePendingWrites) api.retryPending(forceRetry = true)
                syncSharedWork()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                SharedWorkSyncStatus(failed = true)
            }
            val reviews = runCatching { loadSharedWorkReviews() }.getOrDefault(emptyList())
            mutableState.update { current ->
                if (current.session?.accountId != accountId) current
                else current.copy(
                    syncing = false,
                    syncStatus = result,
                    reviewItems = reviews,
                    lastSyncUtc = if (result.failed) current.lastSyncUtc else kotlin.time.Clock.System.now().toString(),
                )
            }
        }
    }

    fun keepLocalOnly(localWorkId: Int) {
        val accountId = mutableState.value.session?.accountId ?: return
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true) }
            val kept = runCatching { keepSharedWorkLocalOnly(localWorkId) }.getOrDefault(false)
            if (mutableState.value.session?.accountId != accountId) return@launch
            mutableState.update { it.copy(loading = false) }
            if (kept) retrySync()
        }
    }

    fun resolveSharedWork(localWorkId: Int, resolution: SharedWorkResolution) {
        val accountId = mutableState.value.session?.accountId ?: return
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true) }
            val resolved = runCatching { resolveSharedWorkIntent(localWorkId, resolution) }.getOrDefault(false)
            if (mutableState.value.session?.accountId != accountId) return@launch
            mutableState.update { it.copy(loading = false) }
            if (resolved) retrySync()
        }
    }


    fun setProjectSelected(projectId: Int, selected: Boolean) {
        val accountId = mutableState.value.session?.accountId ?: return
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            val saved = runCatching { api.setProjectSelected(projectId, selected) }.getOrDefault(false)
            if (!saved || mutableState.value.session?.accountId != accountId) {
                mutableState.update { it.copy(loading = false, error = PlannorConnectionFailure.NETWORK) }
            } else {
                val projects = api.selectedAndCachedProjects().filter { it.accountId == accountId }
                mutableState.update { current ->
                    if (current.session?.accountId != accountId) current
                    else current.copy(projects = projects, loading = false)
                }
                onProjectsChanged()
            }
        }
    }

    fun linkProject(projectId: Int, localProjectId: Int?) {
        val current = mutableState.value
        if (current.loading || (localProjectId != null && current.localProjects.none { it.id == localProjectId })) return
        val accountId = current.session?.accountId ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            val saved = runCatching { api.setProjectLink(projectId, localProjectId) }.getOrDefault(false)
            if (!saved || mutableState.value.session?.accountId != accountId) {
                mutableState.update { it.copy(loading = false, error = PlannorConnectionFailure.NETWORK) }
            } else {
                val projects = api.selectedAndCachedProjects().filter { it.accountId == accountId }
                mutableState.update { currentState ->
                    if (currentState.session?.accountId != accountId) currentState
                    else currentState.copy(projects = projects, loading = false)
                }
                onProjectsChanged()
            }
        }
    }

    fun addProjectToTimnor(projectId: Int) {
        val current = mutableState.value
        val linked = current.projects.singleOrNull { it.projectId == projectId } ?: return
        if (current.loading || !linked.selected || linked.archived) return
        val accountId = current.session?.accountId ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val created = persistence.createProject(StoredProject(
                    description = "",
                    name = linked.name,
                    id = 0,
                ))
                if (mutableState.value.session?.accountId != accountId) {
                    persistence.deleteProject(created)
                    mutableState.update { it.copy(loading = false) }
                    return@launch
                }
                val saved = api.setProjectLink(projectId, created.id)
                if (!saved) persistence.deleteProject(created)
                val projects = api.selectedAndCachedProjects().filter { it.accountId == accountId }
                val localProjects = persistence.snapshotAll().projects
                mutableState.update { currentState ->
                    if (currentState.session?.accountId != accountId) currentState
                    else currentState.copy(
                        projects = projects,
                        localProjects = localProjects,
                        loading = false,
                        error = if (saved) null else PlannorConnectionFailure.NETWORK,
                    )
                }
                onProjectsChanged()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, error = PlannorConnectionFailure.NETWORK) }
            }
        }
    }

    fun signOut() {
        val current = mutableState.value
        if (current.session == null) return
        if (current.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            val succeeded = runCatching { api.signOut() }.getOrDefault(false)
            if (!succeeded) {
                mutableState.update { it.copy(loading = false, error = PlannorConnectionFailure.NETWORK) }
                return@launch
            }
            mutableState.value = PlannorAccountUiState(localProjects = persistence.snapshotAll().projects)
            onProjectsChanged()
        }
    }
}
