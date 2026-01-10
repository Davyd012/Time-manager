package org.examples.time_manager.core.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import org.examples.time_manager.App
import org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_CANCEL
import org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_START
import org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_STOP
import org.examples.time_manager.core.service.util.Constants.NOTIFICATION_CHANNEL_ID
import org.examples.time_manager.core.service.util.Constants.NOTIFICATION_CHANNEL_NAME
import org.examples.time_manager.core.service.util.Constants.NOTIFICATION_ID
import org.examples.time_manager.core.service.util.Constants.STOPWATCH_STATE
import org.examples.time_manager.core.service.util.formatTime
import org.examples.time_manager.core.service.util.pad
import org.examples.time_manager.di.NotificationModule
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class StopwatchService : Service() {
    var project: Int = 1
    private val notificationManager by lazy { NotificationModule.provideNotificationManager(App.context) }
    private val notificationBuilder by lazy { NotificationModule.provideNotificationBuilder(App.context) }

    private val binder = StopwatchBinder()

    private var duration: Duration = Duration.ZERO
    private lateinit var timer: Timer

    var seconds = mutableIntStateOf(0)
    private var currentState = mutableStateOf(StopwatchState.Idle)

    var running = false

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(STOPWATCH_STATE)) {
            StopwatchState.Started.name -> {
                setStopButton()
                startForegroundService()
                startStopwatch { seconds ->
                    updateNotification(seconds = seconds)
                }
            }
            StopwatchState.Stopped.name -> {
                stopStopwatch()
                setResumeButton()
            }
            StopwatchState.Canceled.name -> {
                stopStopwatch()
                cancelStopwatch()
                stopForegroundService()
            }
        }
        intent?.action.let {
            when (it) {
                ACTION_SERVICE_START -> {
                    setStopButton()
                    startForegroundService()
                    startStopwatch { seconds ->
                        updateNotification(seconds = seconds)
                    }
                }
                ACTION_SERVICE_STOP -> {
                    stopStopwatch()
                    setResumeButton()
                }
                ACTION_SERVICE_CANCEL -> {
                    stopStopwatch()
                    cancelStopwatch()
                    stopForegroundService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startStopwatch(onTick: (s: Int) -> Unit) {
        running = true
        currentState.value = StopwatchState.Started
        timer = fixedRateTimer(initialDelay = 1000L, period = 1000L) {
            duration = duration.plus(1.seconds)
            updateTimeUnits()
            onTick(seconds.intValue)
        }
    }

    private fun stopStopwatch() {
        running = false
        if (this::timer.isInitialized) {
            timer.cancel()
        }
        currentState.value = StopwatchState.Stopped
    }

    private fun cancelStopwatch() {
        running = false
        duration = Duration.ZERO
        currentState.value = StopwatchState.Idle
        updateTimeUnits()
    }

    private fun updateTimeUnits() {
        duration.toComponents { value, _ ->
            this@StopwatchService.seconds.intValue = value.toInt()
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun stopForegroundService() {
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification(seconds: Int) {
        notificationManager.notify(
            NOTIFICATION_ID,
            notificationBuilder.setContentText(
                formatTime(
                    hours = (seconds / 3600).pad(),
                    minutes = ((seconds % 3600) / 60).pad(),
                    seconds = (seconds % 60).pad(),
                )
            ).build()
        )
    }

    @OptIn(ExperimentalAnimationApi::class)
    @SuppressLint("RestrictedApi")
    private fun setStopButton() {
        notificationBuilder.mActions.removeAt(0)
        notificationBuilder.mActions.add(
            0,
            NotificationCompat.Action(
                0,
                getString(org.examples.time_manager.R.string.stop),
                ServiceHelper.stopPendingIntent(this)
            )
        )
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    @SuppressLint("RestrictedApi")
    @OptIn(ExperimentalAnimationApi::class)
    private fun setResumeButton() {
        notificationBuilder.mActions.removeAt(0)
        notificationBuilder.mActions.add(
            0,
            NotificationCompat.Action(
                0,
                getString(org.examples.time_manager.R.string.resume),
                ServiceHelper.resumePendingIntent(this)
            )
        )
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    inner class StopwatchBinder : Binder() {
        fun getService(): StopwatchService = this@StopwatchService
    }
}

enum class StopwatchState {
    Idle,
    Started,
    Stopped,
    Canceled
}