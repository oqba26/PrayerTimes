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

    private val persianMonths = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private fun toAsciiDigits(s: String): String {
        val map = mapOf('۰' to '0','۱' to '1','۲' to '2','۳' to '3','۴' to '4',
            '۵' to '5','۶' to '6','۷' to '7','۸' to '8','۹' to '9')
        val sb = StringBuilder(s.length)
        for (ch in s) sb.append(map[ch] ?: ch)
        return sb.toString()
    }

    private fun sanitizeTime(raw: String): String {
        return try {
            val s = toAsciiDigits(raw.trim())
            val parts = s.split(":")
            if (parts.size == 2) {
                "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
            } else s
        } catch (_: Exception) {
            raw
        }
    }

    fun parseTimeSafely(timeStr: String): LocalTime? {
        return try {
            LocalTime.parse(sanitizeTime(timeStr), DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            Log.e("PrayerUtils", "⛔ خطا در parseTimeSafely: $timeStr", e)
            null
        }
    }

    private fun loadTimesFromFile(
        context: Context,
        fileName: String,
        date: MultiDate
    ): Map<String, String> {
        return try {
            context.assets.open(fileName).use { stream ->
                InputStreamReader(stream).use { reader ->
                    val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                    val data: Map<String, Map<String, String>> = Gson().fromJson(reader, type)
                    val parts = date.shamsi.split("/")
                    if (parts.size < 3) return emptyMap()
                    val monthNumber = parts[1].toIntOrNull() ?: return emptyMap()
                    val monthName = DateUtils.getPersianMonthName(monthNumber)
                    val day = parts[2].padStart(2, '0')
                    data["$monthName/$day"]?.mapValues { sanitizeTime(it.value) } ?: emptyMap()
                }
            }
        } catch (e: Exception) {
            Log.e("PrayerUtils", "⛔ خطا در خواندن فایل $fileName", e)
            emptyMap()
        }
    }

    suspend fun loadDetailedPrayerTimes(
        context: Context,
        date: MultiDate
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val general = loadTimesFromFile(context, "prayer_times.json", date)
        val specific = loadTimesFromFile(context, "prayer_times_24h.json", date)
        general.toMutableMap().apply { putAll(specific) }
    }

    suspend fun loadSeparatedPrayerTimes(
        context: Context,
        date: MultiDate
    ): Pair<Map<String, String>, Map<String, String>> = withContext(Dispatchers.IO) {
        loadTimesFromFile(context, "prayer_times.json", date) to
        loadTimesFromFile(context, "prayer_times_24h.json", date)
    }

    fun computeHighlightPrayer(now: LocalTime, times: Map<String, String>, order: List<String>): String? {
        val stickMinutes = 30L
        val parsedPrayerTimes = order.mapNotNull { name ->
            times[name]?.let { timeStr ->
                parseTimeSafely(timeStr)?.let { name to it }
            }
        }.sortedBy { it.second }

        if (parsedPrayerTimes.isEmpty()) return null

        // Find the last prayer that has passed
        val currentPrayerIndex = parsedPrayerTimes.indexOfLast { now.isAfter(it.second) || now == it.second }

        // Before the first prayer of the day
        if (currentPrayerIndex == -1) {
            return parsedPrayerTimes.first().first
        }

        val (currentPrayerName, currentPrayerTime) = parsedPrayerTimes[currentPrayerIndex]

        // Check if we are within the sticky window of the current prayer
        val stickyWindowEnd = currentPrayerTime.plusMinutes(stickMinutes)
        return if (now.isAfter(stickyWindowEnd)) {
            // After sticky window, highlight the *next* prayer
            val nextPrayerIndex = (currentPrayerIndex + 1) % parsedPrayerTimes.size
            parsedPrayerTimes[nextPrayerIndex].first
        } else {
            // Within prayer time or its sticky window, highlight current prayer
            currentPrayerName
        }
    }

    suspend fun getNextPrayerNameAndTime(
        context: Context,
        date: MultiDate,
        now: LocalTime,
        todayTimes: Map<String, String>
    ): Pair<String, String>? = withContext(Dispatchers.Default) {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
        val items = order.mapNotNull { name ->
            todayTimes[name]?.let { parseTimeSafely(it)?.let { parsed -> Triple(name, parsed, it) } }
        }.sortedBy { it.second }

        items.firstOrNull { it.second.isAfter(now) }?.let { return@withContext it.first to it.third }

        val tomorrowFajr = loadTomorrowFajr(context, date)
        return@withContext tomorrowFajr?.let { "طلوع بامداد" to it }
    }

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
                        if (!data.containsKey("$thisMonth/$day")) return@withContext null

                        val tomorrowDay = (day.toInt() + 1).toString().padStart(2, '0')
                        val tomorrowKeySameMonth = "$thisMonth/$tomorrowDay"

                        val nextKey = if (data.containsKey(tomorrowKeySameMonth)) {
                            tomorrowKeySameMonth
                        } else {
                            val idx = persianMonths.indexOf(thisMonth).coerceAtLeast(0)
                            val nextMonth = persianMonths[(idx + 1) % 12]
                            "$nextMonth/01".takeIf { data.containsKey(it) }
                        } ?: return@withContext null

                        data[nextKey]?.get("طلوع بامداد")?.let { sanitizeTime(it) }
                    }
                }
            } catch (e: Exception) {
                Log.e("PrayerUtils", "⛔ خطا در loadTomorrowFajr", e)
                null
            }
        }
}
