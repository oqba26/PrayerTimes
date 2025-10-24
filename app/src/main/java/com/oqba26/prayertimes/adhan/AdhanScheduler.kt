package com.oqba26.prayertimes.adhan

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.oqba26.prayertimes.receivers.AdhanAlarmReceiver
import com.oqba26.prayertimes.viewmodels.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AdhanScheduler {

    private const val REQ_FAJR = 201
    private const val REQ_DHUHR = 202
    private const val REQ_ASR = 203
    private const val REQ_MAGHRIB = 204
    private const val REQ_ISHA = 205
    private const val REQ_RESCHEDULE_MIDNIGHT = 299

    private val fmt24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val fmt12 = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    /**
     * prayersMap باید شامل کلید/عنوان و مقدار ساعت هر نماز باشد.
     * هم فارسی و هم انگلیسی ساپورت می‌کنیم (با حدس زدن عنوان‌ها).
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun scheduleFromPrayerMap(context: Context, prayersMap: Map<String, String>) {
        cancelAll(context)

        val silentSettings = runBlocking {
            val ds = context.dataStore.data.first()
            mapOf(
                "fajr" to (ds[booleanPreferencesKey("fajr_silent_enabled")] ?: false),
                "dhuhr" to (ds[booleanPreferencesKey("dhuhr_silent_enabled")] ?: false),
                "asr" to (ds[booleanPreferencesKey("asr_silent_enabled")] ?: false),
                "maghrib" to (ds[booleanPreferencesKey("maghrib_silent_enabled")] ?: false),
                "isha" to (ds[booleanPreferencesKey("isha_silent_enabled")] ?: false)
            )
        }

        val fajr = findTime(prayersMap, "fajr", "اذان صبح", "صبح", "فجر", "طلوع بامداد")
        val dhuhr = findTime(prayersMap, "dhuhr", "zuhr", "ظهر")
        val asr = findTime(prayersMap, "asr", "عصر")
        val maghrib = findTime(prayersMap, "maghrib", "مغرب", "غروب")
        val isha = findTime(prayersMap, "isha", "عشا", "عشاء")

        fajr?.let { scheduleExact(context, REQ_FAJR, "fajr", it, silentSettings["fajr"] ?: false) }
        dhuhr?.let { scheduleExact(context, REQ_DHUHR, "dhuhr", it, silentSettings["dhuhr"] ?: false) }
        asr?.let { scheduleExact(context, REQ_ASR, "asr", it, silentSettings["asr"] ?: false) }
        maghrib?.let { scheduleExact(context, REQ_MAGHRIB, "maghrib", it, silentSettings["maghrib"] ?: false) }
        isha?.let { scheduleExact(context, REQ_ISHA, "isha", it, silentSettings["isha"] ?: false) }

        // آلارم کوچک برای نیمه‌شب تا فردا دوباره زمان‌بندی کنیم (با کمک سرویس اصلی)
        scheduleMidnightReschedule(context)
    }

    private fun findTime(map: Map<String, String>, vararg guesses: String): String? {
        if (map.isEmpty()) return null
        val entries = map.entries
        val lower = entries.associate { it.key.lowercase(Locale.ROOT) to it.value }

        guesses.forEach { g ->
            val gL = g.lowercase(Locale.ROOT)
            lower[gL]?.let { return it }
        }
        // contains جست‌وجوی انعطاف‌پذیر
        entries.forEach { (k, v) ->
            val lk = k.lowercase(Locale.ROOT)
            if (guesses.any { lk.contains(it.lowercase(Locale.ROOT)) }) return v
        }
        return null
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleExact(context: Context, reqCode: Int, prayerId: String, timeStr: String, enableSilentMode: Boolean) {
        val triggerAt = resolveTriggerMillis(timeStr)

        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra("PRAYER_ID", prayerId)
            putExtra("TRIGGER_AT", triggerAt) // برای بررسی drift در Receiver
            putExtra("ENABLE_SILENT_MODE", enableSilentMode)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (PendingIntent.FLAG_IMMUTABLE)
        val pi = PendingIntent.getBroadcast(context, reqCode, intent, flags)

        val am = context.getSystemService<AlarmManager>()!!

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(context.packageName)
            }
        val showIntent = PendingIntent.getActivity(
            context, reqCode, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (PendingIntent.FLAG_IMMUTABLE)
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), pi)

        android.util.Log.d(
            "AdhanScheduler",
            "Scheduled $prayerId for '$timeStr' -> ${java.util.Date(triggerAt)} ($triggerAt) with silent mode: $enableSilentMode"
        )
    }

    private fun resolveTriggerMillis(timeStr: String): Long {
        val now = LocalDateTime.now()
        val time = parseTimeFlexible(timeStr)
        var trigger = LocalDateTime.of(LocalDate.now(), time)
        if (trigger.isBefore(now)) trigger = trigger.plusDays(1)
        return trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun toLatinDigits(s: String): String = buildString {
        for (ch in s) append(
            when (ch) {
                '۰','٠' -> '0'
                '۱','١' -> '1'
                '۲','٢' -> '2'
                '۳','٣' -> '3'
                '۴','٤' -> '4'
                '۵','٥' -> '5'
                '۶','٦' -> '6'
                '۷','٧' -> '7'
                '۸','٨' -> '8'
                '۹','٩' -> '9'
                '٫','،' -> ':' // جداکننده‌های رایج فارسی/عربی
                else -> ch
            }
        )
    }

    private fun parseTimeFlexible(s: String): LocalTime {
        // نرمال‌سازی: اعداد فارسی/عربی + AM/PM فارسی
        val t0 = toLatinDigits(s.trim())
            .replace("ق.ظ", "AM", ignoreCase = true)
            .replace("ب.ظ", "PM", ignoreCase = true)
            .replace("ص", "AM", ignoreCase = true)
            .replace("م", "PM", ignoreCase = true)

        val t = t0.uppercase(Locale.ROOT)
        runCatching { return LocalTime.parse(t, fmt24) }
        runCatching { return LocalTime.parse(t, fmt12) }

        // الگوی انعطاف‌پذیر 24ساعته با جداکننده‌های مختلف
        val m = Regex("""^\s*(\d{1,2})[:.,](\d{1,2})""").find(t)
        if (m != null) {
            val h = m.groupValues[1].padStart(2, '0')
            val min = m.groupValues[2].padStart(2, '0')
            return LocalTime.parse("$h:$min", fmt24)
        }
        // اگر واقعاً نشد، ۵ دقیقه بعد
        return LocalTime.now().plusMinutes(5)
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService<AlarmManager>()!!
        listOf(REQ_FAJR, REQ_DHUHR, REQ_ASR, REQ_MAGHRIB, REQ_ISHA, REQ_RESCHEDULE_MIDNIGHT).forEach { code ->
            val pi = PendingIntent.getBroadcast(
                context, code, Intent(context, AdhanAlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or (PendingIntent.FLAG_IMMUTABLE)
            )
            if (pi != null) am.cancel(pi)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleMidnightReschedule(context: Context) {
        val am = context.getSystemService<AlarmManager>()!!
        val now = LocalDateTime.now()
        var target = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 1))
        // احتیاط: اگر الان نزدیک 00:01 هستیم، چند دقیقه عقب جلو شود
        if (target.isBefore(now)) target = now.plusMinutes(2)

        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            // از همین Receiver هم می‌توان برای استارت سرویس استفاده کرد؛
            // ولی بهتر است سرویس اصلی‌ات فردا خودش دوباره زمان‌ها را محاسبه کند.
            putExtra("PRAYER_ID", "noop")
        }
        val pi = PendingIntent.getBroadcast(
            context, REQ_RESCHEDULE_MIDNIGHT, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (PendingIntent.FLAG_IMMUTABLE)
        )
        val whenMs = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
    }
}
