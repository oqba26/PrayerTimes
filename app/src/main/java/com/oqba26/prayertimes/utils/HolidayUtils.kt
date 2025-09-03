package com.oqba26.prayertimes.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.oqba26.prayertimes.models.MultiDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

object HolidayUtils {

    private const val TAG = "HolidayUtils"
    private const val ASSET_FILE = "iran_holidays.json"

    // کش محتوا
    @Volatile
    private var cached: HolidaysPayload? = null

    // ————— API اصلی: تعطیلات ماه جاری (شمسی) —————
    suspend fun getMonthHolidays(
        context: Context,
        year: Int,
        month: Int
    ): Map<Int, List<String>> = withContext(Dispatchers.Default) {
        val payload = loadPayload(context)

        // 1) تعطیلات ثابت شمسی برای همین ماه
        val mm = month.toString().padStart(2, '0')
        val shamsiInMonth: Map<Int, List<String>> = payload.shamsiFixed
            .filterKeys { it.startsWith("$mm/") }
            .mapKeys { k -> k.key.substring(3).toIntOrNull() ?: -1 }
            .filterKeys { it > 0 }

        // 2) تعطیلات قمری در همین ماه شمسی (با بررسی روز به روز)
        val daysInMonth = DateUtils.daysInShamsiMonth(year, month)
        val lunarForMonth = mutableMapOf<Int, MutableList<String>>()

        for (day in 1..daysInMonth) {
            // تبدیل به تاریخ کامل برای دسترسی به تاریخ قمری همان روز
            val md: MultiDate = DateUtils.createMultiDateFromShamsi(year, month, day)
            // md.hijri به‌صورت "YYYY/MM/DD" است
            val parts = md.hijri.split("/")
            if (parts.size == 3) {
                val hm = parts[1].toIntOrNull()
                val hd = parts[2].toIntOrNull()
                if (hm != null && hd != null) {
                    val key = "${hm.toString().padStart(2, '0')}/${hd.toString().padStart(2, '0')}"
                    val titles = payload.hijriAnnual[key]
                    if (!titles.isNullOrEmpty()) {
                        val bucket = lunarForMonth.getOrPut(day) { mutableListOf() }
                        bucket.addAll(titles)
                    }
                }
            }
        }

        // 3) ادغام شمسی و قمری برای خروجی نهایی
        val result = mutableMapOf<Int, MutableList<String>>()
        // شمسی
        shamsiInMonth.forEach { (d, titles) ->
            val bucket = result.getOrPut(d) { mutableListOf() }
            bucket.addAll(titles)
        }
        // قمری
        lunarForMonth.forEach { (d, titles) ->
            val bucket = result.getOrPut(d) { mutableListOf() }
            bucket.addAll(titles)
        }

        // مرتب و یکتا
        result.mapValues { it.value.distinct() }.toSortedMap()
    }

    // ————— API تکمیلی: مناسبت‌های یک روز مشخص —————
    suspend fun getDayHolidays(
        context: Context,
        date: MultiDate
    ): List<String> = withContext(Dispatchers.Default) {
        val payload = loadPayload(context)
        val (sy, sm, sd) = date.getShamsiParts()
        val mmdd = "${sm.toString().padStart(2, '0')}/${sd.toString().padStart(2, '0')}"

        val out = mutableListOf<String>()

        // شمسی ثابت
        payload.shamsiFixed[mmdd]?.let { out.addAll(it) }

        // قمری سالانه
        val hParts = date.hijri.split("/")
        if (hParts.size == 3) {
            val hm = hParts[1].toIntOrNull()
            val hd = hParts[2].toIntOrNull()
            if (hm != null && hd != null) {
                val hKey = "${hm.toString().padStart(2, '0')}/${hd.toString().padStart(2, '0')}"
                payload.hijriAnnual[hKey]?.let { out.addAll(it) }
            }
        }

        out.distinct()
    }

    fun clearCache() {
        cached = null
    }

    // ————— داخلی‌ها —————
    private suspend fun loadPayload(context: Context): HolidaysPayload = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        try {
            context.assets.open(ASSET_FILE).use { stream ->
                InputStreamReader(stream).use { reader ->
                    val type = object : TypeToken<HolidaysPayload>() {}.type
                    val data: HolidaysPayload = Gson().fromJson(reader, type)
                    // نرمال‌سازی کلیدها (MM/DD دو رقمی)
                    val fixed = data.shamsiFixed.mapKeys { normalizeMmDd(it.key) }
                    val lunar = data.hijriAnnual.mapKeys { normalizeMmDd(it.key) }
                    val payload = HolidaysPayload(fixed, lunar)
                    cached = payload
                    payload
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "⛔ خطا در خواندن/پارس ${ASSET_FILE}", e)
            val empty = HolidaysPayload(emptyMap(), emptyMap())
            cached = empty
            empty
        }
    }

    private fun normalizeMmDd(key: String): String {
        val parts = key.split("/")
        return if (parts.size == 2) {
            val m = parts[0].trim().padStart(2, '0')
            val d = parts[1].trim().padStart(2, '0')
            "$m/$d"
        } else key
    }

    // مدل داده JSON
    data class HolidaysPayload(
        @SerializedName("shamsi_fixed")
        val shamsiFixed: Map<String, List<String>> = emptyMap(),
        @SerializedName("hijri_annual")
        val hijriAnnual: Map<String, List<String>> = emptyMap()
    )
}