package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PrayerUtils
import java.time.LocalTime

@Composable
fun PrayerTimesList(
    prayerTimes: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val order = remember { listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء") }

    // نماز فعلی برای هایلایت (پنجره ۱۵ دقیقه‌ای در PrayerUtils اعمال می‌شود)
    val now = LocalTime.now()
    val current = remember(prayerTimes) { PrayerUtils.getCurrentPrayerForHighlight(prayerTimes, now) }

    // تقسیم مساوی فضای موجود بین 6 ردیف (بدون اسکرول)
    Column(modifier = modifier.fillMaxSize()) {
        order.forEachIndexed { index, name ->
            val time = prayerTimes[name]?.let(DateUtils::convertToPersianNumbers) ?: "--:--"
            val highlighted = (name == current)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(if (highlighted) Color(0xFFDFF3DF) else Color.Transparent)
                    .padding(horizontal = 16.dp),   // ← padding قبلی برگردانده شد
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ساعت سمت چپ
                Text(
                    text = time,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    color = Color(0xFF1A237E),
                    textAlign = TextAlign.Start
                )
                // نام نماز سمت راست
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    color = if (highlighted) Color(0xFF2E7D32) else Color(0xFF0D47A1),
                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.End
                )
            }

            if (index != order.lastIndex) {
                Divider(color = Color(0x22000000), thickness = 1.dp)
            }
        }
    }
}