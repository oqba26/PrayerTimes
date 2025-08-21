package com.oqba26.prayertimes.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oqba26.prayertimes.models.MultiDate
import java.io.InputStreamReader

fun getPrayerTimes(context: Context, date: MultiDate): Map<String, String> {
    try {
        val inputStream = context.assets.open("prayer_times.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
        val data: Map<String, Map<String, String>> = Gson().fromJson(reader, type)
        reader.close()
        inputStream.close()
        
        // استخراج ماه و روز از تاریخ شمسی برای جستجو در داده‌های 12 ماهه
        val monthDay = extractMonthDay(date.shamsi)
        return data[monthDay] ?: getDefaultPrayerTimes()
    } catch (e: Exception) {
        e.printStackTrace()
        return getDefaultPrayerTimes()
    }
}

private fun extractMonthDay(shamsiDate: String): String {
    // از تاریخ کامل مثل "1403/01/15" فقط "01/15" رو استخراج می‌کنه
    val parts = shamsiDate.split("/")
    return if (parts.size >= 3) {
        "${parts[1]}/${parts[2]}"
    } else {
        shamsiDate
    }
}

private fun getDefaultPrayerTimes(): Map<String, String> {
    return linkedMapOf(
        "طلوع بامداد" to "05:00",
        "طلوع خورشید" to "06:30",
        "ظهر" to "12:30",
        "عصر" to "16:00",
        "غروب" to "18:30",
        "عشاء" to "20:00"
    )
}