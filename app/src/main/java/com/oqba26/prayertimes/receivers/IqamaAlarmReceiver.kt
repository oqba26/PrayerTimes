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
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oqba26.prayertimes.MainActivity
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.services.NotificationService
import com.oqba26.prayertimes.viewmodels.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * BroadcastReceiver مسئول نمایش اعلان اقامه نماز
 */
class IqamaAlarmReceiver : BroadcastReceiver() {

    companion object {
        @Suppress("unused")
        private const val TAG = "IqamaAlarmReceiver"
        private const val DEFAULT_NOTIFICATION_ID = 430
        private const val DEFAULT_TEMPLATE = "اکنون زمان اقامه {prayer} است."
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

        // خواندن متن قابل تنظیم نوتیف اقامه از DataStore
        val template = runBlocking {
            val prefs = context.dataStore.data.first()
            val key = stringPreferencesKey("iqama_notification_text")
            val stored = prefs[key]

            // null ، خالی یا مساوی متن پیش‌فرض => از DEFAULT_TEMPLATE استفاده کن
            if (stored.isNullOrBlank() || stored == DEFAULT_TEMPLATE) {
                DEFAULT_TEMPLATE
            } else {
                stored
            }
        }
        val contentText = template.replace("{prayer}", prayerName)

        ensureIqamaChannel(context)

        // Intent برای باز کردن برنامه هنگام کلیک روی اعلان
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerName.hashCode(),
            launchIntent,
            piFlags
        )

        val builder = NotificationCompat.Builder(context, NotificationService.IQAMA_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle("زمان اقامه نماز")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(context).notify(
            DEFAULT_NOTIFICATION_ID + prayerName.hashCode(),
            builder.build()
        )
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