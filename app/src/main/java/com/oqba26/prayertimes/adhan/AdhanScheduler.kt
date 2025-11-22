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
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oqba26.prayertimes.receivers.AdhanAlarmReceiver
import com.oqba26.prayertimes.receivers.IqamaAlarmReceiver
import com.oqba26.prayertimes.receivers.SilentModeReceiver
import com.oqba26.prayertimes.services.AdhanPlayerService
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PrayerUtils
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

    // Iqama request codes (must be different from adhan ones)
    private const val IQAMA_REQ_FAJR = 301
    private const val IQAMA_REQ_DHUHR = 302
    private const val IQAMA_REQ_ASR = 303
    private const val IQAMA_REQ_MAGHRIB = 304
    private const val IQAMA_REQ_ISHA = 305

    // Silent mode request codes (must be different)
    private const val SILENT_REQ_FAJR_START = 401
    private const val SILENT_REQ_FAJR_END = 402
    private const val SILENT_REQ_DHUHR_START = 403
    private const val SILENT_REQ_DHUHR_END = 404
    private const val SILENT_REQ_ASR_START = 405
    private const val SILENT_REQ_ASR_END = 406
    private const val SILENT_REQ_MAGHRIB_START = 407
    private const val SILENT_REQ_MAGHRIB_END = 408
    private const val SILENT_REQ_ISHA_START = 409
    private const val SILENT_REQ_ISHA_END = 410


    private val fmt24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val fmt12 = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    /**
     * prayersMap must contain the key/title and time value of each prayer.
     * We support both Farsi and English (by guessing the titles).
     */
    @Suppress("unused")
    @RequiresApi(Build.VERSION_CODES.M)
    fun scheduleFromPrayerMap(context: Context, prayersMap: Map<String, String>) {
        cancelAll(context)

        val settings = runBlocking {
            val ds = context.dataStore.data.first()
            object {
                val fajrSilent = ds[booleanPreferencesKey("fajr_silent_enabled")] ?: false
                val dhuhrSilent = ds[booleanPreferencesKey("dhuhr_silent_enabled")] ?: false
                val asrSilent = ds[booleanPreferencesKey("asr_silent_enabled")] ?: false
                val maghribSilent = ds[booleanPreferencesKey("maghrib_silent_enabled")] ?: false
                val ishaSilent = ds[booleanPreferencesKey("isha_silent_enabled")] ?: false
                val iqamaEnabled = ds[booleanPreferencesKey("iqama_enabled")] ?: false
                val iqamaOffset = ds[intPreferencesKey("minutes_before_iqama")] ?: 0
                val fajrMinutesBefore = ds[intPreferencesKey("fajr_minutes_before")] ?: 10
                val fajrMinutesAfter = ds[intPreferencesKey("fajr_minutes_after")] ?: 10
                val dhuhrMinutesBefore = ds[intPreferencesKey("dhuhr_minutes_before")] ?: 10
                val dhuhrMinutesAfter = ds[intPreferencesKey("dhuhr_minutes_after")] ?: 10
                val asrMinutesBefore = ds[intPreferencesKey("asr_minutes_before")] ?: 10
                val asrMinutesAfter = ds[intPreferencesKey("asr_minutes_after")] ?: 10
                val maghribMinutesBefore = ds[intPreferencesKey("maghrib_minutes_before")] ?: 10
                val maghribMinutesAfter = ds[intPreferencesKey("maghrib_minutes_after")] ?: 10
                val ishaMinutesBefore = ds[intPreferencesKey("isha_minutes_before")] ?: 10
                val ishaMinutesAfter = ds[intPreferencesKey("isha_minutes_after")] ?: 10
                val fajrAdhanSound = ds[stringPreferencesKey("fajr_adhan_sound")] ?: "off"
                val dhuhrAdhanSound = ds[stringPreferencesKey("dhuhr_adhan_sound")] ?: "off"
                val asrAdhanSound = ds[stringPreferencesKey("asr_adhan_sound")] ?: "off"
                val maghribAdhanSound = ds[stringPreferencesKey("maghrib_adhan_sound")] ?: "off"
                val ishaAdhanSound = ds[stringPreferencesKey("isha_adhan_sound")] ?: "off"
            }
        }

        val detailedPrayers = runBlocking { PrayerUtils.loadDetailedPrayerTimes(context, DateUtils.getCurrentDate()) }

        val fajr = findTime(detailedPrayers, "fajr", "اذان صبح", "صبح")
        val dhuhr = findTime(detailedPrayers, "dhuhr", "zuhr", "ظهر")
        val asr = findTime(detailedPrayers, "asr", "عصر")
        val maghrib = findTime(detailedPrayers, "maghrib", "مغرب")
        val isha = findTime(detailedPrayers, "isha", "عشا", "عشاء")

        fajr?.let { scheduleExact(context, REQ_FAJR, "fajr", it, settings.fajrAdhanSound, settings.fajrSilent, settings.fajrMinutesBefore, settings.fajrMinutesAfter) }
        dhuhr?.let { scheduleExact(context, REQ_DHUHR, "dhuhr", it, settings.dhuhrAdhanSound, settings.dhuhrSilent, settings.dhuhrMinutesBefore, settings.dhuhrMinutesAfter) }
        asr?.let { scheduleExact(context, REQ_ASR, "asr", it, settings.asrAdhanSound, settings.asrSilent, settings.asrMinutesBefore, settings.asrMinutesAfter) }
        maghrib?.let { scheduleExact(context, REQ_MAGHRIB, "maghrib", it, settings.maghribAdhanSound, settings.maghribSilent, settings.maghribMinutesBefore, settings.maghribMinutesAfter) }
        isha?.let { scheduleExact(context, REQ_ISHA, "isha", it, settings.ishaAdhanSound, settings.ishaSilent, settings.ishaMinutesBefore, settings.ishaMinutesAfter) }

        if (settings.iqamaEnabled) {
            val offset = settings.iqamaOffset.toLong()
            detailedPrayers["صبح"]?.let { scheduleIqama(context, IQAMA_REQ_FAJR, "fajr", it, offset) }
            detailedPrayers["ظهر"]?.let { scheduleIqama(context, IQAMA_REQ_DHUHR, "dhuhr", it, offset) }
            detailedPrayers["عصر"]?.let { scheduleIqama(context, IQAMA_REQ_ASR, "asr", it, offset) }
            detailedPrayers["مغرب"]?.let { scheduleIqama(context, IQAMA_REQ_MAGHRIB, "maghrib", it, offset) }
            detailedPrayers["عشاء"]?.let { scheduleIqama(context, IQAMA_REQ_ISHA, "isha", it, offset) }
        }

        // Small alarm for midnight to reschedule for tomorrow (with the help of the main service)
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
        // Flexible search with contains
        entries.forEach { (k, v) ->
            val lk = k.lowercase(Locale.ROOT)
            if (guesses.any { lk.contains(it.lowercase(Locale.ROOT)) }) return v
        }
        return null
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleExact(context: Context, reqCode: Int, prayerId: String, timeStr: String, adhanSound: String, enableSilentMode: Boolean, minutesBefore: Int, minutesAfter: Int) {
        val prayerTime = parseTimeFlexible(timeStr)
        val adhanTime = when (prayerId) {
            "fajr" -> prayerTime.minusMinutes(30)
            "dhuhr", "asr", "isha" -> prayerTime.minusMinutes(20)
            else -> prayerTime // For maghrib, no change
        }

        val triggerAt = resolveTriggerMillis(adhanTime.format(fmt24))

        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra(AdhanPlayerService.EXTRA_PRAYER_ID, prayerId)
            putExtra(AdhanPlayerService.EXTRA_ADHAN_SOUND, adhanSound)
            putExtra("TRIGGER_AT", triggerAt)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (PendingIntent.FLAG_IMMUTABLE)
        val pi = PendingIntent.getBroadcast(context, reqCode, intent, flags)

        val am = context.getSystemService<AlarmManager>()!!

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(context.packageName)
            }
        val showIntent = PendingIntent.getActivity(
            context, reqCode, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or (PendingIntent.FLAG_IMMUTABLE)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            android.util.Log.w("AdhanScheduler", "Missing SCHEDULE_EXACT_ALARM permission")
        } else {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), pi)
        }

        if (enableSilentMode) {
            scheduleSilent(context, prayerId, timeStr, minutesBefore, minutesAfter)
        }

        android.util.Log.d(
            "AdhanScheduler",
            "Scheduled $prayerId for \'$timeStr\' -> ${java.util.Date(triggerAt)} ($triggerAt) with silent mode: $enableSilentMode"
        )
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleIqama(context: Context, reqCode: Int, prayerName: String, timeStr: String, offsetMinutes: Long) {
        val prayerTime = parseTimeFlexible(timeStr)
        val iqamaTime = prayerTime.minusMinutes(offsetMinutes)

        val triggerAt = resolveTriggerMillis(iqamaTime.format(fmt24))

        val intent = Intent(context, IqamaAlarmReceiver::class.java).apply {
            putExtra("PRAYER_NAME", prayerName)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, reqCode, intent, flags)
        val am = context.getSystemService<AlarmManager>()!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                android.util.Log.w("AdhanScheduler", "Missing SCHEDULE_EXACT_ALARM permission for Iqama")
                return
            }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }

        android.util.Log.d(
            "AdhanScheduler",
            "Scheduled Iqama for $prayerName at ${java.util.Date(triggerAt)}"
        )
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleSilent(
        context: Context,
        prayerId: String,
        timeStr: String,
        minutesBefore: Int,
        minutesAfter: Int
    ) {
        val prayerTime = parseTimeFlexible(timeStr)
        val startTime = prayerTime.minusMinutes(minutesBefore.toLong())
        val endTime = prayerTime.plusMinutes(minutesAfter.toLong())

        val startTrigger = resolveTriggerMillis(startTime.format(fmt24))
        val endTrigger = resolveTriggerMillis(endTime.format(fmt24))

        val startIntent = Intent(context, SilentModeReceiver::class.java).apply {
            action = SilentModeReceiver.ACTION_SILENT
            putExtra("PRAYER_ID", prayerId)
        }
        val endIntent = Intent(context, SilentModeReceiver::class.java).apply {
            action = SilentModeReceiver.ACTION_UNSILENT
            putExtra("PRAYER_ID", prayerId)
        }

        val startReqCode = getSilentReqCode(prayerId, true)
        val endReqCode = getSilentReqCode(prayerId, false)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startPi = PendingIntent.getBroadcast(
            context, startReqCode, startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val endPi = PendingIntent.getBroadcast(
            context, endReqCode, endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                android.util.Log.w("AdhanScheduler", "⛔ اجازه SCHEDULE_EXACT_ALARM موجود نیست؛ سکوت برنامه‌ریزی نشد.")
                return
            }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTrigger, startPi)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTrigger, endPi)
        }

        android.util.Log.d(
            "AdhanScheduler",
            "📱 سکوت (ویبره) برنامه‌ریزی شد برای $prayerId از ${java.util.Date(startTrigger)} تا ${java.util.Date(endTrigger)}"
        )
    }

    private fun getSilentReqCode(prayerId: String, isStart: Boolean): Int {
        return when (prayerId) {
            "fajr" -> if (isStart) SILENT_REQ_FAJR_START else SILENT_REQ_FAJR_END
            "dhuhr" -> if (isStart) SILENT_REQ_DHUHR_START else SILENT_REQ_DHUHR_END
            "asr" -> if (isStart) SILENT_REQ_ASR_START else SILENT_REQ_ASR_END
            "maghrib" -> if (isStart) SILENT_REQ_MAGHRIB_START else SILENT_REQ_MAGHRIB_END
            "isha" -> if (isStart) SILENT_REQ_ISHA_START else SILENT_REQ_ISHA_END
            else -> -1
        }
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
                '٫','،' -> ':' // Common Persian/Arabic separators
                else -> ch
            }
        )
    }

    private fun parseTimeFlexible(s: String): LocalTime {
        // Normalization: Persian/Arabic numbers + Persian AM/PM
        val t0 = toLatinDigits(s.trim())
            .replace("ق.ظ", "AM", ignoreCase = true)
            .replace("ب.ظ", "PM", ignoreCase = true)
            .replace("ص", "AM", ignoreCase = true)
            .replace("م", "PM", ignoreCase = true)

        val t = t0.uppercase(Locale.ROOT)
        runCatching { return LocalTime.parse(t, fmt24) }
        runCatching { return LocalTime.parse(t, fmt12) }

        // Flexible 24-hour pattern with different separators
        val m = Regex("""^\s*(\d{1,2})[:.,](\d{1,2})""").find(t)
        if (m != null) {
            val h = m.groupValues[1].padStart(2, '0')
            val min = m.groupValues[2].padStart(2, '0')
            return LocalTime.parse("$h:$min", fmt24)
        }
        // If it really fails, 5 minutes from now
        return LocalTime.now().plusMinutes(5)
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService<AlarmManager>()!!
        val reqCodes = listOf(
            REQ_FAJR, REQ_DHUHR, REQ_ASR, REQ_MAGHRIB, REQ_ISHA, REQ_RESCHEDULE_MIDNIGHT,
            IQAMA_REQ_FAJR, IQAMA_REQ_DHUHR, IQAMA_REQ_ASR, IQAMA_REQ_MAGHRIB, IQAMA_REQ_ISHA,
            SILENT_REQ_FAJR_START, SILENT_REQ_FAJR_END, SILENT_REQ_DHUHR_START, SILENT_REQ_DHUHR_END,
            SILENT_REQ_ASR_START, SILENT_REQ_ASR_END, SILENT_REQ_MAGHRIB_START, SILENT_REQ_MAGHRIB_END,
            SILENT_REQ_ISHA_START, SILENT_REQ_ISHA_END
        )
        val intentClasses = listOf(AdhanAlarmReceiver::class.java, IqamaAlarmReceiver::class.java, SilentModeReceiver::class.java)

        reqCodes.forEach { code ->
            intentClasses.forEach { intentClass ->
                val pi = PendingIntent.getBroadcast(
                    context, code, Intent(context, intentClass),
                    PendingIntent.FLAG_NO_CREATE or (PendingIntent.FLAG_IMMUTABLE)
                )
                if (pi != null) am.cancel(pi)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleMidnightReschedule(context: Context) {
        val am = context.getSystemService<AlarmManager>()!!
        val now = LocalDateTime.now()
        var target = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 1))
        // Precaution: if it's already close to 00:01, move it a few minutes
        if (target.isBefore(now)) target = now.plusMinutes(2)

        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            // This same receiver can be used to start the service,
            // but it's better if your main service recalculates the times tomorrow.
            putExtra("PRAYER_ID", "noop")
        }
        val pi = PendingIntent.getBroadcast(
            context, REQ_RESCHEDULE_MIDNIGHT, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (PendingIntent.FLAG_IMMUTABLE)
        )
        val whenMs = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            android.util.Log.w("AdhanScheduler", "Missing SCHEDULE_EXACT_ALARM permission for Reschedule")
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
        }
    }
}
