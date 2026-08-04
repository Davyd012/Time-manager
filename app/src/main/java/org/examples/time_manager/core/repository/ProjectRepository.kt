package org.examples.time_manager.core.repository

import kotlinx.coroutines.flow.Flow
import org.examples.time_manager.core.database.project.Project

interface ProjectRepository {
    fun observeAll(): Flow<List<Project>>
    suspend fun upsert(project: Project)
    suspend fun delete(project: Project)
}
