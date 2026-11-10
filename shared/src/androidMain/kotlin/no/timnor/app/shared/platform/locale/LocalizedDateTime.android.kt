package no.timnor.app.shared.platform.locale

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.time.Instant as JavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant

@Composable
actual fun formatLocalizedDateTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(LocalConfiguration.current.locales[0])
        .withZone(ZoneId.systemDefault())
        .format(JavaInstant.ofEpochMilli(instant.toEpochMilliseconds()))
