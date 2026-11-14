package no.timnor.app.core.database.sharedhours

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedHoursRecordDao {
    @Query("SELECT * FROM shared_hours_record WHERE accountId = :accountId AND recordType = :recordType ORDER BY updatedAtUtc")
    suspend fun records(accountId: Int, recordType: String): List<SharedHoursRecord>

    @Query("SELECT * FROM shared_hours_record WHERE accountId = :accountId AND recordType = :recordType AND recordKey = :recordKey LIMIT 1")
    suspend fun record(accountId: Int, recordType: String, recordKey: String): SharedHoursRecord?

    @Query("SELECT * FROM shared_hours_record WHERE recordType = :recordType AND recordKey = :recordKey")
    suspend fun recordsForKey(recordType: String, recordKey: String): List<SharedHoursRecord>

    @Query("SELECT * FROM shared_hours_record WHERE recordType = :recordType")
    suspend fun recordsForType(recordType: String): List<SharedHoursRecord>

    @Query("SELECT * FROM shared_hours_record WHERE recordType = :recordType")
    fun observeRecordsForType(recordType: String): Flow<List<SharedHoursRecord>>

    @Query("SELECT * FROM shared_hours_record WHERE recordType IN (:recordTypes)")
    fun observeRecordsForTypes(recordTypes: List<String>): Flow<List<SharedHoursRecord>>

    @Query("SELECT * FROM shared_hours_record WHERE recordType IN (:recordTypes)")
    suspend fun recordsForTypes(recordTypes: List<String>): List<SharedHoursRecord>

    @Upsert
    suspend fun upsert(record: SharedHoursRecord)

    @Upsert
    suspend fun upsertAll(records: List<SharedHoursRecord>)

    @Query("DELETE FROM shared_hours_record WHERE accountId = :accountId AND recordType = :recordType")
    suspend fun clearType(accountId: Int, recordType: String)

    @Query("DELETE FROM shared_hours_record WHERE accountId = :accountId AND recordType = :recordType AND recordKey = :recordKey")
    suspend fun delete(accountId: Int, recordType: String, recordKey: String)
}
