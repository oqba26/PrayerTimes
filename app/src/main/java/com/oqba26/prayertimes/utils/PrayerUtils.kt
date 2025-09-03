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

                    Log.d("HighlightDebug", "📂 JSON اوقات لود شد (${data.size} کلید).")

                    val parts = date.shamsi.split("/")
                    if (parts.size < 3) return@withContext emptyMap<String, String>()

                    val monthNumber = parts[1].toIntOrNull() ?: return@withContext emptyMap<String, String>()
                    val monthName = DateUtils.getPersianMonthName(monthNumber)
                    val day = parts[2].padStart(2, '0')
                    val key = "$monthName/$day"

                    val result = data[key] ?: emptyMap()
                    Log.d("HighlightDebug", "🔑 کلید جستجو: $key → نتیجه ${result.size} رکورد.")

                    // 🛠 نرمال‌سازی مقادیر
                    val normalized = result.mapValues { normalizeTimeFormat(it.value) }

                    Log.d("HighlightDebug", "✅ بعد از نرمالایز = $normalized")
                    normalized
                }
            }
        } catch (e: Exception) {
            Log.e("HighlightDebug", "⛔ خطا در loadPrayerTimes", e)
            emptyMap()
        }
    }

    /**
     * هایلایت نماز فعلی با پنجره ۱۵ دقیقه‌ای
     * اگر از آخرین نماز (عشاء) ۱۵ دقیقه گذشت، به «طلوع بامداد» (آیتم اول) wrap می‌کند.
     */
    fun getCurrentPrayerForHighlight(prayerTimes: Map<String, String>, now: LocalTime): String {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        val parsedTimes = order.mapNotNull { name ->
            prayerTimes[name]?.let { raw ->
                runCatching {
                    val parsed = LocalTime.parse(raw, formatter)
                    val adjusted = if ((name == "عصر" || name == "غروب" || name == "عشاء") && parsed.hour < 8)
                        parsed.plusHours(12) else parsed
                    name to adjusted
                }.getOrNull()
            }
        }.sortedBy { it.second }

        if (parsedTimes.isEmpty()) return order.first()

        val lastPrayer = parsedTimes.lastOrNull { it.second <= now }
        val nextPrayer = parsedTimes.firstOrNull { it.second > now }

        return when {
            lastPrayer == null -> parsedTimes.first().first
            now.isBefore(lastPrayer.second.plusMinutes(15)) -> lastPrayer.first
            nextPrayer != null -> nextPrayer.first
            else -> parsedTimes.first().first // wrap به طلوع بامداد
        }
    }

    suspend fun getNextPrayerNameAndTime(
        context: Context,
        date: MultiDate,
        now: LocalTime,
        todayTimes: Map<String, String>
    ): Pair<String, String>? = withContext(Dispatchers.Default) {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        val items = order.mapNotNull { name ->
            val t = todayTimes[name] ?: return@mapNotNull null
            val base = runCatching { LocalTime.parse(t, formatter) }.getOrNull() ?: return@mapNotNull null
            val adjusted = when (name) {
                "ظهر", "عصر", "غروب", "عشاء" -> if (base.hour < 8) base.plusHours(12) else base
                else -> base
            }
            Triple(name, adjusted, t)
        }.sortedBy { it.second }

        val nextToday = items.firstOrNull { it.second.isAfter(now) }
        if (nextToday != null) return@withContext nextToday.first to normalizeTimeFormat(nextToday.third)

        // بعد از عشاء → طلوع بامداد فردا
        val tomorrowFajr = loadTomorrowFajr(context, date)
        return@withContext tomorrowFajr?.let { "طلوع بامداد" to it }
    }

    /**
     * ساعت «طلوع بامداد» فردا را از JSON برمی‌گرداند.
     * از همان کلیدهایی که loadPrayerTimes استفاده می‌کند بهره می‌برد.
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

                        // کاندید 1: فردا همین ماه
                        val tomorrowDay = (day.toInt() + 1).toString().padStart(2, '0')
                        val tomorrowKeySameMonth = "$thisMonth/$tomorrowDay"

                        val nextKey = if (data.containsKey(tomorrowKeySameMonth)) {
                            tomorrowKeySameMonth
                        } else {
                            // کاندید 2: ماه بعد، روز 01
                            val idx = persianMonths.indexOf(thisMonth).coerceAtLeast(0)
                            val nextMonth = persianMonths[(idx + 1) % 12]
                            val k = "$nextMonth/01"
                            if (data.containsKey(k)) k else null
                        } ?: return@withContext null

                        val fajr = data[nextKey]?.get("طلوع بامداد") ?: return@withContext null
                        return@withContext normalizeTimeFormat(fajr)
                    }
                }
            } catch (e: Exception) {
                Log.e("PrayerUtils", "⛔ خطا در loadTomorrowFajr", e)
                null
            }
        }

    /**
     * نرمال‌سازی فرمت ساعت
     * "6:2" → "06:02"
     */
    private fun normalizeTimeFormat(raw: String): String {
        return try {
            val parts = raw.split(":")
            if (parts.size == 2) {
                val h = parts[0].padStart(2, '0')
                val m = parts[1].padStart(2, '0')
                "$h:$m"
            } else raw
        } catch (e: Exception) {
            raw
        }
    }

    /**
     * تبدیل امن ساعت به LocalTime
     */
    fun parseTimeSafely(timeStr: String): LocalTime? {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                val h = parts[0].padStart(2, '0')
                val m = parts[1].padStart(2, '0')
                LocalTime.parse("$h:$m", formatter)
            } else null
        } catch (e: Exception) {
            Log.e("PrayerUtils", "⛔ خطا در parseTimeSafely: $timeStr", e)
            null
        }
    }

    /**
     * نام نماز فعلی برای نمایش وضعیت عمومی (در صورت نیاز)
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