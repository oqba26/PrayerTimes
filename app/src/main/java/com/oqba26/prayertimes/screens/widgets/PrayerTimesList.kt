package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.oqba26.prayertimes.utils.convertToPersianNumbers
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun PrayerTimesList(
    prayerTimes: Map<String, String>,
    modifier: Modifier = Modifier
) {
    // ترتیب صحیح نمازها مطابق تصویر
    val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")

    val currentPrayer = remember(System.currentTimeMillis(), prayerTimes) {
        getNextPrayerName(prayerTimes)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        order.forEachIndexed { index, name ->
            val time = prayerTimes[name] ?: "--:--"
            val isNext = name == currentPrayer

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ساعت
                    Text(
                        text = convertToPersianNumbers(time),
                        fontSize = 18.sp,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                    )

                    // اسم نماز
                    Text(
                        text = name,
                        fontSize = 18.sp,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                    )
                }

                if (index != order.lastIndex) {
                    Divider(
                        thickness = 0.7.dp,
                        color = Color(0xFFAAAAAA)
                    )
                }
            }
        }
    }
}

fun getNextPrayerName(prayers: Map<String, String>): String? {
    val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
    val now = LocalTime.now()
    val formatter = DateTimeFormatter.ofPattern("H:mm")

    return order.firstOrNull { name ->
        val raw = prayers[name]?.trim()?.padStart(5, '0') ?: return@firstOrNull false
        runCatching {
            val time = LocalTime.parse(raw, formatter)
            now < time
        }.getOrElse { false }
    }
}