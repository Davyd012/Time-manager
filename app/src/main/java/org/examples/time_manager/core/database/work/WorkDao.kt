package org.examples.time_manager.core.database.work

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    @Insert
    suspend fun upsert(person: Work)

    @Delete
    suspend fun delete(person: Work)

    @Query("SELECT * FROM work WHERE date >= :startOfDay AND date <= :endOfDay")
    fun getAllWorks(startOfDay: Long, endOfDay: Long): Flow<List<Work>>

    @Query("SELECT SUM(time) FROM work WHERE date >= :startOfDay AND date <= :endOfDay")
    fun getHoursByDay(startOfDay: Long, endOfDay: Long): Int
}