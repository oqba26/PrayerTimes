@file:Suppress("UnusedVariable", "AssignedValueIsNeverRead")

package com.oqba26.prayertimes.screens.widgets

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

@Composable
private fun CreateEditNoteTopBar(
    isEditMode: Boolean,
    isDarkThemeActive: Boolean,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val bg = if (isDarkThemeActive) Color(0xFF4F378B) else Color(0xFF0E7490)
    val fg = Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = fg)
            }

            Text(
                text = if (isEditMode) "ویرایش یادداشت" else "یادداشت جدید",
                color = fg,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditMode) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف یادداشت", tint = fg)
                    }
                }
                IconButton(onClick = onSaveClick) {
                    Icon(Icons.Filled.Check, contentDescription = "ذخیره یادداشت", tint = fg)
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
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
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditMode = initialNoteContent.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            isDark = isDark,
            onConfirm = {
                showDeleteDialog = false
                onDeleteClick()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    var noteContent by rememberSaveable { mutableStateOf(initialNoteContent) }
    var isNotificationEnabled by rememberSaveable { mutableStateOf(isNotificationEnabledInitial) }

    var selectedReminderDate by rememberSaveable(reminderTimeInitial) {
        mutableStateOf(reminderTimeInitial?.let { millis ->
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            DateUtils.convertCalendarToMultiDate(cal)
        })
    }
    var selectedReminderHour by rememberSaveable(reminderTimeInitial) {
        mutableIntStateOf(
            reminderTimeInitial?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) } ?: 12
        )
    }
    var selectedReminderMinute by rememberSaveable(reminderTimeInitial) {
        mutableIntStateOf(
            reminderTimeInitial?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE) } ?: 0
        )
    }

    var combinedReminderTimeMillis by rememberSaveable { mutableStateOf(reminderTimeInitial) }

    LaunchedEffect(selectedReminderDate, selectedReminderHour, selectedReminderMinute, isNotificationEnabled) {
        combinedReminderTimeMillis =
            if (isNotificationEnabled && selectedReminderDate != null) {
                val (jy, jm, jd) = selectedReminderDate!!.getShamsiParts()
                val (gy, gm, gd) = DateUtils.convertPersianToGregorian(jy, jm, jd)
                Calendar.getInstance().apply {
                    set(gy, gm - 1, gd, selectedReminderHour, selectedReminderMinute, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            } else null
    }

    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    if (showReminderDatePicker) {
        ShamsiDatePicker(
            initialDate = selectedReminderDate ?: selectedDate,
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
        PersianAnalogTimePickerDialog(
            initialHour = selectedReminderHour,
            initialMinute = selectedReminderMinute,
            is24Hour = use24HourFormat,
            usePersianNumbers = usePersianNumbers,
            isDark = isDark,
            onConfirm = { h, m ->
                selectedReminderHour = h
                selectedReminderMinute = m
                showReminderTimePicker = false
            },
            onDismiss = { showReminderTimePicker = false }
        )
    }

    Scaffold(
        topBar = {
            CreateEditNoteTopBar(
                isEditMode = isEditMode,
                isDarkThemeActive = isDark,
                onBackClick = onBackClick,
                onSaveClick = { onSaveClick(selectedDate, noteContent, isNotificationEnabled, combinedReminderTimeMillis) },
                onDeleteClick = { showDeleteDialog = true }
            )
        },
        contentWindowInsets = WindowInsets(0),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "یادداشت برای تاریخ: ${DateUtils.formatShamsiLong(selectedDate, usePersianNumbers)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                            selectedReminderDate = selectedDate
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
                "یادآوری در: ${DateUtils.formatShamsiLong(selectedReminderDate!!, usePersianNumbers)} ساعت $displayTime"
            } else {
                "تنظیم یادآوری"
            }

            Button(
                onClick = { if (isNotificationEnabled) showReminderDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = isNotificationEnabled
            ) {
                Text(reminderButtonText)
            }

            if (isNotificationEnabled && selectedReminderDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val clearReminderContentColor = MaterialTheme.colorScheme.error
                val clearReminderBorderColor = MaterialTheme.colorScheme.outline

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

@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersianAnalogTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    usePersianNumbers: Boolean,
    isDark: Boolean,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val headerColor = if (isDark) Color(0xFF4F378B) else Color(0xFF0E7490)
    val headerTextColor = if (isDark) Color(0xFFEADDFF) else Color.White

    var hour by remember { mutableIntStateOf(if (is24Hour) initialHour else ((initialHour - 1).let { if (it < 0) 11 else it } % 12) + 1) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    var isPm by remember { mutableStateOf(!is24Hour && initialHour >= 12) }

    val headerText = remember(hour, minute, is24Hour, isPm, usePersianNumbers) {
        val h24 = if (is24Hour) hour else {
            when {
                isPm && hour in 1..11 -> hour + 12
                !isPm && hour == 12 -> 0
                else -> hour
            }
        }
        DateUtils.formatDisplayTime(String.format(Locale.US, "%02d:%02d", h24, minute), is24Hour, usePersianNumbers)
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(headerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("انتخاب ساعت", color = headerTextColor, style = MaterialTheme.typography.titleLarge)
                }

                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)
                )

                var mode by remember { mutableStateOf("hour") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(selected = mode == "hour", onClick = { mode = "hour" }, label = { Text("ساعت") })
                    FilterChip(selected = mode == "minute", onClick = { mode = "minute" }, label = { Text("دقیقه") })
                }

                if (!is24Hour) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(selected = !isPm, onClick = { isPm = false }, label = { Text("ق.ظ") })
                        FilterChip(selected = isPm, onClick = { isPm = true }, label = { Text("ب.ظ") })
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (mode == "hour") {
                    CircularDialNote(
                        count = if (is24Hour) 24 else 12,
                        selectedIndex = if (is24Hour) hour.coerceIn(0, 23) else ((hour - 1).let { if (it < 0) 11 else it } % 12),
                        labelForIndex = { idx -> DateUtils.convertToPersianNumbers((if (is24Hour) idx else (idx + 1)).toString(), usePersianNumbers) },
                        onSelect = { idx -> hour = if (is24Hour) idx else (idx + 1) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    CircularDialNote(
                        count = 60,
                        selectedIndex = minute.coerceIn(0, 59),
                        labelForIndex = { idx -> DateUtils.convertToPersianNumbers(String.format(Locale.US, "%02d", idx), usePersianNumbers) },
                        onSelect = { idx -> minute = idx },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterStart),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                        ) { Text("انصراف") }

                        Button(
                            onClick = {
                                val finalHour = if (is24Hour) hour else {
                                    when {
                                        isPm && hour in 1..11 -> hour + 12
                                        !isPm && hour == 12 -> 0
                                        else -> hour
                                    }
                                }
                                onConfirm(finalHour, minute)
                            },
                            modifier = Modifier.align(Alignment.CenterEnd),
                            colors = ButtonDefaults.buttonColors(containerColor = headerColor, contentColor = headerTextColor)
                        ) { Text("تایید") }
                    }
                }
            }
        }
    }
}

@Suppress("unused", "UNUSED_PARAMETER")
@Composable
private fun CircularDialNote(
    count: Int,
    selectedIndex: Int,
    labelForIndex: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dialSize: Dp = 260.dp,
    ringRadiusFraction: Float = 0.8f
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlightBackground = primaryColor.copy(alpha = 0.22f)
    val glowColor = primaryColor.copy(alpha = 0.35f)
    val labelTextSizePx = with(LocalDensity.current) { 16.sp.toPx() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(modifier = modifier.size(dialSize), contentAlignment = Alignment.Center) {
        val prevAngle = remember { mutableFloatStateOf(selectedIndex * (360f / count)) }
        val targetAngle = selectedIndex * (360f / count)

        var delta = targetAngle - prevAngle.floatValue
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f

        val animatedAngle by animateFloatAsState(
            targetValue = prevAngle.floatValue + delta,
            animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
            finishedListener = { prevAngle.floatValue = targetAngle }
        )

        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(count) {
                detectTapGestures { offset ->
                    val idx = angleIndexFromOffset(offset.x, offset.y, size.width.toFloat(), size.height.toFloat(), count, isRtl)
                    onSelect(idx)
                }
            }.pointerInput(count) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val idx = angleIndexFromOffset(offset.x, offset.y, size.width.toFloat(), size.height.toFloat(), count, isRtl)
                        onSelect(idx)
                    },
                    onDrag = { change, _ ->
                        val idx = angleIndexFromOffset(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat(), count, isRtl)
                        onSelect(idx)
                    }
                )
            }
        ) {
            val canvasSize = this.size
            val r = canvasSize.minDimension / 2f * ringRadiusFraction

            drawCircle(color = surfaceVariantColor, radius = r + 24.dp.toPx())

            val stepAngle = 360f / count
            val cx = canvasSize.width / 2f
            val cy = canvasSize.height / 2f

            val selAngleRad = Math.toRadians((animatedAngle - 90f).toDouble())
            val handLen = r * 0.75f
            val handX = cx + (handLen * kotlin.math.cos(selAngleRad)).toFloat()
            val handY = cy + (handLen * kotlin.math.sin(selAngleRad)).toFloat()

            drawLine(
                color = glowColor,
                start = Offset(cx, cy),
                end = Offset(handX, handY),
                strokeWidth = 8.dp.toPx(),
                alpha = 0.3f
            )
            drawLine(
                color = primaryColor,
                start = Offset(cx, cy),
                end = Offset(handX, handY),
                strokeWidth = 4.dp.toPx()
            )
            drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(cx, cy))

            for (i in 0 until count) {
                val shouldDraw = count <= 24 || i % 5 == 0 || i == selectedIndex

                if (shouldDraw) {
                    val angleRad = Math.toRadians((i * stepAngle - 90).toDouble())
                    val x = cx + (r * kotlin.math.cos(angleRad)).toFloat()
                    val y = cy + (r * kotlin.math.sin(angleRad)).toFloat()

                    if (i == selectedIndex) {
                        drawCircle(color = highlightBackground, radius = 18.dp.toPx(), center = Offset(x, y))
                    }

                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = labelTextSizePx
                        color = (if (i == selectedIndex) primaryColor else onSurfaceVariantColor).toArgb()
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    drawContext.canvas.nativeCanvas.drawText(labelForIndex(i), x, y + 6.dp.toPx(), paint)
                }
            }
        }
    }
}

private fun angleIndexFromOffset(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    count: Int,
    isRtl: Boolean
): Int {
    val cx = w / 2f
    val cy = h / 2f
    val dx = x - cx
    val dy = y - cy

    var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble()))
    if (isRtl) angle = -angle

    angle = -angle

    val degFrom12 = ((90 - angle + 360) % 360).toFloat()
    val stepAngle = 360f / count
    return ((degFrom12 / stepAngle) + 0.5f).toInt() % count
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteConfirmationDialog(
    isDark: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val topBarLight = Color(0xFF0E7490)
    val topBarDark  = Color(0xFF4F378B)
    val headerColor = if (isDark) topBarDark else topBarLight
    val headerTextColor = if (isDark) Color(0xFFEADDFF) else Color.White

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(headerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "حذف یادداشت",
                        color = headerTextColor,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text(
                    text = "آیا از حذف این یادداشت مطمئن هستید؟",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterStart),
                            colors = ButtonDefaults.buttonColors(containerColor = headerColor, contentColor = headerTextColor)
                        ) { Text("انصراف") }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                        ) { Text("حذف") }
                    }
                }
            }
        }
    }
}

@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersianTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    usePersianNumbers: Boolean,
    isDark: Boolean,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val headerColor = if (isDark) Color(0xFF4F378B) else Color(0xFF0E7490)
    val headerTextColor = if (isDark) Color(0xFFEADDFF) else Color.White

    var hour by remember { mutableIntStateOf(if (is24Hour) initialHour else ((initialHour - 1).let { if (it < 0) 11 else it } % 12) + 1) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    var isPm by remember { mutableStateOf(!is24Hour && initialHour >= 12) }

    val headerText = remember(hour, minute, is24Hour, isPm, usePersianNumbers) {
        val h24 = if (is24Hour) hour else {
            when {
                isPm && hour in 1..11 -> hour + 12
                !isPm && hour == 12 -> 0
                else -> hour
            }
        }
        DateUtils.formatDisplayTime(String.format(Locale.US, "%02d:%02d", h24, minute), is24Hour, usePersianNumbers)
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).background(headerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("انتخاب ساعت", color = headerTextColor, style = MaterialTheme.typography.titleLarge)
                }

                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelNumberPicker(value = hour, range = if (is24Hour) 0..23 else 1..12, onValueChange = { hour = it }, twoDigits = is24Hour, usePersianNumbers = usePersianNumbers, modifier = Modifier.weight(1f))
                    WheelNumberPicker(value = minute, range = 0..59, onValueChange = { minute = it }, twoDigits = true, usePersianNumbers = usePersianNumbers, modifier = Modifier.weight(1f))
                }

                if (!is24Hour) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(selected = !isPm, onClick = { isPm = false }, label = { Text("ق.ظ") })
                        FilterChip(selected = isPm, onClick = { isPm = true }, label = { Text("ب.ظ") })
                    }
                    Spacer(Modifier.height(4.dp))
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    ) {
                        Button(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) { Text("انصراف") }
                        Button(
                            onClick = {
                                val finalHour = if (is24Hour) hour else {
                                    when {
                                        isPm && hour in 1..11 -> hour + 12
                                        !isPm && hour == 12 -> 0
                                        else -> hour
                                    }
                                }
                                onConfirm(finalHour, minute)
                            },
                            modifier = Modifier.align(Alignment.CenterEnd),
                            colors = ButtonDefaults.buttonColors(containerColor = headerColor, contentColor = headerTextColor)
                        ) { Text("تایید") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelNumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    twoDigits: Boolean,
    usePersianNumbers: Boolean,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 44.dp
) {
    val items = remember(range) { range.toList() }
    val half = remember(visibleCount) { visibleCount / 2 }
    val decorated = remember(items, half) { List(half) { null } + items.map { it } + List(half) { null } }

    val itemHeightPx = with(LocalDensity.current) { itemHeight.roundToPx() }
    val initialIndex = remember(value, items, half) { items.indexOf(value).coerceAtLeast(0) }

    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { inProgress ->
            if (!inProgress) {
                val first = state.firstVisibleItemIndex
                val offset = state.firstVisibleItemScrollOffset
                val centerIndexApprox = first + half + if (offset >= itemHeightPx / 2) 1 else 0
                var center = centerIndexApprox
                while (center < decorated.size && decorated[center] == null) center++
                if (center >= decorated.size) {
                    center = centerIndexApprox
                    while (center >= 0 && decorated[center] == null) center--
                }
                val targetFirst = (center - half).coerceIn(0, decorated.size - 1)
                state.animateScrollToItem(targetFirst)
            }
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { Pair(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) }.collect {
            val first = state.firstVisibleItemIndex
            val offset = state.firstVisibleItemScrollOffset
            val centerIndex = (first + half + if (offset >= itemHeightPx / 2) 1 else 0).coerceIn(0, decorated.lastIndex)
            val sel = decorated[centerIndex]
            if (sel != null && sel != value) onValueChange(sel)
        }
    }

    val boxHeight = itemHeight * visibleCount

    Box(modifier = modifier.height(boxHeight)) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = true
        ) {
            items(decorated.size) { idx ->
                val v = decorated[idx]
                val isSelected = v != null && v == value
                Box(modifier = Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (v != null) {
                        val raw = if (twoDigits) String.format(Locale.US, "%02d", v) else v.toString()
                        val txt = DateUtils.convertToPersianNumbers(raw, usePersianNumbers)
                        Text(
                            text = txt,
                            style = if (isSelected) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        val lineColor = MaterialTheme.colorScheme.outlineVariant
        HorizontalDivider(modifier = Modifier.align(Alignment.Center).offset(y = -(itemHeight / 2)).fillMaxWidth(0.9f), thickness = 1.dp, color = lineColor)
        HorizontalDivider(modifier = Modifier.align(Alignment.Center).offset(y = itemHeight / 2).fillMaxWidth(0.9f), thickness = 1.dp, color = lineColor)
    }
}

@Suppress("unused")
@Composable
private fun TimeStepper(
    label: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    twoDigits: Boolean,
    usePersianNumbers: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onIncrement) {
            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "افزایش")
        }

        val raw = if (twoDigits) String.format(Locale.US, "%02d", value) else value.toString()
        val txt = DateUtils.convertToPersianNumbers(raw, usePersianNumbers)
        Text(text = txt, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center)

        FilledTonalIconButton(onClick = onDecrement) {
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "کاهش")
        }

        Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
