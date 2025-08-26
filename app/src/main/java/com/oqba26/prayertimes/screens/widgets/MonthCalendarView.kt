package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.LayoutDirection
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.*
import com.oqba26.prayertimes.utils.DateUtils.convertPersianToGregorian
import com.oqba26.prayertimes.utils.DateUtils.convertToPersianNumbers
import com.oqba26.prayertimes.utils.DateUtils.createMultiDateFromShamsi
import com.oqba26.prayertimes.utils.DateUtils.daysInShamsiMonth
import com.oqba26.prayertimes.utils.DateUtils.getPersianMonthName
import java.util.*

@Composable
fun MonthCalendarView(
    currentDate: MultiDate,
    selectedDate: MultiDate,
    onDateChange: (MultiDate) -> Unit
) {
    val (year, month, selectedDay) = selectedDate.getShamsiParts()
    val daysInMonth = remember(year, month) { daysInShamsiMonth(year, month) }
    val firstDayOffset = remember(year, month) {
        val (gy, gm, gd) = convertPersianToGregorian(year, month, 1)
        val cal = Calendar.getInstance().apply { set(gy, gm - 1, gd) }
        cal.get(Calendar.DAY_OF_WEEK) % 7
    }

    val totalCells = 42
    val days = List(firstDayOffset) { null } +
            (1..daysInMonth).toList() +
            List(totalCells - firstDayOffset - daysInMonth) { null }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ماه و سال
            Text(
                text = "${getPersianMonthName(month)} ${convertToPersianNumbers(year.toString())}",
                fontSize = 16.sp,
                color = Color(0xFF0E7490),
                modifier = Modifier.padding(vertical = 8.dp),
                fontWeight = FontWeight.Medium
            )

            // روزهای هفته - راست به چپ
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val headers = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                    headers.forEachIndexed { index, label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = if (index == 6) Color.Red else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // تقویم
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    userScrollEnabled = false
                ) {
                    items(days.size) { i ->
                        val day = days[i]
                        if (day == null) {
                            Box(modifier = Modifier.size(30.dp))
                        } else {
                            val isSelectedDay = selectedDay == day
                            val isFriday = (i % 7) == 6
                            
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelectedDay -> Color(0xFFE0E0E0)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable {
                                        onDateChange(createMultiDateFromShamsi(year, month, day))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = convertToPersianNumbers(day.toString()),
                                    fontSize = 12.sp,
                                    color = when {
                                        isSelectedDay -> Color.Black
                                        isFriday -> Color.Red
                                        else -> Color.Black
                                    },
                                    fontWeight = if (isSelectedDay) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // دکمه‌های تغییر ماه - در وسط عمودی
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val (prevYear, prevMonth) = if (month > 1) year to (month - 1) else (year - 1) to 12
                    val newDay = selectedDay.coerceAtMost(daysInShamsiMonth(prevYear, prevMonth))
                    onDateChange(createMultiDateFromShamsi(prevYear, prevMonth, newDay))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Text("‹", fontSize = 16.sp, color = Color.Gray)
            }

            IconButton(
                onClick = {
                    val (nextYear, nextMonth) = if (month < 12) year to (month + 1) else (year + 1) to 1
                    val newDay = selectedDay.coerceAtMost(daysInShamsiMonth(nextYear, nextMonth))
                    onDateChange(createMultiDateFromShamsi(nextYear, nextMonth, newDay))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Text("›", fontSize = 16.sp, color = Color.Gray)
            }
        }
    }
}