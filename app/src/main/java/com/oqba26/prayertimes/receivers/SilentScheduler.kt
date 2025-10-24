package com.oqba26.prayertimes.receivers

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import androidx.core.content.edit

class SilentScheduler(private val context: Context) {

    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("silent_sched", Context.MODE_PRIVATE)
    private val settingsPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_TIMES_JSON = "times_json"
        private const val PREF_TIMES_DAY  = "times_day"
        private const val PREF_DURATION_MIN = "duration_min"
        private val PRAYER_KEYS = listOf("fajr","dhuhr","asr","maghrib","isha")
    }

    fun updateAndSchedule(
        timesMillis: Map<String, Long>,
        enabled: Map<String, Boolean>? = null,
        durationMinutes: Int? = null
    ) {
        saveTodayTimes(timesMillis)
        if (enabled != null) saveEnabled(enabled)
        if (durationMinutes != null) prefs.edit { putInt(PREF_DURATION_MIN, durationMinutes) }
        cancelAll()
        schedule()
    }

    fun updateFromStrings(
        timesHHmm: Map<String, String>,
        durationMinutes: Int = 20,
        enabled: Map<String, Boolean>? = null
    ) {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val out = mutableMapOf<String, Long>()
        for ((k, v) in timesHHmm) {
            runCatching {
                val t = LocalTime.parse(v)
                val epoch = t.atDate(today).atZone(zone).toInstant().toEpochMilli()
                out[k] = epoch
            }
        }
        updateAndSchedule(out, enabled, durationMinutes)
    }

    fun schedule() {
        val now = System.currentTimeMillis()
        val day = currentDay()
        val savedDay = prefs.getString(PREF_TIMES_DAY, null)
        if (savedDay != day) return

        val times = loadTimes()
        val enabled = loadEnabled()
        val durationMin = prefs.getInt(PREF_DURATION_MIN, 20)

        for (key in PRAYER_KEYS) {
            if (enabled[key] != true) continue
            val start = times[key] ?: continue
            val end = start + durationMin * 60_000L
            if (now >= end) continue

            if (now in start until end) {
                // همین الان سایلنت کن و مدت باقیمانده را بفرست
                val remaining = ((end - now) / 60_000L).toInt().coerceAtLeast(1)
                context.sendBroadcast(
                    Intent(SilentModeReceiver.ACTION_SILENT)
                        .setClass(context, SilentModeReceiver::class.java)
                        .putExtra(SilentModeReceiver.EXTRA_DURATION_MINUTES, remaining)
                )
            } else {
                scheduleStart(start, durationMin)
            }
        }
    }

    fun cancelAll() {
        // فقط شروع‌ها را لغو می‌کنیم؛ پایان‌ها را خود Receiver زمان‌بندی می‌کند
        val day = currentDay()
        val savedDay = prefs.getString(PREF_TIMES_DAY, null)
        if (savedDay != day) return
        val times = loadTimes()
        for (k in PRAYER_KEYS) {
            val start = times[k] ?: continue
            cancelStart(start.toInt()) // از زمان برای requestCode استفاده کردیم
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleStart(at: Long, durationMin: Int) {
        val i = Intent(SilentModeReceiver.ACTION_SILENT)
            .setClass(context, SilentModeReceiver::class.java)
            .putExtra(SilentModeReceiver.EXTRA_DURATION_MINUTES, durationMin)

        val requestCode = (at / 1000L).toInt() // یکتا برای هر زمان
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        val pi = PendingIntent.getBroadcast(context, requestCode, i, flags)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancelStart(at: Int) {
        val i = Intent(SilentModeReceiver.ACTION_SILENT)
            .setClass(context, SilentModeReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            (at / 1000),
            i,
            PendingIntent.FLAG_NO_CREATE or immutableFlag()
        )
        if (pi != null) am.cancel(pi)
    }

    private fun saveTodayTimes(times: Map<String, Long>) {
        val json = JSONObject()
        for ((k, v) in times) json.put(k, v)
        prefs.edit {
            putString(PREF_TIMES_DAY, currentDay())
                .putString(PREF_TIMES_JSON, json.toString())
        }
    }

    private fun loadTimes(): Map<String, Long> {
        val s = prefs.getString(PREF_TIMES_JSON, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(s)
            val m = mutableMapOf<String, Long>()
            obj.keys().forEach { k -> m[k] = obj.optLong(k, -1L) }
            m
        }.getOrDefault(emptyMap())
    }

    private fun saveEnabled(enabled: Map<String, Boolean>) {
        prefs.edit {
            for ((k, v) in enabled) putBoolean("en_$k", v)
        }
    }

    private fun loadEnabled(): Map<String, Boolean> {
        val out = mutableMapOf<String, Boolean>()
        for (k in listOf("fajr","dhuhr","asr","maghrib","isha")) {
            val v = prefs.getBoolean("en_$k", settingsPrefs.getBoolean("silent_$k", false))
            out[k] = v
        }
        return out
    }

    private fun currentDay(): String = LocalDate.now().toString()
    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}