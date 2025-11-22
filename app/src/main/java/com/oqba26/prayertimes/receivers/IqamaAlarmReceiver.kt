package com.oqba26.prayertimes.receivers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.oqba26.prayertimes.MainActivity
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.services.NotificationService

/**
 * BroadcastReceiver مسئول نمایش اعلان اقامه نماز
 */
class IqamaAlarmReceiver : BroadcastReceiver() {

    companion object {
        @Suppress("unused")
        private const val TAG = "IqamaAlarmReceiver"
        private const val DEFAULT_NOTIFICATION_ID = 430
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent?) {
        val prayerName = intent?.getStringExtra("PRAYER_NAME")?.let {
            when (it.lowercase()) {
                "fajr" -> "نماز صبح"
                "dhuhr" -> "نماز ظهر"
                "asr" -> "نماز عصر"
                "maghrib" -> "نماز مغرب"
                "isha" -> "نماز عشاء"
                else -> "نماز"
            }
        } ?: "نماز"

        ensureIqamaChannel(context)

        // ایجاد Intent برای باز کردن برنامه هنگام کلیک روی اعلان
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, prayerName.hashCode(), launchIntent, piFlags)

        val builder = NotificationCompat.Builder(context, NotificationService.IQAMA_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle("زمان اقامه نماز")
            .setContentText("اکنون زمان اقامه $prayerName است.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(context).apply {
            notify(DEFAULT_NOTIFICATION_ID + prayerName.hashCode(), builder.build())
        }
    }

    private fun ensureIqamaChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = NotificationService.IQAMA_CHANNEL_ID
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                val ch = NotificationChannel(
                    channelId,
                    "اعلان اقامه نماز",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "برای یادآوری زمان اقامه نماز"
                    enableVibration(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(ch)
            }
        }
    }
}