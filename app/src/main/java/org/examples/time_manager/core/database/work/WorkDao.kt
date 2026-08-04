package org.examples.time_manager.core.database.work

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    @Upsert
    suspend fun upsert(work: Work)

    @Delete
    suspend fun delete(work: Work)

    @Query("SELECT * FROM work WHERE date >= :startOfDay AND date <= :endOfDay")
    fun getAllWorks(startOfDay: Long, endOfDay: Long): Flow<List<Work>>

    @Query("SELECT * FROM work WHERE date >= :startOfDay AND date <= :endOfDay AND project IN (:ids)")
    fun getAllWorksForProjects(startOfDay: Long, endOfDay: Long, ids: List<Int>): Flow<List<Work>>

    @Query("SELECT SUM(time) FROM work WHERE date >= :startOfDay AND date <= :endOfDay")
    fun getHoursByDay(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT SUM(time) FROM work WHERE date >= :startOfDay AND date <= :endOfDay AND project IN (:ids)")
    fun getHoursByDayAndProject(startOfDay: Long, endOfDay: Long, ids: List<Int>): Int
}
