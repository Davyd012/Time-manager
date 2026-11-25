package no.timnor.app.core.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.timnor.app.R
import no.timnor.app.core.repository.AppMutationCoordinator
import no.timnor.app.core.service.util.Constants.ACTION_SERVICE_CANCEL
import no.timnor.app.core.service.util.Constants.ACTION_SERVICE_START
import no.timnor.app.core.service.util.Constants.ACTION_SERVICE_STOP
import no.timnor.app.core.service.util.Constants.NOTIFICATION_CHANNEL_ID
import no.timnor.app.core.service.util.Constants.NOTIFICATION_CHANNEL_NAME
import no.timnor.app.core.service.util.Constants.NOTIFICATION_ID
import no.timnor.app.core.service.util.formatTime
import no.timnor.app.core.service.util.pad
import no.timnor.app.di.NotificationModule
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform
import no.timnor.app.shared.platform.stopwatch.StopwatchSnapshot

class StopwatchService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val binder = StopwatchBinder()
    private val coordinator = AppMutationCoordinator.instance
    private val notificationManager by lazy { NotificationModule.provideNotificationManager(this) }
    private val preferences by lazy { getSharedPreferences("stopwatch_session", MODE_PRIVATE) }
    private val bootCount by lazy { Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1) }
    private var session = StopwatchSession()
    private var selectedProject = 1
    private var observer: ((StopwatchSnapshot) -> Unit)? = null
    private var saveInProgress = false
    private var isForeground = false

    private val ticker = object : Runnable {
        override fun run() {
            if (session.startedAtMillis == null) return
            val elapsedMillis = session.elapsedMillis(SystemClock.elapsedRealtime())
            publish(elapsedMillis)
            handler.postDelayed(this, MILLIS_PER_SECOND - elapsedMillis % MILLIS_PER_SECOND)
        }
    }

    override fun onCreate() {
        super.onCreate()
        selectedProject = preferences.getInt(PROJECT, 1)
        val now = SystemClock.elapsedRealtime()
        val startedAt = preferences.getLong(STARTED_AT, -1L).takeIf {
            bootCount >= 0 && preferences.getInt(BOOT_COUNT, -1) == bootCount && it in 0..now
        }
        session = StopwatchSession(
            accumulatedMillis = preferences.getLong(ACCUMULATED, 0L).coerceAtLeast(0),
            startedAtMillis = startedAt,
            pausedAtEpochSeconds = preferences.getLong(PAUSED_AT, -1L).takeIf { it >= 0 },
        )
        coordinator.timerObserved(session.startedAtMillis != null || session.accumulatedMillis > 0)
        ticker.run()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SERVICE_START -> {
                if (!saveInProgress && coordinator.timerStarted()) {
                    session = session.start(SystemClock.elapsedRealtime())
                    persist()
                    handler.removeCallbacks(ticker)
                    ticker.run()
                    showNotification()
                } else if (!isForeground) {
                    stopSelf(startId)
                }
            }
            ACTION_SERVICE_STOP -> {
                pause()
                if (!isForeground) {
                    if (session.accumulatedMillis > 0) showNotification() else stopSelf(startId)
                }
            }
            ACTION_SERVICE_CANCEL -> if (!saveInProgress) clear()
            null -> {
                if (session.startedAtMillis != null || session.accumulatedMillis > 0) showNotification()
                else stopSelf(startId)
            }
        }
        return if (isForeground) START_STICKY else START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    internal fun observe(observer: (StopwatchSnapshot) -> Unit) {
        this.observer = observer
        publish()
    }

    internal fun setProject(projectId: Int) {
        if (saveInProgress) return
        selectedProject = projectId
        persist()
        publish()
    }

    internal fun pause() {
        if (session.startedAtMillis == null) return
        session = session.pause(SystemClock.elapsedRealtime()).copy(
            pausedAtEpochSeconds = System.currentTimeMillis() / MILLIS_PER_SECOND,
        )
        handler.removeCallbacks(ticker)
        persist()
        publish()
        if (isForeground) showNotification()
    }

    internal suspend fun savePaused(save: suspend (StopwatchSnapshot) -> Unit) {
        if (saveInProgress) return
        saveInProgress = true
        try {
            pause()
            save(snapshot(session.elapsedMillis(SystemClock.elapsedRealtime())))
        } finally {
            saveInProgress = false
        }
    }

    internal fun cancel() {
        if (!saveInProgress) clear()
    }

    internal fun clear() {
        session = StopwatchSession()
        handler.removeCallbacks(ticker)
        persist()
        coordinator.timerCleared()
        publish()
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
    }

    private fun persist() {
        preferences.edit()
            .putLong(ACCUMULATED, session.accumulatedMillis)
            .putLong(STARTED_AT, session.startedAtMillis ?: -1L)
            .putLong(PAUSED_AT, session.pausedAtEpochSeconds ?: -1L)
            .putInt(BOOT_COUNT, bootCount)
            .putInt(PROJECT, selectedProject)
            .apply()
    }

    private fun snapshot(elapsedMillis: Long) = StopwatchSnapshot(
        elapsedSeconds = (elapsedMillis / MILLIS_PER_SECOND).toInt(),
        isRunning = session.startedAtMillis != null,
        selectedProject = selectedProject,
        pausedAtEpochSeconds = session.pausedAtEpochSeconds,
    )

    private fun publish(elapsedMillis: Long = session.elapsedMillis(SystemClock.elapsedRealtime())) {
        observer?.invoke(snapshot(elapsedMillis))
    }

    @SuppressLint("InlinedApi")
    private fun showNotification() {
        val notification = buildNotification()
        if (isForeground) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            notificationManager.createNotificationChannel(
                NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW),
            )
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            isForeground = true
        }
    }

    private fun buildNotification(): Notification {
        val running = session.startedAtMillis != null
        val elapsedMillis = session.elapsedMillis(SystemClock.elapsedRealtime())
        val seconds = (elapsedMillis / MILLIS_PER_SECOND).toInt()
        val builder = NotificationModule.provideNotificationBuilder(this)
            .setShowWhen(running)
            .setUsesChronometer(running)
        if (running) {
            builder.setWhen(System.currentTimeMillis() - elapsedMillis)
                .addAction(0, getString(R.string.stop), ServiceHelper.stopPendingIntent(this))
        } else {
            builder.setContentText(formatTime((seconds / 3600).pad(), ((seconds % 3600) / 60).pad(), (seconds % 60).pad()))
                .addAction(0, getString(R.string.resume), ServiceHelper.resumePendingIntent(this))
        }
        return builder.addAction(0, getString(R.string.cancel), ServiceHelper.cancelPendingIntent(this)).build()
    }

    inner class StopwatchBinder : Binder() {
        fun getService(): StopwatchService = this@StopwatchService
    }

    private companion object {
        const val ACCUMULATED = "accumulatedMillis"
        const val STARTED_AT = "startedAtMillis"
        const val PAUSED_AT = "pausedAtEpochSeconds"
        const val BOOT_COUNT = "bootCount"
        const val PROJECT = "project"
    }
}

class StopwatchGateway(
    private val context: Context,
) : StopwatchPlatform, ServiceConnection {
    private val _state = MutableStateFlow(StopwatchSnapshot())
    override val state: StateFlow<StopwatchSnapshot> = _state.asStateFlow()
    private var service: StopwatchService? = null
    private var pendingProject: Int? = null

    fun bind() {
        context.bindService(Intent(context, StopwatchService::class.java), this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        val connected = (binder as StopwatchService.StopwatchBinder).getService()
        service = connected
        pendingProject?.let(connected::setProject)
        pendingProject = null
        connected.observe { _state.value = it }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service = null
    }

    override fun start() = ServiceHelper.triggerForegroundService(context, ACTION_SERVICE_START)

    override fun pause() {
        service?.pause()
    }

    override fun cancel() {
        service?.cancel()
    }

    override fun timerSaved() {
        service?.clear()
    }

    override suspend fun withPausedTimerForSave(save: suspend (StopwatchSnapshot) -> Unit) {
        service?.savePaused(save)
    }

    override fun selectProject(projectId: Int) {
        val connected = service
        if (connected != null) {
            connected.setProject(projectId)
        } else {
            pendingProject = projectId
            _state.value = _state.value.copy(selectedProject = projectId)
        }
    }
}
