package org.examples.time_manager.core.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.examples.time_manager.MainActivity
import org.examples.time_manager.core.service.util.Constants.CANCEL_REQUEST_CODE
import org.examples.time_manager.core.service.util.Constants.CLICK_REQUEST_CODE
import org.examples.time_manager.core.service.util.Constants.RESUME_REQUEST_CODE
import org.examples.time_manager.core.service.util.Constants.STOP_REQUEST_CODE

object ServiceHelper {

    private const val FLAG = PendingIntent.FLAG_IMMUTABLE

    fun clickPendingIntent(context: Context): PendingIntent {
        val clickIntent = Intent(context, MainActivity::class.java).apply {
        }
        return PendingIntent.getActivity(
            context, CLICK_REQUEST_CODE, clickIntent, FLAG
        )
    }

    fun stopPendingIntent(context: Context): PendingIntent {
        val stopIntent = Intent(context, StopwatchService::class.java).apply {
            action = org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_STOP
        }
        return PendingIntent.getService(
            context, STOP_REQUEST_CODE, stopIntent, FLAG
        )
    }

    fun resumePendingIntent(context: Context): PendingIntent {
        val resumeIntent = Intent(context, StopwatchService::class.java).apply {
            action = org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_START
        }
        return PendingIntent.getService(
            context, RESUME_REQUEST_CODE, resumeIntent, FLAG
        )
    }

    fun cancelPendingIntent(context: Context): PendingIntent {
        val cancelIntent = Intent(context, StopwatchService::class.java).apply {
            action = org.examples.time_manager.core.service.util.Constants.ACTION_SERVICE_CANCEL
        }
        return PendingIntent.getService(
            context, CANCEL_REQUEST_CODE, cancelIntent, FLAG
        )
    }

    fun triggerForegroundService(context: Context, action: String) {
        Intent(context, StopwatchService::class.java).apply {
            this.action = action
            ContextCompat.startForegroundService(context, this)
        }
    }
}
