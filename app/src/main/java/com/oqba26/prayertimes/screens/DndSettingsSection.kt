@file:Suppress("AssignedValueIsNeverRead")

package com.oqba26.prayertimes.screens

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.viewmodels.SettingsViewModel

@Composable
fun PermissionRequestRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
            Button(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("اعطای دسترسی")
            }
        }
    }
}

/**
 * تایم‌پیکر آنالوگ سفارشی
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalogTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    usePersianNumbers: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "انتخاب زمان",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                PersianAnalogTimePicker(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onChange = { h, m ->
                        selectedHour = h
                        selectedMinute = m
                    },
                    usePersianNumbers = usePersianNumbers
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("انصراف")
                    }

                    Button(
                        onClick = { onConfirm(selectedHour, selectedMinute) }
                    ) {
                        Text("تأیید")
                    }
                }
            }
        }
    }
}


/**
 * دیالوگ انتخاب روزهای هفته
 */
@Composable
fun DaySelectorDialog(
    selectedDays: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    val dayNames = listOf(
        0 to "شنبه",
        1 to "یکشنبه",
        2 to "دوشنبه",
        3 to "سه‌شنبه",
        4 to "چهارشنبه",
        5 to "پنجشنبه",
        6 to "جمعه"
    )

    var tempSelectedDays by remember { mutableStateOf(selectedDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "انتخاب روزها",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                dayNames.forEach { (dayIndex, dayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = tempSelectedDays.contains(dayIndex),
                                onClick = {
                                    tempSelectedDays = if (tempSelectedDays.contains(dayIndex)) {
                                        tempSelectedDays - dayIndex
                                    } else {
                                        tempSelectedDays + dayIndex
                                    }
                                },
                                role = Role.Checkbox
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempSelectedDays.contains(dayIndex),
                            onCheckedChange = null
                        )
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("انصراف")
                }
                Button(onClick = { onConfirm(tempSelectedDays) }) {
                    Text("تأیید")
                }
            }
        },
        dismissButton = { }
    )
}

/**
 * تبدیل روزهای انتخاب‌شده به متن خلاصه
 */
fun formatSelectedDays(days: Set<Int>): String {
    if (days.size == 7) return "هر روز"
    if (days.isEmpty()) return "هیچ روزی"

    val shortNames = mapOf(
        0 to "ش",
        1 to "ی",
        2 to "د",
        3 to "س",
        4 to "چ",
        5 to "پ",
        6 to "ج"
    )

    return days.toSortedSet().mapNotNull { shortNames[it] }.joinToString(", ")
}

@SuppressLint("DefaultLocale")
@Composable
fun DndSettingsSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    settingsViewModel: SettingsViewModel,
    usePersianNumbers: Boolean,
    isDark: Boolean,
    lastInteractionTime: Long,
    onInteraction: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun checkDndPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    var hasDndPermission by remember { mutableStateOf(checkDndPermission()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasDndPermission = checkDndPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val dndEnabled by settingsViewModel.dndEnabled.collectAsState()
    val dndStartTime by settingsViewModel.dndStartTime.collectAsState()
    val dndEndTime by settingsViewModel.dndEndTime.collectAsState()
    val dndDays by settingsViewModel.dndDays.collectAsState()

    // State برای دیالوگ‌ها
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDaySelector by remember { mutableStateOf(false) }

    // تایم‌پیکر شروع
    if (showStartTimePicker) {
        val parts = dndStartTime.split(":").map { it.toInt() }
        AnalogTimePickerDialog(
            initialHour = parts[0],
            initialMinute = parts[1],
            usePersianNumbers = usePersianNumbers,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                val formattedTime = String.format("%02d:%02d", hour, minute)
                settingsViewModel.updateDndStartTime(formattedTime)
                showStartTimePicker = false
                onInteraction()
            }
        )
    }

    // تایم‌پیکر پایان
    if (showEndTimePicker) {
        val parts = dndEndTime.split(":").map { it.toInt() }
        AnalogTimePickerDialog(
            initialHour = parts[0],
            initialMinute = parts[1],
            usePersianNumbers = usePersianNumbers,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                val formattedTime = String.format("%02d:%02d", hour, minute)
                settingsViewModel.updateDndEndTime(formattedTime)
                showEndTimePicker = false
                onInteraction()
            }
        )
    }

    // دیالوگ انتخاب روزها
    if (showDaySelector) {
        DaySelectorDialog(
            selectedDays = dndDays,
            onDismiss = { showDaySelector = false },
            onConfirm = { days ->
                settingsViewModel.updateDndDays(days)
                showDaySelector = false
                onInteraction()
            }
        )
    }

    ExpandableSettingCard(
        title = "حالت مزاحم نشوید",
        expanded = expanded,
        onToggle = onToggle,
        isDark = isDark,
        lastInteractionTime = lastInteractionTime
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasDndPermission) {
            PermissionRequestRow(
                title = "دسترسی به حالت مزاحم نشوید",
                subtitle = "برای فعال کردن حالت مزاحم نشوید، این مجوز لازم است.",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        SwitchSettingRow(
            title = "فعال کردن حالت مزاحم نشوید",
            subtitle = "گوشی در بازه زمانی مشخص شده به حالت مزاحم نشوید می‌رود",
            checked = dndEnabled,
            onCheckedChange = { settingsViewModel.updateDndEnabled(it) },
            enabled = hasDndPermission,
            onInteraction = onInteraction
        )

        AnimatedVisibility(visible = dndEnabled && hasDndPermission) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ClickableSettingRow(
                    title = "زمان شروع",
                    value = DateUtils.formatDisplayTime(dndStartTime, true, usePersianNumbers),
                    onClick = { showStartTimePicker = true }
                )
                HorizontalDivider()
                ClickableSettingRow(
                    title = "زمان پایان",
                    value = DateUtils.formatDisplayTime(dndEndTime, true, usePersianNumbers),
                    onClick = { showEndTimePicker = true }
                )
                HorizontalDivider()

                // انتخاب روزهای هفته
                ClickableSettingRow(
                    title = "روزهای فعال",
                    value = formatSelectedDays(dndDays),
                    onClick = { showDaySelector = true }
                )
            }
        }
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
        Text(text = display, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(bottom = 8.dp))

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
