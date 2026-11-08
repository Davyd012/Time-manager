package no.timnor.app.shared.platform

import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.window.ComposeUIViewController
import no.timnor.app.shared.data.persistence.createTimnorPersistence
import no.timnor.app.shared.platform.stopwatch.IosStopwatchPlatform
import no.timnor.app.shared.ui.TimnorApp
import no.timnor.app.shared.ui.TimnorHomeViewModel
import no.timnor.app.shared.ui.theme.TimnorTheme
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    val persistence = remember { createTimnorPersistence() }
    val stopwatch = remember { IosStopwatchPlatform() }
    val viewModel = remember { TimnorHomeViewModel(persistence, stopwatch) }
    TimnorTheme(darkTheme = isSystemInDarkTheme()) {
        TimnorApp(viewModel)
    }
}
