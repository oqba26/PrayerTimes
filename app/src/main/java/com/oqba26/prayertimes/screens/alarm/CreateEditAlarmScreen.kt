package com.oqba26.prayertimes.screens.alarm

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.Alarm
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditAlarmScreen(
    existingAlarm: Alarm?,
    onSave: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onBack: () -> Unit,
    usePersianNumbers: Boolean
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    var selectedHour by remember {
        mutableIntStateOf(existingAlarm?.hour ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }
    var selectedMinute by remember {
        mutableIntStateOf(existingAlarm?.minute ?: Calendar.getInstance().get(Calendar.MINUTE))
    }
    val is24Hour = true

    var label by remember { mutableStateOf(existingAlarm?.label ?: "") }
    var repeatDays by remember { mutableStateOf(existingAlarm?.repeatDays?.toSet() ?: emptySet()) }
    var vibrate by remember { mutableStateOf(existingAlarm?.vibrate ?: true) }
    var ringtoneUri by remember { mutableStateOf(existingAlarm?.ringtoneUri) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                ringtoneUri = it.toString()
            }
        }
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AlarmTopBar(
                title = stringResource(id = if (existingAlarm == null) R.string.add_alarm else R.string.edit_alarm),
                showDelete = existingAlarm != null,
                onBack = onBack,
                onDelete = { showDeleteDialog = true },
                onSave = {
                    val newOrUpdatedAlarm = (existingAlarm ?: Alarm(hour = 0, minute = 0, label = null)).copy(
                        hour = selectedHour,
                        minute = selectedMinute,
                        label = label.ifBlank { null },
                        repeatDays = repeatDays.toList().sorted(),
                        vibrate = vibrate,
                        ringtoneUri = ringtoneUri
                    )
                    onSave(newOrUpdatedAlarm)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PersianAnalogTimePicker(
                hour = selectedHour,
                minute = selectedMinute,
                is24Hour = is24Hour,
                onChange = { h, m ->
                    selectedHour = h
                    selectedMinute = m
                },
                usePersianNumbers = usePersianNumbers
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(id = R.string.label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(id = R.string.repeat),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Right
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                ) {
                    val weekDays = listOf(
                        stringResource(id = R.string.saturday_short) to Calendar.SATURDAY,
                        stringResource(id = R.string.sunday_short) to Calendar.SUNDAY,
                        stringResource(id = R.string.monday_short) to Calendar.MONDAY,
                        stringResource(id = R.string.tuesday_short) to Calendar.TUESDAY,
                        stringResource(id = R.string.wednesday_short) to Calendar.WEDNESDAY,
                        stringResource(id = R.string.thursday_short) to Calendar.THURSDAY,
                        stringResource(id = R.string.friday_short) to Calendar.FRIDAY
                    ).reversed()

                    weekDays.forEach { (dayName, dayConstant) ->
                        FilterChip(
                            selected = repeatDays.contains(dayConstant),
                            onClick = {
                                repeatDays = if (repeatDays.contains(dayConstant)) {
                                    repeatDays - dayConstant
                                } else {
                                    repeatDays + dayConstant
                                }
                            },
                            label = { Text(dayName) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                Spacer(modifier = Modifier.weight(1f))
                Text(stringResource(id = R.string.vibrate), style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { ringtonePickerLauncher.launch("audio/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${stringResource(id = R.string.ringtone)}: ${getRingtoneTitle(context, ringtoneUri)}")
            }
        }
    }

    if (showDeleteDialog) {
        DeleteAlarmConfirmDialog(
            isDark = MaterialTheme.colorScheme.primary == Color(0xFF4F378B),
            onCancel = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                existingAlarm?.let { onDelete(it) }
            }
        )
    }
}

@Composable
private fun PersianAnalogTimePicker(
    hour: Int,
    minute: Int,
    is24Hour: Boolean = true,
    onChange: (Int, Int) -> Unit,
    usePersianNumbers: Boolean,
    @SuppressLint("ModifierParameter")
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf("hour") } // "hour" یا "minute"

    val display = remember(hour, minute, usePersianNumbers) {
        com.oqba26.prayertimes.utils.DateUtils.formatDisplayTime(
            String.format(java.util.Locale.US, "%02d:%02d", hour, minute),
            true,
            usePersianNumbers
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = display, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(selected = mode == "hour", onClick = { mode = "hour" }, label = { Text("ساعت") })
            FilterChip(selected = mode == "minute", onClick = { mode = "minute" }, label = { Text("دقیقه") })
        }

        Spacer(Modifier.height(12.dp))

        if (mode == "hour") {
            val count = if (is24Hour) 24 else 12
            val selectedIndex = if (is24Hour) hour.coerceIn(0, 23) else ((hour - 1).let { if (it < 0) 11 else it } % 12)
            CircularDial(
                count = count,
                selectedIndex = selectedIndex,
                labelForIndex = { idx ->
                    val value = if (is24Hour) idx else (idx + 1)
                    com.oqba26.prayertimes.utils.DateUtils.convertToPersianNumbers(value.toString(), enabled = usePersianNumbers)
                },
                onSelect = { idx ->
                    val value = if (is24Hour) idx else (idx + 1)
                    onChange(value, minute)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally) // وسط‌چین
            )
        } else {
            val count = 60
            val selectedIndex = minute.coerceIn(0, 59)
            CircularDial(
                count = count,
                selectedIndex = selectedIndex,
                labelForIndex = { idx ->
                    val value = idx
                    com.oqba26.prayertimes.utils.DateUtils.convertToPersianNumbers(
                        String.format(java.util.Locale.US, "%02d", value),
                        enabled = usePersianNumbers
                    )
                },
                onSelect = { idx -> onChange(hour, idx) },
                modifier = Modifier.align(Alignment.CenterHorizontally) // وسط‌چین
            )
        }
    }
}

@Suppress("unused", "UNUSED_PARAMETER")
@Composable
private fun CircularDial(
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
    val glowColor = primaryColor.copy(alpha = 0.35f) // 🔹 نور مخملی اطراف عقربه
    val labelTextSizePx = with(LocalDensity.current) { 16.sp.toPx() }

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(modifier = modifier.size(dialSize), contentAlignment = Alignment.Center) {
        // زاویه فعلی (با اصلاح حرکت بین نزدیک‌ترین مسیر)
        val prevAngle = remember { mutableFloatStateOf(selectedIndex * (360f / count)) }
        val targetAngle = selectedIndex * (360f / count)

// اختلاف زاویه
        var delta = targetAngle - prevAngle.floatValue
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f

// 📽️ انیمیشن چرخش بین زوایای دورانی
        val animatedAngle by animateFloatAsState(
            targetValue = prevAngle.floatValue + delta,
            animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
            finishedListener = { prevAngle.floatValue = targetAngle }
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(count) {
                    detectTapGestures { offset ->
                        val idx = angleIndexFromOffset(
                            offset.x, offset.y,
                            size.width.toFloat(), size.height.toFloat(),
                            count, isRtl
                        )
                        onSelect(idx)
                    }
                }
                .pointerInput(count) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val idx = angleIndexFromOffset(
                                offset.x, offset.y,
                                size.width.toFloat(), size.height.toFloat(),
                                count, isRtl
                            )
                            onSelect(idx)
                        },
                        onDrag = { change, _ ->
                            val idx = angleIndexFromOffset(
                                change.position.x, change.position.y,
                                size.width.toFloat(), size.height.toFloat(),
                                count, isRtl
                            )
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

            // زاویه‌ فعلی عقربه (با انیمیشن)
            val selAngleRad = Math.toRadians((animatedAngle - 90f).toDouble())
            val handLen = r * 0.75f
            val handX = cx + (handLen * kotlin.math.cos(selAngleRad)).toFloat()
            val handY = cy + (handLen * kotlin.math.sin(selAngleRad)).toFloat()

            // نور ملایم اطراف عقربه
            drawLine(
                color = glowColor,
                start = Offset(cx, cy),
                end = Offset(handX, handY),
                strokeWidth = 8.dp.toPx(),
                alpha = 0.3f
            )
            // عقربه اصلی
            drawLine(
                color = primaryColor,
                start = Offset(cx, cy),
                end = Offset(handX, handY),
                strokeWidth = 4.dp.toPx()
            )
            drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(cx, cy))

            // اعداد
            for (i in 0 until count) {
                val shouldDraw =
                    count <= 24 || i % 5 == 0 || i == selectedIndex

                if (shouldDraw) {
                    val angleRad = Math.toRadians((i * stepAngle - 90).toDouble())
                    val x = cx + (r * kotlin.math.cos(angleRad)).toFloat()
                    val y = cy + (r * kotlin.math.sin(angleRad)).toFloat()

                    if (i == selectedIndex) {
                        drawCircle(
                            color = highlightBackground,
                            radius = 18.dp.toPx(),
                            center = Offset(x, y)
                        )
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

// ⚙️ جایگزین کد قبلی کن:
// ✅ جایگزین نسخه‌ی قدیمی بشه
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

    // زاویه چرخش
    var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble()))

    // اگر زبان فارسی یا RTL است، جهت زاویه را برعکس کن
    if (isRtl) angle = -angle
    // 🔹 اضافه کن: برعکس کردن کلی زاویه (برای تصحیح جهت لمس)
    angle = -angle

    val degFrom12 = ((90 - angle + 360) % 360).toFloat()
    val stepAngle = 360f / count
    return ((degFrom12 / stepAngle) + 0.5f).toInt() % count
}
@Suppress("unused")

private fun angleIndexFromOffset(x: Float, y: Float, w: Float, h: Float, count: Int): Int {
    val cx = w / 2f
    val cy = h / 2f
    val dx = x - cx
    val dy = y - cy
    val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble()))
    val degFrom12 = ((90 - angle + 360) % 360).toFloat()
    val stepAngle = 360f / count
    return ((degFrom12 / stepAngle) + 0.5f).toInt() % count
}
@Composable
private fun AlarmTopBar(
    title: String,
    showDelete: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit
) {
    val bg = MaterialTheme.colorScheme.primary
    val fg = MaterialTheme.colorScheme.onPrimary

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
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = fg
                )
            }

            Text(
                text = title,
                color = fg,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = fg)
                    }
                }
                IconButton(onClick = onSave) {
                    Icon(Icons.Default.Check, contentDescription = "ذخیره", tint = fg)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAlarmConfirmDialog(
    isDark: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val headerColor = if (isDark) Color(0xFF4F378B) else Color(0xFF0E7490)
    val headerTextColor = if (isDark) Color(0xFFEADDFF) else Color.White

    BasicAlertDialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // هدر تمام‌عرض
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(headerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "حذف زنگ هشدار",
                        color = headerTextColor,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // متن
                Text(
                    text = "آیا از حذف این زنگ مطمئن هستید؟",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Right,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                // اکشن‌ها: «انصراف» چپ (هم‌رنگ هدر)، «حذف» راست (قرمز)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    ) {
                        // انصراف
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.align(Alignment.CenterStart),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = headerColor,
                                contentColor = headerTextColor
                            )
                        ) { Text("انصراف") }

                        // حذف (قرمز)
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) { Text("حذف") }
                    }
                }
            }
        }
    }
}

private fun getRingtoneTitle(context: Context, uriString: String?): String {
    if (uriString == null) {
        // Get title of the default alarm ringtone
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        return RingtoneManager.getRingtone(context, defaultUri).getTitle(context) ?: context.getString(R.string.default_ringtone)
    }
    
    val uri = uriString.toUri()
    var title = context.getString(R.string.unknown_ringtone)
    
    // Try to get title from content resolver for files picked by user
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        title = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (_: Exception) {
            // Could be a SecurityException or others, fallback to last path segment
            title = uri.lastPathSegment ?: context.getString(R.string.unknown_ringtone)
        }
    } else {
        // Fallback for non-content URIs
        title = uri.lastPathSegment ?: context.getString(R.string.unknown_ringtone)
    }
    return title
}
