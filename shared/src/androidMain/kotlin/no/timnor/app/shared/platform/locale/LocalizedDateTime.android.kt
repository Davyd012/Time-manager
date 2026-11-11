package no.timnor.app.shared.platform.locale

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.datetime.LocalDate
import java.time.Instant as JavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.text.DecimalFormatSymbols
import kotlin.time.Instant

@Composable
actual fun formatLocalizedDateTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(LocalConfiguration.current.locales[0])
        .withZone(ZoneId.systemDefault())
        .format(JavaInstant.ofEpochMilli(instant.toEpochMilliseconds()))

@Composable
actual fun formatLocalizedWeekday(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val weekday = java.time.DayOfWeek.of(date.dayOfWeek.ordinal + 1)
        .getDisplayName(TextStyle.FULL, locale)
    return weekday.replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase(locale) else character.toString()
    }
}

@Composable
actual fun localizedDecimalSeparator(): Char =
    DecimalFormatSymbols.getInstance(LocalConfiguration.current.locales[0]).decimalSeparator
