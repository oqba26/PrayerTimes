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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PrayerUtils
import java.time.LocalTime
import java.util.Timer
import java.util.TimerTask

@Composable
fun PrayerTimesList(
    prayerTimes: Map<String, String>,
    specificTimes: Map<String, String>,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    usePersianNumbers: Boolean,
    use24HourFormat: Boolean,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val order = remember { listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء") }

    // Create a temporary, complete map and order for highlight calculation
    val highlightOrder = remember { listOf("صبح", "طلوع خورشید", "ظهر", "عصر", "مغرب", "عشاء") }
    val highlightTimes = remember(prayerTimes, specificTimes) {
        specificTimes.toMutableMap().apply {
            prayerTimes["طلوع خورشید"]?.let { put("طلوع خورشید", it) }
        }
    }

    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    // Lifecycle-aware time updater to fix update issue on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var timer: Timer? = null

        val observer = LifecycleEventObserver { _, event ->
            // Update time immediately when lifecycle starts or resumes
            if (event == Lifecycle.Event.ON_START) {
                currentTime = LocalTime.now() // Immediate update
                timer = Timer().apply {
                    scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            currentTime = LocalTime.now()
                        }
                    }, 60_000L, 60_000L) // Start after 1 min, then every minute
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                timer?.cancel()
                timer = null
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            timer?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Calculate the highlighted prayer using the complete temporary map
    val specificHighlightName = remember(highlightTimes, currentTime) {
        PrayerUtils.computeHighlightPrayer(currentTime, highlightTimes, highlightOrder)
    }
    // Convert the specific name (e.g., "صبح") back to a general name (e.g., "طلوع بامداد") for UI matching
    val generalHighlightName = PrayerUtils.getGeneralPrayerName(specificHighlightName)

    Column(modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            order.forEachIndexed { index, prayerName ->
                val timeToFormat = prayerTimes[prayerName] ?: "--:--"
                val formattedTime = DateUtils.formatDisplayTime(timeToFormat, use24HourFormat, usePersianNumbers)

                // Highlight if the general name matches
                val isHighlighted = (prayerName == generalHighlightName)

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
                        .padding(horizontal = 8.dp),
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
