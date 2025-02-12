package org.examples.time_manager.core.database

import android.content.Context
import androidx.room.InvalidationTracker
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.examples.time_manager.core.database.project.Project
import org.examples.time_manager.core.database.project.ProjectDao
import org.examples.time_manager.core.database.work.Work
import org.examples.time_manager.core.database.work.WorkDao

fun getDatabase(context: Context?): Database {
    if (context == null){
        return DummyDatabase()
    }
    val dbFile = context.getDatabasePath("work.db")
    return Room.databaseBuilder<Database>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
        .build()
}

class DummyDatabase : Database() {
    override fun workDao(): WorkDao {
        return DummyWorkDao()
    }

    override fun projectDao(): ProjectDao {
        return DummyProjectDao()
    }

    override fun clearAllTables() {
        TODO("Not yet implemented")
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        TODO("Not yet implemented")
    }
}

class DummyWorkDao : WorkDao {
    override suspend fun upsert(work: Work) {

    }

    override suspend fun delete(work: Work) {

    }

    override fun getAllWorks(startOfDay: Long, endOfDay: Long): Flow<List<Work>> {
        return emptyFlow<List<Work>>()
    }

    override fun getHoursByDay(startOfDay: Long, endOfDay: Long): Int {
        return 0
    }

    override fun getHoursByDayAndProject(startOfDay: Long, endOfDay: Long, ids: List<Int>): Int {
        return 0
    }
}

class DummyProjectDao : ProjectDao {
    override suspend fun upsert(person: Project) {}

    override suspend fun delete(person: Project) {}

    override fun getAllProjects(): Flow<List<Project>> {
        return emptyFlow<List<Project>>()
    }
}