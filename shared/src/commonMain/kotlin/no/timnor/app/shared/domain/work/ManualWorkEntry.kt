package no.timnor.app.shared.domain.work

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime

/** Builds the persisted record used by the initial shared manual-entry form. */
fun createManualWork(
    projectId: Int?,
    date: String,
    hours: Int?,
    minutes: Int?,
    description: String,
): StoredWork? {
    val id = projectId ?: return null
    val entryDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    val duration = manualWorkDurationSeconds(hours, minutes) ?: return null
    return StoredWork(
        description = description.trim(),
        dateEpochSeconds = utcEpochSecondsFromLocalDateTime(LocalDateTime(entryDate, LocalTime(7, 0))),
        project = id,
        task = 0,
        time = duration,
        id = 0,
    )
}

/** Returns a positive, storable duration or null for incomplete/out-of-range input. */
fun manualWorkDurationSeconds(hours: Int?, minutes: Int?): Int? {
    if (hours == null || minutes == null || hours < 0 || minutes !in 0..59) return null
    val seconds = hours.toLong() * 3600L + minutes.toLong() * 60L
    return seconds.takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt()
}
