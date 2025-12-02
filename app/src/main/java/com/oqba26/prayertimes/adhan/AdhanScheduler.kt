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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AdhanScheduler {

    private const val TAG = "AdhanScheduler"

    private const val REQ_FAJR = 201
    private const val REQ_DHUHR = 202
    private const val REQ_ASR = 203
    private const val REQ_MAGHRIB = 204
    private const val REQ_ISHA = 205
    private const val REQ_RESCHEDULE_MIDNIGHT = 299

    private const val IQAMA_REQ_FAJR = 301
    private const val IQAMA_REQ_DHUHR = 302
    private const val IQAMA_REQ_ASR = 303
    private const val IQAMA_REQ_MAGHRIB = 304
    private const val IQAMA_REQ_ISHA = 305

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

    private const val CANCEL_IQAMA_REQ_OFFSET = 1000

    private val fmt24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val fmt12 = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    @Suppress("unused", "UNUSED_PARAMETER")
    @RequiresApi(Build.VERSION_CODES.M)
    fun scheduleFromPrayerMap(context: Context, prayersMap: Map<String, String>) {
        cancelAll(context)

        val settings = runBlocking {
            val ds = context.dataStore.data.first()
            object {
                val autoSilentEnabled = ds[booleanPreferencesKey("auto_silent_enabled")] ?: false

                val fajrSilent = ds[booleanPreferencesKey("silent_enabled_fajr")] ?: false
                val dhuhrSilent = ds[booleanPreferencesKey("silent_enabled_dhuhr")] ?: false
                val asrSilent = ds[booleanPreferencesKey("silent_enabled_asr")] ?: false
                val maghribSilent = ds[booleanPreferencesKey("silent_enabled_maghrib")] ?: false
                val ishaSilent = ds[booleanPreferencesKey("silent_enabled_isha")] ?: false

                val fajrMinutesBefore = ds[intPreferencesKey("minutes_before_silent_fajr")] ?: 10
                val fajrMinutesAfter = ds[intPreferencesKey("minutes_after_silent_fajr")] ?: 10
                val dhuhrMinutesBefore = ds[intPreferencesKey("minutes_before_silent_dhuhr")] ?: 10
                val dhuhrMinutesAfter = ds[intPreferencesKey("minutes_after_silent_dhuhr")] ?: 10
                val asrMinutesBefore = ds[intPreferencesKey("minutes_before_silent_asr")] ?: 10
                val asrMinutesAfter = ds[intPreferencesKey("minutes_after_silent_asr")] ?: 10
                val maghribMinutesBefore = ds[intPreferencesKey("minutes_before_silent_maghrib")] ?: 10
                val maghribMinutesAfter = ds[intPreferencesKey("minutes_after_silent_maghrib")] ?: 10
                val ishaMinutesBefore = ds[intPreferencesKey("minutes_before_silent_isha")] ?: 10
                val ishaMinutesAfter = ds[intPreferencesKey("minutes_after_silent_isha")] ?: 10

                val fajrAdhanSound = ds[stringPreferencesKey("adhan_sound_fajr")] ?: "off"
                val dhuhrAdhanSound = ds[stringPreferencesKey("adhan_sound_dhuhr")] ?: "off"
                val asrAdhanSound = ds[stringPreferencesKey("adhan_sound_asr")] ?: "off"
                val maghribAdhanSound = ds[stringPreferencesKey("adhan_sound_maghrib")] ?: "off"
                val ishaAdhanSound = ds[stringPreferencesKey("adhan_sound_isha")] ?: "off"

                val fajrMinutesBeforeAdhan = ds[intPreferencesKey("minutes_before_adhan_fajr")] ?: 0
                val dhuhrMinutesBeforeAdhan = ds[intPreferencesKey("minutes_before_adhan_dhuhr")] ?: 0
                val asrMinutesBeforeAdhan = ds[intPreferencesKey("minutes_before_adhan_asr")] ?: 0
                val maghribMinutesBeforeAdhan = ds[intPreferencesKey("minutes_before_adhan_maghrib")] ?: 0
                val ishaMinutesBeforeAdhan = ds[intPreferencesKey("minutes_before_adhan_isha")] ?: 0

                val iqamaFajrEnabled = ds[booleanPreferencesKey("iqama_enabled_fajr")] ?: false
                val iqamaDhuhrEnabled = ds[booleanPreferencesKey("iqama_enabled_dhuhr")] ?: false
                val iqamaAsrEnabled = ds[booleanPreferencesKey("iqama_enabled_asr")] ?: false
                val iqamaMaghribEnabled = ds[booleanPreferencesKey("iqama_enabled_maghrib")] ?: false
                val iqamaIshaEnabled = ds[booleanPreferencesKey("iqama_enabled_isha")] ?: false
                val iqamaOffset = ds[intPreferencesKey("minutes_before_iqama")] ?: 0
            }
        }

        val detailedPrayers = runBlocking {
            PrayerUtils.loadDetailedPrayerTimes(context, DateUtils.getCurrentDate())
        }

        val fajr = findTime(detailedPrayers, "fajr", "اذان صبح", "صبح")
        val dhuhr = findTime(detailedPrayers, "dhuhr", "zuhr", "ظهر")
        val asr = findTime(detailedPrayers, "asr", "عصر")
        val maghrib = findTime(detailedPrayers, "maghrib", "مغرب")
        val isha = findTime(detailedPrayers, "isha", "عشا", "عشاء")

        fajr?.let {
            scheduleExact(
                context,
                REQ_FAJR,
                "fajr",
                it,
                settings.fajrAdhanSound,
                settings.fajrMinutesBeforeAdhan,
                settings.autoSilentEnabled && settings.fajrSilent,
                settings.fajrMinutesBefore,
                settings.fajrMinutesAfter
            )
        }
        dhuhr?.let {
            scheduleExact(
                context,
                REQ_DHUHR,
                "dhuhr",
                it,
                settings.dhuhrAdhanSound,
                settings.dhuhrMinutesBeforeAdhan,
                settings.autoSilentEnabled && settings.dhuhrSilent,
                settings.dhuhrMinutesBefore,
                settings.dhuhrMinutesAfter
            )
        }
        asr?.let {
            scheduleExact(
                context,
                REQ_ASR,
                "asr",
                it,
                settings.asrAdhanSound,
                settings.asrMinutesBeforeAdhan,
                settings.autoSilentEnabled && settings.asrSilent,
                settings.asrMinutesBefore,
                settings.asrMinutesAfter
            )
        }
        maghrib?.let {
            scheduleExact(
                context,
                REQ_MAGHRIB,
                "maghrib",
                it,
                settings.maghribAdhanSound,
                settings.maghribMinutesBeforeAdhan,
                settings.autoSilentEnabled && settings.maghribSilent,
                settings.maghribMinutesBefore,
                settings.maghribMinutesAfter
            )
        }
        isha?.let {
            scheduleExact(
                context,
                REQ_ISHA,
                "isha",
                it,
                settings.ishaAdhanSound,
                settings.ishaMinutesBeforeAdhan,
                settings.autoSilentEnabled && settings.ishaSilent,
                settings.ishaMinutesBefore,
                settings.ishaMinutesAfter
            )
        }

        // --- زمان‌بندی اعلان اقامه ---
        if (settings.iqamaOffset > 0) {
            val offset = settings.iqamaOffset.toLong()
            if (settings.iqamaFajrEnabled) {
                detailedPrayers["صبح"]?.let {
                    scheduleIqama(context, IQAMA_REQ_FAJR, "fajr", it, offset)
                }
            }
            if (settings.iqamaDhuhrEnabled) {
                detailedPrayers["ظهر"]?.let {
                    scheduleIqama(context, IQAMA_REQ_DHUHR, "dhuhr", it, offset)
                }
            }
            if (settings.iqamaAsrEnabled) {
                detailedPrayers["عصر"]?.let {
                    scheduleIqama(context, IQAMA_REQ_ASR, "asr", it, offset)
                }
            }
            if (settings.iqamaMaghribEnabled) {
                detailedPrayers["مغرب"]?.let {
                    scheduleIqama(context, IQAMA_REQ_MAGHRIB, "maghrib", it, offset)
                }
            }
            if (settings.iqamaIshaEnabled) {
                detailedPrayers["عشاء"]?.let {
                    scheduleIqama(context, IQAMA_REQ_ISHA, "isha", it, offset)
                }
            }
        }

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
        entries.forEach { (k, v) ->
            val lk = k.lowercase(Locale.ROOT)
            if (guesses.any { lk.contains(it.lowercase(Locale.ROOT)) }) return v
        }
        return null
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleExact(
        context: Context,
        reqCode: Int,
        prayerId: String,
        timeStr: String,
        adhanSound: String,
        minutesBeforeAdhan: Int,
        enableSilentMode: Boolean,
        minutesBefore: Int,
        minutesAfter: Int
    ) {
        val prayerTime = parseTimeFlexible(timeStr)
        val adhanTime = prayerTime.minusMinutes(minutesBeforeAdhan.toLong())
        val triggerAt = resolveTriggerMillis(adhanTime.format(fmt24))

        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra(AdhanPlayerService.EXTRA_PRAYER_ID, prayerId)
            putExtra(AdhanPlayerService.EXTRA_ADHAN_SOUND, adhanSound)
            putExtra("TRIGGER_AT", triggerAt)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, reqCode, intent, flags)
        val am = context.getSystemService<AlarmManager>()!!

        val launch =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(context.packageName)
                }
        val showIntent = PendingIntent.getActivity(
            context,
            reqCode,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            android.util.Log.w(
                TAG,
                "SCHEDULE_EXACT_ALARM not granted; will try to schedule Adhan anyway (may be inexact)."
            )
        }

        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), pi)

        if (enableSilentMode) {
            scheduleSilent(context, prayerId, timeStr, minutesBefore, minutesAfter)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleIqama(
        context: Context,
        @Suppress("unused") reqCode: Int,
        prayerName: String,
        timeStr: String,
        offsetMinutes: Long
    ) {
        val prayerTime = parseTimeFlexible(timeStr)
        val iqamaStartTime = prayerTime.minusMinutes(offsetMinutes)
        val iqamaStartMillis = resolveTriggerMillis(iqamaStartTime.format(fmt24))
        val prayerMillis = resolveTriggerMillis(prayerTime.format(fmt24))

        if (iqamaStartMillis < System.currentTimeMillis()) return // Don't schedule for past times

        val am = context.getSystemService<AlarmManager>()!!
        val notificationId =
            IqamaAlarmReceiver.DEFAULT_NOTIFICATION_ID + prayerName.hashCode()

        // Intent برای شروع/آپدیت نوتیف اقامه
        val updateIntent = Intent(context, IqamaAlarmReceiver::class.java).apply {
            action = IqamaAlarmReceiver.ACTION_UPDATE_IQAMA
            putExtra("PRAYER_NAME", prayerName)
            putExtra("PRAYER_TIME", timeStr)
            // تم و فونت دیگر اینجا ست نمی‌شوند؛
            // خود Receiver هر بار از DataStore می‌خواند.
        }
        val updatePi = PendingIntent.getBroadcast(
            context,
            notificationId,
            updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent برای لغو نوتیف اقامه در وقت نماز
        val cancelIntent = Intent(context, IqamaAlarmReceiver::class.java).apply {
            action = IqamaAlarmReceiver.ACTION_CANCEL_IQAMA
            putExtra(IqamaAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val cancelPi = PendingIntent.getBroadcast(
            context,
            notificationId + CANCEL_IQAMA_REQ_OFFSET,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                android.util.Log.w(
                    TAG,
                    "SCHEDULE_EXACT_ALARM not granted for Iqama; will try to schedule anyway (may be inexact)."
                )
            }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, iqamaStartMillis, updatePi)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, prayerMillis, cancelPi)
        }

        android.util.Log.d(
            TAG,
            "Scheduled Iqama for $prayerName at ${java.util.Date(iqamaStartMillis)}"
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
            context,
            startReqCode,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val endPi = PendingIntent.getBroadcast(
            context,
            endReqCode,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                android.util.Log.w(
                    TAG,
                    "SCHEDULE_EXACT_ALARM not granted; will try to schedule silent mode anyway (may be inexact)."
                )
            }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTrigger, startPi)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTrigger, endPi)
        }
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
                '۰', '٠' -> '0'
                '۱', '١' -> '1'
                '۲', '٢' -> '2'
                '۳', '٣' -> '3'
                '۴', '٤' -> '4'
                '۵', '٥' -> '5'
                '۶', '٦' -> '6'
                '۷', '٧' -> '7'
                '۸', '٨' -> '8'
                '۹', '٩' -> '9'
                '٫', '،' -> ':'
                else -> ch
            }
        )
    }

    private fun parseTimeFlexible(s: String): LocalTime {
        val t0 = toLatinDigits(s.trim())
            .replace("ق.ظ", "AM", true)
            .replace("ب.ظ", "PM", true)
            .replace("ص", "AM", true)
            .replace("م", "PM", true)
        val t = t0.uppercase(Locale.ROOT)
        runCatching { return LocalTime.parse(t, fmt24) }
        runCatching { return LocalTime.parse(t, fmt12) }

        val m = Regex("""^\s*(\d{1,2})[:.,](\d{1,2})""").find(t)
        if (m != null) {
            val h = m.groupValues[1].padStart(2, '0')
            val min = m.groupValues[2].padStart(2, '0')
            return LocalTime.parse("$h:$min", fmt24)
        }
        return LocalTime.now().plusMinutes(5)
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService<AlarmManager>()!!
        val reqCodes = listOf(
            REQ_FAJR, REQ_DHUHR, REQ_ASR, REQ_MAGHRIB, REQ_ISHA, REQ_RESCHEDULE_MIDNIGHT,
            IQAMA_REQ_FAJR, IQAMA_REQ_DHUHR, IQAMA_REQ_ASR, IQAMA_REQ_MAGHRIB, IQAMA_REQ_ISHA,
            SILENT_REQ_FAJR_START, SILENT_REQ_FAJR_END,
            SILENT_REQ_DHUHR_START, SILENT_REQ_DHUHR_END,
            SILENT_REQ_ASR_START, SILENT_REQ_ASR_END,
            SILENT_REQ_MAGHRIB_START, SILENT_REQ_MAGHRIB_END,
            SILENT_REQ_ISHA_START, SILENT_REQ_ISHA_END
        ).flatMap { listOf(it, it + CANCEL_IQAMA_REQ_OFFSET) }
        val intentClasses = listOf(
            AdhanAlarmReceiver::class.java,
            IqamaAlarmReceiver::class.java,
            SilentModeReceiver::class.java
        )

        reqCodes.forEach { code ->
            intentClasses.forEach { intentClass ->
                val pi = PendingIntent.getBroadcast(
                    context,
                    code,
                    Intent(context, intentClass),
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
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
        if (target.isBefore(now)) target = now.plusMinutes(2)

        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra("PRAYER_ID", "noop")
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQ_RESCHEDULE_MIDNIGHT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val whenMs = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            android.util.Log.w(
                TAG,
                "SCHEDULE_EXACT_ALARM not granted for midnight reschedule; will try anyway (may be inexact)."
            )
        }
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
    }

    // ------------------------
    // رفرش آنی نوتیف اقامه
    // ------------------------
    /**
     * این تابع وقتی صدا زده شود، برای نمازهایی که الان در بازه‌ی
     * [زمان شروع اقامه تا خود نماز] هستند، یک UPDATE_IQAMA می‌فرستد
     * تا نوتیف اقامه‌ی فعلی فوراً با تم/فونت جدید رفرش شود.
     */
    suspend fun refreshIqamaNotifications(context: Context) {
        withContext(Dispatchers.IO) {
            val ds = context.dataStore.data.first()

            val iqamaOffset = ds[intPreferencesKey("minutes_before_iqama")] ?: 0
            if (iqamaOffset <= 0) return@withContext

            val iqamaFajrEnabled = ds[booleanPreferencesKey("iqama_enabled_fajr")] ?: false
            val iqamaDhuhrEnabled = ds[booleanPreferencesKey("iqama_enabled_dhuhr")] ?: false
            val iqamaAsrEnabled = ds[booleanPreferencesKey("iqama_enabled_asr")] ?: false
            val iqamaMaghribEnabled = ds[booleanPreferencesKey("iqama_enabled_maghrib")] ?: false
            val iqamaIshaEnabled = ds[booleanPreferencesKey("iqama_enabled_isha")] ?: false

            if (!iqamaFajrEnabled && !iqamaDhuhrEnabled && !iqamaAsrEnabled &&
                !iqamaMaghribEnabled && !iqamaIshaEnabled
            ) {
                return@withContext
            }

            val detailedPrayers =
                PrayerUtils.loadDetailedPrayerTimes(context, DateUtils.getCurrentDate())
            val now = LocalDateTime.now()
            val offsetMinutes = iqamaOffset.toLong()

            fun maybeRefresh(prayerKey: String, englishName: String, enabled: Boolean) {
                if (!enabled) return
                val timeStr = detailedPrayers[prayerKey] ?: return

                val prayerTime = parseTimeFlexible(timeStr)
                var prayerDateTime = LocalDateTime.of(LocalDate.now(), prayerTime)
                if (prayerDateTime.isBefore(now)) {
                    prayerDateTime = prayerDateTime.plusDays(1)
                }
                val iqamaStartDateTime = prayerDateTime.minusMinutes(offsetMinutes)

                // فقط اگر الان بین شروع اقامه و خود نماز هستیم
                if (now.isBefore(iqamaStartDateTime)) return   // هنوز شروع نشده
                if (!now.isBefore(prayerDateTime)) return      // از وقت نماز گذشته

                val intent = Intent(context, IqamaAlarmReceiver::class.java).apply {
                    action = IqamaAlarmReceiver.ACTION_UPDATE_IQAMA
                    putExtra("PRAYER_NAME", englishName)
                    putExtra("PRAYER_TIME", timeStr)
                }
                context.sendBroadcast(intent)
            }

            maybeRefresh("صبح", "fajr", iqamaFajrEnabled)
            maybeRefresh("ظهر", "dhuhr", iqamaDhuhrEnabled)
            maybeRefresh("عصر", "asr", iqamaAsrEnabled)
            maybeRefresh("مغرب", "maghrib", iqamaMaghribEnabled)
            maybeRefresh("عشاء", "isha", iqamaIshaEnabled)
        }
    }
}