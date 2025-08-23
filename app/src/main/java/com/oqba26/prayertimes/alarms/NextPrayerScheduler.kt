package com.oqba26.prayertimes.alarms


import android.app.PendingIntent
import com.oqba26.prayertimes.services.PrayerForegroundService
import android.content.Context
import android.content.Intent
import android.os.Build
import com.oqba26.prayertimes.utils.AlarmUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object NextPrayerScheduler {

    private const val REQ_CODE_NEXT_PRAYER = 1010

    fun scheduleForNextPrayer(context: Context, prayerTimes: Map<String, String>) {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
        val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val today = LocalDate.now(zone)

        val candidates = order.mapNotNull { key ->
            val t = prayerTimes[key] ?: return@mapNotNull null
            val lt = try { LocalTime.parse(t, fmt) } catch (_: Exception) { return@mapNotNull null }
            var zdt = ZonedDateTime.of(today, lt, zone)
            if (zdt.isBefore(now)) zdt = zdt.plusDays(1)
            zdt
        }

        val next = candidates.minByOrNull { it.toInstant().toEpochMilli() } ?: return
        val triggerAtMillis = next.toInstant().toEpochMilli()

        val intent = Intent(context, PrayerForegroundService::class.java).apply {
            // می‌تونی "REFRESH" جدا بسازی؛ اینجا از RESTART استفاده می‌کنیم که سرویس دیتای جدید رو لود و نوتیف رو آپدیت کنه
            action = "RESTART"
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT

        val pi = PendingIntent.getService(context, REQ_CODE_NEXT_PRAYER, intent, flags)

        AlarmUtils.scheduleExactOrFallback(
            context = context,
            triggerAtMillis = triggerAtMillis,
            pendingIntent = pi,
            allowWhileIdle = true,
            logTag = "NextPrayer"
        )
    }
}