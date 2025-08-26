package com.oqba26.prayertimes.utils


import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oqba26.prayertimes.models.MultiDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private const val TAG = "PrayerUtils"

object PrayerUtils {


    suspend fun getPrayerTimes(context: Context): Map<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                context.assets.open("prayer_times.json").use { stream ->
                    InputStreamReader(stream).use { reader ->
                        val type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                        com.google.gson.Gson().fromJson<Map<String, String>>(reader, type)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PrayerUtils", "Error loading prayer times", e)
                emptyMap()
            }
        }
    }

    fun getPersianDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1  // ماه‌ها از صفر شروع می‌شوند
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val persianMonthName = DateUtils.getPersianMonthName(month)

        return "$day $persianMonthName $year"
    }


    fun getHijriGregorianDate(): String {
        val today = Calendar.getInstance()

        // تاریخ میلادی
        val gregorianFormat = SimpleDateFormat("d MMMM yyyy", Locale("fa"))
        val gregorianDate = gregorianFormat.format(today.time)

        // تاریخ قمری - این بخش بسته به کتابخونه تغییر می‌کنه
        // 🔹 اگر UmmalquraCalendar یا مشابه داری:
        /*
        val hijriCalendar = UmmalquraCalendar()
        hijriCalendar.time = today.time
        val hijriDay = hijriCalendar.get(Calendar.DAY_OF_MONTH)
        val hijriMonth = hijriCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("fa"))
        val hijriYear = hijriCalendar.get(Calendar.YEAR)
        val hijriDate = "$hijriDay $hijriMonth $hijriYear"
        */

        // 🔹 موقت (اگه کتابخونه نداری):
        val hijriDate = "۳۰ صفر ۱۴۴۷" // بعداً جایگزین کن

        // ترکیب نهایی
        return "$hijriDate | $gregorianDate"
    }



    /**
     * تابع کمکی برای تبدیل زمان به فرمت استاندارد
     * @param timeStr زمان ورودی به صورت "HH:mm"
     * @return LocalTime یا null در صورت خطا
     */
    fun parseTimeSafely(timeStr: String): LocalTime? {
        val formatter = DateTimeFormatter.ofPattern("H:mm") // پشتیبانی از فرمت ساعت بدون صفر اول
        return try {
            // نرمال‌سازی برای فرمت تک رقمی ساعت، مثلا "7:30" به "07:30"
            val normalizedTime =
                if (timeStr.length == 4 && timeStr[1] == ':') "0$timeStr" else timeStr
            LocalTime.parse(
                normalizedTime.padStart(5, '0'),
                formatter
            ) // padStart برای اطمینان از فرمت HH:mm
        } catch (e: Exception) {
            Log.e("PrayerUtils", "خطا در پارس زمان: $timeStr", e)
            null
        }
    }

    /**
     * نام نماز فعلی را بر اساس منطق هایلایت (تا ۱۵ دقیقه بعد) برمی‌گرداند
     * @param prayerTimes اوقات نماز
     * @param now زمان فعلی
     * @return نام نماز برای هایلایت
     */
    fun getCurrentPrayerForHighlight(prayerTimes: Map<String, String>, now: LocalTime): String {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        val parsedTimes = order.mapNotNull { name ->
            prayerTimes[name]?.let { raw ->
                runCatching {
                    var timeStr = raw.padStart(5, '0')

                    // 🚀 اصلاح: اگر ساعت متعلق به عصر/غروب/عشاء بود ولی مقدارش < 08:00 parse شد → +12 ساعت
                    val parsed = LocalTime.parse(timeStr, formatter)
                    if ((name == "عصر" || name == "غروب" || name == "عشاء") && parsed.hour < 8) {
                        name to parsed.plusHours(12)
                    } else {
                        name to parsed
                    }
                }.getOrNull()
            }
        }.sortedBy { it.second }

        Log.d("HighlightDebug", "Parsed Times FIXED: " + parsedTimes.joinToString { "${it.first}=${it.second}" })
        Log.d("HighlightDebug", "Now = $now")

        if (parsedTimes.isEmpty()) return "طلوع بامداد"

        val lastPrayer = parsedTimes.lastOrNull { it.second <= now }
        val nextPrayer = parsedTimes.firstOrNull { it.second > now }

        Log.d("HighlightDebug", "LastPrayer = $lastPrayer, NextPrayer = $nextPrayer")

        val result = when {
            lastPrayer == null -> parsedTimes.first().first
            now.isBefore(lastPrayer.second.plusMinutes(15)) -> lastPrayer.first
            nextPrayer != null -> nextPrayer.first
            else -> parsedTimes.first().first
        }

        Log.d("HighlightDebug", "Result Highlight = $result")
        return result
    }


    /**
     * نام نماز فعلی را برای نمایش وضعیت (نه لزوماً برای هایلایت) برمی‌گرداند.
     * @param prayerTimes اوقات نماز
     * @return نام نماز فعلی یا "نامشخص"
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
            // اگر بعد از عشاء هستیم یا قبل از اولین نماز و عشاء موجود است
            isha != null && now >= isha -> "عشاء"
            // اگر قبل از همه نمازها هستیم و فجر موجود است
            fajr != null && now < fajr -> "طلوع بامداد" // قبل از فجر، هنوز زمان فجر است
            else -> "نامشخص"
        }
    }

    /**
     * بارگذاری اوقات نماز از فایل JSON در assets
     * @param context Context
     * @param date تاریخ مورد نظر از نوع MultiDate
     * @return Map اوقات نماز یا Map خالی در صورت خطا
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
                    if (parts.size < 3) {
                        android.util.Log.e("PrayerUtils", "فرمت تاریخ شمسی نامعتبر: ${date.shamsi}") // تگ اصلاح شد
                        return@withContext emptyMap<String, String>()
                    }

                    val monthNumber = parts[1].toIntOrNull()
                    if (monthNumber == null) {
                        android.util.Log.e("PrayerUtils", "شماره ماه نامعتبر: ${parts[1]}") // تگ اصلاح شد
                        return@withContext emptyMap<String, String>()
                    }

                    val monthName = DateUtils.getPersianMonthName(monthNumber)
                    val day = parts[2].padStart(2, '0')
                    val key = "$monthName/$day"

                    android.util.Log.d("PrayerUtils", "کلید جستجو: $key") // تگ اصلاح شد
                    data[key] ?: emptyMap<String, String>()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PrayerUtils", "خطا در بارگذاری اوقات نماز", e) // تگ اصلاح شد
            emptyMap()
        }
    }
    }
