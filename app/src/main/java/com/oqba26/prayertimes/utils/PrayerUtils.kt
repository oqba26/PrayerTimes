
package com.oqba26.prayertimes.utils
import com.oqba26.prayertimes.models.MultiDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * بر اساس Map<نام‌نماز, زمان> و زمان فعلی
 * نماز فعلی (که باید خوانده شود) را برمی‌گرداند.
 * این تابع قدیمی است و استفاده از getCurrentPrayerNameFixed توصیه می‌شود.
 * @deprecated استفاده از getCurrentPrayerNameFixed را جایگزین کنید
 */
@Deprecated(
    message = "این تابع قدیمی است، از getCurrentPrayerNameFixed استفاده کنید",
    replaceWith = ReplaceWith("getCurrentPrayerNameFixed(prayerTimes)")
)
fun getCurrentPrayerName(prayerTimes: Map<String, String>): String {
    val currentTime = Calendar.getInstance()
    val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentTime.get(Calendar.MINUTE)
    val currentTimeInMinutes = currentHour * 60 + currentMinute

    // ترتیب نمازها در طول شبانه‌روز
    val prayerOrder = listOf(
        "طلوع بامداد",
        "طلوع خورشید",
        "ظهر",
        "عصر",
        "غروب",
        "عشاء"
    )

    // تبدیل زمان نمازها به دقیقه از شروع روز
    val prayerTimesInMinutes = mutableMapOf<String, Int>()
    prayerOrder.forEach { prayerName ->
        val timeString = prayerTimes[prayerName]
        if (!timeString.isNullOrEmpty()) {
            try {
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()
                    prayerTimesInMinutes[prayerName] = hour * 60 + minute
                }
            } catch (e: Exception) {
                // در صورت خطا در پارس کردن زمان، آن نماز را نادیده می‌گیریم
                android.util.Log.w("PrayerUtils", "خطا در پارس زمان نماز $prayerName: $timeString", e)
            }
        }
    }

    // پیدا کردن اولین نمازی که هنوز وقتش نرسیده (نماز بعدی)
    for (prayerName in prayerOrder) {
        val prayerTime = prayerTimesInMinutes[prayerName] ?: continue
        // اگر زمان این نماز هنوز نرسیده، این نماز بعدی است
        if (currentTimeInMinutes < prayerTime) {
            return prayerName
        }
    }

    // اگر همه نمازها گذشته، نماز بعدی "طلوع بامداد" فردا است
    return "طلوع بامداد"
}

/**
 * MultiDate جدید برای روز قبل از currentDate می‌سازد.
 * @deprecated استفاده از توابع DateUtils را جایگزین کنید
 */
@Deprecated(
    message = "این تابع قدیمی است، از توابع DateUtils استفاده کنید",
    replaceWith = ReplaceWith("getPreviousDate(currentDate)")
)
fun getPreviousDate(currentDate: MultiDate): MultiDate {
    val parts = currentDate.gregorian.split("/")
    val cal = Calendar.getInstance().apply {
        set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        add(Calendar.DAY_OF_MONTH, -1)
    }
    return createMultiDate(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

/**
 * MultiDate جدید برای روز بعد از currentDate می‌سازد.
 * @deprecated استفاده از توابع DateUtils را جایگزین کنید
 */
@Deprecated(
    message = "این تابع قدیمی است، از توابع DateUtils استفاده کنید",
    replaceWith = ReplaceWith("getNextDate(currentDate)")
)
fun getNextDate(currentDate: MultiDate): MultiDate {
    val parts = currentDate.gregorian.split("/")
    val cal = Calendar.getInstance().apply {
        set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        add(Calendar.DAY_OF_MONTH, 1)
    }
    return createMultiDate(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

/**
 * نام روز هفتهٔ فارسی را برای یک MultiDate برمی‌گرداند.
 * @deprecated استفاده از getWeekDayName در DateUtils را جایگزین کنید
 */

fun getDayOfWeekPersian(date: MultiDate): String {
    val parts = date.gregorian.split("/")
    val cal = Calendar.getInstance().apply {
        set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
    }
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY   -> "شنبه"
        Calendar.SUNDAY     -> "یکشنبه"
        Calendar.MONDAY     -> "دوشنبه"
        Calendar.TUESDAY    -> "سه‌شنبه"
        Calendar.WEDNESDAY  -> "چهارشنبه"
        Calendar.THURSDAY   -> "پنج‌شنبه"
        Calendar.FRIDAY     -> "جمعه"
        else                -> ""
    }
}

/**
 * تابع کمکی برای تبدیل زمان به فرمت استاندارد
 * @param timeStr زمان ورودی به صورت "HH:mm"
 * @return LocalTime یا null در صورت خطا
 */
fun parseTimeSafely(timeStr: String): LocalTime? {
    return try {
        LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        android.util.Log.e("PrayerUtils", "خطا در پارس زمان: $timeStr", e)
        null
    }
}

/**
 * بررسی می‌کند که آیا زمان فعلی در بازه زمانی مشخصی قرار دارد یا نه
 * @param startTime زمان شروع
 * @param endTime زمان پایان
 * @param currentTime زمان فعلی (اختیاری، در صورت عدم ارسال زمان فعلی سیستم استفاده می‌شود)
 * @return true اگر زمان فعلی در بازه باشد
 */
fun isTimeInRange(startTime: String, endTime: String, currentTime: LocalTime = LocalTime.now()): Boolean {
    val start = parseTimeSafely(startTime) ?: return false
    val end = parseTimeSafely(endTime) ?: return false

    return if (start <= end) {
        currentTime in start..end
    } else {
        // برای بازه‌هایی که از نیمه‌شب عبور می‌کنند (مثل 23:00 تا 02:00)
        currentTime >= start || currentTime <= end
    }
}

/**
 * محاسبه زمان باقی مانده تا نماز بعدی
 * @param prayerTimes اوقات نماز
 * @return زمان باقی مانده به میلی‌ثانیه
 */
fun getTimeUntilNextPrayer(prayerTimes: Map<String, String>): Long {
    val currentTime = LocalTime.now()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    val prayerOrder = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")

    for (prayerName in prayerOrder) {
        val timeStr = prayerTimes[prayerName] ?: continue
        val prayerTime = parseTimeSafely(timeStr) ?: continue

        if (currentTime < prayerTime) {
            val duration = java.time.Duration.between(currentTime, prayerTime)
            return duration.toMillis()
        }
    }

    // اگر همه نمازهای امروز گذشته، زمان تا فجر فردا
    val fajrTime = parseTimeSafely(prayerTimes["طلوع بامداد"] ?: "04:00") ?: return 24 * 60 * 60 * 1000L
    val tomorrowFajr = fajrTime.plusHours(24)
    val duration = java.time.Duration.between(currentTime, tomorrowFajr)
    return duration.toMillis()
}

/**
 * فرمت‌بندی زمان به صورت خوانا
 * @paramMillis زمان به میلی‌ثانیه
 * @return متن فرمت‌بندی شده (مثلاً "2 ساعت و 30 دقیقه")
 */
fun formatDuration(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)

    return when {
        hours > 0 && minutes > 0 -> "$hours ساعت و $minutes دقیقه"
        hours > 0 -> "$hours ساعت"
        minutes > 0 -> "$minutes دقیقه"
        else -> "کمتر از یک دقیقه"
    }
}

/**
 * دریافت نام نماز فعلی با توضیحات بیشتر
 * @param prayerTimes اوقات نماز
 * @return جفت شامل نام نماز و توضیحات وضعیت
 */
fun getCurrentPrayerWithStatus(prayerTimes: Map<String, String>): Pair<String, String> {
    val currentPrayer = getCurrentPrayerNameFixed(prayerTimes)
    val currentTime = LocalTime.now()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    return when (currentPrayer) {
        "طلوع بامداد" -> {
            val sunrise = parseTimeSafely(prayerTimes["طلوع خورشید"] ?: "06:00")
            if (sunrise != null && currentTime < sunrise) {
                Pair("طلوع بامداد", "وقت نماز")
            } else {
                Pair("طلوع بامداد", "گذشته")
            }
        }
        "طلوع خورشید" -> {
            val dhuhr = parseTimeSafely(prayerTimes["ظهر"] ?: "12:00")
            if (dhuhr != null && currentTime < dhuhr) {
                Pair("طلوع خورشید", "وقت نهی")
            } else {
                Pair("طلوع خورشید", "گذشته")
            }
        }
        "ظهر" -> {
            val asr = parseTimeSafely(prayerTimes["عصر"] ?: "15:00")
            if (asr != null && currentTime < asr) {
                Pair("ظهر", "وقت نماز")
            } else {
                Pair("ظهر", "گذشته")
            }
        }
        "عصر" -> {
            val maghrib = parseTimeSafely(prayerTimes["غروب"] ?: "18:00")
            if (maghrib != null && currentTime < maghrib) {
                Pair("عصر", "وقت نماز")
            } else {
                Pair("عصر", "گذشته")
            }
        }
        "غروب" -> {
            val isha = parseTimeSafely(prayerTimes["عشاء"] ?: "20:00")
            if (isha != null && currentTime < isha) {
                Pair("غروب", "وقت نماز")
            } else {
                Pair("غروب", "گذشته")
            }
        }
        "عشاء" -> {
            val fajr = parseTimeSafely(prayerTimes["طلوع بامداد"] ?: "04:00")
            if (fajr != null && currentTime >= fajr) {
                Pair("عشاء", "گذشته")
            } else {
                Pair("عشاء", "وقت نماز")
            }
        }
        else -> Pair("نامشخص", "وضعیت نامشخص")
    }
}