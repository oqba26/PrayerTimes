package com.oqba26.prayertimes.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oqba26.prayertimes.models.MultiDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object PrayerUtils {

    // لیست ماه‌های شمسی مطابق کلیدهای JSON
    private val persianMonths = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    // تبدیل ارقام فارسی به لاتین
    private fun toAsciiDigits(s: String): String {
        val map = mapOf('۰' to '0','۱' to '1','۲' to '2','۳' to '3','۴' to '4',
            '۵' to '5','۶' to '6','۷' to '7','۸' to '8','۹' to '9')
        val sb = StringBuilder(s.length)
        for (ch in s) sb.append(map[ch] ?: ch)
        return sb.toString()
    }

    // نرمال‌سازی ساعت به HH:mm (همراه با تبدیل ارقام)
    private fun sanitizeTime(raw: String): String {
        return try {
            val s = toAsciiDigits(raw.trim())
            val parts = s.split(":")
            if (parts.size == 2) {
                val h = parts[0].padStart(2, '0')
                val m = parts[1].padStart(2, '0')
                "$h:$m"
            } else s
        } catch (_: Exception) {
            raw
        }
    }

    // تبدیل امن ساعت (با تبدیل ارقام)
    fun parseTimeSafely(timeStr: String): LocalTime? {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return try {
            val s = sanitizeTime(timeStr)
            val parts = s.split(":")
            if (parts.size == 2) {
                LocalTime.parse("${parts[0]}:${parts[1]}", formatter)
            } else null
        } catch (e: Exception) {
            Log.e("PrayerUtils", "⛔ خطا در parseTimeSafely: $timeStr", e)
            null
        }
    }

    // پارس ساعت با درنظر گرفتن 24 ساعته‌کردن عصر/غروب/عشاء
    private fun parseTimeFor(name: String, raw: String): LocalTime? {
        val s = sanitizeTime(raw)
        val parts = s.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        var hh = h
        // عصر/غروب/عشاء: اگر 12ساعته آمده باشند، به 24ساعته تبدیل کنیم
        if (name == "عصر" || name == "غروب" || name == "عشاء") {
            if (hh in 0..11) hh += 12
        }
        return try { LocalTime.of(hh % 24, m) } catch (_: Exception) { null }
    }

    /**
     * بارگذاری اوقات نماز روز از فایل JSON داخل assets
     */
    suspend fun loadPrayerTimes(
        context: Context,
        date: MultiDate
    ): Map<String, String> = withContext(Dispatchers.IO) {
        return@withContext try {
            context.assets.open("prayer_times.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                    val data: Map<String, Map<String, String>> = Gson().fromJson(reader, type)

                    val parts = date.shamsi.split("/")
                    if (parts.size < 3) return@withContext emptyMap()

                    val monthNumber = parts[1].toIntOrNull() ?: return@withContext emptyMap<String, String>()
                    val monthName = DateUtils.getPersianMonthName(monthNumber)
                    val day = parts[2].padStart(2, '0')
                    val key = "$monthName/$day"

                    val result = data[key] ?: emptyMap()

                    // نرمال‌سازی کامل (ارقام + HH:mm)
                    result.mapValues { sanitizeTime(it.value) }
                }
            }
        } catch (e: Exception) {
            Log.e("PrayerUtils", "⛔ خطا در loadPrayerTimes", e)
            emptyMap()
        }
    }

    /**
     * هایلایت "نماز بعدی" با چسبندگی 30 دقیقه‌ای برای هر 6 وقت
     */
    fun getCurrentPrayerForHighlight(times: Map<String, String>, now: LocalTime): String? {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
        val stickMinutes = 30L

        val parsed = order.mapNotNull { name ->
            val s = times[name] ?: return@mapNotNull null
            parseTimeFor(name, s)?.let { t -> name to t }
        }.sortedBy { it.second }

        if (parsed.isEmpty()) return null

        for ((name, t) in parsed) {
            if (!now.isAfter(t.plusMinutes(stickMinutes))) return name
        }
        return parsed.first().first
    }

    /**
     * نام و زمان "وقت بعدی" برای نمایش
     */
    suspend fun getNextPrayerNameAndTime(
        context: Context,
        date: MultiDate,
        now: LocalTime,
        todayTimes: Map<String, String>
    ): Pair<String, String>? = withContext(Dispatchers.Default) {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")

        val items = order.mapNotNull { name ->
            val t = todayTimes[name] ?: return@mapNotNull null
            parseTimeFor(name, t)?.let { parsed -> Triple(name, parsed, sanitizeTime(t)) }
        }.sortedBy { it.second }

        // "وقت بعدی" = اولین وقتی که بعد از now است (بدون چسبندگی)
        val nextToday = items.firstOrNull { it.second.isAfter(now) }
        if (nextToday != null) return@withContext nextToday.first to nextToday.third

        // بعد از عشاء → طلوع بامداد فردا
        val tomorrowFajr = loadTomorrowFajr(context, date)
        return@withContext tomorrowFajr?.let { "طلوع بامداد" to it }
    }

    /**
     * ساعت "طلوع بامداد" فردا از JSON
     */
    private suspend fun loadTomorrowFajr(context: Context, date: MultiDate): String? =
        withContext(Dispatchers.IO) {
            try {
                context.assets.open("prayer_times.json").use { stream ->
                    InputStreamReader(stream).use { reader ->
                        val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                        val data: Map<String, Map<String, String>> = Gson().fromJson(reader, type)

                        val parts = date.shamsi.split("/")
                        if (parts.size < 3) return@withContext null

                        val monthNumber = parts[1].toIntOrNull() ?: return@withContext null
                        val thisMonth = DateUtils.getPersianMonthName(monthNumber)
                        val day = parts[2].padStart(2, '0')

                        val todayKey = "$thisMonth/$day"
                        if (!data.containsKey(todayKey)) return@withContext null

                        // فردا همین ماه
                        val tomorrowDay = (day.toInt() + 1).toString().padStart(2, '0')
                        val tomorrowKeySameMonth = "$thisMonth/$tomorrowDay"

                        val nextKey = if (data.containsKey(tomorrowKeySameMonth)) {
                            tomorrowKeySameMonth
                        } else {
                            // ماه بعد، روز 01
                            val idx = persianMonths.indexOf(thisMonth).coerceAtLeast(0)
                            val nextMonth = persianMonths[(idx + 1) % 12]
                            val k = "$nextMonth/01"
                            if (data.containsKey(k)) k else null
                        } ?: return@withContext null

                        val fajr = data[nextKey]?.get("طلوع بامداد") ?: return@withContext null
                        sanitizeTime(fajr)
                    }
                }
            } catch (e: Exception) {
                Log.e("PrayerUtils", "⛔ خطا در loadTomorrowFajr", e)
                null
            }
        }

    /**
     * نام نماز فعلی (در صورت نیاز جای دیگر)
     */
    fun getCurrentPrayerNameFixed(prayerTimes: Map<String, String>): String {
        val now = LocalTime.now()

        val fajr = prayerTimes["طلوع بامداد"]?.let { parseTimeSafely(it) }
        val sunrise = prayerTimes["طلوع خورشید"]?.let { parseTimeSafely(it) }
        val dhuhr = prayerTimes["ظهر"]?.let { parseTimeSafely(it) }
        val asr = prayerTimes["عصر"]?.let { parseTimeSafely(it) }
        val maghrib = prayerTimes["غروب"]?.let { parseTimeSafely(it) }
        val isha = prayerTimes["عشاء"]?.let { parseTimeSafely(it) }

        return when {
            fajr != null && sunrise != null && now >= fajr && now < sunrise -> "طلوع بامداد"
            sunrise != null && dhuhr != null && now >= sunrise && now < dhuhr -> "طلوع خورشید"
            dhuhr != null && asr != null && now >= dhuhr && now < asr -> "ظهر"
            asr != null && maghrib != null && now >= asr && now < maghrib -> "عصر"
            maghrib != null && isha != null && now >= maghrib && now < isha -> "غروب"
            isha != null && now >= isha -> "عشاء"
            fajr != null && now < fajr -> "طلوع بامداد"
            else -> "نامشخص"
        }
    }
}