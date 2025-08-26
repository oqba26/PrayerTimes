package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.DateUtils.convertToPersianNumbers
import com.oqba26.prayertimes.utils.DateUtils.getPersianMonthName
import com.oqba26.prayertimes.utils.DateUtils.getWeekDayName

@Composable
fun DayHeaderBar(
    date: MultiDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val shamsi = date.getShamsiParts()
    val hijri = date.hijriParts()
    val greg = date.gregorianParts()
    val weekDay = remember(date) { getWeekDayName(date) }

    var isShortFormat by remember { mutableStateOf(false) }
    val slash = " / "

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF00ACC1))
            .height(56.dp) // ارتفاع مناسب‌تر
            .clickable { isShortFormat = !isShortFormat } // کلیک روی هدر = تغییر حالت
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 60.dp), // فضای خالی سمت چپ/راست برای دکمه‌ها
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // خط اول: روز هفته + تاریخ شمسی
            Text(
                text = if (isShortFormat) {
                    "$weekDay ${convertToPersianNumbers(shamsi.third.toString())}$slash" +
                            "${convertToPersianNumbers(shamsi.second.toString())}$slash" +
                            "${convertToPersianNumbers(shamsi.first.toString())}"
                } else {
                    "$weekDay ${convertToPersianNumbers(shamsi.third.toString())} " +
                            "${getPersianMonthName(shamsi.second)} " +
                            "${convertToPersianNumbers(shamsi.first.toString())}"
                },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // خط دوم: قمری و میلادی
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = if (isShortFormat) {
                            val parts = date.hijri.split("/")
                            "${convertToPersianNumbers(parts[2])}$slash" +
                                    "${convertToPersianNumbers(parts[1])}$slash" +
                                    "${convertToPersianNumbers(parts[0])}"
                        } else {
                            "${hijri.third} ${hijri.second} ${hijri.first}"
                        },
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(" | ", color = Color.White, fontSize = 13.sp)
                    Text(
                        text = if (isShortFormat) {
                            val parts = date.gregorian.split("/")
                            "${convertToPersianNumbers(parts[2])}$slash" +
                                    "${convertToPersianNumbers(parts[1])}$slash" +
                                    "${convertToPersianNumbers(parts[0])}"
                        } else {
                            "${greg.first} ${greg.second} ${greg.third}"
                        },
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // دکمه روز بعد ← سمت راست
        IconButton(
            onClick = onNextDay,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(32.dp)
        ) {
            Text("‹", fontSize = 16.sp, color = Color.White)
        }

        // دکمه روز قبل ← سمت چپ
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(32.dp)
        ) {
            Text("›", fontSize = 16.sp, color = Color.White)
        }
    }
}