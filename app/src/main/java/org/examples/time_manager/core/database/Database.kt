package org.examples.time_manager.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.project.ProjectDao
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.database.work.WorkDao

@Database(
    entities = [Work::class, Project::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class Database: RoomDatabase() {

    abstract fun workDao(): WorkDao

    abstract fun projectDao(): ProjectDao
}