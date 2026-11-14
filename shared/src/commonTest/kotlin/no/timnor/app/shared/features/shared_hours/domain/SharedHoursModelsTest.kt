package no.timnor.app.shared.features.shared_hours.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedHoursModelsTest {
    @Test
    fun accountSwitchNeverSelectsAnotherAccountsOutboxOrProjectCache() {
        val firstAccount = listOf(
            PlannorProjectSelection(10, 4, "Design", refreshedAtUtc = "now", selected = true),
            PlannorProjectSelection(20, 4, "Design", refreshedAtUtc = "now", selected = true),
        )

        assertEquals(listOf(10), accountScoped(10, firstAccount) { it.accountId }.map { it.accountId })
        assertTrue(accountScoped(30, firstAccount) { it.accountId }.isEmpty())
    }

    @Test
    fun allocationBalanceCountsAllocationsOnceAndRejectsCrossAccountRows() {
        val entry = entry(duration = 12 * 3600L)
        val allocations = listOf(allocation(entry, "a", 3 * 3600L), allocation(entry, "b", 2 * 3600L))

        assertEquals(7 * 3600L, allocationBalance(entry, allocations))
        assertEquals(12 * 3600L, personalTimeTotal(listOf(entry)))
        assertNull(allocationBalance(entry, allocations + allocation(entry, "foreign", 60L, accountId = 8)))
        assertFalse(entry.copy(allocatedSeconds = 5 * 3600L).canAllocate(7 * 3600L + 1))
    }

    @Test
    fun sharingProjectionDoesNotExposePrivateNote() {
        val shared = toProjectAllocation(
            entry = entry(duration = 3600L, privateNote = "Personal medical appointment"),
            allocationId = "allocation-1",
            projectId = 21,
            taskId = 44,
            durationSeconds = 900L,
            sharedNote = null,
        )

        assertEquals(900L, shared?.durationSeconds)
        assertNull(shared?.sharedNote)
        assertFalse(shared.toString().contains("medical"))
    }

    @Test
    fun reviewedImportUsesStableSourceIdsAndNeverMatchesBySimilarValues() {
        val candidates = eligibleForImport(
            sourceIds = listOf("work-1", "work-2", "work-2", ""),
            importedSourceIds = setOf("work-1"),
        )

        assertEquals(listOf("work-2"), candidates)
        val preview = LocalEntryImportPreview(4, alreadyImported = 1, eligibleEntries = 3)
        assertFalse(preview.privateNotesIncluded)
        assertIs<LocalEntryImportPreview>(preview)
    }

    @Test
    fun atomicCommandRetainsTheOriginalRetryKeyAndUnknownOutcome() {
        val command = SharedTimeOutboxItem(
            accountId = 5,
            operation = SharedTimeOperation.CREATE_AND_ALLOCATE,
            outcome = SharedTimeOutcome.OUTCOME_UNKNOWN,
            projectId = 9,
            taskId = 12,
            workDate = "2026-11-12",
            durationSeconds = 1200,
            atomicIdempotencyKey = "stable-key-123456789",
        )
        val retry = command.copy(outcome = SharedTimeOutcome.PENDING)

        assertEquals(command.atomicIdempotencyKey, retry.atomicIdempotencyKey)
        assertEquals(command.operation, retry.operation)
        assertEquals(command.durationSeconds, retry.durationSeconds)
    }

    @Test
    fun existingEntryAllocationRetainsEntryRevisionAndItsOwnIdempotencyKey() {
        val command = SharedTimeOutboxItem(
            accountId = 5,
            operation = SharedTimeOperation.ALLOCATE_EXISTING,
            outcome = SharedTimeOutcome.PENDING,
            entryId = "entry-1",
            entryRevision = 3,
            projectId = 9,
            taskId = 12,
            workDate = "2026-11-12",
            durationSeconds = 600,
            allocationIdempotencyKey = "allocation-key-123456",
        )

        assertEquals(3, command.entryRevision)
        assertEquals("allocation-key-123456", command.allocationIdempotencyKey)
        assertNull(command.entryIdempotencyKey)
    }

    private fun entry(duration: Long, privateNote: String? = null) = PersonalTimeEntry(
        accountId = 7,
        serverId = "entry-1",
        workDate = "2026-11-12",
        durationSeconds = duration,
        allocatedSeconds = 0,
        revision = 1,
        privateNote = privateNote,
    )

    private fun allocation(entry: PersonalTimeEntry, id: String, duration: Long, accountId: Int = entry.accountId) =
        ProjectTimeAllocation(
            accountId = accountId,
            allocationId = id,
            entryId = entry.serverId,
            projectId = 9,
            taskId = null,
            workDate = entry.workDate,
            durationSeconds = duration,
            revision = 1,
        )
}
