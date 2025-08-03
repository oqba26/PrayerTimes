package com.oqba26.prayertimes.utils

import com.oqba26.prayertimes.models.MultiDate
import java.util.Calendar

fun getCurrentDate(): MultiDate {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val gregorian = String.format("%d/%02d/%02d", year, month, day)

    val shamsi = convertToShamsi(year, month, day)
    val hijri = convertToHijri(year, month, day)

    return MultiDate(shamsi, hijri, gregorian)
}

private fun convertToShamsi(gy: Int, gm: Int, gd: Int): String {
    var gy2 = if (gm > 2) gy + 1 else gy
    val days = (365 * gy) + ((gy2 + 3) / 4).toInt() - ((gy2 + 99) / 100).toInt() + ((gy2 + 399) / 400).toInt() - 80 + gd + intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)[gm - 1]
    var jy = -1595 + (33 * (days / 12053))
    var days2 = days % 12053
    jy += 4 * (days2 / 1461)
    days2 %= 1461
    if (days2 > 365) {
        jy += ((days2 - 1) / 365)
        days2 = (days2 - 1) % 365
    }
    val jm = if (days2 < 186) (1 + (days2 / 31)) else (7 + ((days2 - 186) / 30))
    val jd = 1 + if (days2 < 186) (days2 % 31) else ((days2 - 186) % 30)
    return String.format("%d/%02d/%02d", jy, jm, jd)
}

private fun convertToHijri(gy: Int, gm: Int, gd: Int): String {
    val jd = gregorianToJulianDay(gy, gm, gd)
    val l = jd - 1948440 + 1062
    val n = ((l - 1) / 10631).toInt()
    var j = l - 10631 * n + 354
    val p = ((10985 - j) / 5316) * ((50 * 5316 - j) / 5316) + (j / 1584) * ((5000 - j) / 1584)
    j = j - p + (15 * p - (p / 30)) * (p / 30)
    j = j - (j / 112899) * 30 + (j % 112899) % 30
    val hy = 30 * n + (j / 355) + 1
    val hm = (j % 355) / 30 + 1
    val hd = (j % 355) % 30 + 1
    return String.format("%d/%02d/%02d", hy, hm, hd)
}

private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Int {
    var a = (14 - month) / 12
    var y = year + 4800 - a
    var m = month + 12 * a - 3
    return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
}