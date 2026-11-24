package no.timnor.app.di

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import no.timnor.app.R
import no.timnor.app.core.service.ServiceHelper
import no.timnor.app.core.service.util.Constants.NOTIFICATION_CHANNEL_ID

object NotificationModule {
    fun provideNotificationBuilder(context: Context): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.working_notification_title))
            .setSmallIcon(R.drawable.baseline_timer_24)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(ServiceHelper.clickPendingIntent(context))
    }

    fun provideNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}
