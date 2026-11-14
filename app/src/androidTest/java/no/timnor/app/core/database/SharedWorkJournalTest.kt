package no.timnor.app.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import no.timnor.app.core.database.work.Work
import kotlinx.coroutines.runBlocking
import no.timnor.app.core.repository.PlannorApi
import no.timnor.app.core.repository.PlannorSession
import no.timnor.app.core.repository.PlannorSessionStorage
import no.timnor.app.core.repository.SharedHoursLocalStore
import no.timnor.app.core.repository.SharedWorkIntent
import no.timnor.app.core.repository.SharedWorkJournal
import no.timnor.app.core.repository.SharedWorkResolution
import no.timnor.app.core.repository.SharedWorkBaseline
import no.timnor.app.core.repository.SharedWorkAllocationSnapshot
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.features.shared_hours.domain.PersonalTimeEntry
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class SharedWorkJournalTest {
    @Test
    fun adoptsCompleteRemoteSplitWithoutDuplicatingOrShrinkingPersonalWork() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val records = SharedHoursLocalStore(database)
            records.put(42, PlannorApi.TYPE_PROJECT, "7",
                PlannorProjectSelection(42, 7, "Workshop", selected = true,
                    refreshedAtUtc = "now", linkedLocalProjectId = 3),
                PlannorProjectSelection.serializer(), "now")
            val workId = database.workDao().insert(Work(
                description = "Private note",
                date = LocalDateTime.parse("2026-11-12T09:30:00"),
                project = 3,
                time = 7200,
            )).toInt()
            records.put(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(), SharedWorkIntent(
                accountId = 42, localWorkId = workId, sourceId = "source", createKey = "create-key",
                projectId = 7, localProjectId = 3, localVersion = 1, queuedVersion = 1,
                workDate = "2026-11-12", durationSeconds = 7200, privateNote = "Private note",
            ), SharedWorkIntent.serializer(), "now")
            records.put(42, SharedWorkJournal.TYPE_WORK_BASELINE, workId.toString(), SharedWorkBaseline(
                entryId = "entry", entryRevision = 2, allocationId = "allocation",
                allocationRevision = 1, entryDurationSeconds = 7200,
                allocationDurationSeconds = 7200, allocationTracksEntry = true,
                entryWorkDate = "2026-11-12", entryPrivateNote = "Private note", allocationProjectId = 7,
                allocationSnapshots = listOf(SharedWorkAllocationSnapshot(
                    "allocation", 1, 7, null, 7200, null,
                )),
            ), SharedWorkBaseline.serializer(), "now")
            val entry = PersonalTimeEntry(42, "entry", "2026-11-12", 7200, 7200, 4,
                "Private note", "source")
            val allocations = listOf(
                ProjectTimeAllocation(42, "allocation", "entry", 7, null,
                    "2026-11-12", 5400, revision = 2),
                ProjectTimeAllocation(42, "allocation-task", "entry", 7, 91,
                    "2026-11-12", 1800, sharedNote = "Shared detail", revision = 1),
            )
            val journal = SharedWorkJournal(database, session)

            assertEquals(0, journal.prepareReconciliations(listOf(entry), allocations))
            val updatedBaseline = records.get(42, SharedWorkJournal.TYPE_WORK_BASELINE,
                workId.toString(), SharedWorkBaseline.serializer())!!
            val updatedIntent = records.get(42, SharedWorkJournal.TYPE_WORK_INTENT,
                workId.toString(), SharedWorkIntent.serializer())!!
            val savedWork = database.workDao().getAll().single()
            assertEquals(4, updatedBaseline.entryRevision)
            assertEquals(2, updatedBaseline.allocationSnapshots.size)
            assertEquals(7200L, updatedBaseline.entryDurationSeconds)
            assertEquals(7200, savedWork.time)
            assertEquals(0, savedWork.task)
            assertEquals(1L, updatedIntent.localVersion)
            assertEquals(updatedIntent.localVersion, updatedIntent.queuedVersion)
            assertNull(updatedIntent.activeCommandKey)
        } finally {
            database.close()
        }
    }

    @Test
    fun restoringSharedBackupPreservesUnknownCreateKeyForIdempotentReplay() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val journal = SharedWorkJournal(database, session)
            val records = SharedHoursLocalStore(database)
            val workId = 7
            val intent = SharedWorkIntent(
                accountId = 42, localWorkId = workId, sourceId = "source", createKey = "immutable-key",
                projectId = 9, localProjectId = 3, localVersion = 1, queuedVersion = 1,
                workDate = "2026-11-12", durationSeconds = 3600, privateNote = "private",
            )
            val command = SharedTimeOutboxItem(
                accountId = 42, operation = SharedTimeOperation.CREATE_AND_ALLOCATE,
                outcome = SharedTimeOutcome.OUTCOME_UNKNOWN, projectId = 9, workDate = "2026-11-12",
                durationSeconds = 3600, sourceId = "source", privateNote = "private",
                atomicIdempotencyKey = "immutable-key",
            )
            records.put(42, PlannorApi.TYPE_PROJECT, "9",
                PlannorProjectSelection(42, 9, "Workshop", selected = true,
                    refreshedAtUtc = "now", linkedLocalProjectId = 3),
                PlannorProjectSelection.serializer(), "now")
            records.put(42, SharedWorkJournal.TYPE_WORK_INTENT, "7", intent,
                SharedWorkIntent.serializer(), "now")
            records.put(42, SharedWorkJournal.TYPE_WORK_BASELINE, "7",
                SharedWorkBaseline("entry", 2, "allocation", 1, 3600, 3600, null, true,
                    "2026-11-12", "private", 9), SharedWorkBaseline.serializer(), "now")
            records.put(42, PlannorApi.TYPE_OUTBOX, "immutable-key", command,
                SharedTimeOutboxItem.serializer(), "now")

            val backup = journal.exportBackupRecords()
            journal.detachForRestore()
            journal.restoreBackupRecords(backup)

            assertEquals(intent, records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, "7",
                SharedWorkIntent.serializer()))
            assertEquals(SharedTimeOutcome.OUTCOME_UNKNOWN, records.get(42, PlannorApi.TYPE_OUTBOX,
                "immutable-key", SharedTimeOutboxItem.serializer())!!.outcome)
            assertEquals(null, records.get(42, PlannorApi.TYPE_OUTBOX, "immutable-key",
                SharedTimeOutboxItem.serializer())!!.errorCode)
            assertTrue(records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, "detached:source",
                SharedWorkIntent.serializer())!!.detachedFromLocalWork)
        } finally {
            database.close()
        }
    }

    @Test
    fun localEditMarksOtherAccountIntentForReviewWithoutQueuingIt() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            var activeAccountId = 42
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", activeAccountId, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val records = SharedHoursLocalStore(database)
            val journal = SharedWorkJournal(database, session)
            listOf(42, 43).forEach { accountId ->
                records.put(accountId, PlannorApi.TYPE_PROJECT, "7",
                    PlannorProjectSelection(accountId, 7, "Workshop", selected = true,
                        refreshedAtUtc = "now", linkedLocalProjectId = 3),
                    PlannorProjectSelection.serializer(), "now")
            }
            val workId = database.workDao().insert(Work(
                description = "Edited privately",
                date = LocalDateTime.parse("2026-11-12T09:30:00"),
                project = 3,
                time = 1800,
            )).toInt()
            val original = SharedWorkIntent(
                accountId = 42, localWorkId = workId, sourceId = "source-a", createKey = "create-a",
                projectId = 7, localProjectId = 3, localVersion = 1,
                workDate = "2026-11-12", durationSeconds = 3600, privateNote = "Original",
            )
            val otherAccount = original.copy(accountId = 43, sourceId = "source-b", createKey = "create-b")
            records.put(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(), original,
                SharedWorkIntent.serializer(), "now")
            records.put(43, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(), otherAccount,
                SharedWorkIntent.serializer(), "now")

            SharedWorkJournal(database, session).changed(
                StoredWork("Edited privately", 1799919000, 3, 0, 1800, workId),
            )

            assertEquals(2L, records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(),
                SharedWorkIntent.serializer())!!.localVersion)
            assertEquals(1800L, records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(),
                SharedWorkIntent.serializer())!!.durationSeconds)
            val inactive = records.get(43, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(),
                SharedWorkIntent.serializer())!!
            assertEquals(2L, inactive.localVersion)
            assertEquals(1800L, inactive.durationSeconds)
            assertEquals(SharedWorkJournal.REVIEW_LOCAL_CHANGE_OTHER_ACCOUNT, inactive.reviewReason)
            activeAccountId = 43
            assertEquals(0, journal.prepareCreates())
            assertNull(records.get(43, PlannorApi.TYPE_OUTBOX, otherAccount.createKey,
                SharedTimeOutboxItem.serializer()))
        } finally {
            database.close()
        }
    }

    @Test
    fun keepingVersionsSeparateDetachesMultiAllocationRemoteWorkWithoutDeletingLocalRow() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val records = SharedHoursLocalStore(database)
            val workId = database.workDao().insert(Work(
                description = "Private note",
                date = LocalDateTime.parse("2026-11-12T09:30:00"),
                project = 3,
                time = 1800,
            )).toInt()
            records.put(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(), SharedWorkIntent(
                accountId = 42, localWorkId = workId, sourceId = "source", createKey = "create-key",
                projectId = 7, localProjectId = 3, localVersion = 1, queuedVersion = 1,
                workDate = "2026-11-12", durationSeconds = 3600, privateNote = "Private note",
                reviewReason = SharedWorkJournal.REVIEW_REMOTE_ALLOCATIONS_CHANGED,
            ), SharedWorkIntent.serializer(), "now")
            records.put(42, SharedWorkJournal.TYPE_WORK_BASELINE, workId.toString(),
                SharedWorkBaseline("entry", 2, "allocation", 1, 3600, 3600, null, true,
                    "2026-11-12", "Private note", 7), SharedWorkBaseline.serializer(), "now")
            val entry = PersonalTimeEntry(42, "entry", "2026-11-12", 3600, 3600, 3,
                "Private note", "source")
            val allocations = listOf(
                ProjectTimeAllocation(42, "allocation", "entry", 7, null, "2026-11-12", 1800, revision = 2),
                ProjectTimeAllocation(42, "other-allocation", "entry", 8, 91, "2026-11-12", 1800, revision = 1),
            )

            val journal = SharedWorkJournal(database, session)
            assertTrue(journal.resolve(workId, SharedWorkResolution.KEEP_LOCAL_ONLY, listOf(entry), allocations))
            assertNull(records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(), SharedWorkIntent.serializer()))
            assertNull(records.get(42, SharedWorkJournal.TYPE_WORK_BASELINE, workId.toString(), SharedWorkBaseline.serializer()))
            assertEquals(1, database.workDao().getAll().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun keepTimnorResolutionRebasesThenQueuesOneRevisionCheckedCorrection() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val records = SharedHoursLocalStore(database)
            val project = PlannorProjectSelection(42, 7, "Workshop", selected = true,
                refreshedAtUtc = "now", linkedLocalProjectId = 3)
            records.put(42, PlannorApi.TYPE_PROJECT, "7", project,
                PlannorProjectSelection.serializer(), "now")
            val workId = database.workDao().insert(Work(
                description = "Timnor private note",
                date = LocalDateTime.parse("2026-11-12T09:30:00"),
                project = 3,
                time = 3600,
            )).toInt()
            val intent = SharedWorkIntent(
                accountId = 42, localWorkId = workId, sourceId = "source", createKey = "create-key",
                projectId = 7, localProjectId = 3, localVersion = 2, queuedVersion = 2,
                workDate = "2026-11-12", durationSeconds = 3600, privateNote = "Timnor private note",
                reviewReason = SharedWorkJournal.REVIEW_REMOTE_ENTRY_CHANGED,
            )
            records.put(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(), intent,
                SharedWorkIntent.serializer(), "now")
            records.put(42, SharedWorkJournal.TYPE_WORK_BASELINE, workId.toString(),
                SharedWorkBaseline(
                    "entry", 2, "allocation", 1, 5400, 3600, null, false,
                    "2026-11-11", "Old private note", 7,
                ), SharedWorkBaseline.serializer(), "now")
            val entry = PersonalTimeEntry(42, "entry", "2026-11-12", 5400, 3600, 3,
                "Plannor private note", "source")
            val allocation = ProjectTimeAllocation(42, "allocation", "entry", 7, null,
                "2026-11-12", 3600, revision = 1)
            val journal = SharedWorkJournal(database, session)

            assertTrue(journal.resolve(workId, SharedWorkResolution.KEEP_TIMNOR,
                listOf(entry), listOf(allocation)))
            assertEquals(null, records.get(42, SharedWorkJournal.TYPE_WORK_INTENT,
                workId.toString(), SharedWorkIntent.serializer())?.reviewReason)
            assertEquals(1, journal.prepareReconciliations(listOf(entry), listOf(allocation)))
            val updatedIntent = records.get(42, SharedWorkJournal.TYPE_WORK_INTENT,
                workId.toString(), SharedWorkIntent.serializer())!!
            val command = records.get(42, PlannorApi.TYPE_OUTBOX, updatedIntent.activeCommandKey!!,
                SharedTimeOutboxItem.serializer())!!
            assertEquals(SharedTimeOperation.UPDATE_ENTRY, command.operation)
            assertEquals(3, command.entryRevision)
            assertEquals(3600L, command.durationSeconds)
        } finally {
            database.close()
        }
    }

    @Test
    fun keepingTimnorAfterRemoteAllocationDeletionRecreatesOnlyItsPartialAllocationOnce() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val records = SharedHoursLocalStore(database)
            records.put(42, PlannorApi.TYPE_PROJECT, "7",
                PlannorProjectSelection(42, 7, "Workshop", selected = true,
                    refreshedAtUtc = "now", linkedLocalProjectId = 3),
                PlannorProjectSelection.serializer(), "now")
            val workId = database.workDao().insert(Work(
                description = "private",
                date = LocalDateTime.parse("2026-11-12T09:30:00"),
                project = 3,
                time = 3600,
            )).toInt()
            val intent = SharedWorkIntent(
                accountId = 42, localWorkId = workId, sourceId = "source", createKey = "create-key",
                projectId = 7, localProjectId = 3, localVersion = 2, queuedVersion = 1,
                workDate = "2026-11-12", durationSeconds = 1800, privateNote = "private",
                reviewReason = SharedWorkJournal.REVIEW_REMOTE_ALLOCATIONS_CHANGED,
            )
            records.put(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(), intent,
                SharedWorkIntent.serializer(), "now")
            records.put(42, SharedWorkJournal.TYPE_WORK_BASELINE, workId.toString(),
                SharedWorkBaseline(
                    entryId = "entry", entryRevision = 2, allocationId = "deleted-allocation",
                    allocationRevision = 1, entryDurationSeconds = 3600, allocationDurationSeconds = 1800,
                    allocationTaskId = null, allocationTracksEntry = false,
                    entryWorkDate = "2026-11-12", entryPrivateNote = "private", allocationProjectId = 7,
                ), SharedWorkBaseline.serializer(), "now")
            val journal = SharedWorkJournal(database, session)
            val entry = PersonalTimeEntry(42, "entry", "2026-11-12", 3600, 1800, 3, "private", "source")
            val otherAllocation = ProjectTimeAllocation(
                42, "other-allocation", "entry", 8, 91, "2026-11-12", 1800, revision = 1,
            )

            assertTrue(journal.resolve(workId, SharedWorkResolution.KEEP_TIMNOR,
                listOf(entry), listOf(otherAllocation)))
            assertEquals(1, journal.prepareReconciliations(listOf(entry), listOf(otherAllocation)))
            val updatedIntent = records.get(42, SharedWorkJournal.TYPE_WORK_INTENT,
                workId.toString(), SharedWorkIntent.serializer())!!
            val commandKey = updatedIntent.activeCommandKey!!
            val command = records.get(42, PlannorApi.TYPE_OUTBOX, commandKey,
                SharedTimeOutboxItem.serializer())!!
            assertEquals(SharedTimeOperation.ALLOCATE_EXISTING, command.operation)
            assertEquals("entry", command.entryId)
            assertEquals(3, command.entryRevision)
            assertEquals(7, command.projectId)
            assertEquals(1800L, command.durationSeconds)

            assertEquals(0, journal.prepareReconciliations(listOf(entry), listOf(otherAllocation)))
            assertEquals(commandKey, records.get(42, SharedWorkJournal.TYPE_WORK_INTENT,
                workId.toString(), SharedWorkIntent.serializer())?.activeCommandKey)
            assertEquals(command, records.get(42, PlannorApi.TYPE_OUTBOX, commandKey,
                SharedTimeOutboxItem.serializer()))
        } finally {
            database.close()
        }
    }

    @Test
    fun remoteEntryEditIsFlaggedWithoutReplacingTheLocalWork() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val records = SharedHoursLocalStore(database)
            val intent = SharedWorkIntent(
                accountId = 42, localWorkId = 9, sourceId = "source", createKey = "create-key",
                projectId = 7, localProjectId = 3, localVersion = 1, queuedVersion = 1,
                workDate = "2026-11-12", durationSeconds = 3600, privateNote = "private",
            )
            records.put(42, SharedWorkJournal.TYPE_WORK_INTENT, "9", intent,
                SharedWorkIntent.serializer(), "now")
            records.put(42, SharedWorkJournal.TYPE_WORK_BASELINE, "9",
                SharedWorkBaseline(
                    "entry", 2, "allocation", 1, 3600, 3600, null, true,
                    "2026-11-12", "private", 7,
                ), SharedWorkBaseline.serializer(), "now")

            val journal = SharedWorkJournal(database, session)
            val editedEntry = PersonalTimeEntry(42, "entry", "2026-11-12", 4200, 3600, 3,
                "remote edit", "source")
            val allocation = ProjectTimeAllocation(42, "allocation", "entry", 7, null,
                "2026-11-12", 3600, revision = 1)
            assertEquals(0, journal.prepareReconciliations(listOf(editedEntry), listOf(allocation)))
            assertEquals(SharedWorkJournal.REVIEW_REMOTE_ENTRY_CHANGED,
                records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, "9", SharedWorkIntent.serializer())?.reviewReason)
            assertEquals(1, journal.needsReviewCount())
        } finally {
            database.close()
        }
    }

    @Test
    fun deletedRemoteEntryIsKeptLocallyAndFlaggedForReview() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val records = SharedHoursLocalStore(database)
            val intent = SharedWorkIntent(
                accountId = 42, localWorkId = 9, sourceId = "source", createKey = "create-key",
                projectId = 7, localProjectId = 3, localVersion = 1, queuedVersion = 1,
                workDate = "2026-11-12", durationSeconds = 3600, privateNote = "private",
            )
            records.put(42, SharedWorkJournal.TYPE_WORK_INTENT, "9", intent,
                SharedWorkIntent.serializer(), "now")
            records.put(42, SharedWorkJournal.TYPE_WORK_BASELINE, "9",
                SharedWorkBaseline("entry", 2, "allocation", 1, 3600, 3600, null, true),
                SharedWorkBaseline.serializer(), "now")

            val journal = SharedWorkJournal(database, session)
            assertEquals(0, journal.prepareReconciliations(emptyList(), emptyList()))
            assertEquals(SharedWorkJournal.REVIEW_REMOTE_ENTRY_MISSING,
                records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, "9", SharedWorkIntent.serializer())?.reviewReason)
            assertEquals(1, journal.needsReviewCount())
            assertNull(records.get(42, PlannorApi.TYPE_OUTBOX, intent.createKey, SharedTimeOutboxItem.serializer()))
        } finally {
            database.close()
        }
    }

    @Test
    fun removedProjectAccessKeepsLocalWorkAndBlocksFurtherSharing() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val records = SharedHoursLocalStore(database)
            val workId = database.workDao().insert(Work(
                description = "private",
                date = LocalDateTime.parse("2026-11-12T09:30:00"),
                project = 3,
                time = 1800,
            )).toInt()
            records.put(42, PlannorApi.TYPE_PROJECT, "7",
                PlannorProjectSelection(
                    accountId = 42,
                    projectId = 7,
                    name = "Unavailable project",
                    selected = true,
                    refreshedAtUtc = "now",
                    linkedLocalProjectId = 3,
                    archived = true,
                    available = false,
                ), PlannorProjectSelection.serializer(), "now")
            val intent = SharedWorkIntent(
                accountId = 42, localWorkId = workId, sourceId = "source", createKey = "create-key",
                projectId = 7, localProjectId = 3, localVersion = 2, queuedVersion = 1,
                workDate = "2026-11-12", durationSeconds = 1800, privateNote = "private",
            )
            val baseline = SharedWorkBaseline(
                entryId = "entry", entryRevision = 2, allocationId = "allocation",
                allocationRevision = 1, entryDurationSeconds = 3600, allocationDurationSeconds = 3600,
                allocationTaskId = null, allocationTracksEntry = true,
                entryWorkDate = "2026-11-12", entryPrivateNote = "private", allocationProjectId = 7,
                allocationSnapshots = listOf(SharedWorkAllocationSnapshot(
                    "allocation", 1, 7, null, 3600, null,
                )),
            )
            records.put(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(), intent,
                SharedWorkIntent.serializer(), "now")
            records.put(42, SharedWorkJournal.TYPE_WORK_BASELINE, workId.toString(), baseline,
                SharedWorkBaseline.serializer(), "now")
            val journal = SharedWorkJournal(database, session)
            val entry = PersonalTimeEntry(42, "entry", "2026-11-12", 3600, 3600, 2,
                "private", "source")
            val allocation = ProjectTimeAllocation(42, "allocation", "entry", 7, null,
                "2026-11-12", 3600, revision = 1)

            assertEquals(0, journal.prepareReconciliations(listOf(entry), listOf(allocation)))
            assertEquals(SharedWorkJournal.REVIEW_ACCESS_REMOVED,
                records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(),
                    SharedWorkIntent.serializer())!!.reviewReason)
            assertEquals(1, journal.needsReviewCount())
            assertEquals(1800, database.workDao().getAll().single().time)
            assertNull(records.get(42, PlannorApi.TYPE_OUTBOX, intent.createKey,
                SharedTimeOutboxItem.serializer()))
        } finally {
            database.close()
        }
    }

    @Test
    fun confirmedDeletionConflictRemainsReviewableAfterLocalRowIsGone() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val records = SharedHoursLocalStore(database)
            val project = PlannorProjectSelection(42, 7, "Workshop", selected = true,
                refreshedAtUtc = "now", linkedLocalProjectId = 3)
            records.put(42, PlannorApi.TYPE_PROJECT, "7", project,
                PlannorProjectSelection.serializer(), "now")
            val intent = SharedWorkIntent(
                accountId = 42, localWorkId = 91, sourceId = "source", createKey = "create-key",
                projectId = 7, localProjectId = 3, localVersion = 2, queuedVersion = 2,
                workDate = "2026-11-12", durationSeconds = 3600, privateNote = "private",
                deleted = true, deletionApproved = true,
            )
            records.put(42, SharedWorkJournal.TYPE_WORK_INTENT, "91", intent,
                SharedWorkIntent.serializer(), "now")
            records.put(42, SharedWorkJournal.TYPE_WORK_BASELINE, "91",
                SharedWorkBaseline(
                    entryId = "entry", entryRevision = 2, allocationId = "allocation",
                    allocationRevision = 1, entryDurationSeconds = 3600, allocationDurationSeconds = 3600,
                    allocationTracksEntry = true, entryWorkDate = "2026-11-12",
                    entryPrivateNote = "private", allocationProjectId = 7,
                ), SharedWorkBaseline.serializer(), "now")
            val journal = SharedWorkJournal(database, session)
            val entry = PersonalTimeEntry(42, "entry", "2026-11-12", 3600, 3600, 3,
                "private", "source")
            val allocation = ProjectTimeAllocation(42, "allocation", "entry", 7, null,
                "2026-11-12", 3600, revision = 1)

            assertEquals(0, journal.prepareReconciliations(listOf(entry), listOf(allocation)))
            val review = journal.reviewItems(emptyList(), listOf(entry), listOf(allocation)).single()
            assertTrue(review.localDeleted)
            assertEquals("2026-11-12", review.localDate)
            assertTrue(journal.resolve(91, SharedWorkResolution.KEEP_TIMNOR,
                listOf(entry), listOf(allocation)))
            assertEquals(1, journal.prepareReconciliations(listOf(entry), listOf(allocation)))
            val updated = records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, "91",
                SharedWorkIntent.serializer())!!
            val command = records.get(42, PlannorApi.TYPE_OUTBOX, updated.activeCommandKey!!,
                SharedTimeOutboxItem.serializer())!!
            assertEquals(SharedTimeOperation.DELETE_ALLOCATION, command.operation)
            assertEquals(3, command.entryRevision)
            assertEquals("allocation", command.allocationId)
            assertNull(database.workDao().getAll().singleOrNull { it.id == 91 })
        } finally {
            database.close()
        }
    }

    @Test
    fun removedRemoteAllocationCanBeKeptLocalWithoutDeletingTimnorWork() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val records = SharedHoursLocalStore(database)
            val workId = database.workDao().insert(Work(
                description = "private",
                date = LocalDateTime.parse("2026-11-12T09:30:00"),
                project = 3,
                time = 3600,
            )).toInt()
            val intent = SharedWorkIntent(
                accountId = 42, localWorkId = workId, sourceId = "source", createKey = "create-key",
                projectId = 7, localProjectId = 3, localVersion = 1, queuedVersion = 1,
                workDate = "2026-11-12", durationSeconds = 3600, privateNote = "private",
                reviewReason = SharedWorkJournal.REVIEW_REMOTE_ALLOCATIONS_CHANGED,
            )
            records.put(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(), intent,
                SharedWorkIntent.serializer(), "now")
            records.put(42, SharedWorkJournal.TYPE_WORK_BASELINE, workId.toString(),
                SharedWorkBaseline("entry", 2, "allocation", 1, 3600, 3600, null, true),
                SharedWorkBaseline.serializer(), "now")
            val journal = SharedWorkJournal(database, session)
            val unallocatedEntry = PersonalTimeEntry(42, "entry", "2026-11-12", 3600, 0, 3,
                "private", "source")

            assertTrue(journal.resolve(workId, SharedWorkResolution.KEEP_LOCAL_ONLY,
                listOf(unallocatedEntry), emptyList()))
            assertEquals(null, records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, workId.toString(),
                SharedWorkIntent.serializer()))
            assertEquals(null, records.get(42, SharedWorkJournal.TYPE_WORK_BASELINE, workId.toString(),
                SharedWorkBaseline.serializer()))
            assertEquals(1, database.workDao().getAll().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun localWorkAndItsIntentTrackEditsAndDeletionTogether() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, Database::class.java,
        ).allowMainThreadQueries().build()
        try {
            val session = object : PlannorSessionStorage {
                override fun read() = PlannorSession("access", "refresh", 42, "user")
                override fun write(session: PlannorSession) = Unit
                override fun clear() = Unit
            }
            val records = SharedHoursLocalStore(database)
            val project = PlannorProjectSelection(42, 7, "Workshop", selected = true,
                refreshedAtUtc = "now", linkedLocalProjectId = 3)
            records.put(42, PlannorApi.TYPE_PROJECT, "7", project, PlannorProjectSelection.serializer(), "now")
            val journal = SharedWorkJournal(database, session)
            val persistence = RoomPersistenceStore(database, journal)
            persistence.upsertWork(StoredWork("private", 1700000000, 3, 91, 3600, 0))
            val work = persistence.snapshotAll().works.single()
            val created = records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, work.id.toString(),
                SharedWorkIntent.serializer())!!
            assertEquals(1L, created.localVersion)
            assertEquals(7, created.projectId)
            assertEquals(91, created.taskId)
            assertEquals("private", created.privateNote)
            assertEquals(1, journal.prepareCreates())
            val queued = records.get(42, PlannorApi.TYPE_OUTBOX, created.createKey,
                SharedTimeOutboxItem.serializer())!!
            assertEquals(created.sourceId, queued.sourceId)
            assertEquals(3600L, queued.durationSeconds)
            assertEquals(91, queued.taskId)
            assertEquals(null, queued.sharedNote)

            val edited = work.copy(time = 1800, task = 92)
            persistence.upsertWork(edited)
            val updated = records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, work.id.toString(),
                SharedWorkIntent.serializer())!!
            assertEquals(created.sourceId, updated.sourceId)
            assertEquals(created.createKey, updated.createKey)
            assertEquals(2L, updated.localVersion)
            assertEquals(1800L, updated.durationSeconds)
            assertEquals(92, updated.taskId)
            assertEquals(0, journal.prepareCreates())
            assertEquals(queued, records.get(42, PlannorApi.TYPE_OUTBOX, created.createKey,
                SharedTimeOutboxItem.serializer()))

            records.put(42, PlannorApi.TYPE_OUTBOX, created.createKey,
                queued.copy(outcome = SharedTimeOutcome.SUCCEEDED, entryId = "entry", entryRevision = 2),
                SharedTimeOutboxItem.serializer(), "now")
            val entry = PersonalTimeEntry(42, "entry", created.workDate, 3600, 3600, 2,
                "private", created.sourceId)
            val allocation = ProjectTimeAllocation(42, "allocation", "entry", 7, 91,
                created.workDate, 3600, revision = 1)
            journal.recordInitialBaselines(listOf(entry.copy(revision = 3)), listOf(allocation))
            assertEquals(null, records.get(42, SharedWorkJournal.TYPE_WORK_BASELINE, work.id.toString(),
                SharedWorkBaseline.serializer()))
            journal.recordInitialBaselines(listOf(entry), listOf(allocation))
            assertEquals(SharedWorkBaseline(
                "entry", 2, "allocation", 1, 3600, 3600, 91, true,
                entryWorkDate = created.workDate, entryPrivateNote = "private", allocationProjectId = 7,
                allocationSnapshots = listOf(SharedWorkAllocationSnapshot(
                    "allocation", 1, 7, 91, 3600, null,
                )),
            ),
                records.get(42, SharedWorkJournal.TYPE_WORK_BASELINE, work.id.toString(),
                    SharedWorkBaseline.serializer()))
            assertEquals(1, journal.prepareReconciliations(listOf(entry), listOf(allocation)))
            val firstKey = records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, work.id.toString(),
                SharedWorkIntent.serializer())!!.activeCommandKey!!
            val first = records.get(42, PlannorApi.TYPE_OUTBOX, firstKey, SharedTimeOutboxItem.serializer())!!
            assertEquals(SharedTimeOperation.UPDATE_ALLOCATION, first.operation)
            assertEquals(1800L, first.durationSeconds)
            assertEquals(92, first.taskId)
            records.put(42, PlannorApi.TYPE_OUTBOX, firstKey,
                first.copy(outcome = SharedTimeOutcome.SUCCEEDED, entryRevision = 3),
                SharedTimeOutboxItem.serializer(), "now")
            val smallerEntry = entry.copy(allocatedSeconds = 1800, revision = 3)
            val smallerAllocation = allocation.copy(taskId = 92, durationSeconds = 1800, revision = 2)
            assertEquals(1, journal.prepareReconciliations(listOf(smallerEntry), listOf(smallerAllocation)))
            val secondKey = records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, work.id.toString(),
                SharedWorkIntent.serializer())!!.activeCommandKey!!
            val second = records.get(42, PlannorApi.TYPE_OUTBOX, secondKey, SharedTimeOutboxItem.serializer())!!
            assertEquals(SharedTimeOperation.UPDATE_ENTRY, second.operation)
            assertEquals(3, second.entryRevision ?: -1)
            records.put(42, PlannorApi.TYPE_OUTBOX, created.createKey, queued,
                SharedTimeOutboxItem.serializer(), "now")

            persistence.deleteWork(edited, sharedDeletionConfirmed = true)
            val deleted = records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, work.id.toString(),
                SharedWorkIntent.serializer())!!
            assertTrue(deleted.deleted)
            assertTrue(deleted.deletionApproved)
            assertTrue(persistence.snapshotAll().works.isEmpty())

            persistence.replaceAll(emptyList(), listOf(work))
            assertEquals(null, records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, work.id.toString(),
                SharedWorkIntent.serializer()))
            val detached = records.get(42, SharedWorkJournal.TYPE_WORK_INTENT, "detached:${created.sourceId}",
                SharedWorkIntent.serializer())!!
            assertTrue(detached.deleted)
            val paused = records.get(42, PlannorApi.TYPE_OUTBOX, created.createKey,
                SharedTimeOutboxItem.serializer())!!
            assertEquals(SharedTimeOutcome.CONFLICT, paused.outcome)
            assertEquals("restore_review_required", paused.errorCode)
            assertEquals(null, records.get(42, PlannorApi.TYPE_PROJECT, "7",
                PlannorProjectSelection.serializer())!!.linkedLocalProjectId)
            persistence.upsertWork(work.copy(time = 900))
            assertEquals(detached, records.get(42, SharedWorkJournal.TYPE_WORK_INTENT,
                "detached:${created.sourceId}", SharedWorkIntent.serializer()))
        } finally {
            database.close()
        }
    }
}
