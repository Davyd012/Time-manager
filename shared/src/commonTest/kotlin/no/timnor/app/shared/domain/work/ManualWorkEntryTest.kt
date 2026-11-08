package no.timnor.app.shared.domain.work

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

class ManualWorkEntryTest {
    @Test
    fun createsRecordAtExistingSevenAmEntryTime() {
        val work = createManualWork(12, "2026-11-08", 1, 30, "  Planning  ")

        assertEquals(12, work?.project)
        assertEquals(5_400, work?.time)
        assertEquals("Planning", work?.description)
        assertEquals(
            utcEpochSecondsFromLocalDateTime(LocalDateTime(LocalDate(2026, 11, 8), LocalTime(7, 0))),
            work?.dateEpochSeconds,
        )
    }

    @Test
    fun rejectsInvalidOrUnrepresentableValues() {
        assertNull(createManualWork(null, "2026-11-08", 1, 0, ""))
        assertNull(createManualWork(1, "invalid", 1, 0, ""))
        assertNull(createManualWork(1, "2026-11-08", 0, 0, ""))
        assertNull(manualWorkDurationSeconds(-1, 0))
        assertNull(manualWorkDurationSeconds(1, 60))
        assertNull(manualWorkDurationSeconds(Int.MAX_VALUE, 0))
    }
}
