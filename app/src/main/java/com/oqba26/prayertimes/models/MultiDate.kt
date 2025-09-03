package com.oqba26.prayertimes.models

import android.util.Log
import com.oqba26.prayertimes.utils.DateUtils
import java.io.Serializable
import java.time.YearMonth
import java.util.Calendar
import java.util.Objects

/**
 * تاریخ چندفرمتـی (شمسی، قمری، میلادی) برای استفاده داخل اپلیکیشن.
 */
data class MultiDate(
    val shamsi: String,     // مثال "1403/03/15"
    val hijri: String,      // مثال "1445/11/27"
    val gregorian: String   // مثال "2024/06/04"
) : Serializable {

    companion object {
        fun fromYearMonth(yearMonth: YearMonth): MultiDate {
            val (gy, gm, _) = DateUtils.convertPersianToGregorian(
                yearMonth.year,
                yearMonth.monthValue,
                1
            )
            return DateUtils.createMultiDate(gy, gm, 1)
        }
    }

    fun getDisplayShamsi(): String = shamsi.replace("/", " / ")
    fun getDisplayHijri(): String = hijri.replace("/", " / ")
    fun getDisplayGregorian(): String = gregorian.replace("/", " / ")

    fun isSameDate(other: MultiDate): Boolean = this.shamsi == other.shamsi

    /**
     * اجزای تاریخ شمسی: سال، ماه، روز
     * "1403/03/15" -> Triple(1403, 3, 15)
     */
    fun getShamsiParts(): Triple<Int, Int, Int> {
        val parts = shamsi.split("/")
        return try {
            Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } catch (e: Exception) {
            Log.e("MultiDate", "Error parsing Shamsi date: $shamsi", e)
            Triple(0, 0, 0)
        }
    }

    /**
     * اجزای تاریخ قمری: سال (فارسی)، نام ماه، روز (فارسی)
     */
    fun hijriParts(): Triple<String, String, String> {
        val parts = hijri.split("/")
        return try {
            val year = DateUtils.convertToPersianNumbers(parts[0])
            val monthName = DateUtils.getHijriMonthName(parts[1].toInt())
            val day = DateUtils.convertToPersianNumbers(parts[2])
            Triple(year, monthName, day)
        } catch (e: Exception) {
            Log.e("MultiDate", "Error parsing Hijri date: $hijri", e)
            Triple("", "", "")
        }
    }

    /**
     * اجزای تاریخ میلادی: روز (فارسی)، نام ماه، سال (فارسی)
     */
    fun gregorianParts(): Triple<String, String, String> {
        val parts = gregorian.split("/")
        return try {
            val day = DateUtils.convertToPersianNumbers(parts[2])
            val monthName = DateUtils.getGregorianMonthName(parts[1].toInt())
            val year = DateUtils.convertToPersianNumbers(parts[0])
            Triple(day, monthName, year)
        } catch (e: Exception) {
            Log.e("MultiDate", "Error parsing Gregorian date: $gregorian", e)
            Triple("", "", "")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MultiDate
        return shamsi == other.shamsi &&
                hijri == other.hijri &&
                gregorian == other.gregorian
    }

    override fun hashCode(): Int = Objects.hash(shamsi, hijri, gregorian)

    override fun toString(): String {
        return "MultiDate(shamsi='$shamsi', hijri='$hijri', gregorian='$gregorian')"
    }
}

/**
 * ✅ Extension functions
 */

fun MultiDate.getFormattedShamsiDateForWidget(): String {
    return try {
        val (year, month, day) = this.getShamsiParts()
        val dayName = DateUtils.getWeekDayName(this)
        val monthName = DateUtils.getPersianMonthName(month)
        val yearInFarsi = DateUtils.convertToPersianNumbers(year.toString())
        val dayInFarsi = DateUtils.convertToPersianNumbers(day.toString())
        "$dayName $dayInFarsi $monthName $yearInFarsi"
    } catch (e: Exception) {
        Log.e("MultiDateExt", "Error formatting Shamsi date for widget: $this", e)
        this.shamsi
    }
}

/**
 * شنبه = 0، یکشنبه = 1 … جمعه = 6
 */
fun MultiDate.getShamsiDayOfWeekIndex(): Int {
    return try {
        val parts = this.gregorian.split("/")
        val gy = parts[0].toInt()
        val gm = parts[1].toInt()
        val gd = parts[2].toInt()
        val calendar = Calendar.getInstance().apply {
            set(gy, gm - 1, gd)
        }
        (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    } catch (e: Exception) {
        Log.e("MultiDateExt", "Error getting Shamsi day of week index for: $this", e)
        0
    }
}