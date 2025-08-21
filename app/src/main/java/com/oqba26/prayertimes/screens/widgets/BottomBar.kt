package com.oqba26.prayertimes.screens.widgets

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.*
import kotlinx.coroutines.launch

@Composable
fun BottomBar(
    currentDate: MultiDate,
    prayers: Map<String, String>,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF00ACC1))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* تنظیمات */ }) {
                Icon(Icons.Default.Settings, contentDescription = "تنظیمات", tint = Color.White)
            }

            IconButton(onClick = { onToggleDarkMode() }) {
                Icon(
                    imageVector = Icons.Default.Nightlight,
                    contentDescription = "تغییر حالت شب/روز",
                    tint = Color.White
                )
            }

            IconButton(onClick = {
                scope.launch {
                    val shamsi = currentDate.getShamsiParts()
                    val hijri = currentDate.hijriParts()
                    val greg = currentDate.gregorianParts()
                    val weekDay = getWeekDayName(currentDate)

                    val message = buildString {
                        append("📅 $weekDay ${convertToPersianNumbers(shamsi.third.toString())} ${getPersianMonthName(shamsi.second)} ${convertToPersianNumbers(shamsi.first.toString())}\n")
                        append("🕋 ${hijri.third} ${hijri.second} ${hijri.first} | ${greg.first} ${greg.second} ${greg.third}\n\n")
                        prayers.forEach { (key, value) ->
                            append("  $key: ${convertToPersianNumbers(value)}\n")
                        }
                    }

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    }

                    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری با"))
                }
            }) {
                Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری", tint = Color.White)
            }
        }
    }
}