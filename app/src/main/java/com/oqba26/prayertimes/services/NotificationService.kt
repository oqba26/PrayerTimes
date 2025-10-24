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
    private const val TAG = "NotifService"

    /**
     * ساخت/ایجاد کانال نوتیف (یکبار کافی است).
     * نکته: اگر کانال قبلاً ساخته شده باشد، اهمیت (importance) بعداً با کد قابل تغییر نیست.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val existing = nm.getNotificationChannel(DAILY_CHANNEL_ID)
        if (existing == null) {
            // برای اینکه «مزاحم نباشد» LOW می‌گذاریم؛ اگر برای تست می‌خواهی حتماً دیده شود، می‌توانی موقتاً DEFAULT بذاری.
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
            Log.d(TAG, "channel created: id=$DAILY_CHANNEL_ID, importance=${ch.importance}")
        } else {
            Log.d(TAG, "channel exists: id=$DAILY_CHANNEL_ID, importance=${existing.importance}, enabledLights=${existing.shouldShowLights()}")
        }
    }

}