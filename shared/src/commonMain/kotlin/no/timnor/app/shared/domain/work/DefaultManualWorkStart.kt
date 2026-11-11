package no.timnor.app.shared.domain.work

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Matches the Android manual-entry default when the user has not picked a start time. */
internal fun defaultManualWorkStart(
    selectedDate: LocalDate,
    durationSeconds: Int,
    now: LocalDateTime,
): LocalDateTime = if (selectedDate == now.date) {
    var date = now.date
    var minutes = now.hour * 60 + now.minute - durationSeconds.coerceAtLeast(0) / 60
    while (minutes < 0) {
        date = date.minus(1, DateTimeUnit.DAY)
        minutes += 24 * 60
    }
    while (minutes >= 24 * 60) {
        date = date.plus(1, DateTimeUnit.DAY)
        minutes -= 24 * 60
    }
    LocalDateTime(date, LocalTime(minutes / 60, minutes % 60))
} else {
    LocalDateTime(selectedDate, LocalTime(0, 0))
}
