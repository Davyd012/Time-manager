package no.timnor.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import no.timnor.app.core.database.project.Project
import no.timnor.app.core.database.project.ProjectDao
import no.timnor.app.core.database.work.Work
import no.timnor.app.core.database.work.WorkDao

@Database(
    entities = [Work::class, Project::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class Database: RoomDatabase() {

    abstract fun workDao(): WorkDao

    abstract fun projectDao(): ProjectDao
}