package no.timnor.app.shared.domain.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDateTime

class UtcWorkTimestampTest {
    @Test
    fun epochZeroDecodesToUtcStartOfEpoch() {
        assertEquals(
            LocalDateTime(1970, 1, 1, 0, 0),
            utcLocalDateTimeFromEpochSeconds(0),
        )
    }

    @Test
    fun negativeEpochSecondDecodesBeforeEpoch() {
        assertEquals(
            LocalDateTime(1969, 12, 31, 23, 59, 59),
            utcLocalDateTimeFromEpochSeconds(-1),
        )
    }

    @Test
    fun leapDayDatetimeUsesTheExistingUtcEpochSecondsValue() {
        val dateTime = LocalDateTime(2024, 2, 29, 12, 34, 56)

        assertEquals(1_709_210_096L, utcEpochSecondsFromLocalDateTime(dateTime))
        assertEquals(dateTime, utcLocalDateTimeFromEpochSeconds(1_709_210_096L))
    }

    @Test
    fun subsecondPrecisionIsDiscardedLikeTheRoomConverter() {
        assertEquals(
            1L,
            utcEpochSecondsFromLocalDateTime(LocalDateTime(1970, 1, 1, 0, 0, 1, 900_000_000)),
        )
        assertEquals(
            -1L,
            utcEpochSecondsFromLocalDateTime(LocalDateTime(1969, 12, 31, 23, 59, 59, 900_000_000)),
        )
    }
}
