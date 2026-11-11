package no.timnor.app.shared.features.settings.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import no.timnor.app.shared.data.backup.TimnorBackupV1
import no.timnor.app.shared.data.backup.UnsupportedTimnorBackupVersionException
import no.timnor.app.shared.data.backup.decodeTimnorBackupV1
import no.timnor.app.shared.data.backup.encodeTimnorBackupV1
import no.timnor.app.shared.data.backup.toStoredProjects
import no.timnor.app.shared.data.backup.toStoredWorks
import no.timnor.app.shared.data.export.createTimnorXlsx
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.platform.documents.TimnorRestoreGate

data class TimnorRestorePreview(
    val projectCount: Int,
    val workCount: Int,
    val createdAt: String,
)

data class TimnorSettingsUiState(
    val busy: Boolean = false,
    val progress: TimnorSettingsProgress? = null,
    val message: TimnorSettingsMessage? = null,
    val preview: TimnorRestorePreview? = null,
    val timerBlocked: Boolean = false,
)

enum class TimnorSettingsProgress { CreatingBackup, CheckingBackup, RestoringBackup, CreatingSpreadsheet }

enum class TimnorSettingsMessage {
    BackupSaved,
    BackupShared,
    SpreadsheetSaved,
    InvalidBackup,
    UnsupportedBackupVersion,
    BackupUnreadable,
    RestoreSucceeded,
    RestoreFailed,
    DocumentFailed,
    TimerActive,
}

enum class TimnorDocumentAction { Backup, Share, Spreadsheet }

sealed interface TimnorSettingsEffect {
    data class SaveBackup(val contents: String, val fileName: String) : TimnorSettingsEffect
    data class ShareBackup(val contents: String, val fileName: String) : TimnorSettingsEffect
    data class ShareText(val subject: String, val contents: String) : TimnorSettingsEffect
    data class OpenBackup(val mimeTypes: List<String>) : TimnorSettingsEffect
    data class SaveSpreadsheet(val contents: ByteArray, val fileName: String) : TimnorSettingsEffect
}

/** Shared backup/restore/export state. Native document UI is requested through [effects]. */
class TimnorSettingsViewModel(
    private val persistence: TimnorPersistence,
    private val restoreGate: TimnorRestoreGate,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TimnorSettingsUiState())
    val state: StateFlow<TimnorSettingsUiState> = mutableState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<TimnorSettingsEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<TimnorSettingsEffect> = mutableEffects.asSharedFlow()

    private var pendingBackup: TimnorBackupV1? = null

    fun createBackup() = createBackup(share = false)

    fun shareBackup() = createBackup(share = true)

    fun shareText(subject: String, contents: String) {
        mutableEffects.tryEmit(TimnorSettingsEffect.ShareText(subject, contents))
    }

    private fun createBackup(share: Boolean) {
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.value = TimnorSettingsUiState(busy = true, progress = TimnorSettingsProgress.CreatingBackup)
            try {
                val snapshot = persistence.snapshotAll()
                val contents = encodeTimnorBackupV1(snapshot.projects, snapshot.works)
                val today = currentLocalDate()
                val name = if (share) "timnor-backup-$today.json" else "time-manager-backup-$today.json"
                mutableEffects.emit(
                    if (share) TimnorSettingsEffect.ShareBackup(contents, name)
                    else TimnorSettingsEffect.SaveBackup(contents, name),
                )
                mutableState.value = TimnorSettingsUiState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.value = TimnorSettingsUiState(message = TimnorSettingsMessage.DocumentFailed)
            }
        }
    }

    fun requestBackupRestore() {
        mutableEffects.tryEmit(TimnorSettingsEffect.OpenBackup(listOf("*/*")))
    }

    /** Pass null when the platform picker was cancelled. */
    fun previewBackup(contents: String?) {
        if (contents == null || mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.value = TimnorSettingsUiState(busy = true, progress = TimnorSettingsProgress.CheckingBackup)
            try {
                val backup = decodeTimnorBackupV1(contents)
                pendingBackup = backup
                mutableState.value = TimnorSettingsUiState(
                    preview = TimnorRestorePreview(
                        projectCount = backup.projects.size,
                        workCount = backup.works.size,
                        createdAt = backup.createdAt,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: UnsupportedTimnorBackupVersionException) {
                mutableState.value = TimnorSettingsUiState(message = TimnorSettingsMessage.UnsupportedBackupVersion)
            } catch (_: IllegalArgumentException) {
                mutableState.value = TimnorSettingsUiState(message = TimnorSettingsMessage.InvalidBackup)
            } catch (_: Exception) {
                mutableState.value = TimnorSettingsUiState(message = TimnorSettingsMessage.BackupUnreadable)
            }
        }
    }

    fun cancelRestorePreview() {
        pendingBackup = null
        mutableState.value = TimnorSettingsUiState()
    }

    fun restoreBackup() {
        val backup = pendingBackup ?: return
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, progress = TimnorSettingsProgress.RestoringBackup, timerBlocked = false) }
            try {
                val restored = restoreGate.withTimerClear {
                    persistence.replaceAll(backup.toStoredProjects(), backup.toStoredWorks())
                }
                if (!restored) {
                    mutableState.value = mutableState.value.copy(
                        busy = false,
                        progress = null,
                        timerBlocked = true,
                        message = TimnorSettingsMessage.TimerActive,
                    )
                } else {
                    pendingBackup = null
                    mutableState.value = TimnorSettingsUiState(message = TimnorSettingsMessage.RestoreSucceeded)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                pendingBackup = null
                mutableState.value = TimnorSettingsUiState(message = TimnorSettingsMessage.RestoreFailed)
            }
        }
    }

    fun exportSpreadsheet(year: Int, month: Int, projectIds: Set<Int>? = null) {
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.value = TimnorSettingsUiState(busy = true, progress = TimnorSettingsProgress.CreatingSpreadsheet)
            try {
                val snapshot = persistence.snapshotAll()
                val selectedWorks = projectIds?.let { ids -> snapshot.works.filter { it.project in ids } }
                    ?: snapshot.works
                val contents = createTimnorXlsx(selectedWorks, year, month)
                mutableEffects.emit(
                    TimnorSettingsEffect.SaveSpreadsheet(
                        contents = contents,
                        fileName = "Timer-${year}-${month.toString().padStart(2, '0')}.xlsx",
                    ),
                )
                mutableState.value = TimnorSettingsUiState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableState.value = TimnorSettingsUiState(message = TimnorSettingsMessage.DocumentFailed)
            }
        }
    }

    fun reportDocumentFailure() {
        mutableState.value = TimnorSettingsUiState(message = TimnorSettingsMessage.DocumentFailed)
    }

    /** Text sharing from Home has no success banner; only surface a failure if the chooser cannot open. */
    fun reportTextShareResult(shared: Boolean) {
        if (!shared) reportDocumentFailure()
    }

    fun reportInvalidBackupDocument() {
        mutableState.value = TimnorSettingsUiState(message = TimnorSettingsMessage.InvalidBackup)
    }

    fun reportUnreadableBackupDocument() {
        mutableState.value = TimnorSettingsUiState(message = TimnorSettingsMessage.BackupUnreadable)
    }

    fun reportDocumentSuccess(action: TimnorDocumentAction) {
        mutableState.value = TimnorSettingsUiState(
            message = when (action) {
                TimnorDocumentAction.Backup -> TimnorSettingsMessage.BackupSaved
                TimnorDocumentAction.Share -> TimnorSettingsMessage.BackupShared
                TimnorDocumentAction.Spreadsheet -> TimnorSettingsMessage.SpreadsheetSaved
            },
        )
    }

    fun dismissMessage() {
        mutableState.update { it.copy(message = null, timerBlocked = false) }
    }

    private fun currentLocalDate(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
