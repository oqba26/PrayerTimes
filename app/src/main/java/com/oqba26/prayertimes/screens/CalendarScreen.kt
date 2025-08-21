package com.oqba26.prayertimes.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.screens.widgets.*
import com.oqba26.prayertimes.utils.*
import com.oqba26.prayertimes.viewmodels.PrayerViewModel
import kotlinx.coroutines.delay

@Composable
fun CalendarScreen(
    currentDate: MultiDate,
    uiState: PrayerViewModel.Result<Map<String, String>>,
    onDateChange: (MultiDate) -> Unit,
    onRetry: () -> Unit,
    viewModel: PrayerViewModel,
    onToggleDarkMode: () -> Unit
) {
    val today = remember { getCurrentDate() }
    val isToday = currentDate.shamsi == today.shamsi

    val tick = remember { mutableStateOf(0L) }
    LaunchedEffect(isToday) {
        if (isToday) {
            while (true) {
                tick.value = System.currentTimeMillis()
                delay(50_000)
            }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Bar - نوار بالایی
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (!isToday) {
                    onDateChange(today)
                } else {
                    showDatePicker = true
                }
            }) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "تقویم",
                    tint = Color.Gray
                )
            }
        }

        // Calendar Section - بخش تقویم
        MonthCalendarView(
            currentDate = currentDate,
            selectedDate = currentDate,
            onDateChange = onDateChange
        )

        // Date Display Bar - نوار نمایش تاریخ
        DayHeaderBar(
            date = currentDate,
            onPreviousDay = { onDateChange(getPreviousDate(currentDate)) },
            onNextDay = { onDateChange(getNextDate(currentDate)) }
        )

        // Prayer Times Section - بخش اوقات نماز
        when (uiState) {
            is PrayerViewModel.Result.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is PrayerViewModel.Result.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = uiState.message)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onRetry) {
                            Text("تلاش مجدد")
                        }
                    }
                }
            }

            is PrayerViewModel.Result.Success -> {
                PrayerTimesList(
                    prayerTimes = uiState.data,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bottom Navigation Bar - نوار پایین
        BottomBar(
            currentDate = currentDate,
            prayers = if (uiState is PrayerViewModel.Result.Success) uiState.data else emptyMap(),
            onToggleDarkMode = onToggleDarkMode
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