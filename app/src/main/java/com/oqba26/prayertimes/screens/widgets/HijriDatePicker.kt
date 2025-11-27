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

private fun getHijriMonthNameLocal(month: Int): String {
    return DateUtils.getHijriMonthName(month)
}

private fun getDayOfWeekOffsetForHijri(year: Int, month: Int, day: Int = 1): Int {
    val multiDate = DateUtils.createMultiDateFromHijri(year, month, day)
    val gregorianParts = multiDate.gregorian.split("/").map { it.toInt() }
    val cal = java.util.Calendar.getInstance().apply { set(gregorianParts[0], gregorianParts[1] - 1, gregorianParts[2]) }
    return (cal.get(java.util.Calendar.DAY_OF_WEEK) + 1) % 7 // Assuming Saturday is 0
}

private fun daysInHijriMonth(year: Int, month: Int): Int {
    val isLeap = (11 * year + 14) % 30 < 11
    return if (month == 12 && isLeap) 30 else if (month % 2 != 0) 30 else 29
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HijriYearPickerDialog(
    initialYear: Int,
    onDismissRequest: () -> Unit,
    onYearSelected: (Int) -> Unit,
    usePersianNumbers: Boolean,
    isDark: Boolean
) {
    val currentYear = DateUtils.getCurrentDate().hijri.split("/")[0].toInt()
    val startYear = 1340
    val endYear = currentYear + 50
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
                    Text("انتخاب سال قمری", color = headerTextColor, style = MaterialTheme.typography.titleLarge)
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
@Composable
fun HijriDatePicker(
    initialDate: MultiDate = DateUtils.getCurrentDate(),
    onDateSelected: (MultiDate) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean,
    usePersianNumbers: Boolean
) {
    val initialHijriParts = initialDate.hijri.split("/").map { it.toInt() }
    val initialYear = initialHijriParts[0]
    val initialMonth = initialHijriParts[1]
    val initialDay = initialHijriParts[2]

    var displayedYear by remember { mutableIntStateOf(initialYear) }
    var displayedMonth by remember { mutableIntStateOf(initialMonth) }
    var selectedUserClickedDay by remember { mutableIntStateOf(initialDay) }

    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(displayedYear, displayedMonth) {
        val daysInNewMonth = daysInHijriMonth(displayedYear, displayedMonth)
        if (selectedUserClickedDay > daysInNewMonth) {
            selectedUserClickedDay = daysInNewMonth
        }
    }

    if (showYearPickerDialog) {
        HijriYearPickerDialog(
            initialYear = displayedYear,
            onDismissRequest = { showYearPickerDialog = false },
            onYearSelected = { year -> displayedYear = year; showYearPickerDialog = false },
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
                                text = "انتخاب تاریخ قمری",
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
                                    displayedMonth--
                                    if (displayedMonth < 1) {
                                        displayedMonth = 12; displayedYear--
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
                                            Text(getHijriMonthNameLocal(displayedMonth), style = MaterialTheme.typography.titleMedium)
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
                                                            getHijriMonthNameLocal(month),
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    },
                                                    onClick = {
                                                        displayedMonth = month
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
                                        val yearLabel = DateUtils.convertToPersianNumbers(displayedYear.toString(), usePersianNumbers)
                                        Text(yearLabel, style = MaterialTheme.typography.titleMedium)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "انتخاب سال")
                                    }
                                }
                                IconButton(onClick = {
                                    displayedMonth++
                                    if (displayedMonth > 12) {
                                        displayedMonth = 1; displayedYear++
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
                                items(getDayOfWeekOffsetForHijri(displayedYear, displayedMonth)) {
                                    Spacer(Modifier.aspectRatio(1f).padding(2.dp))
                                }
                                items(daysInHijriMonth(displayedYear, displayedMonth)) { dayIndex ->
                                    val dayNumber = dayIndex + 1
                                    val isSelected = (dayNumber == selectedUserClickedDay)

                                    val todayMultiDate = DateUtils.getCurrentDate()
                                    val todayHijriParts = todayMultiDate.hijri.split("/").map{it.toInt()}

                                    val isCurrentDayToday = dayNumber == todayHijriParts[2] &&
                                            displayedMonth == todayHijriParts[1] &&
                                            displayedYear == todayHijriParts[0]

                                    val backgroundColor = when {
                                        isSelected && isDarkTheme -> DarkThemeSelectedDayBackground
                                        isSelected && !isDarkTheme -> MaterialTheme.colorScheme.primary
                                        isCurrentDayToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    }
                                    val textColor = when {
                                        isSelected && isDarkTheme -> DarkThemeSelectedDayText
                                        isSelected && !isDarkTheme -> MaterialTheme.colorScheme.onPrimary
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
                                            val resultDate = DateUtils.createMultiDateFromHijri(
                                                displayedYear, displayedMonth, selectedUserClickedDay
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
