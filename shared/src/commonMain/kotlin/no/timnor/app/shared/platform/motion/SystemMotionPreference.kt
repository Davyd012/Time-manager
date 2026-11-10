package no.timnor.app.shared.platform.motion

import androidx.compose.runtime.Composable

/** Reports whether the host system allows nonessential motion. */
@Composable
expect fun rememberSystemAnimationsEnabled(): Boolean
