package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
    modifier: Modifier = Modifier,
    isDark: Boolean,
    usePersianNumbers: Boolean,
    use24HourFormat: Boolean,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val order = remember { listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء") }
    val now = LocalTime.now()
    val currentPrayerName = remember(prayerTimes, now) { PrayerUtils.getCurrentPrayerForHighlight(prayerTimes, now) }

    Column(modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            order.forEachIndexed { index, prayerName ->
                val timeToFormat = prayerTimes[prayerName] ?: "--:--"
                val formattedTime = DateUtils.formatDisplayTime(timeToFormat, use24HourFormat, usePersianNumbers)

                val isHighlighted = (prayerName == currentPrayerName)

                val rowBackgroundColor: Color
                val timeTextColor: Color
                val nameTextColor: Color

                if (isHighlighted) {
                    rowBackgroundColor = Color(0xFFDFF3DF)
                    timeTextColor = Color(0xFF1A237E)
                    nameTextColor = Color(0xFF2E7D32)
                } else {
                    if (isDark) {
                        rowBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
                        timeTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        nameTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        rowBackgroundColor = Color.Transparent
                        timeTextColor = Color(0xFF1A237E)
                        nameTextColor = Color(0xFF0D47A1)
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(rowBackgroundColor)
                        .padding(horizontal = 1.dp), // padding داخل Row برای فاصله از لبه‌ها
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp,
                        color = timeTextColor,
                        textAlign = TextAlign.Start
                    )

                    Text(
                        text = prayerName,
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp,
                        color = nameTextColor,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.End
                    )
                }

                if (index != order.lastIndex) {
                    val dividerColor = if (isDark) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        Color(0x22000000)
                    }
                    HorizontalDivider(thickness = 1.dp, color = dividerColor)
                }
            }
        }
        Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
    }
}
