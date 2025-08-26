package com.oqba26.prayertimes.models

import android.util.Log
import com.oqba26.prayertimes.utils.DateUtils
import java.io.Serializable
import java.time.YearMonth
import java.util.Calendar
import java.util.Objects

/**
 * Extension function to get a formatted Shamsi date string for widgets or display.
 * Example: "شنبه ۱۵ خرداد ۱۴۰۳"
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
        this.shamsi // Fallback to raw shamsi date
    }
}

/**
 * Extension function to get the Shamsi day of the week index.
 * شنبه = 0، یکشنبه = 1، … جمعه = 6
 */
fun MultiDate.getShamsiDayOfWeekIndex(): Int {
<<<<<<< HEAD
    return try {
        val parts = this.gregorian.split("/")
        val gy = parts[0].toInt()
        val gm = parts[1].toInt()
        val gd = parts[2].toInt()
        val calendar = Calendar.getInstance().apply {
            set(gy, gm - 1, gd) // Calendar.MONTH is 0-indexed
        }
        // در Calendar: یکشنبه=1 … شنبه=7  → با این فرمول شنبه=0 … جمعه=6
        (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    } catch (e: Exception) {
        Log.e("MultiDateExt", "Error getting Shamsi day of week index for: $this", e)
        0 // Default to Saturday or handle error appropriately
    }
}

data class MultiDate(
    val shamsi: String, // e.g., "1403/03/15"
    val hijri: String,  // e.g., "1445/11/27"
    val gregorian: String // e.g., "2024/06/04"
) : Serializable {

    companion object {
        fun fromYearMonth(yearMonth: YearMonth): MultiDate {
            // استفاده از توابع DateUtils
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
     * Returns parts of the Shamsi date.
     * Example: "1403/03/15" -> Triple(1403, 3, 15)
     */
    fun getShamsiParts(): Triple<Int, Int, Int> {
        val parts = shamsi.split("/")
        return try {
            Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } catch (e: Exception) {
            // Log error or return default in case of parsing failure
            Log.e("MultiDate", "Error parsing Shamsi date: $shamsi", e)
            Triple(0, 0, 0) // Or throw an exception
        }
    }

    /**
     * Returns parts of the Hijri date formatted for display with Persian numbers and month name.
     * Example: "1445/11/27" -> Triple("۱۴۴۵", "ذی‌القعده", "۲۷") (Year, Month Name, Day)
     */
    fun hijriParts(): Triple<String, String, String> {
        val parts = hijri.split("/")
        return try {
            val year = DateUtils.convertToPersianNumbers(parts[0])
            val monthName =
                DateUtils.getHijriMonthName(parts[1].toInt()) // Assuming this is in DateUtils
            val day = DateUtils.convertToPersianNumbers(parts[2])
            Triple(year, monthName, day)
        } catch (e: Exception) {
            Log.e("MultiDate", "Error parsing Hijri date: $hijri", e)
            Triple("", "", "")
        }
    }

    /**
     * Returns parts of the Gregorian date formatted for display with Persian numbers and month name.
     * Example: "2024/06/04" -> Triple("۴", "ژوئن", "۲۰۲۴") (Day, Month Name, Year)
     */
    fun gregorianParts(): Triple<String, String, String> {
        val parts = gregorian.split("/")
        return try {
            val day = DateUtils.convertToPersianNumbers(parts[2])
            val monthName = DateUtils.getGregorianMonthName(parts[1].toInt()) // From DateUtils
            val year = DateUtils.convertToPersianNumbers(parts[0])
            Triple(day, monthName, year)
        } catch (e: Exception) {
            Log.e("MultiDate", "Error parsing Gregorian date: $gregorian", e)
            Triple("", "", "")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false // More robust check
        other as MultiDate
        return shamsi == other.shamsi &&
                hijri == other.hijri &&
                gregorian == other.gregorian
    }

    override fun hashCode(): Int {
        return Objects.hash(shamsi, hijri, gregorian)
    }

    override fun toString(): String {
        return "MultiDate(shamsi='$shamsi', hijri='$hijri', gregorian='$gregorian')"
    }
=======
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
>>>>>>> f0bcccde0307a3dfe302af294c0b253896eaed36
}