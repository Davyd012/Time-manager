package no.timnor.app.shared.domain.project

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.calendar.calendarVisibleWorks

class HourlyPayTest {
    @Test
    fun rateUsesExactMinorUnitsAndAcceptsBothDecimalSeparators() {
        assertEquals(25_050, parseHourlyRateOre("250,5"))
        assertEquals(25_050, parseHourlyRateOre("250.50"))
        assertEquals(1, parseHourlyRateOre("0,01"))
        assertEquals(50, parseHourlyRateOre(",5"))
        assertEquals(25_000, parseHourlyRateOre("250,"))
        assertEquals(25_050, parseHourlyRateOre("\u0662\u0665\u0660\u066B\u0665"))
        assertEquals(0, parseHourlyRateOre("0"))
        listOf("", ".", "-5", "12,345", "1,234.50", "1e3", "1000000").forEach {
            assertNull(parseHourlyRateOre(it))
        }
    }

    @Test
    fun calendarEstimatesOnlyForOneRatedProjectWithTrackedTime() {
        val paid = StoredProject("", "Paid", 1, hourlyRateOre = 25_001)
        val unpaid = StoredProject("", "Unpaid", 2)
        val works = listOf(
            StoredWork("", 0, 1, 0, 900, 1, appliedHourlyRateOre = 25_001),
            StoredWork("", 0, 1, 0, 900, 2, appliedHourlyRateOre = 25_001),
            StoredWork("", 0, 2, 0, 3600, 3),
        )
        val projects = listOf(paid, unpaid)

        assertEquals(12_500L, calendarHourlyPayOre(projects, setOf(1), works))
        assertEquals(12_500L, calendarHourlyPayOre(listOf(paid), null, works))
        assertNull(calendarHourlyPayOre(projects, null, works))
        assertNull(calendarHourlyPayOre(projects, setOf(1, 2), works))
        assertNull(calendarHourlyPayOre(projects, setOf(2), works))
        assertNull(calendarHourlyPayOre(projects, setOf(1), emptyList()))
    }

    @Test
    fun monthPayTotalsTrackedTimeAtEachVisibleProjectRate() {
        val projects = listOf(
            StoredProject("", "Weekend", 1, hourlyRateOre = 22_000),
            StoredProject("", "Weekday", 2, hourlyRateOre = 18_000),
            StoredProject("", "Unrated", 3),
            StoredProject("", "Unused", 4),
        )
        val ratedWorks = listOf(
            StoredWork("", 0, 1, 0, 7 * 3600, 1, appliedHourlyRateOre = 22_000),
            StoredWork("", 0, 1, 0, 6 * 3600, 2, appliedHourlyRateOre = 22_000),
            StoredWork("", 0, 2, 0, 4 * 3600 + 10 * 60, 3, appliedHourlyRateOre = 18_000),
        )
        val works = ratedWorks + listOf(
            StoredWork("", 0, 3, 0, 3600, 4),
            StoredWork("", 0, 0, 0, 900, 5),
        )

        fun payFor(filter: Set<Int>?) = calendarMonthPay(calendarVisibleWorks(works, projects, filter))

        assertEquals(CalendarMonthPay(361_000L, 0L), calendarMonthPay(ratedWorks))
        assertEquals(CalendarMonthPay(361_000L, 4_500L), payFor(null))
        assertEquals(CalendarMonthPay(361_000L, 0L), payFor(setOf(1, 2)))
        assertEquals(CalendarMonthPay(75_000L, 3_600L), payFor(setOf(2, 3)))
        assertEquals(CalendarMonthPay(286_000L, 0L), payFor(setOf(1)))
        assertEquals(CalendarMonthPay(null, 3_600L), payFor(setOf(3)))
        assertEquals(CalendarMonthPay(null, 0L), payFor(emptySet()))
    }

    @Test
    fun weekendUsesItsOwnRateIncludingZeroAndFallsBackToTheStandardRate() {
        val project = StoredProject("", "Project", 1, hourlyRateOre = 18_000, weekendHourlyRateOre = 0)
        assertEquals(18_000, project.hourlyRateFor(LocalDate(2026, 11, 13)))
        assertEquals(0, project.hourlyRateFor(LocalDate(2026, 11, 14)))
        assertEquals(0, project.hourlyRateFor(LocalDate(2026, 11, 15)))
        assertEquals(18_000, project.copy(weekendHourlyRateOre = null).hourlyRateFor(LocalDate(2026, 11, 15)))
    }

    @Test
    fun workPayDistinguishesZeroFromAnUnknownHistoricalRate() {
        val oldWork = StoredWork("", 0, 1, 0, 3600, 1)
        assertNull(workPayOre(oldWork))
        val zeroRateWork = oldWork.copy(appliedHourlyRateOre = 0)
        assertEquals(0L, workPayOre(zeroRateWork))
        assertEquals(CalendarMonthPay(0L, 3600L), calendarMonthPay(listOf(oldWork, zeroRateWork)))
    }

    @Test
    fun monthPayFormatsKronerWithLocalizedGroupingAndDecimalSeparators() {
        assertEquals("3\u00A0610", formatPayAmount(361_000L, ",", "\u00A0"))
        assertEquals("3,610.50", formatPayAmount(361_050L, ".", ","))
        assertEquals("3610.50", formatPayAmount(361_050L, "."))
    }
}
