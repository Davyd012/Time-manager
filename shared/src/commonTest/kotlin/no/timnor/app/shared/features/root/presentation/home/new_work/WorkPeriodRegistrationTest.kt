package no.timnor.app.shared.features.root.presentation.home.new_work

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkPeriodRegistrationTest {
    @Test
    fun selectedPeriodIncludesBothEndpoints() {
        assertEquals(
            listOf(LocalDate(2026, 6, 5), LocalDate(2026, 6, 6), LocalDate(2026, 6, 7)),
            inclusivePeriodDates(LocalDate(2026, 6, 5), LocalDate(2026, 6, 7)),
        )
    }

    @Test
    fun oneDayPeriodIsKeptAsAValidInclusiveSelection() {
        val date = LocalDate(2026, 6, 8)
        assertEquals(listOf(date), inclusivePeriodDates(date, date))
    }

    @Test
    fun incompleteOrReversedPeriodHasNoDates() {
        assertEquals(emptyList(), inclusivePeriodDates(null, LocalDate(2026, 6, 7)))
        assertEquals(emptyList(), inclusivePeriodDates(LocalDate(2026, 6, 7), LocalDate(2026, 6, 5)))
    }
}
