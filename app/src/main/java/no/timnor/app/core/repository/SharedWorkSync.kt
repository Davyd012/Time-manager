package no.timnor.app.core.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

data class SharedWorkSyncResult(val writes: List<SharedWriteResult>, val needsReview: Int)

/** One sync at a time; requests are prepared before the existing idempotent outbox is retried. */
class SharedWorkSync(private val journal: SharedWorkJournal, private val api: PlannorApi) {
    private val mutex = Mutex()

    suspend fun sync(): SharedWorkSyncResult = mutex.withLock {
        journal.prepareCreates()
        val writes = api.retryPending().toMutableList()
        repeat(8) {
            try {
                // Pull on every sync, even when the local outbox is empty. A second device,
                // project-access change, or remote deletion may be the only new event.
                val allocations = api.refreshProjectSnapshots()
                val entries = api.loadPersonalEntries()
                journal.recordInitialBaselines(entries, allocations)
                if (journal.prepareReconciliations(entries, allocations) == 0) {
                    return@withLock SharedWorkSyncResult(writes, journal.needsReviewCount())
                }
                writes += api.retryPending()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Keep the local intent and review state until the server can be checked.
                return@withLock SharedWorkSyncResult(writes, journal.needsReviewCount())
            }
        }
        SharedWorkSyncResult(writes, journal.needsReviewCount())
    }
}
