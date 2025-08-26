package com.oqba26.prayertimes.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat

class NotificationService {
    companion object {
        const val DAILY_CHANNEL_ID = "prayer_times_daily"          // فقط برای foreground service

        fun stopPrayerNotificationService(context: Context) {
            val serviceIntent = Intent(context, PrayerForegroundService::class.java).apply {
                action = "STOP"
            }
            try {
                context.startService(serviceIntent)
                Log.d("NotificationService", "سرویس foreground متوقف شد")
            } catch (e: Exception) {
                Log.e("NotificationService", "خطا در متوقف کردن foreground service", e)
            }
        }

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // حذف کانال‌های قدیمی اضافی
                listOf("prayer_times_daily_v2", "prayer_times_daily_v3").forEach { oldId ->
                    runCatching { notificationManager.deleteNotificationChannel(oldId) }
                }

                // کانال برای foreground service
                val dailyChannel = NotificationChannel(
                    DAILY_CHANNEL_ID,
                    "اوقات نماز روزانه",
                    NotificationManager.IMPORTANCE_LOW  // اهمیت پایین تا مزاحم نباشه
                ).apply {
                    description = "نمایش دائمی تاریخ و اوقات نماز"
                    setShowBadge(false)  // نمایش badge روی آیکون اپ
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)  // بدون صدا
                    enableVibration(false)  // بدون لرزش
                }

                notificationManager.createNotificationChannel(dailyChannel)
            }
        }

        // حذف تمام notification های قدیمی با ID های مختلف
        fun clearAllOldNotifications(context: Context) {
            val nm = NotificationManagerCompat.from(context)
            listOf(2000, 2001, 2002, 2003, 10001).forEach { oldId ->
                nm.cancel(oldId)
            }
        }
    }
}