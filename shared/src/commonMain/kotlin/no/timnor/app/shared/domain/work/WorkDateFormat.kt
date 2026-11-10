package no.timnor.app.shared.domain.work

import kotlinx.datetime.LocalDate

/** Keeps the ISO date in state/storage while matching Android's work-editor date label. */
fun formatWorkDateInput(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate)
}.getOrNull()?.let { date ->
    "${date.day.toString().padStart(2, '0')}-${(date.month.ordinal + 1).toString().padStart(2, '0')}-${date.year}"
} ?: isoDate
