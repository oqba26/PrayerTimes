package com.oqba26.prayertimes.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.oqba26.prayertimes.services.PrayerForegroundService
import com.oqba26.prayertimes.utils.PermissionHandler
import com.oqba26.prayertimes.utils.PrayerUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object NextPrayerScheduler {

    private const val TAG = "NextPrayerScheduler"

    /**
     * برنامه‌ریزی ForegroundService برای نزدیک‌ترین نماز بعدی
     *
     * @param context  کانتکست اپلیکیشن
     * @param prayerTimes  اوقات نماز امروز
     */
    fun scheduleForNextPrayer(context: Context, prayerTimes: Map<String, String>) {
        try {
            val now = LocalTime.now()

            val nextPrayer = findNextPrayerTime(now, prayerTimes)
            if (nextPrayer != null) {
                val (name, triggerTimeMillis) = nextPrayer
                Log.d(TAG, "Next prayer: $name at $triggerTimeMillis")

                // Intent شروع سرویس Foreground
                val intent = Intent(context, PrayerForegroundService::class.java).apply {
                    action = "RESTART"
                }

                val pi = PendingIntent.getService(
                    context,
                    0,
                    intent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    else
                        PendingIntent.FLAG_UPDATE_CURRENT
                )

                scheduleExactOrFallback(
                    context = context,
                    triggerAtMillis = triggerTimeMillis,
                    pendingIntent = pi,
                    allowWhileIdle = true
                )
            } else {
                Log.w(TAG, "No next prayer found for today")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling next prayer", e)
        }
    }

    /**
     * پیدا کردن نزدیک‌ترین نماز بعد از زمان فعلی
     *
     * خروجی: Pair(نام نماز، زمان به میلی‌ثانیه)
     */
    private fun findNextPrayerTime(
        now: LocalTime,
        prayerTimes: Map<String, String>
    ): Pair<String, Long>? {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")

        val parsedTimes = order.mapNotNull { prayerName ->
            prayerTimes[prayerName]?.let { timeStr ->
                PrayerUtils.parseTimeSafely(timeStr)?.let { parsedLocalTime ->
                    prayerName to parsedLocalTime
                }
            }
        }.sortedBy { it.second }

        val next = parsedTimes.firstOrNull { it.second.isAfter(now) }

        return if (next != null) {
            val today = LocalDate.now()
            val triggerDateTime: LocalDateTime = today.atTime(next.second)
            val triggerMillis = triggerDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            next.first to triggerMillis
        } else null
    }

    /**
     * مدیریت آلارم دقیق با چک permission (Android 12+)
     */
    private fun scheduleExactOrFallback(
        context: Context,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        allowWhileIdle: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // چک Permission دقیق برای Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!PermissionHandler.canScheduleExactAlarms(context)) {
                Log.w(TAG, "⚠️ Cannot schedule exact alarms → asking user for permission...")
                PermissionHandler.requestExactAlarmPermission(context)
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && allowWhileIdle) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
        Log.d(TAG, "✅ Scheduled alarm at $triggerAtMillis (allowWhileIdle=$allowWhileIdle)")
    }
}