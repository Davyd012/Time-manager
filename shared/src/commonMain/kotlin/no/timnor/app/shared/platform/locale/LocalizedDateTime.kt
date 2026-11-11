package no.timnor.app.shared.platform.locale

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** Formats an instant using the host locale, timezone, and medium date/time style. */
@Composable
expect fun formatLocalizedDateTime(instant: Instant): String

/** Formats a full weekday using the host locale, matching the platform's calendar conventions. */
@Composable
expect fun formatLocalizedWeekday(date: LocalDate): String

/** Returns the decimal separator used by the host locale for duration labels. */
@Composable
expect fun localizedDecimalSeparator(): Char
