package no.timnor.app.shared.domain.project

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
    if (!hourlyRatePattern.matches(normalized)) return null
    val parts = normalized.split('.')
    val kroner = parts[0].ifEmpty { "0" }.toIntOrNull() ?: return null
    if (kroner > MAX_HOURLY_RATE_ORE / ORE_PER_KRONE) return null
    val ore = parts.getOrNull(1)?.padEnd(2, '0')?.toInt() ?: 0
    return (kroner * ORE_PER_KRONE + ore).takeIf { it > 0 }
}

internal fun formatPayAmount(ore: Long, decimalSeparator: String): String {
    val kroner = ore / ORE_PER_KRONE
    val fraction = ore % ORE_PER_KRONE
    return if (fraction == 0L) kroner.toString()
    else "$kroner$decimalSeparator${fraction.toString().padStart(2, '0')}"
}

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
    val rate = project.hourlyRateOre ?: return null
    val seconds = monthWorks.sumOf { if (it.project == project.id) it.time.toLong() else 0L }
    return if (seconds > 0) estimateHourlyPayOre(rate, seconds) else null
}
