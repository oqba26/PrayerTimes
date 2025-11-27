package com.oqba26.prayertimes.utils

import com.oqba26.prayertimes.models.MultiDate
import java.util.Calendar
import java.util.Locale

object DateUtils {

    private val dateCache = mutableMapOf<String, MultiDate>()

    private val persianNumbers = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
    private val englishNumbers = arrayOf('0','1','2','3','4','5','6','7','8','9')

    fun getHijriMonthName(month: Int): String = when (month) {
        1 -> "محرم"; 2 -> "صفر"; 3 -> "ربیع‌الاول"; 4 -> "ربیع‌الثانی"
        5 -> "جمادی‌الاول"; 6 -> "جمادی‌الثانی"; 7 -> "رجب"; 8 -> "شعبان"; 9 -> "رمضان"; 10 -> "شوال"; 11 -> "ذی‌القعده"; 12 -> "ذی‌الحجه"; else -> ""
    }

    fun getCurrentDate(): MultiDate {
        val calendar = Calendar.getInstance()
        val cacheKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
        return dateCache.getOrPut(cacheKey) {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            createMultiDateFromGregorian(year, month, day)
        }
    }

    fun convertCalendarToMultiDate(calendar: Calendar): MultiDate {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return createMultiDateFromGregorian(year, month, day)
    }

    fun createMultiDateFromShamsi(jy: Int, jm: Int, jd: Int): MultiDate {
        val (gy, gm, gd) = convertPersianToGregorian(jy, jm, jd)
        val shamsi = String.format(Locale.US, "%d/%02d/%02d", jy, jm, jd)
        val gregorian = String.format(Locale.US, "%d/%02d/%02d", gy, gm, gd)
        val hijri = convertToHijri(gy, gm, gd)
        return MultiDate(shamsi, hijri, gregorian)
    }

    fun createMultiDateFromGregorian(gy: Int, gm: Int, gd: Int): MultiDate {
        val gregorian = String.format(Locale.US, "%d/%02d/%02d", gy, gm, gd)
        val shamsi = gregorianToJalali(gy, gm, gd)
        val hijri = convertToHijri(gy, gm, gd)
        return MultiDate(shamsi, hijri, gregorian)
    }

    fun createMultiDateFromHijri(hy: Int, hm: Int, hd: Int): MultiDate {
        val jd = hijriToJulianDay(hy, hm, hd)
        val gregorianParts = julianDayToGregorian(jd)
        val gy = gregorianParts[0]; val gm = gregorianParts[1]; val gd = gregorianParts[2]
        val hijri = String.format(Locale.US, "%d/%02d/%02d", hy, hm, hd)
        val gregorian = String.format(Locale.US, "%d/%02d/%02d", gy, gm, gd)
        val shamsi = gregorianToJalali(gy, gm, gd)
        return MultiDate(shamsi, hijri, gregorian)
    }

    private fun hijriToJulianDay(hy: Int, hm: Int, hd: Int): Int {
        var days = (hy - 1) * 354
        days += (11 * hy + 3) / 30
        val monthDays = intArrayOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, if (isHijriLeapYear(hy)) 30 else 29)
        for (i in 0 until hm - 1) { days += monthDays[i] }
        days += hd
        return days + 1948439
    }

    private fun julianDayToGregorian(jd: Int): IntArray {
        val l = jd + 68569; val n = 4 * l / 146097; val l2 = l - (146097 * n + 3) / 4
        val i = 4000 * (l2 + 1) / 1461001; val l3 = l2 - 1461 * i / 4 + 31
        val j = 80 * l3 / 2447; val gd = l3 - 2447 * j / 80; val l4 = j / 11
        val gm = j + 2 - 12 * l4; val gy = 100 * (n - 49) + i + l4
        return intArrayOf(gy, gm, gd)
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
        var jy: Int; var jm: Int; var jd: Int; var gyTemp = gy
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        jy = if (gyTemp > 1600) { gyTemp -= 1600; 979 } else { gyTemp -= 621; 0 }
        var days = 365 * gyTemp + (gyTemp + 3) / 4 - (gyTemp + 99) / 100 + (gyTemp + 399) / 400
        days += gdm[gm - 1] + gd - 1
        if (gm > 2 && ((gyTemp % 4 == 0 && gyTemp % 100 != 0) || (gyTemp % 400 == 0))) days++
        days -= 79; jy += 33 * (days / 12053); days %= 12053; jy += 4 * (days / 1461); days %= 1461
        if (days > 365) { jy += (days - 1) / 365; days = (days - 1) % 365 }
        if (days < 186) { jm = 1 + days / 31; jd = 1 + days % 31 } else { jm = 7 + (days - 186) / 30; jd = 1 + (days - 186) % 30 }
        return intArrayOf(jy, jm, jd)
    }

    private fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): IntArray {
        var gy: Int; var jyTemp = jy
        if (jyTemp > 979) { gy = 1600; jyTemp -= 979 } else { gy = 621 }
        var days = 365 * jyTemp + (jyTemp / 33) * 8 + ((jyTemp % 33 + 3) / 4) + 78 + jd
        days += if (jm <= 6) (jm - 1) * 31 else ((jm - 7) * 30 + 186)
        gy += 400 * (days / 146097); days %= 146097
        if (days > 36524) { gy += 100 * ((days - 1) / 36524); days = (days - 1) % 36524; if (days >= 365) days++ }
        gy += 4 * (days / 1461); days %= 1461
        if (days > 365) { gy += (days - 1) / 365; days = (days - 1) % 365 }
        val monthDays = intArrayOf(0, 31, 28 + if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 1 else 0, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gregorianMonth = 0; var gdTemp = days + 1
        for (i in 1..12) { if (gdTemp <= monthDays[i]) { gregorianMonth = i; break }; gdTemp -= monthDays[i] }
        return intArrayOf(gy, gregorianMonth, gdTemp)
    }

    fun convertToHijri(gy: Int, gm: Int, gd: Int): String {
        val jd = gregorianToJulianDay(gy, gm, gd); val epoch = 1948440; var daysSinceEpoch = jd - epoch; var hy = 1
        while (true) { val isLeap = isHijriLeapYear(hy); val yearDays = if (isLeap) 355 else 354; if (daysSinceEpoch >= yearDays) { daysSinceEpoch -= yearDays; hy++ } else break }
        val monthDaysArr = intArrayOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, if (isHijriLeapYear(hy)) 30 else 29)
        var hm = 1; var hd = daysSinceEpoch + 1
        for (i in monthDaysArr.indices) { if (hd <= monthDaysArr[i]) { hm = i + 1; break }; hd -= monthDaysArr[i] }
        return String.format(Locale.US, "%d/%02d/%02d", hy, hm, hd)
    }

    private fun isHijriLeapYear(year: Int): Boolean {
        val cycle = year % 30
        return cycle in listOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
    }

    private fun gregorianToJulianDay(yr: Int, mo: Int, da: Int): Int {
        val a = (14 - mo) / 12; val y = yr + 4800 - a; val m = mo + 12 * a - 3
        return da + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    fun convertToEnglishNumbers(input: String): String {
        if (input.isEmpty()) return input; var changed = false; val out = CharArray(input.length)
        for (i in input.indices) { val ch = input[i]; val idx = when (ch) { '۰' -> 0; '۱' -> 1; '۲' -> 2; '۳' -> 3; '۴' -> 4; '۵' -> 5; '۶' -> 6; '۷' -> 7; '۸' -> 8; '۹' -> 9; else -> -1 }; if (idx >= 0) { out[i] = englishNumbers[idx]; changed = true } else { out[i] = ch } }
        return if (changed) String(out) else input
    }

    fun convertToPersianNumbers(input: String, enabled: Boolean): String {
        if (input.isEmpty()) return input; val normalized = convertToEnglishNumbers(input); if (!enabled) return normalized; val out = StringBuilder(normalized.length)
        for (ch in normalized) { if (ch in '0'..'9') out.append(persianNumbers[ch - '0']) else out.append(ch) }
        return out.toString()
    }

    fun getPersianMonthName(month: Int): String = when (month) {
        1 -> "فروردین"; 2 -> "اردیبهشت"; 3 -> "خرداد"; 4 -> "تیر"; 5 -> "مرداد"; 6 -> "شهریور"; 7 -> "مهر"; 8 -> "آبان"; 9 -> "آذر"; 10 -> "دی"; 11 -> "بهمن"; 12 -> "اسفند"; else -> ""
    }

    fun daysInShamsiMonth(jy: Int, jm: Int): Int {
        if (jm <= 6) return 31
        if (jm <= 11) return 30
        val year = jalaliToGregorian(jy, jm, 1)[0]
        val isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        return if(isLeap) 30 else 29
    }

    fun getGregorianMonthName(month: Int): String = when (month) {
        1 -> "ژانویه"; 2 -> "فوریه"; 3 -> "مارس"; 4 -> "آوریل"; 5 -> "مه"; 6 -> "ژوئن"; 7 -> "ژوئیه"; 8 -> "اوت"; 9 -> "سپتامبر"; 10 -> "اکتبر"; 11 -> "نوامبر"; 12 -> "دسامبر"; else -> ""
    }

    fun getWeekDayName(date: MultiDate): String {
        val (gy, gm, gd) = date.gregorian.split("/").map { it.toInt() }; val cal = Calendar.getInstance().apply { set(gy, gm - 1, gd) }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"; Calendar.SUNDAY -> "یک‌شنبه"; Calendar.MONDAY -> "دوشنبه"; Calendar.TUESDAY -> "سه‌شنبه"; Calendar.WEDNESDAY -> "چهارشنبه"; Calendar.THURSDAY -> "پنج‌شنبه"; Calendar.FRIDAY -> "جمعه"; else -> ""
        }
    }

    fun getPreviousDate(currentDate: MultiDate): MultiDate {
        val (gy, gm, gd) = currentDate.gregorian.split("/").map { it.toInt() }; val cal = Calendar.getInstance().apply { set(gy, gm - 1, gd); add(Calendar.DAY_OF_MONTH, -1) }
        return convertCalendarToMultiDate(cal)
    }

    fun getNextDate(currentDate: MultiDate): MultiDate {
        val (gy, gm, gd) = currentDate.gregorian.split("/").map { it.toInt() }; val cal = Calendar.getInstance().apply { set(gy, gm - 1, gd); add(Calendar.DAY_OF_MONTH, 1) }
        return convertCalendarToMultiDate(cal)
    }

    fun formatTimeMillis(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }; val h = cal.get(Calendar.HOUR_OF_DAY); val m = cal.get(Calendar.MINUTE)
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun formatDisplayTime(timeStr: String, use24HourFormat: Boolean, usePersianNumbers: Boolean): String {
        if (timeStr.isBlank() || !timeStr.contains(":")) return convertToPersianNumbers(convertToEnglishNumbers(timeStr), usePersianNumbers)
        return try {
            val (h, m) = convertToEnglishNumbers(timeStr).split(":").map { it.toInt() }; var hour = h
            val minuteStr = String.format(Locale.US, "%02d", m)
            if (use24HourFormat) convertToPersianNumbers(String.format(Locale.US, "%02d:%s", hour, minuteStr), usePersianNumbers)
            else {
                val amPm = if (hour < 12) " ق.ظ" else " ب.ظ"; if (hour == 0) hour = 12; if (hour > 12) hour -= 12
                convertToPersianNumbers("$hour:$minuteStr", usePersianNumbers) + amPm
            }
        } catch (_: Exception) { convertToPersianNumbers(convertToEnglishNumbers(timeStr), usePersianNumbers) }
    }

    private fun rtlWrap(str: String): String {
        val rli = '⁧' // RIGHT-TO-LEFT ISOLATE
        val pdi = '⁩' // POP DIRECTIONAL ISOLATE
        return "$rli$str$pdi"
    }

    fun formatShamsiLong(date: MultiDate, usePersianNumbers: Boolean): String {
        val (y, m, d) = date.getShamsiParts()
        val day = convertToPersianNumbers(d.toString(), usePersianNumbers)
        val monthName = getPersianMonthName(m)
        val year = convertToPersianNumbers(y.toString(), usePersianNumbers)
        return rtlWrap("$day $monthName $year")
    }

    fun formatHijriLong(date: MultiDate, usePersianNumbers: Boolean): String {
        val (yearStr, monthName, dayStr) = date.hijriParts()
        val day = convertToPersianNumbers(dayStr, usePersianNumbers)
        val year = convertToPersianNumbers(yearStr, usePersianNumbers)
        return rtlWrap("$day $monthName $year")
    }

    fun formatGregorianLong(date: MultiDate, usePersianNumbers: Boolean): String {
        val (dayStr, monthName, yearStr) = date.gregorianParts()
        val day = convertToPersianNumbers(dayStr, usePersianNumbers)
        val year = convertToPersianNumbers(yearStr, usePersianNumbers)
        return rtlWrap("$day $monthName $year")
    }

    fun formatShamsiShort(date: MultiDate, usePersianNumbers: Boolean): String {
        val (y, m, d) = date.getShamsiParts()
        val year = convertToPersianNumbers(y.toString(), usePersianNumbers)
        val month = convertToPersianNumbers(m.toString(), usePersianNumbers)
        val day = convertToPersianNumbers(d.toString(), usePersianNumbers)
        return rtlWrap("$year/$month/$day")
    }

    fun formatHijriShort(date: MultiDate, usePersianNumbers: Boolean): String {
        val parts = date.hijri.split("/")
        val year = convertToPersianNumbers(parts[0], usePersianNumbers)
        val month = convertToPersianNumbers(parts[1], usePersianNumbers)
        val day = convertToPersianNumbers(parts[2], usePersianNumbers)
        return rtlWrap("$year/$month/$day")
    }

    fun formatGregorianShort(date: MultiDate, usePersianNumbers: Boolean): String {
        val parts = date.gregorian.split("/")
        val year = convertToPersianNumbers(parts[0], usePersianNumbers)
        val month = convertToPersianNumbers(parts[1], usePersianNumbers)
        val day = convertToPersianNumbers(parts[2], usePersianNumbers)
        return rtlWrap("$year/$month/$day")
    }
}
