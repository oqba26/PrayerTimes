package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.LayoutDirection
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.*

@Composable
fun DayHeaderBar(
    date: MultiDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val shamsi = date.getShamsiParts()
    val hijri = date.hijriParts()
    val greg = date.gregorianParts()
    val weekDay = getWeekDayName(date)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF00ACC1))
            .padding(vertical = 8.dp)
            .height(60.dp)
    ) {
        // محتوای مرکزی
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 80.dp), // افزایش فاصله برای دکمه‌ها
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // خط اول: روز هفته و تاریخ شمسی
            Text(
                text = "$weekDay ${convertToPersianNumbers(shamsi.third.toString())} ${getPersianMonthName(shamsi.second)} ${convertToPersianNumbers(shamsi.first.toString())}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // خط دوم: تاریخ‌های هجری و میلادی افقی
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${hijri.third} ${hijri.second} ${hijri.first}",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = " | ",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${greg.first} ${greg.second} ${greg.third}",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // دکمه روز بعد ← سمت راست
        IconButton(
            onClick = onNextDay,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(36.dp)
        ) {
            Text("‹", fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center)
        }

        // دکمه روز قبل ← سمت چپ
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(36.dp)
        ) {
            Text("›", fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}