package no.timnor.app.shared.domain.project

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork

internal const val MAX_HOURLY_RATE_ORE = 99_999_999
private const val ORE_PER_KRONE = 100
private const val SECONDS_PER_HOUR = 3600

private val hourlyRatePattern = Regex("[0-9]*(?:\\.[0-9]{0,2})?")

internal fun parseHourlyRateOre(input: String): Int? {
    val normalized = buildString {
        input.trim().forEach { character ->
            when (character) {
                ',', '\u066B' -> append('.')
                else -> append(character.digitToIntOrNull()?.digitToChar() ?: character)
            }
        }
    }
    if (normalized.none { it.isDigit() } || !hourlyRatePattern.matches(normalized)) return null
    val parts = normalized.split('.')
    val kroner = parts[0].ifEmpty { "0" }.toIntOrNull() ?: return null
    if (kroner > MAX_HOURLY_RATE_ORE / ORE_PER_KRONE) return null
    val ore = parts.getOrNull(1)?.padEnd(2, '0')?.toInt() ?: 0
    return kroner * ORE_PER_KRONE + ore
}

internal fun formatPayAmount(
    ore: Long,
    decimalSeparator: String,
    groupingSeparator: String? = null,
): String {
    val wholeKroner = (ore / ORE_PER_KRONE).toString()
    val kroner = if (groupingSeparator == null) {
        wholeKroner
    } else {
        wholeKroner.reversed().chunked(3).asReversed().joinToString(groupingSeparator) { it.reversed() }
    }
    val fraction = ore % ORE_PER_KRONE
    return if (fraction == 0L) kroner
    else "$kroner$decimalSeparator${fraction.toString().padStart(2, '0')}"
}

internal data class CalendarMonthPay(val amountOre: Long?, val unratedSeconds: Long)

/** Summarizes work already limited to the displayed month and project filter. */
internal fun calendarMonthPay(visibleMonthWorks: List<StoredWork>): CalendarMonthPay {
    var amountOre: Long? = null
    var unratedSeconds = 0L
    for (work in visibleMonthWorks) {
        if (work.time <= 0) continue
        val amount = workPayOre(work)
        if (amount == null) {
            unratedSeconds += work.time
        } else {
            amountOre = (amountOre ?: 0L) + amount
        }
    }
    return CalendarMonthPay(amountOre, unratedSeconds)
}

internal fun StoredProject.hourlyRateFor(date: LocalDate): Int? = when (date.dayOfWeek) {
    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> weekendHourlyRateOre ?: hourlyRateOre
    else -> hourlyRateOre
}

internal fun workPayOre(work: StoredWork): Long? =
    work.appliedHourlyRateOre?.takeIf { work.time > 0 }?.let { estimateHourlyPayOre(it, work.time.toLong()) }

internal fun estimateHourlyPayOre(hourlyRateOre: Int, seconds: Long): Long {
    val wholeHours = seconds / SECONDS_PER_HOUR
    val partialHourOre = (seconds % SECONDS_PER_HOUR) * hourlyRateOre
    return wholeHours * hourlyRateOre +
        (partialHourOre + SECONDS_PER_HOUR / 2) / SECONDS_PER_HOUR
}

internal fun calendarHourlyPayOre(
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
    monthWorks: List<StoredWork>,
): Long? {
    val project = if (selectedProjectIds == null) {
        projects.singleOrNull()
    } else {
        val selectedId = selectedProjectIds.singleOrNull() ?: return null
        projects.firstOrNull { it.id == selectedId }
    } ?: return null
    return calendarMonthPay(monthWorks.filter { it.project == project.id }).amountOre
}
