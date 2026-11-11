package no.timnor.app.shared.platform

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import no.timnor.app.shared.data.persistence.createTimnorPersistence
import no.timnor.app.shared.platform.documents.IosTimnorDocumentEffectHandler
import no.timnor.app.shared.platform.documents.IosTimnorSettingsBridge
import no.timnor.app.shared.platform.navigation.IosTimnorNavigationBridge
import no.timnor.app.shared.platform.stopwatch.IosStopwatchPlatform
import no.timnor.app.shared.features.settings.domain.TimnorSettingsEffect
import no.timnor.app.shared.features.settings.domain.TimnorSettingsViewModel
import no.timnor.app.shared.navigation.TimnorApp
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.ui.theme.TimnorTheme
import platform.UIKit.UIViewController

@OptIn(ExperimentalEncodingApi::class)
fun MainViewController(documentHandler: IosTimnorDocumentEffectHandler): UIViewController = ComposeUIViewController {
    val persistence = remember { createTimnorPersistence() }
    val stopwatch = remember { IosStopwatchPlatform() }
    val viewModel = remember { TimnorHomeViewModel(persistence, stopwatch) }
    val settingsViewModel = remember { TimnorSettingsViewModel(persistence, stopwatch) }
    DisposableEffect(settingsViewModel) {
        IosTimnorSettingsBridge.attach(settingsViewModel)
        onDispose { IosTimnorSettingsBridge.detach(settingsViewModel) }
    }
    TimnorTheme(darkTheme = isSystemInDarkTheme()) {
        TimnorApp(
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            onSettingsEffect = { effect ->
                when (effect) {
                    is TimnorSettingsEffect.SaveBackup -> documentHandler.saveBackup(effect.contents, effect.fileName)
                    is TimnorSettingsEffect.ShareBackup -> documentHandler.shareBackup(effect.contents, effect.fileName)
                    is TimnorSettingsEffect.ShareText -> documentHandler.shareText(effect.subject, effect.contents)
                    is TimnorSettingsEffect.OpenBackup -> documentHandler.openBackup()
                    is TimnorSettingsEffect.SaveSpreadsheet -> documentHandler.saveSpreadsheetBase64(
                        Base64.encode(effect.contents),
                        effect.fileName,
                    )
                }
            },
            onPlatformBackHandlerChanged = IosTimnorNavigationBridge::update,
        )
    }
}
