package no.timnor.app.shared.features.root.presentation.home.components

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeWeekdayLabelTest {
    @Test
    fun inMonthWeekdayUsesLegacyIsoName() {
        assertEquals(
            "MONDAY",
            legacyHomeWeekdayLabel(
                date = LocalDate(2026, 11, 9),
                selectedDate = LocalDate(2026, 11, 1),
                localizedWeekday = "Man",
            ),
        )
    }

    @Test
    fun adjacentMonthWeekdayUsesLegacyNorwegianFallback() {
        assertEquals(
            "Man",
            legacyHomeWeekdayLabel(
                date = LocalDate(2026, 10, 26),
                selectedDate = LocalDate(2026, 11, 1),
                localizedWeekday = "Man",
            ),
        )
    }
}
