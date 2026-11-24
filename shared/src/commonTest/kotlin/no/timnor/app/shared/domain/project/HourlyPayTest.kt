package no.timnor.app.shared.domain.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork

class HourlyPayTest {
    @Test
    fun rateUsesExactMinorUnitsAndAcceptsBothDecimalSeparators() {
        assertEquals(25_050, parseHourlyRateOre("250,5"))
        assertEquals(25_050, parseHourlyRateOre("250.50"))
        assertEquals(1, parseHourlyRateOre("0,01"))
        assertEquals(50, parseHourlyRateOre(",5"))
        assertEquals(25_000, parseHourlyRateOre("250,"))
        assertEquals(25_050, parseHourlyRateOre("\u0662\u0665\u0660\u066B\u0665"))
        listOf("", ".", "0", "-5", "12,345", "1,234.50", "1e3", "1000000").forEach {
            assertNull(parseHourlyRateOre(it))
        }
    }

    @Test
    fun calendarEstimatesOnlyForOneRatedProjectWithTrackedTime() {
        val paid = StoredProject("", "Paid", 1, hourlyRateOre = 25_001)
        val unpaid = StoredProject("", "Unpaid", 2)
        val works = listOf(
            StoredWork("", 0, 1, 0, 900, 1),
            StoredWork("", 0, 1, 0, 900, 2),
            StoredWork("", 0, 2, 0, 3600, 3),
        )
        val projects = listOf(paid, unpaid)

        assertEquals(12_501L, calendarHourlyPayOre(projects, setOf(1), works))
        assertEquals(12_501L, calendarHourlyPayOre(listOf(paid), null, works))
        assertNull(calendarHourlyPayOre(projects, null, works))
        assertNull(calendarHourlyPayOre(projects, setOf(1, 2), works))
        assertNull(calendarHourlyPayOre(projects, setOf(2), works))
        assertNull(calendarHourlyPayOre(projects, setOf(1), emptyList()))
    }
}
