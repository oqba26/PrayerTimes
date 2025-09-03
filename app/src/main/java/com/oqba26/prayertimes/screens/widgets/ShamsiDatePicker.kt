package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils.convertToPersianNumbers
import com.oqba26.prayertimes.utils.DateUtils.createMultiDateFromShamsi
import com.oqba26.prayertimes.utils.DateUtils.daysInShamsiMonth
import com.oqba26.prayertimes.utils.DateUtils.getCurrentDate
import com.oqba26.prayertimes.utils.DateUtils.getFirstDayOfWeekIndex
import com.oqba26.prayertimes.utils.DateUtils.getPersianMonthName
import java.util.*

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

    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

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
                Text("انصراف", color = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Text(
                text = "رفتن به تاریخ مورد نظر:",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                color = onSurface
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (step) {
                    DatePickerStep.Year -> {
                        YearSelection(
                            selectedYear = selectedYear,
                            onYearSelected = { year ->
                                selectedYear = year
                                step = DatePickerStep.Month
                            }
                        )
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
                                onDateSelected(createMultiDateFromShamsi(selectedYear, selectedMonth, selectedDay))
                                step = DatePickerStep.Year
                                onDismiss()
                            },
                            onBack = { step = DatePickerStep.Month }
                        )
                    }
                }
            }
        },
        // رنگ و شکل دیالوگ روشن‌تر و خواناتر در هر دو تم
        containerColor = surface,
        titleContentColor = onSurface,
        textContentColor = onSurface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp
    )
}

@Composable
fun YearSelection(
    selectedYear: Int,
    onYearSelected: (Int) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(300.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(161) { index ->
                val year = 1320 + index
                val isSelected = year == selectedYear
                Text(
                    text = convertToPersianNumbers(year.toString()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onYearSelected(year) }
                        .padding(vertical = 12.dp),
                    color = if (isSelected) primary else onSurface,
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
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val shape = RoundedCornerShape(8.dp)

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
                    color = onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(12) { monthIdx ->
                    val month = monthIdx + 1
                    val monthName = getPersianMonthName(month)
                    val isSelected = month == selectedMonth

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(if (isSelected) highlight else Color.Transparent)
                            .clickable { onMonthSelected(month) }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$monthName | ${convertToPersianNumbers(month.toString())}",
                            color = if (isSelected) primary else onSurface,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
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
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val shape = RoundedCornerShape(6.dp)

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
                    color = onSurface,
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
                                .clip(shape)
                                .background(if (isSelectedDay) highlight else Color.Transparent)
                                .clickable { onDaySelected(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = convertToPersianNumbers(day.toString()),
                                color = if (isSelectedDay) primary else onSurface.copy(alpha = 0.85f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}