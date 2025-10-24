package com.oqba26.prayertimes.utils

import com.oqba26.prayertimes.models.MultiDate

object ShareUtils {

    const val RLE = "\u202B" // Right-to-Left Embedding
    const val PDF = "\u202C" // Pop Directional Formatting
    private val prayerOrder = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")

    fun buildShareText(
        date: MultiDate,
        prayerTimes: Map<String, String>,
        usePersianNumbers: Boolean,
        use24HourFormat: Boolean
    ): String {
        val shamsi = date.getShamsiParts()
        val weekDay = DateUtils.getWeekDayName(date)

        val persianTitle = buildString {
            append(weekDay).append(" ")
            append(DateUtils.convertToPersianNumbers(shamsi.third.toString(), usePersianNumbers)).append(" ")
            append(DateUtils.getPersianMonthName(shamsi.second)).append(" ")
            append(DateUtils.convertToPersianNumbers(shamsi.first.toString(), usePersianNumbers))
        }

        val hijri = date.hijriParts()          // (yearString, monthNamePersian, dayString)
        val greg = date.gregorianParts()       // در پروژه‌ات: (dayString, monthNamePersian, yearString)

        val hijriLine = "${DateUtils.convertToPersianNumbers(hijri.third, usePersianNumbers)} ${hijri.second} ${DateUtils.convertToPersianNumbers(hijri.first, usePersianNumbers)}"
        val gregLine  = "${DateUtils.convertToPersianNumbers(greg.first, usePersianNumbers)} ${greg.second} ${DateUtils.convertToPersianNumbers(greg.third, usePersianNumbers)}"

        val timesLine = buildTimesLine(prayerTimes, usePersianNumbers, use24HourFormat)

        return buildString {
            append(RLE)
            append(persianTitle).append("\n")
            append(hijriLine).append(" | ").append(gregLine).append("\n")
            append(timesLine)
            append(PDF)
        }.trim()
    }

    private fun buildTimesLine(
        prayerTimes: Map<String, String>,
        usePersianNumbers: Boolean,
        use24HourFormat: Boolean,
        sep: String = " | "
    ): String {
        val items = prayerOrder.mapNotNull { name ->
            val rawTime = prayerTimes[name]?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val formattedTime = DateUtils.formatDisplayTime(rawTime, use24HourFormat, usePersianNumbers)
            "$name : $formattedTime"
        }
        return items.joinToString(sep).trim()
    }
}