package no.timnor.app.shared.platform.stopwatch

import kotlinx.coroutines.flow.StateFlow

/** Observable stopwatch state shared by platform adapters and feature logic. */
data class StopwatchSnapshot(
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val selectedProject: Int = 1,
    val pausedAtEpochSeconds: Long? = null,
)

/**
 * Platform boundary for stopwatch lifecycle and project selection.
 * Implementations own background execution, notifications, and lifecycle policy.
 */
interface StopwatchPlatform {
    val state: StateFlow<StopwatchSnapshot>

    fun start()
    fun pause()
    fun cancel()
    fun timerSaved()
    fun selectProject(projectId: Int)

    suspend fun withPausedTimerForSave(save: suspend (StopwatchSnapshot) -> Unit) {
        pause()
        save(state.value)
    }
}
