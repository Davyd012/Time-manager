package no.timnor.app.core.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOperation
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation

class SharedWorkReconciliationTest {
    private val intent = SharedWorkIntent(
        accountId = 4, localWorkId = 12, sourceId = "source", createKey = "create",
        projectId = 20, localProjectId = 8, localVersion = 2,
        workDate = "2026-11-12", durationSeconds = 3_600, privateNote = "private", taskId = 31,
    )
    private val baseline = SharedWorkBaseline(
        entryId = "entry", entryRevision = 2, allocationId = "allocation", allocationRevision = 1,
        entryDurationSeconds = 7_200, allocationDurationSeconds = 3_600,
        allocationTaskId = 31, allocationTracksEntry = false,
        entryWorkDate = "2026-11-12", entryPrivateNote = "private", allocationProjectId = 20,
    )
    private val remote = SharedWorkRemoteState(
        entryId = "entry", entryRevision = 2, durationSeconds = 7_200, workDate = "2026-11-12",
        privateNote = "private", allocatedSeconds = 5_400, allocationId = "allocation",
        allocationRevision = 1, allocationProjectId = 20, allocationTaskId = 31,
        allocationSeconds = 3_600, allocationCount = 2, allocationRowsTotalSeconds = 5_400,
    )

    @Test
    fun additionalAllocationsArePreservedAndReduceAvailableCapacity() {
        assertEquals(
            SharedWorkReconciliation.UpdateAllocation(1_800, 31),
            planSharedWorkReconciliation(intent, baseline, remote, 8),
        )
    }

    @Test
    fun createdAllocationIsMatchedByStableIdAcrossPartialMultiProjectEntry() {
        val allocations = listOf(
            ProjectTimeAllocation(4, "allocation-a", "entry", 20, 31, "2026-11-12", 900, "task note", 2),
            ProjectTimeAllocation(4, "allocation-b", "entry", 21, null, "2026-11-12", 2_700, null, 1),
        )

        assertEquals("allocation-a", selectCreatedAllocation(
            allocations, "allocation-a", 20, 31, "2026-11-12", 900, "task note",
        )?.allocationId)
        assertEquals("allocation-a", selectCreatedAllocation(
            allocations, null, 20, 31, "2026-11-12", 900, "task note",
        )?.allocationId)
    }

    @Test
    fun ambiguousLegacyCreateMatchRequiresReviewInsteadOfChoosingWrongAllocation() {
        val duplicateRows = listOf(
            ProjectTimeAllocation(4, "allocation-a", "entry", 20, 31, "2026-11-12", 900, null, 1),
            ProjectTimeAllocation(4, "allocation-b", "entry", 20, 31, "2026-11-12", 900, null, 1),
        )

        assertEquals(null, selectCreatedAllocation(
            duplicateRows, null, 20, 31, "2026-11-12", 900, null,
        ))
    }

    @Test
    fun projectMoveDeletesOwnedAllocationThenCreatesOnlyWhatFits() {
        val moved = intent.copy(projectId = 21, durationSeconds = 1_800)
        assertEquals(SharedWorkReconciliation.DeleteAllocation,
            planSharedWorkReconciliation(moved, baseline, remote, 8))
        val afterDelete = baseline.copy(entryRevision = 3, allocationId = null, allocationRevision = null)
        val deletedRemote = remote.copy(
            entryRevision = 3,
            allocatedSeconds = 1_800,
            allocationId = null,
            allocationRevision = null,
            allocationProjectId = null,
            allocationTaskId = null,
            allocationSeconds = null,
            allocationCount = 1,
            allocationRowsTotalSeconds = 1_800,
        )
        assertEquals(SharedWorkReconciliation.CreateAllocation(1_800, 31),
            planSharedWorkReconciliation(moved, afterDelete, deletedRemote, 8))
    }

    @Test
    fun deletionRequiresApprovalAndLeavesOtherAllocationsOutOfScope() {
        assertEquals(SharedWorkReconciliation.Conflict,
            planSharedWorkReconciliation(intent.copy(deleted = true), baseline, remote, 8))
        assertEquals(SharedWorkReconciliation.DeleteAllocation,
            planSharedWorkReconciliation(intent.copy(deleted = true, deletionApproved = true), baseline, remote, 8))
    }

    @Test
    fun remotelyDeletedManagedAllocationCanBeRecreatedWithoutTouchingOtherAllocations() {
        val deletedManaged = baseline.copy(allocationId = null, allocationRevision = null)
        val remoteWithoutManaged = remote.copy(
            allocatedSeconds = 1_800,
            allocationId = null,
            allocationRevision = null,
            allocationProjectId = null,
            allocationTaskId = null,
            allocationSeconds = null,
            allocationCount = 1,
            allocationRowsTotalSeconds = 1_800,
        )
        assertEquals(
            SharedWorkReconciliation.CreateAllocation(3_600, 31),
            planSharedWorkReconciliation(intent, deletedManaged, remoteWithoutManaged, 8),
        )
    }

    @Test
    fun simultaneousRemoteRevisionChangeStopsAStaleLocalWrite() {
        val newerRemoteRevision = remote.copy(entryRevision = remote.entryRevision + 1)

        assertEquals(
            SharedWorkReconciliation.Conflict,
            planSharedWorkReconciliation(intent, baseline, newerRemoteRevision, 8),
        )
    }

    @Test
    fun remoteEditToAnotherAllocationRequiresReview() {
        val changedOthers = remote.copy(
            entryRevision = remote.entryRevision + 1,
            allocatedSeconds = 6_000,
            allocationRowsTotalSeconds = 6_000,
            allocationCount = 3,
        )

        assertEquals(
            SharedWorkReviewReasons.REMOTE_ALLOCATIONS_CHANGED,
            sharedWorkRemoteChangeReason(baseline, changedOthers, expectedProjectId = 20),
        )
    }

    @Test
    fun unknownOutcomesStayPendingAndAccessRevocationNeedsAttention() {
        val pendingIntent = intent.copy(createKey = "stable-key", queuedVersion = 1)
        val pending = SharedTimeOutboxItem(
            accountId = 4, operation = SharedTimeOperation.CREATE_AND_ALLOCATE,
            outcome = SharedTimeOutcome.OUTCOME_UNKNOWN, projectId = 20,
            workDate = "2026-11-12", durationSeconds = 3_600, atomicIdempotencyKey = "stable-key",
        )
        assertEquals(
            SharedWorkSyncIndicator.PENDING,
            sharedWorkIndicators(listOf(pendingIntent), emptySet(), mapOf(4 to "stable-key" to pending), 4)[12],
        )
        val revoked = pending.copy(outcome = SharedTimeOutcome.ACCESS_REVOKED)
        assertEquals(
            SharedWorkSyncIndicator.ACCESS_REMOVED,
            sharedWorkIndicators(listOf(pendingIntent), emptySet(), mapOf(4 to "stable-key" to revoked), 4)[12],
        )
    }

    @Test
    fun previousAccountsPendingWorkRequiresItsOriginalAccount() {
        val pendingIntent = intent.copy(createKey = "stable-key", queuedVersion = 1)
        val revoked = SharedTimeOutboxItem(
            accountId = 4, operation = SharedTimeOperation.CREATE_AND_ALLOCATE,
            outcome = SharedTimeOutcome.ACCESS_REVOKED, projectId = 20,
            workDate = "2026-11-12", durationSeconds = 3_600, atomicIdempotencyKey = "stable-key",
        )

        assertEquals(
            SharedWorkSyncIndicator.ACCOUNT_REQUIRED,
            sharedWorkIndicators(listOf(pendingIntent), emptySet(), mapOf(4 to "stable-key" to revoked), 5)[12],
        )
    }

    @Test
    fun otherAccountEditRequiresReviewOnlyWhenOriginalAccountIsActive() {
        val changedElsewhere = intent.copy(reviewReason = SharedWorkSyncCoordinator.REVIEW_LOCAL_CHANGE_OTHER_ACCOUNT)

        assertEquals(
            SharedWorkSyncIndicator.ACCOUNT_REQUIRED,
            sharedWorkIndicators(listOf(changedElsewhere), setOf(4 to "12"), emptyMap(), 5)[12],
        )
        assertEquals(
            SharedWorkSyncIndicator.NEEDS_REVIEW,
            sharedWorkIndicators(listOf(changedElsewhere), setOf(4 to "12"), emptyMap(), 4)[12],
        )
    }
}
