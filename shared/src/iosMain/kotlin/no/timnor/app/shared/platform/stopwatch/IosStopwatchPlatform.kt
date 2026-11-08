package no.timnor.app.shared.platform.stopwatch

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.timnor.app.shared.domain.stopwatch.stopwatchElapsedSeconds
import platform.Foundation.NSUserDefaults
import kotlin.time.Clock

/**
 * iOS stopwatch adapter. It persists elapsed seconds and the wall-clock start instant,
 * then derives elapsed time when the app is active. iOS may suspend this process in
 * the background, so this implementation does not promise continuous background work.
 */
@OptIn(ExperimentalForeignApi::class)
class IosStopwatchPlatform : StopwatchPlatform {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var accumulatedSeconds = defaults.doubleForKey(ELAPSED_KEY).toLong().coerceAtLeast(0)
    private var startedAtEpochSeconds: Long? = if (defaults.boolForKey(RUNNING_KEY)) {
        defaults.doubleForKey(STARTED_AT_KEY).toLong()
    } else {
        null
    }
    private val mutableState = MutableStateFlow(snapshot())
    private var ticker: Job? = null

    override val state: StateFlow<StopwatchSnapshot> = mutableState.asStateFlow()

    init {
        if (startedAtEpochSeconds != null) startTicker()
    }

    override fun start() {
        if (startedAtEpochSeconds != null) return
        startedAtEpochSeconds = Clock.System.now().epochSeconds
        defaults.setDouble(startedAtEpochSeconds!!.toDouble(), STARTED_AT_KEY)
        defaults.setBool(true, RUNNING_KEY)
        publish()
        startTicker()
    }

    override fun pause() {
        val start = startedAtEpochSeconds ?: return
        accumulatedSeconds = stopwatchElapsedSeconds(
            accumulatedSeconds,
            start,
            Clock.System.now().epochSeconds,
        ).toLong()
        startedAtEpochSeconds = null
        defaults.setDouble(accumulatedSeconds.toDouble(), ELAPSED_KEY)
        defaults.setBool(false, RUNNING_KEY)
        ticker?.cancel()
        ticker = null
        publish()
    }

    override fun cancel() {
        accumulatedSeconds = 0
        startedAtEpochSeconds = null
        defaults.setDouble(0.0, ELAPSED_KEY)
        defaults.setDouble(0.0, STARTED_AT_KEY)
        defaults.setBool(false, RUNNING_KEY)
        ticker?.cancel()
        ticker = null
        publish()
    }

    override fun timerSaved() = cancel()

    override fun selectProject(projectId: Int) {
        defaults.setInteger(projectId.toLong(), PROJECT_KEY)
        publish()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                publish()
                delay(1_000)
            }
        }
    }

    private fun publish() {
        mutableState.value = snapshot()
    }

    private fun snapshot(): StopwatchSnapshot {
        val elapsed = stopwatchElapsedSeconds(
            accumulatedSeconds,
            startedAtEpochSeconds,
            Clock.System.now().epochSeconds,
        )
        val selectedProject = defaults.objectForKey(PROJECT_KEY)?.let { defaults.integerForKey(PROJECT_KEY).toInt() } ?: 1
        return StopwatchSnapshot(
            elapsedSeconds = elapsed,
            isRunning = startedAtEpochSeconds != null,
            selectedProject = selectedProject,
        )
    }

    private companion object {
        const val ELAPSED_KEY = "timnor.stopwatch.elapsed-seconds"
        const val STARTED_AT_KEY = "timnor.stopwatch.started-at"
        const val RUNNING_KEY = "timnor.stopwatch.running"
        const val PROJECT_KEY = "timnor.stopwatch.project"
    }
}
