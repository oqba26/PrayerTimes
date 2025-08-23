package com.oqba26.prayertimes.models

import java.io.Serializable
import java.time.YearMonth
import java.util.Objects
import com.oqba26.prayertimes.utils.*
import java.util.Calendar

data class MultiDate(
    val shamsi: String,
    val hijri: String,
    val gregorian: String
) : Serializable {
    companion object {
        fun fromYearMonth(yearMonth: YearMonth): MultiDate {
            val (gy, gm, _) = convertPersianToGregorian(yearMonth.year, yearMonth.monthValue, 1)
            return createMultiDate(gy, gm, 1)
        }
    }
    fun getDisplayShamsi() = shamsi.replace("/", " / ")
    fun getDisplayHijri() = hijri.replace("/", " / ")
    fun getDisplayGregorian() = gregorian.replace("/", " / ")
    fun isSameDate(other: MultiDate) = this.shamsi == other.shamsi
    fun getShamsiParts(): Triple<Int, Int, Int> {
        val parts = shamsi.split("/")
        return Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }
    fun hijriParts(): Triple<String, String, String> {
        val parts = hijri.split("/")
        return Triple(
            convertToPersianNumbers(parts[0]),
            getHijriMonthName(parts[1].toInt()),
            convertToPersianNumbers(parts[2])
        )
    }
    fun gregorianParts(): Triple<String, String, String> {
        val parts = gregorian.split("/")
        return Triple(
            convertToPersianNumbers(parts[2]),
            getGregorianMonthName(parts[1].toInt()),
            convertToPersianNumbers(parts[0])
        )
    }
    private fun getHijriMonthName(month: Int) = when(month) {
        1 -> "محرم"; 2 -> "صفر"; 3 -> "ربیع‌الاول"; 4 -> "ربیع‌الثانی"
        5 -> "جمادی‌الاول"; 6 -> "جمادی‌الثانی"; 7 -> "رجب"
        8 -> "شعبان"; 9 -> "رمضان"; 10 -> "شوال"
        11 -> "ذی‌القعده"; 12 -> "ذی‌الحجه"
        else -> ""
    }
    private fun getGregorianMonthName(month: Int) = when(month) {
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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MultiDate) return false
        return shamsi == other.shamsi && hijri == other.hijri && gregorian == other.gregorian
    }
    override fun hashCode(): Int {
        return Objects.hash(shamsi, hijri, gregorian)
    }
}

fun MultiDate.getShamsiDayOfWeekIndex(): Int {
    val (gy, gm, gd) = this.gregorian.split("/").map { it.toInt() }
    val calendar = Calendar.getInstance().apply { set(gy, gm - 1, gd) }
    return calendar.get(Calendar.DAY_OF_WEEK) % 7
}

fun MultiDate.getFormattedShamsiDateForWidget(): String {
    val (year, month, day) = this.getShamsiParts()
    val dayName = getWeekDayName(this)
    val monthName = getPersianMonthName(month)

    val yearInFarsi = convertToPersianNumbers(year.toString())
    val dayInFarsi = convertToPersianNumbers(day.toString())

    return "$dayName $dayInFarsi $monthName $yearInFarsi"
}