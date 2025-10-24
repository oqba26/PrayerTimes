package com.oqba26.prayertimes.models

import android.util.Log
import com.oqba26.prayertimes.utils.DateUtils
import java.io.Serializable
import java.util.Objects

/**
 * تاریخ چندفرمتـی (شمسی، قمری، میلادی) برای استفاده داخل اپلیکیشن.
 * نکته: این مدل اعداد را تبدیل نمی‌کند؛ UI بر اساس usePersianNumbers تصمیم می‌گیرد.
 */
data class MultiDate(
    val shamsi: String,     // مثال "1403/03/15" (اعداد لاتین)
    val hijri: String,      // مثال "1445/11/27" (اعداد لاتین)
    val gregorian: String   // مثال "2024/06/04" (اعداد لاتین)
) : Serializable {

    companion object;

    /** اجزای تاریخ شمسی: سال، ماه، روز */
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
     * اجزای تاریخ قمری: سال (خام لاتین)، نام ماه (فارسی)، روز (خام لاتین)
     * تبدیل اعداد در UI انجام می‌شود.
     */
    fun hijriParts(): Triple<String, String, String> {
        val parts = hijri.split("/")
        return try {
            val year = parts[0] // خام (لاتین)
            val monthName = DateUtils.getHijriMonthName(parts[1].toInt())
            val day = parts[2]  // خام (لاتین)
            Triple(year, monthName, day)
        } catch (e: Exception) {
            Log.e("MultiDate", "Error parsing Hijri date: $hijri", e)
            Triple("", "", "")
        }
    }

    /**
     * اجزای تاریخ میلادی: روز (خام لاتین)، نام ماه (فارسی)، سال (خام لاتین)
     * تبدیل اعداد در UI انجام می‌شود.
     */
    fun gregorianParts(): Triple<String, String, String> {
        val parts = gregorian.split("/")
        return try {
            val day = parts[2] // خام (لاتین)
            val monthName = DateUtils.getGregorianMonthName(parts[1].toInt())
            val year = parts[0] // خام (لاتین)
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