
package com.oqba26.prayertimes.utils
import com.oqba26.prayertimes.models.MultiDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * تابع کمکی برای تبدیل زمان به فرمت استاندارد
 * @param timeStr زمان ورودی به صورت "HH:mm"
 * @return LocalTime یا null در صورت خطا
 */
fun parseTimeSafely(timeStr: String): LocalTime? {
    val formatter = DateTimeFormatter.ofPattern("H:mm")
    return try {
        // Handle single-digit hour format
        val normalizedTime = if (timeStr.length == 4 && timeStr[1] == ':') "0$timeStr" else timeStr
        LocalTime.parse(normalizedTime, formatter)
    } catch (e: Exception) {
        android.util.Log.e("PrayerUtils", "خطا در پارس زمان: $timeStr", e)
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
    val prayerOrder = listOf(
        "طلوع بامداد",
        "طلوع خورشید",
        "ظهر",
        "عصر",
        "غروب",
        "عشاء"
    )

    val parsedTimes = prayerOrder.mapNotNull { name ->
        prayerTimes[name]?.let { timeStr ->
            parseTimeSafely(timeStr)?.let { Pair(name, it) }
        }
    }

    if (parsedTimes.isEmpty()) {
        return "طلوع بامداد" // Default
    }

    // پیدا کردن نماز قبلی و بعدی نسبت به زمان حال
    val nextPrayer = parsedTimes.firstOrNull { it.second > now }
    val lastPrayer = parsedTimes.lastOrNull { it.second <= now }

    // اگر قبل از اولین نماز روز هستیم، نماز هایلایت شده عشاء روز قبل است
    if (lastPrayer == null) {
        return "عشاء"
    }

    // بررسی قانون ۱۵ دقیقه
    // اگر از وقت نماز قبلی کمتر از ۱۵ دقیقه گذشته، همان را هایلایت کن
    val fifteenMinutesAfterLastPrayer = lastPrayer.second.plusMinutes(15)
    if (now.isBefore(fifteenMinutesAfterLastPrayer)) {
        return lastPrayer.first
    }

    // در غیر این صورت، نماز بعدی را هایلایت کن
    return nextPrayer?.first ?: "عشاء" // اگر نماز بعدی وجود نداشت (بعد از عشاء)، همان عشاء را نشان بده
}