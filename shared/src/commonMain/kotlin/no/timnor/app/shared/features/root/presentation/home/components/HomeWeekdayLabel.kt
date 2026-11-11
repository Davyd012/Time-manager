package no.timnor.app.shared.features.root.presentation.home.components

import kotlinx.datetime.LocalDate

/** Matches the legacy Home week row: ISO names for in-month days and Norwegian fallback labels outside it. */
internal fun legacyHomeWeekdayLabel(
    date: LocalDate,
    selectedDate: LocalDate,
    localizedWeekday: String,
): String = if (date.year == selectedDate.year && date.month == selectedDate.month) {
    date.dayOfWeek.name
} else {
    localizedWeekday
}
