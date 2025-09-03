package com.oqba26.prayertimes.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.screens.widgets.BottomBar
import com.oqba26.prayertimes.screens.widgets.DayHeaderBar
import com.oqba26.prayertimes.screens.widgets.MonthCalendarView
import com.oqba26.prayertimes.screens.widgets.PrayerTimesList
import com.oqba26.prayertimes.screens.widgets.ShamsiDatePickerDialog
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.DateUtils.convertPersianToGregorian
import com.oqba26.prayertimes.utils.DateUtils.convertToPersianNumbers
import com.oqba26.prayertimes.utils.DateUtils.getCurrentDate
import com.oqba26.prayertimes.utils.DateUtils.getNextDate
import com.oqba26.prayertimes.utils.DateUtils.getPreviousDate
import com.oqba26.prayertimes.utils.HolidayUtils
import com.oqba26.prayertimes.viewmodels.PrayerViewModel
import java.time.LocalDate
import java.time.Period

@Composable
fun CalendarScreen(
    currentDate: MultiDate,
    uiState: PrayerViewModel.Result<Map<String, String>>,
    onDateChange: (MultiDate) -> Unit,
    onRetry: () -> Unit,
    viewModel: PrayerViewModel,
    onToggleDarkMode: () -> Unit,
    onOpenSettings: () -> Unit   // ← جدید
) {
    val context = LocalContext.current

    val today = remember { getCurrentDate() }
    val isToday = currentDate.shamsi == today.shamsi
    var showDatePicker by remember { mutableStateOf(false) }

    // برچسب اختلاف تاریخ (ثابت‌فضا)
    val diffLabel = remember(currentDate.shamsi, today.shamsi) {
        buildRelativeDiffLabel(today, currentDate)
    }
    val diffColor = Color(0xFFF48FB1)

    // لود تعطیلات همین ماه (شمسی) و پاس به تقویم
    val (y, m, _) = currentDate.getShamsiParts()
    var monthHolidays by remember(y, m) { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }
    LaunchedEffect(y, m) {
        monthHolidays = HolidayUtils.getMonthHolidays(context, y, m)
        android.util.Log.d("HolidayDebug", "Holidays for $y/$m = $monthHolidays")
    }

    val bottomBarHeight = 32.dp

    Column(modifier = Modifier.fillMaxSize()) {

        // هدر بالا: دکمه تقویم (چپ) + عنوان (راست) + زیرش برچسب اختلاف با فضای ثابت
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (!isToday) onDateChange(today) else showDatePicker = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "تقویم",
                        tint = Color.Gray
                    )
                }

                Text(
                    text = "تقویم و اوقات نماز",
                    color = Color.Gray
                )
            }

            // فضای ثابت برای برچسب اختلاف
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (diffLabel.isNotEmpty()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = diffLabel,
                            textAlign = TextAlign.Center,
                            color = diffColor
                        )
                    }
                }
            }
        }

        // تقویم ماه (با تعطیلات)
        MonthCalendarView(
            currentDate = currentDate,
            selectedDate = currentDate,
            onDateChange = onDateChange,
            holidays = monthHolidays
        )

        // هدر تاریخ روز
        DayHeaderBar(
            date = currentDate,
            onPreviousDay = { onDateChange(getPreviousDate(currentDate)) },
            onNextDay = { onDateChange(getNextDate(currentDate)) }
        )

        // اوقات نماز - کل فضای باقیمانده (بدون فاصله اضافی پایین)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 0.dp)
        ) {
            when (uiState) {
                is PrayerViewModel.Result.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PrayerViewModel.Result.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = uiState.message)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onRetry) { Text("تلاش مجدد") }
                        }
                    }
                }
                is PrayerViewModel.Result.Success -> {
                    PrayerTimesList(
                        prayerTimes = uiState.data,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // BottomBar کوچک‌تر + دکمه تنظیمات
        BottomBar(
            currentDate = currentDate,
            prayers = if (uiState is PrayerViewModel.Result.Success) uiState.data else emptyMap(),
            onToggleDarkMode = onToggleDarkMode,
            onOpenSettings = onOpenSettings,   // ← جدید
            height = bottomBarHeight
        )
    }

    if (showDatePicker) {
        ShamsiDatePickerDialog(
            currentDate = currentDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                onDateChange(it)
                showDatePicker = false
            }
        )
    }
}

@Composable
fun CalendarScreenEntryPoint(
    viewModel: PrayerViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenSettings: () -> Unit   // ← جدید
) {
    var currentDate by remember { mutableStateOf<MultiDate>(DateUtils.getCurrentDate()) }
    val uiState = viewModel.uiState.collectAsState().value

    LaunchedEffect(currentDate) {
        viewModel.updateDate(currentDate)
    }

    CalendarScreen(
        currentDate = currentDate,
        uiState = uiState,
        onDateChange = { currentDate = it },
        onRetry = { viewModel.updateDate(currentDate) },
        viewModel = viewModel,
        onToggleDarkMode = onToggleDarkMode,
        onOpenSettings = onOpenSettings   // ← جدید
    )
}

// اختلاف تاریخ با امروز (فارسی، ترتیب: سال، ماه، روز)
private fun buildRelativeDiffLabel(today: MultiDate, target: MultiDate): String {
    if (today.shamsi == target.shamsi) return ""

    val (ty, tm, td) = today.getShamsiParts()
    val (gy1, gm1, gd1) = convertPersianToGregorian(ty, tm, td)
    val d1 = LocalDate.of(gy1, gm1, gd1)

    val (sy, sm, sd) = target.getShamsiParts()
    val (gy2, gm2, gd2) = convertPersianToGregorian(sy, sm, sd)
    val d2 = LocalDate.of(gy2, gm2, gd2)

    val isAfter = d2.isAfter(d1)
    val (start, end) = if (isAfter) d1 to d2 else d2 to d1
    val p = Period.between(start, end)

    val y = p.years
    val m = p.months
    val d = p.days

    val parts = mutableListOf<String>()
    if (y > 0) parts += "${convertToPersianNumbers(y.toString())} سال"
    if (m > 0) parts += "${convertToPersianNumbers(m.toString())} ماه"
    if (d > 0) parts += "${convertToPersianNumbers(d.toString())} روز"

    if (parts.isEmpty()) return ""
    val suffix = if (isAfter) "بعد" else "قبل"
    return parts.joinToString(" و ") + " $suffix"
}