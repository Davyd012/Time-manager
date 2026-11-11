package no.timnor.app.shared.features.settings.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.timnor.app.shared.data.backup.encodeTimnorBackupV1
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.data.persistence.TimnorDataSnapshot
import no.timnor.app.shared.platform.documents.TimnorRestoreGate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TimnorSettingsViewModelTest {
    @Test
    fun createBackupUsesLegacySuggestedFilename() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = TimnorSettingsViewModel(
                SettingsTestPersistence(),
                SettingsTestRestoreGate(allowRestore = true),
            )
            val nextEffect = async { viewModel.effects.first() }
            runCurrent()

            viewModel.createBackup()
            runCurrent()

            val effect = assertIs<TimnorSettingsEffect.SaveBackup>(nextEffect.await())
            assertTrue(effect.fileName.startsWith("time-manager-backup-"))
            assertTrue(effect.fileName.endsWith(".json"))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun restorePickerUsesTheLegacyBroadDocumentFilter() = runTest {
        val viewModel = TimnorSettingsViewModel(
            SettingsTestPersistence(),
            SettingsTestRestoreGate(allowRestore = true),
        )
        val nextEffect = async { viewModel.effects.first() }
        runCurrent()

        viewModel.requestBackupRestore()

        assertEquals(TimnorSettingsEffect.OpenBackup(listOf("*/*")), nextEffect.await())
    }

    @Test
    fun documentResultsExposeSharedSuccessFailureAndDismissalState() {
        val viewModel = TimnorSettingsViewModel(
            SettingsTestPersistence(),
            SettingsTestRestoreGate(allowRestore = true),
        )

        viewModel.reportDocumentSuccess(TimnorDocumentAction.Backup)
        assertEquals(TimnorSettingsMessage.BackupSaved, viewModel.state.value.message)
        viewModel.reportDocumentSuccess(TimnorDocumentAction.Share)
        assertEquals(TimnorSettingsMessage.BackupShared, viewModel.state.value.message)
        viewModel.reportDocumentSuccess(TimnorDocumentAction.Spreadsheet)
        assertEquals(TimnorSettingsMessage.SpreadsheetSaved, viewModel.state.value.message)
        viewModel.reportDocumentFailure()
        assertEquals(TimnorSettingsMessage.DocumentFailed, viewModel.state.value.message)

        viewModel.dismissMessage()
        assertEquals(null, viewModel.state.value.message)
    }

    @Test
    fun successfulHomeTextShareDoesNotLeaveBackupFeedbackInSettings() {
        val viewModel = TimnorSettingsViewModel(
            SettingsTestPersistence(),
            SettingsTestRestoreGate(allowRestore = true),
        )

        viewModel.reportTextShareResult(shared = true)

        assertNull(viewModel.state.value.message)
        assertFalse(viewModel.state.value.busy)
        viewModel.reportTextShareResult(shared = false)
        assertEquals(TimnorSettingsMessage.DocumentFailed, viewModel.state.value.message)
    }

    @Test
    fun spreadsheetExportUsesTheSelectedProjectsAndRequestedMonth() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = SettingsTestPersistence()
            persistence.upsertWork(
                StoredWork("selected", epochSeconds(2024, 2, 12), 8, 0, 3_600, 1),
            )
            persistence.upsertWork(
                StoredWork("other project", epochSeconds(2024, 2, 12), 9, 0, 7_200, 2),
            )
            persistence.upsertWork(
                StoredWork("other month", epochSeconds(2024, 3, 12), 8, 0, 9_000, 3),
            )
            val viewModel = TimnorSettingsViewModel(persistence, SettingsTestRestoreGate(allowRestore = true))
            val nextEffect = async { viewModel.effects.first() }
            runCurrent()

            viewModel.exportSpreadsheet(year = 2024, month = 2, projectIds = setOf(8))
            runCurrent()

            val effect = assertIs<TimnorSettingsEffect.SaveSpreadsheet>(nextEffect.await())
            assertEquals("Timer-2024-02.xlsx", effect.fileName)
            val workbook = effect.contents.decodeToString(throwOnInvalidSequence = false)
            assertTrue(workbook.contains("<v>1.0</v>"))
            assertFalse(workbook.contains("<v>2.0</v>"))
            assertFalse(workbook.contains("<v>2.5</v>"))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun restorePreviewsDataAndWaitsForTimerGateBeforeReplacing() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val persistence = SettingsTestPersistence().apply {
                upsertProject(StoredProject("current", "Current", 1))
            }
            val importedProjects = listOf(StoredProject("new", "Imported", 8))
            val importedWorks = listOf(StoredWork("log", 1_710_000_000, 8, 0, 3_600, 12))
            val encoded = encodeTimnorBackupV1(
                importedProjects,
                importedWorks,
                "2024-03-09T10:00:00Z",
            )
            val gate = SettingsTestRestoreGate(allowRestore = false)
            val viewModel = TimnorSettingsViewModel(persistence, gate)

            viewModel.previewBackup(encoded)
            runCurrent()
            assertEquals(1, viewModel.state.value.preview?.projectCount)
            assertEquals(1, viewModel.state.value.preview?.workCount)

            viewModel.restoreBackup()
            runCurrent()
            assertTrue(viewModel.state.value.timerBlocked)
            assertEquals(listOf(1), persistence.projects.value.map(StoredProject::id))

            gate.allowRestore = true
            viewModel.restoreBackup()
            runCurrent()
            assertEquals(TimnorSettingsMessage.RestoreSucceeded, viewModel.state.value.message)
            assertEquals(importedProjects, persistence.projects.value)
            assertEquals(importedWorks, persistence.works.value)
            assertFalse(viewModel.state.value.busy)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cancellingTimerBlockedPreviewPreventsLaterRestore() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val currentProject = StoredProject("current", "Current", 1)
            val persistence = SettingsTestPersistence().apply { upsertProject(currentProject) }
            val imported = encodeTimnorBackupV1(
                listOf(StoredProject("replacement", "Replacement", 8)),
                emptyList(),
                "2024-03-09T10:00:00Z",
            )
            val gate = SettingsTestRestoreGate(allowRestore = false)
            val viewModel = TimnorSettingsViewModel(persistence, gate)

            viewModel.previewBackup(imported)
            runCurrent()
            viewModel.restoreBackup()
            runCurrent()
            assertTrue(viewModel.state.value.timerBlocked)

            viewModel.cancelRestorePreview()
            assertNull(viewModel.state.value.preview)
            assertFalse(viewModel.state.value.timerBlocked)
            gate.allowRestore = true
            viewModel.restoreBackup()
            runCurrent()

            assertEquals(listOf(currentProject), persistence.projects.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private fun epochSeconds(year: Int, month: Int, day: Int): Long =
    no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime(
        kotlinx.datetime.LocalDateTime(
            kotlinx.datetime.LocalDate(year, month, day),
            kotlinx.datetime.LocalTime(12, 0),
        ),
    )

private class SettingsTestRestoreGate(var allowRestore: Boolean) : TimnorRestoreGate {
    override suspend fun withTimerClear(restore: suspend () -> Unit): Boolean {
        if (!allowRestore) return false
        restore()
        return true
    }
}

private class SettingsTestPersistence : TimnorPersistence {
    val projects = MutableStateFlow(emptyList<StoredProject>())
    val works = MutableStateFlow(emptyList<StoredWork>())

    override fun observeProjects(): Flow<List<StoredProject>> = projects

    override suspend fun snapshotAll() = TimnorDataSnapshot(projects.value.toList(), works.value.toList())

    override fun observeWorks(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>): Flow<List<StoredWork>> =
        works

    override suspend fun hoursForRange(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>): Int =
        works.value.filter { it.dateEpochSeconds in startEpochSeconds..endEpochSeconds }
            .filter { projectIds.isEmpty() || it.project in projectIds }
            .sumOf(StoredWork::time)

    override suspend fun upsertProject(project: StoredProject) {
        projects.value = projects.value.filterNot { it.id == project.id } + project
    }

    override suspend fun deleteProject(project: StoredProject) {
        projects.value = projects.value.filterNot { it.id == project.id }
    }

    override suspend fun upsertWork(work: StoredWork) {
        works.value = works.value.filterNot { it.id == work.id } + work
    }

    override suspend fun upsertWorks(works: List<StoredWork>) {
        val updated = this.works.value.toMutableList()
        works.forEach { work ->
            val index = updated.indexOfFirst { it.id == work.id }
            if (index < 0) updated += work else updated[index] = work
        }
        this.works.value = updated
    }

    override suspend fun deleteWork(work: StoredWork) {
        works.value = works.value.filterNot { it.id == work.id }
    }

    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) {
        this.projects.value = projects
        this.works.value = works
    }
}
