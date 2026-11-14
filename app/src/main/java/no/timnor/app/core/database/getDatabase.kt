package no.timnor.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `shared_hours_record` (
                `accountId` INTEGER NOT NULL,
                `recordType` TEXT NOT NULL,
                `recordKey` TEXT NOT NULL,
                `payloadJson` TEXT NOT NULL,
                `updatedAtUtc` TEXT NOT NULL,
                PRIMARY KEY(`accountId`, `recordType`, `recordKey`)
            )""".trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_shared_hours_record_accountId_recordType` " +
                "ON `shared_hours_record` (`accountId`, `recordType`)",
        )
    }
}

fun getDatabase(context: Context): Database =
    Room.databaseBuilder<Database>(
        context.applicationContext,
        context.getDatabasePath("work.db").absolutePath,
    ).addMigrations(MIGRATION_1_2).build()
