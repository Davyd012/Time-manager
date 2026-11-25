package no.timnor.app.shared.domain.calendar

import kotlinx.datetime.LocalDate
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds

internal fun worksInCalendarMonth(works: List<StoredWork>, month: LocalDate): List<StoredWork> =
    works.filter { work ->
        val date = utcLocalDateTimeFromEpochSeconds(work.dateEpochSeconds).date
        date.year == month.year && date.month == month.month
    }

internal fun calendarVisibleWorks(
    works: List<StoredWork>,
    projects: List<StoredProject>,
    selectedProjectIds: Set<Int>?,
): List<StoredWork> {
    if (selectedProjectIds == null) return works
    val validProjectIds = projects.mapTo(mutableSetOf(), StoredProject::id)
    return works.filter { it.project in selectedProjectIds && it.project in validProjectIds }
}
