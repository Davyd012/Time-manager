package org.examples.time_manager.core.database.project

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Upsert
    suspend fun upsert(project: Project)

    @Delete
    suspend fun delete(project: Project)

    @Query("SELECT * FROM project")
    fun getAllProjects(): Flow<List<Project>>
}