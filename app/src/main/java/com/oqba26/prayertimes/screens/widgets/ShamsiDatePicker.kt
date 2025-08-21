package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.*
import java.util.*
import kotlin.math.min

enum class DatePickerStep { Year, Month, Day }

@Composable
fun ShamsiDatePickerDialog(
    currentDate: MultiDate,
    onDateSelected: (MultiDate) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(DatePickerStep.Year) }
    val today = getCurrentDate()
    var selectedYear by remember { mutableIntStateOf(today.getShamsiParts().first) }
    var selectedMonth by remember { mutableIntStateOf(today.getShamsiParts().second) }
    var selectedDay by remember { mutableIntStateOf(today.getShamsiParts().third) }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            step = DatePickerStep.Year
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                step = DatePickerStep.Year
            }) {
                Text("انصراف")
            }
        },
        title = {
            Text(
                text = ":رفتن به تاریخ مورد نظر",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (step) {
                    DatePickerStep.Year -> {
                        YearSelection(selectedYear) { year ->
                            selectedYear = year
                            step = DatePickerStep.Month
                        }
                    }
                    DatePickerStep.Month -> {
                        MonthSelection(
                            year = selectedYear,
                            selectedMonth = selectedMonth,
                            onMonthSelected = {
                                selectedMonth = it
                                step = DatePickerStep.Day
                            },
                            onBack = { step = DatePickerStep.Year }
                        )
                    }
                    DatePickerStep.Day -> {
                        DaySelection(
                            year = selectedYear,
                            month = selectedMonth,
                            selectedDay = selectedDay,
                            onDaySelected = { day ->
                                selectedDay = day
                                onDateSelected(
                                    createMultiDateFromShamsi(selectedYear, selectedMonth, selectedDay)
                                )
                                step = DatePickerStep.Year
                                onDismiss()
                            },
                            onBack = { step = DatePickerStep.Month }
                        )
                    }
                }
            }
        }
    )
}

// ❤️ سال از ۱۳۲۰ تا ۱۵۸۰ مثلاً

@Composable
fun YearSelection(
    selectedYear: Int,
    onYearSelected: (Int) -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(300.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(161) { index ->
                val year = 1320 + index
                Text(
                    text = convertToPersianNumbers(year.toString()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onYearSelected(year) }
                        .padding(vertical = 12.dp),
                    color = if (year == selectedYear) Color(0xFF00ACC1) else Color.Black,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MonthSelection(
    year: Int,
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(text = "‹", fontSize = 20.sp, color = Color.Gray)
                }
                Text(
                    text = convertToPersianNumbers(year.toString()),
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(220.dp)
            ) {
                items(12) { month ->
                    val monthName = getPersianMonthName(month + 1)
                    Text(
                        text = "$monthName | ${convertToPersianNumbers((month + 1).toString())}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMonthSelected(month + 1) }
                            .padding(vertical = 12.dp),
                        color = if (month + 1 == selectedMonth) Color(0xFF00ACC1) else Color.Black,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun DaySelection(
    year: Int,
    month: Int,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val daysInMonth = daysInShamsiMonth(year, month)
        val firstDayOffset = getFirstDayOfWeekIndex(java.time.YearMonth.of(year, month))
        val totalCells = 42
        val trailingNulls = maxOf(0, totalCells - firstDayOffset - daysInMonth)
        val days = List(firstDayOffset) { null } +
                (1..daysInMonth).toList() +
                List(trailingNulls) { null }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(text = "‹", fontSize = 20.sp, color = Color.Gray)
                }
                Text(
                    text = "${getPersianMonthName(month)} ${convertToPersianNumbers(year.toString())}",
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(250.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(days.size) { i ->
                    val day = days[i]
                    if (day == null) {
                        Box(modifier = Modifier.size(32.dp))
                    } else {
                        val isSelectedDay = selectedDay == day
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (isSelectedDay) Color(0xFF00ACC1) else Color.Transparent
                                )
                                .clickable { onDaySelected(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = convertToPersianNumbers(day.toString()),
                                color = if (isSelectedDay) Color.White else Color(0xFF004D40),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}