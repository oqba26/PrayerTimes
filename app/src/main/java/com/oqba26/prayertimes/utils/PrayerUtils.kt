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
        return data["tehran"]?.get(date.shamsi) ?: emptyMap()
    } catch (e: Exception) {
        return emptyMap()
    }
}