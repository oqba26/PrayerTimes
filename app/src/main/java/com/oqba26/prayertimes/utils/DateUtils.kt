package com.oqba26.prayertimes.utils
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.models.getShamsiDayOfWeekIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.math.abs


// Cache برای بهبود performance
private val dateCache = mutableMapOf<String, MultiDate>()

fun getCurrentDate(): MultiDate {
    val calendar = Calendar.getInstance()
    val cacheKey = "${calendar.get(Calendar.YEAR)}-" +
            "${calendar.get(Calendar.MONTH)}-" +
            "${calendar.get(Calendar.DAY_OF_MONTH)}"
    return dateCache.getOrPut(cacheKey) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val gregorian = String.format("%d/%02d/%02d", year, month, day)
        val shamsiDate = gregorianToJalali(year, month, day)
        val hijri = convertToHijri(year, month, day)
        MultiDate(shamsiDate, hijri, gregorian)
    }
}

fun convertCalendarToMultiDate(calendar: Calendar): MultiDate {
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val gregorian = String.format("%d/%02d/%02d", year, month, day)
    val shamsi = gregorianToJalali(year, month, day)
    val hijri = convertToHijri(year, month, day)
    return MultiDate(shamsi, hijri, gregorian)
}

fun createMultiDate(year: Int, month: Int, day: Int): MultiDate {
    val cacheKey = "$year-$month-$day"
    return dateCache.getOrPut(cacheKey) {
        val gregorian = String.format("%d/%02d/%02d", year, month, day)
        val shamsi = gregorianToJalali(year, month, day)
        val hijri = convertToHijri(year, month, day)
        MultiDate(shamsi, hijri, gregorian)
    }
}

fun createMultiDateFromShamsi(jy: Int, jm: Int, jd: Int): MultiDate {
    val (gy, gm, gd) = convertPersianToGregorian(jy, jm, jd)
    val shamsi = String.format("%d/%02d/%02d", jy, jm, jd)
    val gregorian = String.format("%d/%02d/%02d", gy, gm, gd)
    val hijri = convertToHijri(gy, gm, gd)
    return MultiDate(shamsi, hijri, gregorian)
}

fun getCurrentPersianDate(): String = getCurrentDate().shamsi

fun convertPersianToGregorian(
    persianYear: Int,
    persianMonth: Int,
    persianDay: Int
): Triple<Int, Int, Int> {
    val date = jalaliToGregorian(persianYear, persianMonth, persianDay)
    return Triple(date[0], date[1], date[2])
}




fun getCurrentPrayerNameFixed(prayerTimes: Map<String, String>): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val now = LocalTime.now()

    // تبدیل زمان‌ها (با محافظت)
    fun parseTime(name: String): LocalTime? {
        return try {
            val timeStr = prayerTimes[name]?.trim()?.padStart(5, '0')
            LocalTime.parse(timeStr, formatter)
        } catch (e: Exception) {
            null
        }
    }

    val fajr = parseTime("طلوع بامداد")
    val sunrise = parseTime("طلوع خورشید")
    val dhuhr = parseTime("ظهر")
    val asr = parseTime("عصر")
    val maghrib = parseTime("غروب")
    val isha = parseTime("عشاء")

    // نماز فعلی دقیق بین بازه‌ها
    return when {
        fajr != null && sunrise != null && now >= fajr && now < sunrise -> "طلوع بامداد"
        sunrise != null && dhuhr != null && now >= sunrise && now < dhuhr -> "طلوع خورشید"
        dhuhr != null && asr != null && now >= dhuhr && now < asr -> "ظهر"
        asr != null && maghrib != null && now >= asr && now < maghrib -> "عصر"
        maghrib != null && isha != null && now >= maghrib && now < isha -> "غروب"
        isha != null && now >= isha -> "عشاء"
        // قبل از همه نمازها
        fajr != null && now < fajr -> "طلوع بامداد"
        else -> "نامشخص"
    }
}


/**
 * تابع کمکی برای debug کردن - ساعت جاری را نمایش می‌دهد
 */
fun getCurrentTimeForDebug(): String {
    val currentTime = LocalTime.now()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return currentTime.format(formatter)
}

// Load JSON prayer times from assets - نسخه اصلاح شده
suspend fun loadPrayerTimes(
    context: Context,
    date: MultiDate
): Map<String, String> = withContext(Dispatchers.IO) {
    return@withContext try {
        context.assets.open("prayer_times.json").use { stream ->
            InputStreamReader(stream).use { reader ->
                val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                val data: Map<String, Map<String, String>> = Gson().fromJson(reader, type)

                // ساخت کلید جستجو با فرمت صحیح
                val parts = date.shamsi.split("/")
                val monthName = getPersianMonthName(parts[1].toInt())
                val day = parts[2].padStart(2, '0') // 🔧 اصلاح کلیدی اینجاست
                val key = "$monthName/$day"

                android.util.Log.d("LoadPrayerTimes", "کلید نهایی جستجو: $key")

                val result = data[key] ?: emptyMap<String, String>()

                // لاگ‌های بررسی و اشکال‌زدایی
                if (result.isNotEmpty()) {
                    android.util.Log.d("loadPrayerTimes", "اوقات نماز با موفقیت بارگذاری شد:")
                    result.forEach { (prayer, time) ->
                        android.util.Log.d("loadPrayerTimes", "  $prayer: $time")
                    }
                    if (!result.containsKey("عشاء")) {
                        android.util.Log.w("loadPrayerTimes", "⚠️ نماز عشاء موجود نیست")
                    }
                } else {
                    android.util.Log.e("loadPrayerTimes", "❌ هیچ داده‌ای برای کلید $key یافت نشد")
                }

                result
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("loadPrayerTimes", "خطا در بارگذاری اوقات نماز", e)
        emptyMap()
    }
}

// تبدیل میلادی به شمسی
private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): String {
    val jalaliDate = calculateJalaliDate(gy, gm, gd)
    return String.format("%d/%02d/%02d", jalaliDate[0], jalaliDate[1], jalaliDate[2])
}

private fun calculateJalaliDate(gy: Int, gm: Int, gd: Int): IntArray {
    var jy: Int
    var jm: Int
    var jd: Int
    var gyTemp = gy
    val gdm = intArrayOf(0,31,59,90,120,151,181,212,243,273,304,334)
    jy = if (gyTemp > 1600) { gyTemp -= 1600; 979 } else { gyTemp -= 621; 0 }
    var days = 365 * gyTemp + (gyTemp+3)/4 - (gyTemp+99)/100 + (gyTemp+399)/400
    days += gdm[gm-1] + gd - 1
    if (gm > 2 && ((gyTemp%4==0 && gyTemp%100!=0) || (gyTemp%400==0))) days++
    days -= 79
    jy += 33 * (days/12053); days %= 12053
    jy += 4 * (days/1461); days %= 1461
    if (days > 365) { jy += (days-1)/365; days = (days-1)%365 }
    if (days < 186) {
        jm = 1 + days/31; jd = 1 + days%31
    } else {
        jm = 7 + (days-186)/30; jd = 1 + (days-186)%30
    }
    return intArrayOf(jy, jm, jd)
}

// تبدیل شمسی به میلادی
private fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): IntArray {
    var gy: Int
    var jyTemp = jy
    if (jyTemp > 979) { gy = 1600; jyTemp -= 979 }
    else { gy = 621 }
    var days = 365*jyTemp + (jyTemp/33)*8 + ((jyTemp%33+3)/4) + 78 + jd
    days += if (jm <= 6) (jm-1)*31 else ((jm-7)*30 + 186)
    gy += 400 * (days/146097); days %= 146097
    if (days > 36524) {
        gy += 100*((days-1)/36524); days = (days-1)%36524
        if (days >= 365) days++
    }
    gy += 4*(days/1461); days %= 1461
    if (days > 365) { gy += (days-1)/365; days = (days-1)%365 }
    val monthDays = intArrayOf(0,31,28 + if ((gy%4==0 && gy%100!=0)|| (gy%400==0)) 1 else 0,
        31,30,31,30,31,31,30,31,30,31)
    var gm = 0; var gdTemp = days+1
    for (i in 1..12) {
        if (gdTemp <= monthDays[i]) { gm = i; break }
        gdTemp -= monthDays[i]
    }
    return intArrayOf(gy, gm, gdTemp)
}

// تبدیل میلادی به هجری قمری
private fun convertToHijri(gy: Int, gm: Int, gd: Int): String {
    val jd = gregorianToJulianDay(gy, gm, gd)
    val epoch = 1948439
    var daysSinceEpoch = jd - epoch
    var hy = 1
    while (true) {
        val isLeap = isHijriLeapYear(hy)
        val yearDays = if (isLeap) 355 else 354
        if (daysSinceEpoch >= yearDays) {
            daysSinceEpoch -= yearDays; hy++
        } else break
    }
    val monthDaysArr = intArrayOf(30,29,30,29,30,29,30,29,30,29,30,
        if (isHijriLeapYear(hy)) 30 else 29)
    var hm = 1
    var hd = daysSinceEpoch + 1
    for (i in monthDaysArr.indices) {
        if (hd <= monthDaysArr[i]) { hm = i+1; break }
        hd -= monthDaysArr[i]
    }
    return String.format("%d/%02d/%02d", hy, hm, hd)
}

private fun isHijriLeapYear(year: Int): Boolean {
    val cycle = year % 30
    return cycle in listOf(2,5,7,10,13,16,18,21,24,26,29)
}

// محاسبه Julian Day
private fun gregorianToJulianDay(yr: Int, mo: Int, da: Int): Int {
    val a = (14 - mo) / 12
    val y = yr + 4800 - a
    val m = mo + 12 * a - 3
    return da + (153*m + 2)/5 + 365*y + y/4 - y/100 + y/400 - 32045
}

// ==== توابع جدید برای CalendarScreen ====
/** تبدیل اعداد انگلیسی در ورودی به فارسی */
fun convertToPersianNumbers(input: String): String {
    val persian = arrayOf("۰","۱","۲","۳","۴","۵","۶","۷","۸","۹")
    return input.map { ch ->
        if (ch in '0'..'9') persian[ch - '0'] else ch
    }.joinToString("")
}

/** نام ماه شمسی بر اساس عدد 1..12 */
fun getPersianMonthName(month: Int): String = when (month) {
    1  -> "فروردین";   2 -> "اردیبهشت"; 3 -> "خرداد";    4 -> "تیر"
    5  -> "مرداد";     6 -> "شهریور";   7 -> "مهر";      8 -> "آبان"
    9  -> "آذر";       10 -> "دی";      11 -> "بهمن";     12 -> "اسفند"
    else -> ""
}

/**
 * با توجه به ماه (شمسی به صورت YearMonth)، شاخص اولین روز هفته را حساب می‌کند.
 * شنبه = 0، یکشنبه = 1، … جمعه = 6
 */
fun getFirstDayOfWeekIndex(month: YearMonth): Int {
    // تبدیل اولین روز ماه شمسی به معادل میلادی
    val (gy, gm, gd) = convertPersianToGregorian(month.year, month.monthValue, 1)
    val cal = Calendar.getInstance().apply {
        set(gy, gm - 1, gd)
    }
    // در Calendar: یکشنبه=1 … شنبه=7  → با این فرمول شنبه=0 … جمعه=6
    return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
}

fun getShamsiDateDistance(selected: MultiDate, today: MultiDate): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")
    val d1 = java.time.LocalDate.parse(selected.gregorian, formatter)
    val d2 = java.time.LocalDate.parse(today.gregorian, formatter)
    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(d2, d1)

    val absDays = kotlin.math.abs(daysBetween)
    val years = absDays / 365
    val months = (absDays % 365) / 30
    val days = (absDays % 365) % 30

    val parts = mutableListOf<String>()
    if (years > 0) parts.add("${convertToPersianNumbers(years.toString())} سال")
    if (months > 0) parts.add("${convertToPersianNumbers(months.toString())} ماه")
    if (days > 0) parts.add("${convertToPersianNumbers(days.toString())} روز")

    val joined = parts.joinToString(" و ")

    return if (daysBetween > 0) "$joined بعد" else "$joined قبل"
}


fun daysInShamsiMonth(jy: Int, jm: Int): Int {
    val (g1y, g1m, g1d) = convertPersianToGregorian(jy, jm, 1)
    val (g2y, g2m, g2d) = if (jm < 12)
        convertPersianToGregorian(jy, jm + 1, 1)
    else
        convertPersianToGregorian(jy + 1, 1, 1)
    val c1 = Calendar.getInstance().apply {
        set(g1y, g1m - 1, g1d, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val c2 = Calendar.getInstance().apply {
        set(g2y, g2m - 1, g2d, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return ((c2.timeInMillis - c1.timeInMillis) / 86_400_000L).toInt()
}

/**
 * نام روز هفتهٔ فارسی را برای یک MultiDate برمی‌گرداند.
 */


/**
 * نام ماه میلادی را بر اساس عدد 1 تا 12 به فارسی برمی‌گرداند.
 */
fun getGregorianMonthName(month: Int): String = when (month) {
    1  -> "ژانویه"
    2  -> "فوریه"
    3  -> "مارس"
    4  -> "آوریل"
    5  -> "مه"
    6  -> "ژوئن"
    7  -> "ژوئیه"
    8  -> "اوت"
    9  -> "سپتامبر"
    10 -> "اکتبر"
    11 -> "نوامبر"
    12 -> "دسامبر"
    else -> ""
}

fun getWeekDayName(date: MultiDate): String {
    return when (date.getShamsiDayOfWeekIndex()) {
        0 -> "شنبه"
        1 -> "یک‌شنبه"
        2 -> "دوشنبه"
        3 -> "سه‌شنبه"
        4 -> "چهارشنبه"
        5 -> "پنج‌شنبه"
        6 -> "جمعه"
        else -> ""
    }
}
