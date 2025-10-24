package com.oqba26.prayertimes.utils

import com.oqba26.prayertimes.models.MultiDate
import java.util.Calendar
import java.util.Locale

object DateUtils {

    private val dateCache = mutableMapOf<String, MultiDate>()

    private val persianNumbers = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
    private val englishNumbers = arrayOf('0','1','2','3','4','5','6','7','8','9')

    @Volatile
    private var defaultUsePersianNumbers: Boolean = false

    fun setDefaultUsePersianNumbers(enabled: Boolean) {
        defaultUsePersianNumbers = enabled
    }

    fun getHijriMonthName(month: Int): String = when (month) {
        1 -> "محرم"; 2 -> "صفر"; 3 -> "ربیع‌الاول"; 4 -> "ربیع‌الثانی"
        5 -> "جمادی‌الاول"; 6 -> "جمادی‌الثانی"; 7 -> "رجب"
        8 -> "شعبان"; 9 -> "رمضان"; 10 -> "شوال"
        11 -> "ذی‌القعده"; 12 -> "ذی‌الحجه"
        else -> ""
    }

    fun getCurrentDate(): MultiDate {
        val calendar = Calendar.getInstance()
        val cacheKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
        return dateCache.getOrPut(cacheKey) {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val gregorian = String.format(Locale.US, "%d/%02d/%02d", year, month, day)
            val shamsiDate = gregorianToJalali(year, month, day)
            val hijri = convertToHijri(year, month, day)
            MultiDate(shamsiDate, hijri, gregorian)
        }
    }

    fun convertCalendarToMultiDate(calendar: Calendar): MultiDate {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val gregorian = String.format(Locale.US, "%d/%02d/%02d", year, month, day)
        val shamsi = gregorianToJalali(year, month, day)
        val hijri = convertToHijri(year, month, day)
        return MultiDate(shamsi, hijri, gregorian)
    }

    fun createMultiDateFromShamsi(jy: Int, jm: Int, jd: Int): MultiDate {
        val (gy, gm, gd) = convertPersianToGregorian(jy, jm, jd)
        val shamsi = String.format(Locale.US, "%d/%02d/%02d", jy, jm, jd)
        val gregorian = String.format(Locale.US, "%d/%02d/%02d", gy, gm, gd)
        val hijri = convertToHijri(gy, gm, gd)
        return MultiDate(shamsi, hijri, gregorian)
    }

    fun convertPersianToGregorian(persianYear: Int, persianMonth: Int, persianDay: Int): Triple<Int, Int, Int> {
        val date = jalaliToGregorian(persianYear, persianMonth, persianDay)
        return Triple(date[0], date[1], date[2])
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): String {
        val jalaliDate = calculateJalaliDate(gy, gm, gd)
        return String.format(Locale.US, "%d/%02d/%02d", jalaliDate[0], jalaliDate[1], jalaliDate[2])
    }

    private fun calculateJalaliDate(gy: Int, gm: Int, gd: Int): IntArray {
        var jy: Int
        var jm: Int
        var jd: Int
        var gyTemp = gy
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        jy = if (gyTemp > 1600) { gyTemp -= 1600; 979 } else { gyTemp -= 621; 0 }
        var days = 365 * gyTemp + (gyTemp + 3) / 4 - (gyTemp + 99) / 100 + (gyTemp + 399) / 400
        days += gdm[gm - 1] + gd - 1
        if (gm > 2 && ((gyTemp % 4 == 0 && gyTemp % 100 != 0) || (gyTemp % 400 == 0))) days++
        days -= 79
        jy += 33 * (days / 12053); days %= 12053
        jy += 4 * (days / 1461); days %= 1461
        if (days > 365) { jy += (days - 1) / 365; days = (days - 1) % 365 }
        if (days < 186) { jm = 1 + days / 31; jd = 1 + days % 31 }
        else { jm = 7 + (days - 186) / 30; jd = 1 + (days - 186) % 30 }
        return intArrayOf(jy, jm, jd)
    }

    private fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): IntArray {
        var gy: Int
        var jyTemp = jy
        if (jyTemp > 979) { gy = 1600; jyTemp -= 979 } else { gy = 621 }
        var days = 365 * jyTemp + (jyTemp / 33) * 8 + ((jyTemp % 33 + 3) / 4) + 78 + jd
        days += if (jm <= 6) (jm - 1) * 31 else ((jm - 7) * 30 + 186)
        gy += 400 * (days / 146097); days %= 146097
        if (days > 36524) { gy += 100 * ((days - 1) / 36524); days = (days - 1) % 36524; if (days >= 365) days++ }
        gy += 4 * (days / 1461); days %= 1461
        if (days > 365) { gy += (days - 1) / 365; days = (days - 1) % 365 }
        val monthDays = intArrayOf(0, 31, 28 + if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 1 else 0, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gregorianMonth = 0
        var gdTemp = days + 1
        for (i in 1..12) { if (gdTemp <= monthDays[i]) { gregorianMonth = i; break }; gdTemp -= monthDays[i] }
        return intArrayOf(gy, gregorianMonth, gdTemp)
    }

    fun convertToHijri(gy: Int, gm: Int, gd: Int): String {
        val jd = gregorianToJulianDay(gy, gm, gd)
        val epoch = 1948439
        var daysSinceEpoch = jd - epoch
        var hy = 1
        while (true) {
            val isLeap = isHijriLeapYear(hy)
            val yearDays = if (isLeap) 355 else 354
            if (daysSinceEpoch >= yearDays) { daysSinceEpoch -= yearDays; hy++ } else break
        }
        val monthDaysArr = intArrayOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, if (isHijriLeapYear(hy)) 30 else 29)
        var hm = 1
        var hd = daysSinceEpoch + 1
        for (i in monthDaysArr.indices) { if (hd <= monthDaysArr[i]) { hm = i + 1; break }; hd -= monthDaysArr[i] }
        return String.format(Locale.US, "%d/%02d/%02d", hy, hm, hd)
    }

    private fun isHijriLeapYear(year: Int): Boolean {
        val cycle = year % 30
        return cycle in listOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
    }

    private fun gregorianToJulianDay(yr: Int, mo: Int, da: Int): Int {
        val a = (14 - mo) / 12
        val y = yr + 4800 - a
        val m = mo + 12 * a - 3
        return da + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    // نرمال‌سازی: تبدیل فارسی به انگلیسی
    fun convertToEnglishNumbers(input: String): String {
        if (input.isEmpty()) return input
        var changed = false
        val out = CharArray(input.length)
        for (i in input.indices) {
            val ch = input[i]
            val idx = when (ch) {
                '۰' -> 0; '۱' -> 1; '۲' -> 2; '۳' -> 3; '۴' -> 4; '۵' -> 5; '۶' -> 6; '۷' -> 7; '۸' -> 8; '۹' -> 9
                else -> -1
            }
            if (idx >= 0) { out[i] = englishNumbers[idx]; changed = true } else { out[i] = ch }
        }
        return if (changed) String(out) else input
    }

    // نسخه تک‌پارامتری: از فلگ سراسری استفاده می‌کند و همیشه نرمال‌سازی می‌کند
    fun convertToPersianNumbers(input: String): String =
        convertToPersianNumbers(input, defaultUsePersianNumbers)

    // تبدیل اعداد:
    // - همیشه ابتدا به انگلیسی نرمال می‌کنیم (اگر قبلاً فارسی شده بود)
    // - اگر enabled=true بود، انگلیسی را به فارسی تبدیل می‌کنیم
    // - اگر enabled=false بود، همان انگلیسی را برمی‌گردانیم
    fun convertToPersianNumbers(input: String, enabled: Boolean): String {
        if (input.isEmpty()) return input
        val normalized = convertToEnglishNumbers(input) // مهم: اول نرمال‌سازی
        if (!enabled) return normalized
        val out = StringBuilder(normalized.length)
        for (ch in normalized) {
            if (ch in '0'..'9') out.append(persianNumbers[ch - '0']) else out.append(ch)
        }
        return out.toString()
    }

    fun getPersianMonthName(month: Int): String = when (month) {
        1 -> "فروردین"; 2 -> "اردیبهشت"; 3 -> "خرداد"; 4 -> "تیر"
        5 -> "مرداد"; 6 -> "شهریور"; 7 -> "مهر"; 8 -> "آبان"
        9 -> "آذر"; 10 -> "دی"; 11 -> "بهمن"; 12 -> "اسفند"
        else -> ""
    }

    fun daysInShamsiMonth(jy: Int, jm: Int): Int {
        val (g1y, g1m, g1d) = convertPersianToGregorian(jy, jm, 1)
        val (g2y, g2m, g2d) = if (jm < 12) convertPersianToGregorian(jy, jm + 1, 1) else convertPersianToGregorian(jy + 1, 1, 1)
        val c1 = Calendar.getInstance().apply { set(g1y, g1m - 1, g1d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
        val c2 = Calendar.getInstance().apply { set(g2y, g2m - 1, g2d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
        return ((c2.timeInMillis - c1.timeInMillis) / 86_400_000L).toInt()
    }

    fun getGregorianMonthName(month: Int): String = when (month) {
        1 -> "ژانویه"; 2 -> "فوریه"; 3 -> "مارس"; 4 -> "آوریل"
        5 -> "مه"; 6 -> "ژوئن"; 7 -> "ژوئیه"; 8 -> "اوت"
        9 -> "سپتامبر"; 10 -> "اکتبر"; 11 -> "نوامبر"; 12 -> "دسامبر"
        else -> ""
    }

    fun getWeekDayName(date: MultiDate): String {
        val parts = date.gregorian.split("/")
        val gy = parts[0].toInt()
        val gm = parts[1].toInt()
        val gd = parts[2].toInt()
        val cal = Calendar.getInstance().apply { set(gy, gm - 1, gd) }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یک‌شنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> ""
        }
    }

    fun getPreviousDate(currentDate: MultiDate): MultiDate {
        val parts = currentDate.gregorian.split("/")
        val cal = Calendar.getInstance().apply { set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt()); add(Calendar.DAY_OF_MONTH, -1) }
        return convertCalendarToMultiDate(cal)
    }

    fun getNextDate(currentDate: MultiDate): MultiDate {
        val parts = currentDate.gregorian.split("/")
        val cal = Calendar.getInstance().apply { set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt()); add(Calendar.DAY_OF_MONTH, 1) }
        return convertCalendarToMultiDate(cal)
    }

    fun formatTimeMillis(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    fun formatDisplayTime(timeStr: String, use24HourFormat: Boolean, usePersianNumbers: Boolean): String {
        if (timeStr.isBlank() || !timeStr.contains(":")) {
            val eng = convertToEnglishNumbers(timeStr)
            return convertToPersianNumbers(eng, usePersianNumbers)
        }
        return try {
            val normalized = convertToEnglishNumbers(timeStr)
            val parts = normalized.split(":")
            var hour = parts[0].toInt()
            val minute = parts[1].toInt()
            val minuteStr = String.format(Locale.US, "%02d", minute)

            if (use24HourFormat) {
                val hourStr = String.format(Locale.US, "%02d", hour)
                convertToPersianNumbers("$hourStr:$minuteStr", usePersianNumbers)
            } else {
                val amPm = if (hour < 12) { if (usePersianNumbers) " ق.ظ" else " AM" }
                else { if (usePersianNumbers) " ب.ظ" else " PM" }
                if (hour == 0) hour = 12
                if (hour > 12) hour -= 12
                val hourStr = hour.toString()
                convertToPersianNumbers("$hourStr:$minuteStr", usePersianNumbers) + amPm
            }
        } catch (_: Exception) {
            val eng = convertToEnglishNumbers(timeStr)
            convertToPersianNumbers(eng, usePersianNumbers)
        }
    }

    // DateUtils.kt — داخل object DateUtils اضافه کنید

    fun formatShamsiLong(date: MultiDate, usePersianNumbers: Boolean): String {
        val (y, m, d) = date.getShamsiParts()
        val day = convertToPersianNumbers(d.toString(), usePersianNumbers)
        val monthName = getPersianMonthName(m)
        val year = convertToPersianNumbers(y.toString(), usePersianNumbers)
        return "$day $monthName $year"
    }

    fun formatHijriLong(date: MultiDate, usePersianNumbers: Boolean): String {
        val (yearStr, monthName, dayStr) = date.hijriParts() // خام لاتین + نام ماه فارسی
        val day = convertToPersianNumbers(dayStr, usePersianNumbers)
        val year = convertToPersianNumbers(yearStr, usePersianNumbers)
        return "$day $monthName $year"
    }

    fun formatGregorianLong(date: MultiDate, usePersianNumbers: Boolean): String {
        val (dayStr, monthName, yearStr) = date.gregorianParts() // خام لاتین + نام ماه فارسی
        val day = convertToPersianNumbers(dayStr, usePersianNumbers)
        val year = convertToPersianNumbers(yearStr, usePersianNumbers)
        return "$day $monthName $year"
    }
}