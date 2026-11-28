package no.timnor.app.shared.features.root.presentation.home.new_work

import no.timnor.app.shared.domain.work.durationBetweenWorkTimes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkTimeCalculationTest {
    @Test
    fun calculatesSameDayAndOvernightDurations() {
        assertEquals(510, durationBetweenWorkTimes("08:15", "16:45"))
        assertEquals(120, durationBetweenWorkTimes("23:00", "01:00"))
        assertEquals(0, durationBetweenWorkTimes("08:00", "08:00"))
        assertNull(durationBetweenWorkTimes("invalid", "08:00"))
    }
}
