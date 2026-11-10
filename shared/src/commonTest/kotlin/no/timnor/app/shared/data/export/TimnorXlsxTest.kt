package no.timnor.app.shared.data.export

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import kotlin.test.Test
import kotlin.test.assertTrue

class TimnorXlsxTest {
    @Test
    fun workbookContainsEveryDayIncludingZeroHourDays() {
        val workbook = createTimnorXlsx(
            works = listOf(
                StoredWork(
                    description = "",
                    dateEpochSeconds = utcEpochSecondsFromLocalDateTime(
                        LocalDateTime(LocalDate(2024, 2, 29), LocalTime(7, 0)),
                    ),
                    project = 3,
                    task = 0,
                    time = 3_600,
                    id = 1,
                ),
            ),
            year = 2024,
            month = 2,
        ).decodeToString(throwOnInvalidSequence = false)

        assertTrue(workbook.contains("<row r=\"1\""))
        assertTrue(workbook.contains("<row r=\"29\""))
        assertTrue(workbook.contains("<t>29.02.2024</t>"))
        assertTrue(workbook.contains("<v>0.0</v>"))
    }
}
