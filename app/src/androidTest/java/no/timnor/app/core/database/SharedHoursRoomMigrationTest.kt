package no.timnor.app.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import no.timnor.app.core.database.sharedhours.SharedHoursRecord
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedHoursRoomMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        Database::class.java,
    )

    @Test
    fun migrationAddsSharedHoursTableAndKeepsExistingLocalRows() {
        migrationHelper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL("INSERT INTO Project (description, name, id) VALUES ('local project notes', 'Local project', 7)")
            execSQL("INSERT INTO Work (description, date, project, task, time, id) VALUES ('local work', 1700000000, 7, 2, 3600, 11)")
            close()
        }

        migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE, 5, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
        ).use { migrated ->
            assertEquals("Local project", migrated.scalarString("SELECT name FROM Project WHERE id = 7"))
            assertEquals("local work", migrated.scalarString("SELECT description FROM Work WHERE id = 11"))
            assertEquals(1L, migrated.scalarLong("SELECT COUNT(*) FROM Work WHERE id = 11 AND enteredEndMinute IS NULL"))
            assertEquals(1L, migrated.scalarLong("SELECT COUNT(*) FROM Work WHERE id = 11 AND appliedHourlyRateOre IS NULL"))
            assertEquals(0, migrated.scalarLong("SELECT COUNT(*) FROM shared_hours_record"))
            assertEquals(1, migrated.scalarLong("SELECT COUNT(*) FROM Project WHERE id = 7 AND hourlyRateOre IS NULL"))
        }
    }

    @Test
    fun migrationFreezesTheExistingProjectRateForHistoricalWork() {
        val name = "hourly-rate-migration"
        migrationHelper.createDatabase(name, 3).use { database ->
            database.execSQL(
                "INSERT INTO Project (description, name, id, hourlyRateOre) VALUES ('', 'Paid project', 7, 18050)",
            )
            database.execSQL(
                "INSERT INTO Work (description, date, project, task, time, id) VALUES ('', 1700000000, 7, 0, 3600, 11)",
            )
        }
        migrationHelper.runMigrationsAndValidate(name, 4, true, MIGRATION_3_4).use { database ->
            database.execSQL("UPDATE Project SET hourlyRateOre = 20000, weekendHourlyRateOre = 22000 WHERE id = 7")
            assertEquals(18_050L, database.scalarLong("SELECT appliedHourlyRateOre FROM Work WHERE id = 11"))
        }
    }

    @Test
    fun manualRestoreReplacementPreservesAccountScopedSharedHoursRecords() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, Database::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val preserved = SharedHoursRecord(
                accountId = 42,
                recordType = "personal_time_entry",
                recordKey = "entry-9",
                payloadJson = "{\"durationSeconds\":3600}",
                updatedAtUtc = "2026-11-12T12:00:00Z",
            )
            database.sharedHoursRecordDao().upsert(preserved)

            RoomPersistenceStore(database).replaceAll(
                projects = listOf(StoredProject("restored", "Restored project", 3, hourlyRateOre = 25_050, weekendHourlyRateOre = 0)),
                works = listOf(StoredWork("restored work", 1700000000, 3, 0, 1800, 4, appliedHourlyRateOre = 0, enteredEndMinute = 1363)),
            )

            assertEquals(preserved, database.sharedHoursRecordDao().record(42, "personal_time_entry", "entry-9"))
            assertEquals("Restored project", database.projectDao().getAll().single().name)
            assertEquals(25_050, database.projectDao().getAll().single().hourlyRateOre)
            assertEquals(0, database.projectDao().getAll().single().weekendHourlyRateOre)
            assertEquals("restored work", database.workDao().getAll().single().description)
            assertEquals(0, database.workDao().getAll().single().appliedHourlyRateOre)
            assertEquals(1363, RoomPersistenceStore(database).snapshotAll().works.single().enteredEndMinute)
        } finally {
            database.close()
        }
    }

    private fun SupportSQLiteDatabase.scalarString(sql: String): String = query(sql).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getString(0)
    }

    private fun SupportSQLiteDatabase.scalarLong(sql: String): Long = query(sql).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getLong(0)
    }

    private companion object {
        const val TEST_DATABASE = "shared-hours-migration-test"
    }
}
