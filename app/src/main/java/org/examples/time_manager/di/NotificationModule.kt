package org.examples.time_manager.di

import android.app.NotificationManager
import android.content.Context
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.core.app.NotificationCompat
import org.examples.time_manager.R
import org.examples.time_manager.core.service.ServiceHelper
import org.examples.time_manager.core.service.util.Constants.NOTIFICATION_CHANNEL_ID

object NotificationModule {
    @OptIn(ExperimentalAnimationApi::class)
    fun provideNotificationBuilder(context: Context): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.working_notification_title))
            .setContentText("00:00:00")
            .setSmallIcon(R.drawable.baseline_timer_24)
            .setOngoing(true)
            .addAction(0, context.getString(R.string.stop), ServiceHelper.stopPendingIntent(context))
            .addAction(0, context.getString(R.string.cancel), ServiceHelper.cancelPendingIntent(context))
            .setContentIntent(ServiceHelper.clickPendingIntent(context))
    }

    fun provideNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}