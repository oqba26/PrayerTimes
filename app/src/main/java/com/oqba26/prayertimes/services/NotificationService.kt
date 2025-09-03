package com.oqba26.prayertimes.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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

    /**
     * آیا نوتیف برای این اپ روشن است؟ (سطح کل اپ)
     */
    fun areNotificationsEnabled(ctx: Context): Boolean {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            nm.areNotificationsEnabled()
        } else true
    }

    /**
     * باز کردن مستقیم تنظیمات کانال (برای وقتی که کاربر کانال را خاموش کرده باشد).
     */
    fun openChannelSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, DAILY_CHANNEL_ID)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(android.net.Uri.parse("package:${ctx.packageName}"))
            ctx.startActivity(intent)
        }
    }

    /**
     * لاگ وضعیت فعلی کانال برای عیب‌یابی.
     */
    fun logChannelState(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = nm.getNotificationChannel(DAILY_CHANNEL_ID)
        if (ch == null) {
            Log.w(TAG, "channel NOT FOUND: $DAILY_CHANNEL_ID (ساخته نشده)")
        } else {
            Log.d(TAG, "channel state: id=$DAILY_CHANNEL_ID, importance=${ch.importance}, desc=${ch.description}")
        }
        val enabled = areNotificationsEnabled(ctx)
        Log.d(TAG, "app notifications enabled=$enabled")
    }
}