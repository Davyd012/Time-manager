package no.timnor.app.shared.platform.locale

import androidx.compose.runtime.Composable
import kotlin.time.Instant
import kotlinx.datetime.toNSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle

@Composable
actual fun formatLocalizedDateTime(instant: Instant): String {
    val formatter = NSDateFormatter()
    formatter.dateStyle = NSDateFormatterMediumStyle
    formatter.timeStyle = NSDateFormatterMediumStyle
    return formatter.stringFromDate(instant.toNSDate())
}
