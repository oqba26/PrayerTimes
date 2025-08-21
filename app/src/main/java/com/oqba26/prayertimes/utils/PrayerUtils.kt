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
        val type = object : TypeToken<Map<String, Map<String, Map<String, String>>>>() {}.type
        val data: Map<String, Map<String, Map<String, String>>> = Gson().fromJson(reader, type)
        reader.close()
        inputStream.close()
        
        // Try to get data for the specific date, fallback to a default date if not found
        val tehranData = data["tehran"] ?: return getDefaultPrayerTimes()
        return tehranData[date.shamsi] ?: getDefaultPrayerTimes()
    } catch (e: Exception) {
        e.printStackTrace()
        return getDefaultPrayerTimes()
    }
}

private fun getDefaultPrayerTimes(): Map<String, String> {
    return mapOf(
        "صبح" to "05:00",
        "ظهر" to "12:30",
        "عصر" to "16:00",
        "مغرب" to "18:30",
        "عشا" to "20:00"
    )
}