package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.utils.DateUtils.convertToPersianNumbers
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun PrayerTimesList(
    prayerTimes: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")

    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    // هر ۳۰ ثانیه زمان سیستم رو آپدیت کن
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(30_000)
        }
    }

    // پیدا کردن نماز فعلی/بعدی
    val currentPrayer = remember(currentTime, prayerTimes) {
        com.oqba26.prayertimes.utils.PrayerUtils.getCurrentPrayerForHighlight(prayerTimes, currentTime)
    }

    // محتوای راست‌به‌چپ
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            order.forEachIndexed { index, name ->
                val time = prayerTimes[name] ?: "--:--"
                val isNext = name == currentPrayer

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isNext) colorResource(R.color.prayer_highlight_bg)
                            else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                        color = if (isNext) colorResource(R.color.prayer_text_highlight)
                        else colorResource(R.color.prayer_text_normal)
                    )
                    Text(
                        text = convertToPersianNumbers(time),
                        fontSize = 16.sp,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                        color = if (isNext) colorResource(R.color.prayer_text_highlight)
                        else colorResource(R.color.prayer_text_normal),
                        textAlign = TextAlign.End
                    )
                }

                if (index != order.lastIndex) {
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

// پیدا کردن نماز فعلی/بعدی بر اساس زمان جاری
fun getNextPrayerName(prayers: Map<String, String>, now: LocalTime): String? {
    val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    return order.firstOrNull { name ->
        val raw = prayers[name]?.trim()
        if (raw.isNullOrEmpty()) return@firstOrNull false

        runCatching {
            val parts = raw.split(':')
            if (parts.size != 2) return@runCatching false

            val formattedTime = "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
            val time = LocalTime.parse(formattedTime, formatter)
            now < time
        }.getOrElse { false }
    } ?: order.last() // اگر بعد از عشاء باشیم، آخری (عشاء) برمی‌گرده
}