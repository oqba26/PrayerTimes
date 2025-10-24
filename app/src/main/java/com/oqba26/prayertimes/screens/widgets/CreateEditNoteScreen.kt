package com.oqba26.prayertimes.screens.widgets

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils
import java.util.Calendar
import java.util.Locale

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditNoteScreen(
    selectedDate: MultiDate,
    initialNoteContent: String,
    isNotificationEnabledInitial: Boolean,
    reminderTimeInitial: Long?,
    isDark: Boolean,
    usePersianNumbers: Boolean,
    use24HourFormat: Boolean,
    onBackClick: () -> Unit,
    onSaveClick: (noteDate: MultiDate, noteContent: String, notificationEnabled: Boolean, reminderTimeMillis: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var noteContent by rememberSaveable { mutableStateOf(initialNoteContent) }
    var isNotificationEnabled by rememberSaveable { mutableStateOf(isNotificationEnabledInitial) }
    var currentNoteDate by rememberSaveable(selectedDate) { mutableStateOf(selectedDate) }

    var selectedReminderDate by rememberSaveable(reminderTimeInitial) {
        mutableStateOf(reminderTimeInitial?.let { millis ->
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            DateUtils.convertCalendarToMultiDate(cal)
        })
    }
    var selectedReminderHour by rememberSaveable(reminderTimeInitial) {
        mutableIntStateOf(reminderTimeInitial?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) } ?: 12)
    }
    var selectedReminderMinute by rememberSaveable(reminderTimeInitial) {
        mutableIntStateOf(reminderTimeInitial?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE) } ?: 0)
    }

    var combinedReminderTimeMillis by rememberSaveable { mutableStateOf(reminderTimeInitial) }

    LaunchedEffect(selectedReminderDate, selectedReminderHour, selectedReminderMinute, isNotificationEnabled) {
        if (isNotificationEnabled && selectedReminderDate != null) {
            currentNoteDate = selectedReminderDate!!
            val cal = Calendar.getInstance()
            val (jy, jm, jd) = selectedReminderDate!!.getShamsiParts()
            val (gy, gm, gd) = DateUtils.convertPersianToGregorian(jy, jm, jd)
            cal.set(gy, gm - 1, gd, selectedReminderHour, selectedReminderMinute, 0)
            cal.set(Calendar.MILLISECOND, 0)
            combinedReminderTimeMillis = cal.timeInMillis
        } else {
            combinedReminderTimeMillis = null
            currentNoteDate = selectedDate
        }
    }

    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    if (showReminderDatePicker) {
        ShamsiDatePicker(
            initialDate = selectedReminderDate ?: currentNoteDate,
            onDateSelected = { dateFromPicker ->
                selectedReminderDate = dateFromPicker
                showReminderDatePicker = false
                showReminderTimePicker = true
            },
            onDismiss = { showReminderDatePicker = false },
            isDarkTheme = isDark,
            usePersianNumbers = usePersianNumbers,
            use24HourFormat = use24HourFormat
        )
    }

    if (showReminderTimePicker) {
        ComposeTimePickerDialog(
            initialHour = selectedReminderHour,
            initialMinute = selectedReminderMinute,
            is24Hour = use24HourFormat,
            usePersianNumbers = usePersianNumbers,
            onConfirm = { h, m ->
                selectedReminderHour = h
                selectedReminderMinute = m
                showReminderTimePicker = false
            },
            onDismiss = { showReminderTimePicker = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("یادداشت جدید", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } },
                actions = {
                    IconButton(onClick = { onSaveClick(currentNoteDate, noteContent, isNotificationEnabled, combinedReminderTimeMillis) }) {
                        Icon(Icons.Filled.Check, "ذخیره یادداشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "یادداشت برای تاریخ: ${DateUtils.convertToPersianNumbers(currentNoteDate.shamsi, usePersianNumbers)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("متن یادداشت", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                minLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("فعال سازی یادآوری", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Switch(
                    checked = isNotificationEnabled,
                    onCheckedChange = {
                        isNotificationEnabled = it
                        if (!it) {
                            selectedReminderDate = null
                        } else if (selectedReminderDate == null) {
                            selectedReminderDate = currentNoteDate
                            selectedReminderHour = 12
                            selectedReminderMinute = 0
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val reminderButtonText = if (isNotificationEnabled && selectedReminderDate != null) {
                val rawTime = String.format(Locale.US, "%02d:%02d", selectedReminderHour, selectedReminderMinute)
                val displayTime = DateUtils.formatDisplayTime(rawTime, use24HourFormat, usePersianNumbers)
                "یادآوری در: ${DateUtils.convertToPersianNumbers(selectedReminderDate!!.shamsi, usePersianNumbers)} ساعت $displayTime"
            } else {
                "تنظیم یادآوری"
            }

            Button(
                onClick = { if (isNotificationEnabled) showReminderDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = isNotificationEnabled
            ) {
                Text(DateUtils.convertToPersianNumbers(reminderButtonText, usePersianNumbers))
            }

            if (isNotificationEnabled && selectedReminderDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val clearReminderContentColor = if (isDark) Color(0xFFF2B8B5) else MaterialTheme.colorScheme.error
                val clearReminderBorderColor = if (isDark) Color(0xFFF2B8B5).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline

                OutlinedButton(
                    onClick = { selectedReminderDate = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = clearReminderContentColor),
                    border = BorderStroke(1.dp, clearReminderBorderColor)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن یادآوری")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("پاک کردن یادآوری")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    usePersianNumbers: Boolean,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = is24Hour)
    val headerText = remember(state.hour, state.minute, is24Hour, usePersianNumbers) {
        DateUtils.formatDisplayTime(String.format(Locale.US, "%02d:%02d", state.hour, state.minute), is24Hour, usePersianNumbers)
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = headerText, style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), textAlign = TextAlign.Center)
                TimePicker(state = state, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(50)) { Text("انصراف") }
                    Button(onClick = { onConfirm(state.hour, state.minute) }, shape = RoundedCornerShape(50)) { Text("تایید") }
                }
            }
        }
    }
}