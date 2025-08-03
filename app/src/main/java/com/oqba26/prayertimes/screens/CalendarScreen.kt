package com.oqba26.prayertimes.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oqba26.prayertimes.utils.getCurrentDate
import com.oqba26.prayertimes.utils.getPrayerTimes

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val currentDate = remember { mutableStateOf(getCurrentDate()) }
    val prayerTimes = getPrayerTimes(context, currentDate.value)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE8F5E9) // رنگ زمینه سبز روشن اسلامی
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "تقویم امروز",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C), // سبز تیره
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CalendarItem("شمسی: ${currentDate.value.shamsi}")
                        CalendarItem("قمری: ${currentDate.value.hijri}")
                        CalendarItem("میلادی: ${currentDate.value.gregorian}")
                    }
                }
            }

            item {
                Text(
                    text = "اوقات نماز امروز",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (prayerTimes.isNotEmpty()) {
                items(prayerTimes.entries.toList()) { entry ->
                    PrayerItem(entry.key, entry.value)
                }
            } else {
                item {
                    Text(
                        text = "داده‌ای برای امروز موجود نیست. لطفاً JSON را بروز کنید.",
                        fontSize = 16.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarItem(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun PrayerItem(name: String, time: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCEDC8)) // سبز روشن
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = time, fontSize = 16.sp, color = Color(0xFF388E3C))
        }
    }
}