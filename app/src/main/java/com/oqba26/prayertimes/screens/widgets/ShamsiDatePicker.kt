package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils

private val DarkThemeSelectedDayBackground = Color(0xFF4F378B)
private val DarkThemeSelectedDayText = Color(0xFFEADDFF)

private fun getShamsiMonthNameLocal(month: Int): String {
    return DateUtils.getPersianMonthName(month)
}

private fun getDayOfWeekOffsetLocal(year: Int, month: Int, day: Int = 1): Int {
    val multiDate = DateUtils.createMultiDateFromShamsi(year, month, day)
    return when (DateUtils.getWeekDayName(multiDate)) {
        "شنبه" -> 0
        "یک‌شنبه" -> 1
        "دوشنبه" -> 2
        "سه‌شنبه" -> 3
        "چهارشنبه" -> 4
        "پنج‌شنبه" -> 5
        "جمعه" -> 6
        else -> 0
    }
}

private fun isFriday(year: Int, month: Int, day: Int): Boolean {
    val multiDate = DateUtils.createMultiDateFromShamsi(year, month, day)
    return DateUtils.getWeekDayName(multiDate) == "جمعه"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearPickerDialog(
    initialYear: Int,
    onDismissRequest: () -> Unit,
    onYearSelected: (Int) -> Unit,
    usePersianNumbers: Boolean,
    isDark: Boolean
) {
    val currentShamsiYear = DateUtils.getCurrentDate().getShamsiParts().first
    val startYear = 1320
    val endYear = currentShamsiYear + 50
    val years = remember { (startYear..endYear).toList().reversed() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(initialYear) {
        val index = years.indexOf(initialYear)
        if (index != -1) listState.scrollToItem(index)
    }

    val headerColor = if (isDark) Color(0xFF4F378B) else Color(0xFF0E7490)
    val headerTextColor = if (isDark) Color(0xFFEADDFF) else Color.White

    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(headerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("انتخاب سال شمسی", color = headerTextColor, style = MaterialTheme.typography.titleLarge)
                }

                Box(modifier = Modifier.height(300.dp)) {
                    androidx.compose.foundation.lazy.LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                        items(years.size) { idx ->
                            val year = years[idx]
                            val yearLabel = DateUtils.convertToPersianNumbers(year.toString(), usePersianNumbers)
                            Text(
                                text = yearLabel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onYearSelected(year); onDismissRequest() }
                                    .padding(vertical = 12.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    ) {
                        Button(
                            onClick = onDismissRequest,
                            modifier = Modifier.align(Alignment.CenterStart),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = headerColor,
                                contentColor = headerTextColor
                            )
                        ) { Text("بستن") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun ShamsiDatePicker(
    initialDate: MultiDate = DateUtils.getCurrentDate(),
    onDateSelected: (MultiDate) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean,
    usePersianNumbers: Boolean,
    use24HourFormat: Boolean
) {
    val (initialShamsiYear, initialShamsiMonth, initialShamsiDay) = initialDate.getShamsiParts()

    var displayedShamsiYear by remember { mutableIntStateOf(initialShamsiYear) }
    var displayedShamsiMonth by remember { mutableIntStateOf(initialShamsiMonth) }
    var selectedUserClickedDay by remember { mutableIntStateOf(initialShamsiDay) }

    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(displayedShamsiYear, displayedShamsiMonth, initialDate) {
        val daysInNewMonth = DateUtils.daysInShamsiMonth(displayedShamsiYear, displayedShamsiMonth)
        if (displayedShamsiYear == initialShamsiYear && displayedShamsiMonth == initialShamsiMonth) {
            selectedUserClickedDay = initialShamsiDay
        }
        if (selectedUserClickedDay > daysInNewMonth) {
            selectedUserClickedDay = 1
        }
    }

    if (showYearPickerDialog) {
        YearPickerDialog(
            initialYear = displayedShamsiYear,
            onDismissRequest = { showYearPickerDialog = false },
            onYearSelected = { year -> displayedShamsiYear = year; showYearPickerDialog = false },
            usePersianNumbers = usePersianNumbers,
            isDark = isDarkTheme
        )
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(),
        content = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                val headerColor = if (isDarkTheme) Color(0xFF4F378B) else Color(0xFF0E7490)
                val headerTextColor = if (isDarkTheme) Color(0xFFEADDFF) else Color.White

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(headerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = DateUtils.convertToPersianNumbers("انتخاب تاریخ شمسی", usePersianNumbers),
                                color = headerTextColor,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Column(modifier = Modifier.padding(16.dp)) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    displayedShamsiMonth--
                                    if (displayedShamsiMonth < 1) {
                                        displayedShamsiMonth = 12; displayedShamsiYear--
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ماه قبل")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable { showMonthPicker = true }
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Text(DateUtils.convertToPersianNumbers(getShamsiMonthNameLocal(displayedShamsiMonth), usePersianNumbers), style = MaterialTheme.typography.titleMedium)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "انتخاب ماه")
                                        }
                                        DropdownMenu(
                                            expanded = showMonthPicker,
                                            onDismissRequest = { showMonthPicker = false },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                                        ) {
                                            (1..12).forEach { month ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            DateUtils.convertToPersianNumbers(getShamsiMonthNameLocal(month), usePersianNumbers),
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    },
                                                    onClick = {
                                                        displayedShamsiMonth = month
                                                        showMonthPicker = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(0.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable { showYearPickerDialog = true }
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        val yearLabel = DateUtils.convertToPersianNumbers(displayedShamsiYear.toString(), usePersianNumbers)
                                        Text(yearLabel, style = MaterialTheme.typography.titleMedium)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "انتخاب سال")
                                    }
                                }
                                IconButton(onClick = {
                                    displayedShamsiMonth++
                                    if (displayedShamsiMonth > 12) {
                                        displayedShamsiMonth = 1; displayedShamsiYear++
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ماه بعد")
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                val daysOfWeek = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                                daysOfWeek.forEach { dayLabel ->
                                    Text(DateUtils.convertToPersianNumbers(dayLabel, usePersianNumbers), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(7),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 240.dp, max = 280.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(getDayOfWeekOffsetLocal(displayedShamsiYear, displayedShamsiMonth)) {
                                    Spacer(Modifier.aspectRatio(1f).padding(2.dp))
                                }
                                items(DateUtils.daysInShamsiMonth(displayedShamsiYear, displayedShamsiMonth)) { dayIndex ->
                                    val dayNumber = dayIndex + 1
                                    val isSelected = (dayNumber == selectedUserClickedDay)

                                    val currentDate = DateUtils.getCurrentDate()
                                    val isCurrentDayToday = dayNumber == currentDate.getShamsiParts().third &&
                                            displayedShamsiMonth == currentDate.getShamsiParts().second &&
                                            displayedShamsiYear == currentDate.getShamsiParts().first
                                    val isDayFriday = isFriday(displayedShamsiYear, displayedShamsiMonth, dayNumber)

                                    val backgroundColor = when {
                                        isSelected && isDarkTheme -> DarkThemeSelectedDayBackground
                                        isSelected && !isDarkTheme -> MaterialTheme.colorScheme.primary
                                        isCurrentDayToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    }
                                    val textColor = when {
                                        isSelected && isDarkTheme -> DarkThemeSelectedDayText
                                        isSelected && !isDarkTheme -> MaterialTheme.colorScheme.onPrimary
                                        isDayFriday -> Color.Red
                                        isCurrentDayToday -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(backgroundColor)
                                            .clickable { selectedUserClickedDay = dayNumber },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val dayLabel = DateUtils.convertToPersianNumbers(dayNumber.toString(), usePersianNumbers)
                                        Text(dayLabel, color = textColor, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                val monthDayCount = DateUtils.daysInShamsiMonth(displayedShamsiYear, displayedShamsiMonth)
                                val offset = getDayOfWeekOffsetLocal(displayedShamsiYear, displayedShamsiMonth)
                                val remainingCells = (7 - ((offset + monthDayCount) % 7)) % 7
                                items(remainingCells) { Spacer(Modifier.aspectRatio(1f).padding(2.dp)) }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                                ) {
                                    Button(
                                        onClick = onDismiss,
                                        modifier = Modifier.align(Alignment.CenterStart),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        )
                                    ) { Text("انصراف") }

                                    Button(
                                        onClick = {
                                            val resultDate = DateUtils.createMultiDateFromShamsi(
                                                displayedShamsiYear, displayedShamsiMonth, selectedUserClickedDay
                                            )
                                            onDateSelected(resultDate)
                                        },
                                        modifier = Modifier.align(Alignment.CenterEnd),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = headerColor,
                                            contentColor = headerTextColor
                                        )
                                    ) { Text("تایید") }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}