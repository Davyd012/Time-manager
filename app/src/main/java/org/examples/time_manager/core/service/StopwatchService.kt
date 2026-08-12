package org.examples.time_manager.core.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.examples.time_manager.R
import org.examples.time_manager.core.repository.StopwatchSnapshot
import org.examples.time_manager.core.repository.StopwatchRepository
import org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_CANCEL
import org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_START
import org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_STOP
import org.examples.time_manager.core.service.util.Constants.NOTIFICATION_CHANNEL_ID
import org.examples.time_manager.core.service.util.Constants.NOTIFICATION_CHANNEL_NAME
import org.examples.time_manager.core.service.util.Constants.NOTIFICATION_ID
import org.examples.time_manager.core.service.util.formatTime
import org.examples.time_manager.core.service.util.pad
import org.examples.time_manager.di.NotificationModule
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class StopwatchService : Service() {
    private val _snapshot = MutableStateFlow(StopwatchSnapshot())
    val snapshot: StateFlow<StopwatchSnapshot> = _snapshot.asStateFlow()
    private val binder = StopwatchBinder()
    private var timer: Timer? = null
    private var seconds = 0
    private var observer: ((org.examples.time_manager.core.repository.StopwatchSnapshot) -> Unit)? = null

    private val notificationManager by lazy { NotificationModule.provideNotificationManager(this) }
    private val notificationBuilder by lazy { NotificationModule.provideNotificationBuilder(this) }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SERVICE_START -> startStopwatch()
            ACTION_SERVICE_STOP -> pauseStopwatch()
            ACTION_SERVICE_CANCEL -> cancelStopwatch()
        }
        return START_STICKY
    }

    fun setProject(projectId: Int) {
        publish(_snapshot.value.copy(selectedProject = projectId))
    }

    fun observe(observer: (org.examples.time_manager.core.repository.StopwatchSnapshot) -> Unit) {
        this.observer = observer
        observer(_snapshot.value)
    }

    private fun startStopwatch() {
        if (timer != null) return
        publish(_snapshot.value.copy(isRunning = true))
        startForegroundService()
        timer = fixedRateTimer(initialDelay = 1000L, period = 1000L) {
            seconds += 1
            publish(_snapshot.value.copy(elapsedSeconds = seconds, isRunning = true))
            updateNotification(seconds)
        }
    }

    private fun pauseStopwatch() {
        timer?.cancel()
        timer = null
        publish(_snapshot.value.copy(isRunning = false))
        setResumeButton()
    }

    private fun cancelStopwatch() {
        timer?.cancel()
        timer = null
        seconds = 0
        publish(_snapshot.value.copy(elapsedSeconds = 0, isRunning = false))
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("InlinedApi")
    private fun startForegroundService() {
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notificationBuilder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        setStopButton()
    }

    private fun createNotificationChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                android.app.NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun updateNotification(value: Int) {
        notificationManager.notify(
            NOTIFICATION_ID,
            notificationBuilder.setContentText(
                formatTime(
                    hours = (value / 3600).pad(),
                    minutes = ((value % 3600) / 60).pad(),
                    seconds = (value % 60).pad(),
                ),
            ).build(),
        )
    }

    private fun publish(value: org.examples.time_manager.core.repository.StopwatchSnapshot) {
        _snapshot.value = value
        observer?.invoke(value)
    }

    @SuppressLint("RestrictedApi")
    private fun setStopButton() {
        replaceAction(
            getString(R.string.stop),
            ServiceHelper.stopPendingIntent(this),
        )
    }

    @SuppressLint("RestrictedApi")
    private fun setResumeButton() {
        replaceAction(
            getString(R.string.resume),
            ServiceHelper.resumePendingIntent(this),
        )
    }

    @SuppressLint("RestrictedApi")
    private fun replaceAction(label: String, intent: android.app.PendingIntent) {
        if (notificationBuilder.mActions.isNotEmpty()) notificationBuilder.mActions.removeAt(0)
        notificationBuilder.mActions.add(0, NotificationCompat.Action(0, label, intent))
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    inner class StopwatchBinder : Binder() {
        fun getService(): StopwatchService = this@StopwatchService
    }
}

class StopwatchGateway(private val context: android.content.Context) : StopwatchRepository,
    android.content.ServiceConnection {
    private val _state = MutableStateFlow(StopwatchSnapshot())
    override val state: StateFlow<StopwatchSnapshot> = _state.asStateFlow()
    private var service: StopwatchService? = null

    fun bind() {
        context.bindService(
            Intent(context, StopwatchService::class.java),
            this,
            android.content.Context.BIND_AUTO_CREATE,
        )
    }

    override fun onServiceConnected(name: android.content.ComponentName?, binder: IBinder?) {
        service = (binder as StopwatchService.StopwatchBinder).getService()
        service?.observe { snapshot -> _state.value = snapshot }
    }

    override fun onServiceDisconnected(name: android.content.ComponentName?) {
        service = null
        _state.value = _state.value.copy(isRunning = false)
    }

    override fun start() = ServiceHelper.triggerForegroundService(context, ACTION_SERVICE_START)
    override fun pause() = ServiceHelper.triggerForegroundService(context, ACTION_SERVICE_STOP)
    override fun cancel() = ServiceHelper.triggerForegroundService(context, ACTION_SERVICE_CANCEL)
    override fun selectProject(projectId: Int) {
        service?.setProject(projectId)
        _state.value = _state.value.copy(selectedProject = projectId)
    }
}
