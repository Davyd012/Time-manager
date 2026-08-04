package org.examples.time_manager.core.repository

import kotlinx.coroutines.flow.StateFlow

data class StopwatchSnapshot(
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val selectedProject: Int = 1,
)

interface StopwatchRepository {
    val state: StateFlow<StopwatchSnapshot>
    fun start()
    fun pause()
    fun cancel()
    fun selectProject(projectId: Int)
}
