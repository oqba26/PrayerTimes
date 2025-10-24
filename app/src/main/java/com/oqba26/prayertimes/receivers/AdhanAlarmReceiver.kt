package com.oqba26.prayertimes.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.oqba26.prayertimes.services.AdhanPlayerService
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PrayerUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdhanAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AdhanAlarmReceiver"
        private const val MIN_REPLAY_GAP_MS = 8 * 60 * 1000L       // حداقل فاصله برای جلوگیری از تکرار
        private const val MAX_DRIFT_MS = 15 * 60 * 1000L           // تلرانس نسبت به زمان آلارم تنظیم‌شده
        private const val MAX_DIFF_FROM_REAL_MS = 20 * 60 * 1000L  // تلرانس نسبت به زمان واقعی نماز

        private val fmt24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

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
                    '٫','،' -> ':'
                    else -> ch
                }
            )
        }

        private fun parseTimeFlexible(s: String): LocalTime? {
            val t0 = toLatinDigits(s.trim())
                .replace("ق.ظ", "AM", ignoreCase = true)
                .replace("ب.ظ", "PM", ignoreCase = true)
                .replace("ص", "AM", ignoreCase = true)
                .replace("م", "PM", ignoreCase = true)
                .uppercase(Locale.ROOT)

            runCatching { return LocalTime.parse(t0, fmt24) }
            runCatching { return LocalTime.parse(t0, DateTimeFormatter.ofPattern("h:mm a", Locale.US)) }

            val m = Regex("""^\s*(\d{1,2})[:.,](\d{1,2})""").find(t0)
            if (m != null) {
                val h = m.groupValues[1].padStart(2, '0')
                val min = m.groupValues[2].padStart(2, '0')
                return LocalTime.parse("$h:$min", fmt24)
            }
            return null
        }

        private fun findTime(map: Map<String, String>, vararg guesses: String): String? {
            if (map.isEmpty()) return null
            fun norm(s: String): String = s.trim().lowercase(Locale.ROOT)
                .replace('ي', 'ی').replace('ك', 'ک')
                .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')

            val normalized = map.mapKeys { norm(it.key) }
            guesses.forEach { g -> normalized[norm(g)]?.let { return it } }
            val norms = guesses.map { norm(it) }
            normalized.forEach { (k, v) -> if (norms.any { k.contains(it) }) return v }
            return null
        }

        // نسخه بلاکینگ امن برای BroadcastReceiver: فراخوانی suspend داخل runBlocking + Dispatchers.IO
        private fun expectedTriggerForTodayMs(context: Context, prayerId: String): Long? = runBlocking {
            val today = DateUtils.getCurrentDate()
            val map = withContext(Dispatchers.IO) {
                PrayerUtils.loadPrayerTimes(context, today) // suspend
            }

            val keyTime = when (prayerId) {
                "fajr"    -> findTime(map, "طلوع بامداد", "اذان صبح", "صبح", "فجر")
                "dhuhr"   -> findTime(map, "ظهر", "dhuhr", "zuhr")
                "asr"     -> findTime(map, "عصر", "asr")
                "maghrib" -> findTime(map, "غروب", "مغرب", "maghrib")
                "isha"    -> findTime(map, "عشاء", "عشا", "isha")
                else      -> null
            } ?: return@runBlocking null

            val lt = parseTimeFlexible(keyTime) ?: return@runBlocking null
            val ldt = LocalDateTime.of(LocalDate.now(), lt)
            ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }

    @SuppressLint("UseKtx")
    override fun onReceive(context: Context, intent: Intent) {
        val prayerId = intent.getStringExtra("PRAYER_ID") ?: "dhuhr"
        val enableSilentMode = intent.getBooleanExtra("ENABLE_SILENT_MODE", false)

        if (enableSilentMode) {
            val silentIntent = Intent(context, SilentModeReceiver::class.java).apply {
                action = SilentModeReceiver.ACTION_SILENT
            }
            context.sendBroadcast(silentIntent)
        }

        // اگر کاربر پخش اذان را غیرفعال کرده:
        val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!settings.getBoolean("adhan_enabled", true)) {
            Log.d(TAG, "Adhan disabled. Skipping $prayerId")
            return
        }

        val guard = context.getSharedPreferences("adhan_guard", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        // جلوگیری از تکرار پشت‌سرهم برای یک نماز
        val last = guard.getLong("last_$prayerId", 0L)
        if (now - last < MIN_REPLAY_GAP_MS) {
            Log.d(TAG, "Duplicate trigger for $prayerId (gap ${(now - last) / 1000}s). Ignored.")
            return
        }

        // اگر Intent زمان برنامه‌ریزی‌شده را دارد، Drift را چک کن
        val scheduledAt = intent.getLongExtra("TRIGGER_AT", -1L)
        if (scheduledAt > 0 && abs(now - scheduledAt) > MAX_DRIFT_MS) {
            Log.w(TAG, "Trigger drift too large for $prayerId: diff=${abs(now - scheduledAt) / 1000}s. Ignored.")
            return
        }

        // اگر TRIGGER_AT نداریم، با زمان واقعی امروز بسنج (با runBlocking)
        if (scheduledAt <= 0) {
            val expected = expectedTriggerForTodayMs(context, prayerId)
            if (expected != null && abs(now - expected) > MAX_DIFF_FROM_REAL_MS) {
                Log.w(TAG, "Out-of-window trigger for $prayerId (diff=${abs(now - expected) / 1000}s). Ignored.")
                return
            }
        }

        // همه‌چیز اوکی: ثبت آخرین اجرا و پخش
        guard.edit().putLong("last_$prayerId", now).apply()
        Toast.makeText(context, "اذان: $prayerId", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "onReceive: prayerId=$prayerId -> playing")
        AdhanPlayerService.playNow(context.applicationContext, prayerId)
    }
}