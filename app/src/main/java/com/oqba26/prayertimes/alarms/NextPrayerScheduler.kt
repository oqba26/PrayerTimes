package com.oqba26.prayertimes.alarms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.oqba26.prayertimes.services.PrayerForegroundService
import com.oqba26.prayertimes.utils.AlarmUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object NextPrayerScheduler {
    private const val REQ_CODE_NEXT_PRAYER = 1010

    fun scheduleForNextPrayer(context: Context, prayerTimes: Map<String, String>) {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val today = LocalDate.now(zone)

        val candidates = order.mapNotNull { key ->
            val t = prayerTimes[key] ?: return@mapNotNull null
            val lt = runCatching { LocalTime.parse(t.padStart(5, '0'), fmt) }.getOrNull() ?: return@mapNotNull null
            var zdt = ZonedDateTime.of(today, lt, zone)
            if (zdt.isBefore(now)) zdt = zdt.plusDays(1)
            key to zdt
        }

        if (candidates.isEmpty()) return

        val (nextPrayerName, nextTime) = candidates.minByOrNull { it.second.toInstant().toEpochMilli() }!!
        val triggerAtMillis = nextTime.toInstant().toEpochMilli()

        Log.d("NextPrayerScheduler", "Next update scheduled for $nextPrayerName at $nextTime")

        val intent = Intent(context, PrayerForegroundService::class.java).apply {
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
            logTag = "NextPrayerScheduler"
        )
    }
}