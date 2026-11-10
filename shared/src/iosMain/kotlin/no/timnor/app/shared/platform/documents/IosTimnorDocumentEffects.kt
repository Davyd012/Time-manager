package no.timnor.app.shared.platform.documents

import no.timnor.app.shared.features.settings.domain.TimnorDocumentAction
import no.timnor.app.shared.features.settings.domain.TimnorSettingsViewModel

/** Native iOS host entry points for the file picker, exporter and share sheet. */
interface IosTimnorDocumentEffectHandler {
    fun saveBackup(contents: String, fileName: String)
    fun shareBackup(contents: String, fileName: String)
    fun shareText(subject: String, contents: String)
    fun openBackup()
    fun saveSpreadsheetBase64(contentsBase64: String, fileName: String)
}

/** Results and imported backup content return from SwiftUI to the shared Settings state. */
object IosTimnorSettingsBridge {
    private var settingsViewModel: TimnorSettingsViewModel? = null

    fun attach(viewModel: TimnorSettingsViewModel) {
        settingsViewModel = viewModel
    }

    fun detach(viewModel: TimnorSettingsViewModel) {
        if (settingsViewModel === viewModel) settingsViewModel = null
    }

    fun backupContentsSelected(contents: String?) {
        settingsViewModel?.previewBackup(contents)
    }

    fun backupReadFailed() {
        settingsViewModel?.reportUnreadableBackupDocument()
    }

    fun reportBackupSaveResult(succeeded: Boolean) {
        report(TimnorDocumentAction.Backup, succeeded)
    }

    fun reportShareResult(succeeded: Boolean) {
        report(TimnorDocumentAction.Share, succeeded)
    }

    fun reportSpreadsheetSaveResult(succeeded: Boolean) {
        report(TimnorDocumentAction.Spreadsheet, succeeded)
    }

    fun backupSaveCancelled() {
        settingsViewModel?.dismissMessage()
    }

    fun shareCancelled() {
        settingsViewModel?.dismissMessage()
    }

    fun spreadsheetSaveCancelled() {
        settingsViewModel?.dismissMessage()
    }

    private fun report(action: TimnorDocumentAction, succeeded: Boolean) {
        val viewModel = settingsViewModel ?: return
        if (succeeded) viewModel.reportDocumentSuccess(action) else viewModel.reportDocumentFailure()
    }
}
