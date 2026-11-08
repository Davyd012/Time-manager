package no.timnor.app.features.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.timnor.app.core.repository.BackupRepository
import no.timnor.app.core.repository.BackupResult
import no.timnor.app.core.repository.RestorePreview
import no.timnor.app.core.repository.StopwatchRepository

data class SettingsUiState(
    val busy: Boolean = false,
    val progress: Progress? = null,
    val message: BackupResult? = null,
    val preview: RestorePreview? = null,
    val timerBlocked: Boolean = false,
)

enum class Progress { CreatingBackup, CheckingBackup, RestoringBackup }

class SettingsViewModel(
    private val backupRepository: BackupRepository,
    private val stopwatchRepository: StopwatchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    fun createBackup(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = SettingsUiState(busy = true, progress = Progress.CreatingBackup)
            _state.value = SettingsUiState(message = backupRepository.export(uri))
        }
    }

    fun readBackup(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = SettingsUiState(busy = true, progress = Progress.CheckingBackup)
            val (preview, error) = backupRepository.stage(uri)
            _state.value = if (error == null) SettingsUiState(preview = preview) else SettingsUiState(message = error)
        }
    }

    fun cancelPreview() {
        _state.value.preview?.let { backupRepository.discard(it.token) }
        _state.value = SettingsUiState()
    }

    fun restore() {
        val preview = _state.value.preview ?: return
        val timer = stopwatchRepository.state.value
        if (timer.isRunning || timer.elapsedSeconds > 0) {
            _state.value = _state.value.copy(timerBlocked = true)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, progress = Progress.RestoringBackup, timerBlocked = false)
            when (val result = backupRepository.restore(preview.token)) {
                BackupResult.TimerActive -> _state.value = _state.value.copy(busy = false, timerBlocked = true)
                else -> _state.value = SettingsUiState(message = result)
            }
        }
    }

    fun dismissMessage() { _state.value = SettingsUiState() }
}
