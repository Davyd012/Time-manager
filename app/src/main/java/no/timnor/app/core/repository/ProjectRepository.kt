package no.timnor.app.core.repository

import kotlinx.coroutines.flow.Flow
import no.timnor.app.core.database.project.Project

interface ProjectRepository {
    fun observeAll(): Flow<List<Project>>
    suspend fun upsert(project: Project)
    suspend fun delete(project: Project)
}
