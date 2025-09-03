package com.oqba26.prayertimes.utils

import com.oqba26.prayertimes.models.MultiDate

object ShareUtils {
    private val prayerOrder = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")

    fun buildShareText(date: MultiDate, prayerTimes: Map<String, String>): String {
        val shamsi = date.getShamsiParts()
        val weekDay = DateUtils.getWeekDayName(date)

        val persianTitle = buildString {
            append(weekDay).append(" ")
            append(DateUtils.convertToPersianNumbers(shamsi.third.toString())).append(" ")
            append(DateUtils.getPersianMonthName(shamsi.second)).append(" ")
            append(DateUtils.convertToPersianNumbers(shamsi.first.toString()))
        }

        val hijri = date.hijriParts()
        val greg = date.gregorianParts()
        val hijriLine = "${hijri.third} ${hijri.second} ${hijri.first}"
        val gregLine = "${greg.first} ${greg.second} ${greg.third}"

        val timesLine = buildTimesLine(prayerTimes) // افقی: نام : زمان | نام : زمان ...

        // وادار کردن برخی اپ‌ها به راست‌به‌چپ
        val RLE = "\u202B" // Right-to-Left Embedding
        val PDF = "\u202C" // Pop Directional Formatting

        return buildString {
            append(RLE)
            append(persianTitle).append("\n")
            append(hijriLine).append(" | ").append(gregLine).append("\n")
            append(timesLine)
            append(PDF)
        }.trim()
    }

    private fun buildTimesLine(prayerTimes: Map<String, String>, sep: String = " | "): String {
        val items = prayerOrder.mapNotNull { name ->
            val raw = prayerTimes[name]?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val timeFa = DateUtils.convertToPersianNumbers(raw)
            "$name : $timeFa"
        }
        return items.joinToString(sep).trim()
    }
}