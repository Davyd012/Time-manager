package no.timnor.app.shared.platform.stopwatch

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.timnor.app.shared.domain.stopwatch.MAX_STOPWATCH_MILLIS
import no.timnor.app.shared.domain.stopwatch.STOPWATCH_MILLIS_PER_SECOND
import no.timnor.app.shared.domain.stopwatch.stopwatchElapsedMillis
import no.timnor.app.shared.platform.documents.TimnorRestoreGate
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID
import kotlin.time.Clock

data class IosStopwatchNotificationState(val revision: String, val isRunning: Boolean)

/** Process-wide timer shared by Compose and notification actions. Access on the main thread. */
@OptIn(ExperimentalForeignApi::class)
object IosStopwatchPlatform : StopwatchPlatform, TimnorRestoreGate {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val timerJson = Json { encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timer = loadTimer()
    private var elapsedMillis = timer.accumulatedMillis
    private var selectedProject = defaults.objectForKey(PROJECT_KEY)
        ?.let { defaults.integerForKey(PROJECT_KEY).toInt() } ?: 1
    private val mutableState = MutableStateFlow(snapshot())
    private var ticker: Job? = null
    private var appActive = false
    private var restoreInProgress = false
    private var saveInProgress = false
    private var notificationObserver: ((IosStopwatchNotificationState?) -> Unit)? = null

    override val state: StateFlow<StopwatchSnapshot> = mutableState.asStateFlow()

    fun setNotificationObserver(observer: (IosStopwatchNotificationState?) -> Unit) {
        notificationObserver = observer
        publishNotification()
    }

    fun setAppActive(active: Boolean) {
        appActive = active
        publish()
        updateTicker()
    }

    fun handleNotificationAction(revision: String, resume: Boolean) {
        if (revision != timer.revision || restoreInProgress || saveInProgress) return
        if (resume) start() else pause()
    }

    override fun start() {
        if (restoreInProgress || saveInProgress || timer.startedAtMillis != null) return
        updateTimer(timer.copy(startedAtMillis = nowMillis()))
    }

    override fun pause() {
        if (timer.startedAtMillis == null) return
        updateTimer(timer.copy(accumulatedMillis = currentElapsedMillis(), startedAtMillis = null))
    }

    override fun cancel() = updateTimer(PersistedTimer())

    override fun timerSaved() = cancel()

    override fun selectProject(projectId: Int) {
        if (saveInProgress) return
        selectedProject = projectId
        defaults.setInteger(projectId.toLong(), PROJECT_KEY)
        publish()
    }

    override suspend fun withPausedTimerForSave(save: suspend (StopwatchSnapshot) -> Unit) {
        if (saveInProgress || restoreInProgress) return
        saveInProgress = true
        try {
            if (timer.startedAtMillis != null) pause() else publishNotification()
            save(state.value)
        } finally {
            saveInProgress = false
            publishNotification()
        }
    }

    override suspend fun withTimerClear(restore: suspend () -> Unit): Boolean {
        if (restoreInProgress || saveInProgress || state.value.isRunning || state.value.elapsedSeconds > 0) return false
        restoreInProgress = true
        return try {
            restore()
            if (elapsedMillis > 0) cancel()
            true
        } finally {
            restoreInProgress = false
        }
    }

    private fun updateTimer(updated: PersistedTimer) {
        timer = updated.copy(revision = NSUUID().UUIDString)
        defaults.setObject(timerJson.encodeToString(timer), TIMER_KEY)
        elapsedMillis = timer.accumulatedMillis
        publish()
        updateTicker()
        publishNotification()
    }

    private fun updateTicker() {
        if (!appActive || timer.startedAtMillis == null) {
            ticker?.cancel()
            ticker = null
        } else if (ticker?.isActive != true) {
            ticker = scope.launch {
                while (isActive) {
                    val remaining = STOPWATCH_MILLIS_PER_SECOND - elapsedMillis % STOPWATCH_MILLIS_PER_SECOND
                    delay(remaining.coerceAtLeast(100L))
                    publish()
                }
            }
        }
    }

    private fun publish() {
        mutableState.value = snapshot()
    }

    private fun publishNotification() {
        val running = timer.startedAtMillis != null
        val notification = if (!saveInProgress && (running || timer.accumulatedMillis > 0)) {
            IosStopwatchNotificationState(timer.revision, running)
        } else {
            null
        }
        notificationObserver?.invoke(notification)
    }

    private fun snapshot() = StopwatchSnapshot(
        elapsedSeconds = (currentElapsedMillis() / STOPWATCH_MILLIS_PER_SECOND).toInt(),
        isRunning = timer.startedAtMillis != null,
        selectedProject = selectedProject,
    )

    private fun currentElapsedMillis(): Long {
        val calculated = stopwatchElapsedMillis(timer.accumulatedMillis, timer.startedAtMillis, nowMillis())
        elapsedMillis = maxOf(elapsedMillis, calculated)
        return elapsedMillis
    }

    private fun loadTimer(): PersistedTimer {
        defaults.stringForKey(TIMER_KEY)?.let { stored ->
            return runCatching { timerJson.decodeFromString<PersistedTimer>(stored) }.getOrNull()
                ?.let { it.copy(accumulatedMillis = it.accumulatedMillis.coerceIn(0, MAX_STOPWATCH_MILLIS)) }
                ?: PersistedTimer()
        }
        val elapsed = defaults.doubleForKey(LEGACY_ELAPSED_KEY).toLong().coerceIn(0, Int.MAX_VALUE.toLong())
        val started = defaults.doubleForKey(LEGACY_STARTED_AT_KEY).toLong()
            .takeIf { defaults.boolForKey(LEGACY_RUNNING_KEY) && it in 1..(nowMillis() / STOPWATCH_MILLIS_PER_SECOND) }
        val migrated = PersistedTimer(elapsed * STOPWATCH_MILLIS_PER_SECOND, started?.times(STOPWATCH_MILLIS_PER_SECOND))
        defaults.setObject(timerJson.encodeToString(migrated), TIMER_KEY)
        return migrated
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}

@Serializable
@OptIn(ExperimentalForeignApi::class)
private data class PersistedTimer(
    val accumulatedMillis: Long = 0,
    val startedAtMillis: Long? = null,
    val revision: String = NSUUID().UUIDString,
)

private const val TIMER_KEY = "timnor.stopwatch.v2"
private const val PROJECT_KEY = "timnor.stopwatch.project"
private const val LEGACY_ELAPSED_KEY = "timnor.stopwatch.elapsed-seconds"
private const val LEGACY_STARTED_AT_KEY = "timnor.stopwatch.started-at"
private const val LEGACY_RUNNING_KEY = "timnor.stopwatch.running"
