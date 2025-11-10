package com.oqba26.prayertimes.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.oqba26.prayertimes.R

object NotificationService {
    const val DAILY_CHANNEL_ID = "prayer_times_daily"
    const val IQAMA_CHANNEL_ID = "iqama_alarms"
    private const val TAG = "NotifService"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Daily Prayer Times Channel
        if (nm.getNotificationChannel(DAILY_CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                DAILY_CHANNEL_ID,
                context.getString(R.string.app_name) + " - اوقات نماز روزانه",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش دائمی تاریخ و اوقات نماز"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(ch)
            Log.d(TAG, "Channel created: $DAILY_CHANNEL_ID")
        }

        // Iqama Alarm Channel
        if (nm.getNotificationChannel(IQAMA_CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                IQAMA_CHANNEL_ID,
                "اعلان اقامه",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "نوتیفیکیشن برای یادآوری زمان اقامه نماز"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            }
            nm.createNotificationChannel(ch)
            Log.d(TAG, "Channel created: $IQAMA_CHANNEL_ID")
        }
    }
}