package no.timnor.app.shared.platform.locale

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlinx.datetime.toNSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSNumberFormatter

@Composable
actual fun formatLocalizedDateTime(instant: Instant): String {
    val formatter = NSDateFormatter()
    formatter.dateStyle = NSDateFormatterMediumStyle
    formatter.timeStyle = NSDateFormatterMediumStyle
    return formatter.stringFromDate(instant.toNSDate())
}

@Composable
actual fun formatLocalizedWeekday(date: LocalDate): String {
    val formatter = NSDateFormatter()
    val weekday = formatter.weekdaySymbols.get((date.dayOfWeek.ordinal + 1) % 7) as? String ?: return ""
    return weekday.replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase() else character.toString()
    }
}

@Composable
actual fun localizedDecimalSeparator(): Char =
    NSNumberFormatter().decimalSeparator.firstOrNull() ?: ','
