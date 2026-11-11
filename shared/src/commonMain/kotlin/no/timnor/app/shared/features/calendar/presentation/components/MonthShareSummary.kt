package no.timnor.app.shared.features.calendar.presentation.components

internal enum class MonthShareSummaryKind { NoHours, Quiet, Normal, Active }

internal fun monthShareSummaryKind(totalSeconds: Long): MonthShareSummaryKind = when {
    totalSeconds <= 0L -> MonthShareSummaryKind.NoHours
    totalSeconds < 20L * 60L * 60L -> MonthShareSummaryKind.Quiet
    totalSeconds >= 160L * 60L * 60L -> MonthShareSummaryKind.Active
    else -> MonthShareSummaryKind.Normal
}

/** Mirrors the legacy NumberFormat configuration: at most two decimals, half-up rounding. */
internal fun formatMonthShareHours(totalSeconds: Long): String {
    val hundredths = (totalSeconds.coerceAtLeast(0L) + 18L) / 36L
    val hours = hundredths / 100L
    val fraction = hundredths % 100L
    return "$hours,${fraction.toString().padStart(2, '0')}"
}
