package no.timnor.app.shared.platform.locale

import androidx.compose.runtime.Composable
import kotlin.time.Instant

/** Formats an instant using the host locale, timezone, and medium date/time style. */
@Composable
expect fun formatLocalizedDateTime(instant: Instant): String
