package no.timnor.app.core.repository

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupFormatTest {
    private val json = Json { ignoreUnknownKeys = false }

    @Test
    fun roundTripsAllFieldsAndOrphanReferences() {
        val backup = BackupFile(
            format = RoomBackupRepository.FORMAT,
            version = RoomBackupRepository.VERSION,
            createdAt = "2026-11-08T12:30:00Z",
            projects = listOf(BackupProject(-4, "", "Ångström 🧭")),
            works = listOf(BackupWork(71, "équipe 👋", "2024-02-29T23:59:59", 999, 27, 3601)),
        )

        val decoded = json.decodeFromString(BackupFile.serializer(), json.encodeToString(BackupFile.serializer(), backup))

        assertEquals(backup, decoded)
    }

    @Test
    fun roundTripsEmptyTables() {
        val backup = BackupFile(RoomBackupRepository.FORMAT, 1, "2026-11-08T12:30:00Z", emptyList(), emptyList())
        assertEquals(backup, json.decodeFromString(BackupFile.serializer(), json.encodeToString(BackupFile.serializer(), backup)))
    }

    @Test
    fun rejectsMissingRequiredFieldsAndUnknownFields() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString(BackupFile.serializer(), """{"format":"x","version":1}""")
        }
        assertThrows(SerializationException::class.java) {
            json.decodeFromString(BackupFile.serializer(), """{"format":"x","version":1,"createdAt":"2026-11-08T00:00:00Z","projects":[],"works":[],"extra":true}""")
        }
    }

    @Test
    fun rejectsDuplicateIdsAndMalformedDates() {
        val valid = BackupFile(RoomBackupRepository.FORMAT, 1, "2026-11-08T12:30:00Z", emptyList(), emptyList())
        assertThrows(IllegalArgumentException::class.java) {
            validateBackupV1(valid.copy(projects = listOf(BackupProject(2, "", "a"), BackupProject(2, "", "b"))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateBackupV1(valid.copy(works = listOf(BackupWork(1, "", "not-a-date", 88, 1, 15))))
        }
    }

    @Test
    fun rejectsIntegerOverflowInJson() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString(
                BackupFile.serializer(),
                """{"format":"${RoomBackupRepository.FORMAT}","version":1,"createdAt":"2026-11-08T12:30:00Z","projects":[],"works":[{"id":1,"description":"","date":"2026-01-01T00:00:00","project":1,"task":2147483648,"time":1}]}""",
            )
        }
    }
}
