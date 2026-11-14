package no.timnor.app.core.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection

class SharedWorkReconciliationTest {
    private val intent = SharedWorkIntent(42, 3, "source", "key", 7, 8, 2, "2026-11-12", 3600, "private")
    private val baseline = SharedWorkBaseline("entry", 2, "allocation", 1, allocationProjectId = 7)
    private val remote = SharedWorkRemoteState("entry", 2, 3600, "2026-11-12", "private", 3600,
        "allocation", 1, 7, null, 3600, 1)

    @Test fun shrinkingUpdatesAllocationFirst() {
        assertEquals(SharedWorkReconciliation.UpdateAllocation(1800, null),
            planSharedWorkReconciliation(intent.copy(durationSeconds = 1800), baseline, remote, 8))
    }

    @Test fun increasingUpdatesEntryFirst() {
        assertEquals(SharedWorkReconciliation.UpdateEntry(5400, "2026-11-12", "private"),
            planSharedWorkReconciliation(intent.copy(durationSeconds = 5400), baseline, remote, 8))
    }

    @Test fun partialRemoteAllocationStaysPartialWhenEntryDurationChanges() {
        val partialBaseline = baseline.copy(entryDurationSeconds = 3600, allocationDurationSeconds = 1800)
        val partialRemote = remote.copy(allocatedSeconds = 1800, allocationSeconds = 1800)

        assertEquals(SharedWorkReconciliation.Current,
            planSharedWorkReconciliation(intent, partialBaseline, partialRemote, 8))
        assertEquals(SharedWorkReconciliation.UpdateEntry(5400, "2026-11-12", "private"),
            planSharedWorkReconciliation(intent.copy(durationSeconds = 5400), partialBaseline, partialRemote, 8))
    }

    @Test fun shrinkingBelowPartialAllocationReducesAllocationBeforeEntry() {
        val partialBaseline = baseline.copy(
            entryDurationSeconds = 3600,
            allocationDurationSeconds = 2700,
            allocationTracksEntry = false,
        )
        val partialRemote = remote.copy(allocatedSeconds = 2700, allocationSeconds = 2700)

        assertEquals(SharedWorkReconciliation.UpdateAllocation(1200, null),
            planSharedWorkReconciliation(intent.copy(durationSeconds = 1200), partialBaseline, partialRemote, 8))
    }

    @Test fun changingSelectedTaskUpdatesOnlyTheLinkedAllocation() {
        assertEquals(SharedWorkReconciliation.UpdateAllocation(3600, 91),
            planSharedWorkReconciliation(intent.copy(taskId = 91), baseline.copy(
                entryDurationSeconds = 3600,
                allocationDurationSeconds = 3600,
                allocationTaskId = null,
            ), remote, 8))
    }

    @Test fun deletionRemovesAllocationBeforeEntry() {
        assertEquals(SharedWorkReconciliation.DeleteAllocation,
            planSharedWorkReconciliation(intent.copy(deleted = true, deletionApproved = true), baseline, remote, 8))
        assertEquals(SharedWorkReconciliation.DeleteEntry, planSharedWorkReconciliation(
            intent.copy(deleted = true, deletionApproved = true), baseline.copy(allocationId = null, allocationRevision = null),
            remote.copy(allocationId = null, allocationRevision = null, allocationProjectId = null,
                allocationSeconds = null, allocatedSeconds = 0, allocationCount = 0), 8))
    }

    @Test fun remoteChangesAndExtraAllocationsRequireReview() {
        assertEquals(SharedWorkReconciliation.Conflict,
            planSharedWorkReconciliation(intent.copy(deleted = true), baseline, remote, 8))
        assertEquals(SharedWorkReconciliation.Conflict,
            planSharedWorkReconciliation(intent, baseline, remote.copy(entryRevision = 3), 8))
        assertEquals(SharedWorkReconciliation.Current,
            planSharedWorkReconciliation(intent, baseline, remote.copy(allocationCount = 2), 8))
        assertEquals(SharedWorkReconciliation.Conflict,
            planSharedWorkReconciliation(intent.copy(deleted = true), baseline, remote.copy(allocatedSeconds = 5400), 8))
        assertEquals(SharedWorkReconciliation.Conflict,
            planSharedWorkReconciliation(intent, baseline, remote, 9))
    }

    @Test fun remoteSnapshotReviewReasonDistinguishesEntryAndAllocationChanges() {
        val completeBaseline = baseline.copy(
            entryDurationSeconds = 3600,
            allocationDurationSeconds = 3600,
            allocationTaskId = null,
            allocationTracksEntry = true,
            entryWorkDate = "2026-11-12",
            entryPrivateNote = "private",
        )
        assertEquals(null, sharedWorkRemoteChangeReason(completeBaseline, remote, 7))
        assertEquals(SharedWorkJournal.REVIEW_REMOTE_ALLOCATIONS_CHANGED,
            sharedWorkRemoteChangeReason(completeBaseline, remote.copy(entryRevision = 3), 7))
        val multiBaseline = completeBaseline.copy(
            entryDurationSeconds = 7200,
            allocationDurationSeconds = 3600,
            allocationTracksEntry = false,
        )
        assertEquals(SharedWorkJournal.REVIEW_REMOTE_ALLOCATIONS_CHANGED,
            sharedWorkRemoteChangeReason(multiBaseline, remote.copy(
                entryRevision = 3,
                durationSeconds = 7200,
                allocatedSeconds = 5400,
                allocationCount = 2,
                allocationRowsTotalSeconds = 5400,
            ), 7))
    }

    @Test fun otherAllocationsArePreservedWhenManagedAllocationChanges() {
        val multi = remote.copy(
            entryRevision = 3,
            durationSeconds = 7200,
            allocatedSeconds = 5400,
            allocationCount = 2,
            allocationRowsTotalSeconds = 5400,
        )
        val expectedBaseline = baseline.copy(
            entryRevision = 3,
            entryDurationSeconds = 7200,
            allocationDurationSeconds = 3600,
            allocationTracksEntry = false,
        )
        assertEquals(SharedWorkReconciliation.UpdateAllocation(1800, null),
            planSharedWorkReconciliation(intent, expectedBaseline, multi, 8))
    }

    @Test fun remotelyChangedManagedAllocationRequiresReview() {
        val completeBaseline = baseline.copy(
            entryDurationSeconds = 3600,
            allocationDurationSeconds = 3600,
            allocationTaskId = null,
            allocationTracksEntry = true,
            entryWorkDate = "2026-11-12",
            entryPrivateNote = "private",
        )
        assertEquals(SharedWorkJournal.REVIEW_REMOTE_ALLOCATIONS_CHANGED,
            sharedWorkRemoteChangeReason(completeBaseline, remote.copy(
                allocationRevision = 2,
                allocationSeconds = 1800,
                allocatedSeconds = 1800,
                allocationRowsTotalSeconds = 1800,
            ), 7))
    }

    @Test fun projectMoveDeletesOldAllocationThenCreatesInNewProject() {
        val moving = intent.copy(projectId = 9)
        assertEquals(SharedWorkReconciliation.DeleteAllocation,
            planSharedWorkReconciliation(moving, baseline, remote, 8))
        val afterDelete = baseline.copy(
            entryRevision = 3,
            allocationId = null,
            allocationRevision = null,
            allocationDurationSeconds = 3600,
        )
        val remoteAfterDelete = remote.copy(
            entryRevision = 3,
            allocatedSeconds = 0,
            allocationId = null,
            allocationRevision = null,
            allocationProjectId = null,
            allocationTaskId = null,
            allocationSeconds = null,
            allocationCount = 0,
            allocationRowsTotalSeconds = 0,
        )
        assertEquals(SharedWorkReconciliation.CreateAllocation(3600, null),
            planSharedWorkReconciliation(moving, afterDelete, remoteAfterDelete, 8))
    }

    @Test fun unchangedLocalWorkAdoptsCompleteRemotePartialAllocationSplit() {
        val completeBaseline = baseline.copy(
            entryDurationSeconds = 7200,
            allocationDurationSeconds = 7200,
            allocationTracksEntry = true,
            entryWorkDate = "2026-11-12",
            entryPrivateNote = "private note",
            allocationProjectId = 7,
        )
        val ownerSplit = listOf(
            SharedWorkAllocationSnapshot("allocation", 2, 7, null, 5400, null),
            SharedWorkAllocationSnapshot("allocation-task", 1, 7, 91, 1800, "shared detail"),
        )
        val changedRemote = SharedWorkRemoteState(
            entryId = "entry",
            entryRevision = 3,
            durationSeconds = 7200,
            workDate = "2026-11-12",
            privateNote = "private note",
            allocatedSeconds = 7200,
            allocationId = "allocation",
            allocationRevision = 2,
            allocationProjectId = 7,
            allocationTaskId = null,
            allocationSeconds = 5400,
            allocationCount = 2,
            allocationRowsTotalSeconds = 7200,
            allocationSnapshots = ownerSplit,
        )
        val project = PlannorProjectSelection(
            accountId = 3, projectId = 7, name = "Project", selected = true,
            refreshedAtUtc = "2026-11-13T00:00:00Z", linkedLocalProjectId = 8,
        )

        val adopted = adoptRemoteAllocationSnapshot(intent.copy(
            projectId = 7, localProjectId = 8, queuedVersion = intent.localVersion,
        ), completeBaseline, changedRemote, project)

        assertNotNull(adopted)
        assertEquals(3, adopted!!.entryRevision)
        assertEquals(7200L, adopted.entryDurationSeconds)
        assertEquals("private note", adopted.entryPrivateNote)
        assertEquals(2, adopted.allocationSnapshots.size)
        assertEquals(7200L, adopted.allocationSnapshots.sumOf { it.durationSeconds })
    }

    @Test fun splitAdoptionRejectsMissingManagedAllocationAndChangedPrivateEntry() {
        val completeBaseline = baseline.copy(
            entryDurationSeconds = 3600,
            entryWorkDate = "2026-11-12",
            entryPrivateNote = "private",
            allocationProjectId = 7,
        )
        val project = PlannorProjectSelection(
            accountId = 3, projectId = 7, name = "Project", selected = true,
            refreshedAtUtc = "2026-11-13T00:00:00Z", linkedLocalProjectId = 8,
        )
        val ownerSplit = listOf(SharedWorkAllocationSnapshot("other", 1, 7, null, 3600, null))
        val changedRemote = remote.copy(
            entryRevision = 3, allocationCount = 1,
            allocationRowsTotalSeconds = 3600, allocationSnapshots = ownerSplit,
        )
        val queued = intent.copy(projectId = 7, localProjectId = 8, queuedVersion = intent.localVersion)

        assertNull(adoptRemoteAllocationSnapshot(queued, completeBaseline, changedRemote, project))
        assertNull(adoptRemoteAllocationSnapshot(queued, completeBaseline, changedRemote.copy(
            privateNote = "edited elsewhere",
            allocationSnapshots = listOf(SharedWorkAllocationSnapshot("allocation", 2, 7, null, 3600, null)),
        ), project))
    }
}
