package no.timnor.app.shared.data.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import no.timnor.app.shared.data.persistence.SharedHoursBackupRecord
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOperation
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection

class TimnorBackupV2Test {
    @Test
    fun v2RoundTripPreservesAccountScopedMappingsAndUnknownRetryPayloadWithoutCredentials() {
        val command = SharedTimeOutboxItem(
            accountId = 44,
            operation = SharedTimeOperation.CREATE_AND_ALLOCATE,
            outcome = SharedTimeOutcome.OUTCOME_UNKNOWN,
            projectId = 9,
            workDate = "2026-11-12",
            durationSeconds = 3_600,
            sourceId = "stable-source",
            privateNote = "private description",
            atomicIdempotencyKey = "immutable-key",
        )
        val shared = listOf(
            SharedHoursBackupRecord(
                44,
                "plannor_project",
                "9",
                Json.encodeToString(
                    PlannorProjectSelection.serializer(),
                    PlannorProjectSelection(44, 9, "Workshop", selected = true,
                        refreshedAtUtc = "2026-11-13T08:00:00Z", linkedLocalProjectId = 3),
                ),
            ),
            SharedHoursBackupRecord(
                44,
                "shared_time_outbox",
                "immutable-key",
                Json.encodeToString(SharedTimeOutboxItem.serializer(), command),
            ),
        )

        val encoded = encodeTimnorBackupV2(
            projects = listOf(StoredProject("local", "Workshop", 3, hourlyRateOre = 25_050, weekendHourlyRateOre = 35_000)),
            works = listOf(StoredWork("private description", 1_794_456_000, 3, 0, 3_600, 12, appliedHourlyRateOre = 18_000, enteredEndMinute = 840)),
            sharedHoursRecords = shared,
            createdAt = "2026-11-13T08:00:00Z",
        )
        val decoded = decodeTimnorBackup(encoded)

        assertEquals(TIMNOR_BACKUP_V2_VERSION, decoded.version)
        assertEquals(25_050, decoded.toStoredProjects().single().hourlyRateOre)
        assertEquals(35_000, decoded.toStoredProjects().single().weekendHourlyRateOre)
        assertEquals(18_000, decoded.toStoredWorks().single().appliedHourlyRateOre)
        assertEquals(840, decoded.toStoredWorks().single().enteredEndMinute)
        assertEquals(shared, decoded.sharedHoursRecords)
        assertEquals("immutable-key", Json.decodeFromString(
            SharedTimeOutboxItem.serializer(), decoded.sharedHoursRecords.last().payloadJson,
        ).atomicIdempotencyKey)
        assertFalse("accessToken" in encoded)
        assertFalse("refreshToken" in encoded)
    }

    @Test
    fun v2ReaderContinuesToAcceptVersionOneBackups() {
        val legacy = encodeTimnorBackupV1(emptyList(), emptyList(), "2026-11-13T08:00:00Z")

        val decoded = decodeTimnorBackup(legacy)

        assertEquals(TIMNOR_BACKUP_VERSION, decoded.version)
        assertEquals(emptyList(), decoded.sharedHoursRecords)
    }

    @Test
    fun backupRejectsCredentialRecordTypes() {
        assertFailsWith<IllegalArgumentException> {
            encodeTimnorBackupV2(
                emptyList(),
                emptyList(),
                listOf(SharedHoursBackupRecord(44, "plannor_session", "session", "{}")),
                "2026-11-13T08:00:00Z",
            )
        }
    }
}
