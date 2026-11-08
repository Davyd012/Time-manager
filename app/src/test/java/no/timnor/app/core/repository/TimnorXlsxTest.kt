package no.timnor.app.core.repository

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import no.timnor.app.shared.data.export.createTimnorXlsx
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimnorXlsxTest {
    @Test
    fun createsTimerSheetWithDateAndDecimalHourCells() {
        val works = listOf(
            work(1, 2024, 2, 1, 1_800),
            work(2, 2024, 2, 1, 1_800),
            work(3, 2024, 3, 1, 3_600),
        )

        val files = ZipInputStream(ByteArrayInputStream(createTimnorXlsx(works, 2024, 2))).use { zip ->
            buildMap {
                while (true) {
                    val entry = zip.nextEntry ?: break
                    put(entry.name, zip.readBytes().decodeToString())
                }
            }
        }

        assertEquals(
            setOf(
                "[Content_Types].xml",
                "_rels/.rels",
                "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels",
                "xl/styles.xml",
                "xl/worksheets/sheet1.xml",
            ),
            files.keys,
        )
        assertTrue(files.getValue("xl/workbook.xml").contains("name=\"Timer\""))
        val sheet = files.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("<t>01.02.2024</t>"))
        assertTrue(sheet.contains("<v>1.0</v>"))
        assertTrue(!sheet.contains("01.03.2024"))
        assertTrue(files.getValue("xl/styles.xml").contains("horizontal=\"right\""))
    }

    private fun work(id: Int, year: Int, month: Int, day: Int, seconds: Int) = StoredWork(
        id = id,
        description = "",
        dateEpochSeconds = utcEpochSecondsFromLocalDateTime(
            LocalDateTime(LocalDateTime(year, month, day, 7, 0).date, LocalTime(7, 0)),
        ),
        project = 1,
        task = 0,
        time = seconds,
    )
}
