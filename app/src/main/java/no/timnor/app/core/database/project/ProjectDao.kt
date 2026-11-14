package no.timnor.app.core.database.project

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Insert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert
    suspend fun insert(project: Project): Long
    @Query("SELECT * FROM project ORDER BY id")
    suspend fun getAll(): List<Project>

    @Query("DELETE FROM project")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsertAll(projects: List<Project>)

    @Upsert
    suspend fun upsert(project: Project)

    @Delete
    suspend fun delete(project: Project)

    @Query("SELECT * FROM project")
    fun getAllProjects(): Flow<List<Project>>
}
