package no.timnor.app.shared.features.calendar.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

class MonthShareSummaryTest {
    @Test
    fun classifiesLegacySummaryBandsAtTheirBoundaries() {
        assertEquals(MonthShareSummaryKind.NoHours, monthShareSummaryKind(0))
        assertEquals(MonthShareSummaryKind.NoHours, monthShareSummaryKind(-1))
        assertEquals(MonthShareSummaryKind.Quiet, monthShareSummaryKind(1))
        assertEquals(MonthShareSummaryKind.Quiet, monthShareSummaryKind(20L * 60L * 60L - 1))
        assertEquals(MonthShareSummaryKind.Normal, monthShareSummaryKind(20L * 60L * 60L))
        assertEquals(MonthShareSummaryKind.Normal, monthShareSummaryKind(160L * 60L * 60L - 1))
        assertEquals(MonthShareSummaryKind.Active, monthShareSummaryKind(160L * 60L * 60L))
    }

    @Test
    fun formatsHoursWithLegacyNorwegianTwoDecimalRounding() {
        assertEquals("0,02", formatMonthShareHours(59))
        assertEquals("1,00", formatMonthShareHours(3_600))
        assertEquals("1,01", formatMonthShareHours(3_618))
        assertEquals("12,35", formatMonthShareHours(44_460))
    }
}
