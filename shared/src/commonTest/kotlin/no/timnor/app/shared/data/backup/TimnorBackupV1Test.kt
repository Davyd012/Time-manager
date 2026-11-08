package no.timnor.app.shared.data.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork

class TimnorBackupV1Test {
    @Test
    fun roundTripsVersionOneFieldsAndUtcWorkDate() {
        val projects = listOf(StoredProject(description = "Client", name = "Project", id = 3))
        val works = listOf(
            StoredWork(
                description = "Review",
                dateEpochSeconds = 1_793_934_000,
                project = 3,
                task = 0,
                time = 3_600,
                id = 9,
            ),
        )

        val decoded = decodeTimnorBackupV1(
            encodeTimnorBackupV1(projects, works, "2026-11-08T10:15:30Z"),
        )

        assertEquals("org.examples.time_manager.backup", decoded.format)
        assertEquals(1, decoded.version)
        assertEquals(projects, decoded.toStoredProjects())
        assertEquals(works, decoded.toStoredWorks())
    }

    @Test
    fun rejectsUnknownFieldsDuplicateIdsAndBadDates() {
        val duplicateIds = TimnorBackupV1(
            format = TIMNOR_BACKUP_FORMAT,
            version = 1,
            createdAt = "2026-11-08T10:15:30Z",
            projects = listOf(
                TimnorBackupProject(1, "", "A"),
                TimnorBackupProject(1, "", "B"),
            ),
            works = emptyList(),
        )
        assertFailsWith<IllegalArgumentException> { validateTimnorBackupV1(duplicateIds) }
        assertFailsWith<Exception> {
            decodeTimnorBackupV1(
                """{"format":"org.examples.time_manager.backup","version":1,"createdAt":"2026-11-08T10:15:30Z","projects":[],"works":[],"extra":true}""",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            decodeTimnorBackupV1(
                """{"format":"org.examples.time_manager.backup","version":1,"createdAt":"2026-11-08T10:15:30Z","projects":[],"works":[{"id":1,"description":"","date":"bad","project":0,"task":0,"time":1}]}""",
            )
        }
    }

    @Test
    fun identifiesFutureVersionsBeforeDecodingTheirFields() {
        val error = assertFailsWith<UnsupportedTimnorBackupVersionException> {
            decodeTimnorBackupV1("""{"version":2}""")
        }
        assertEquals(2, error.backupVersion)
    }
}
