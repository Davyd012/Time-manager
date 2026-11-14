package no.timnor.app.core.database.sharedhours

import androidx.room.Entity
import androidx.room.Index

/** Account-partitioned cache/outbox row. Never stores credentials. */
@Entity(
    tableName = "shared_hours_record",
    primaryKeys = ["accountId", "recordType", "recordKey"],
    indices = [Index(value = ["accountId", "recordType"])],
)
data class SharedHoursRecord(
    val accountId: Int,
    val recordType: String,
    val recordKey: String,
    val payloadJson: String,
    val updatedAtUtc: String,
)
