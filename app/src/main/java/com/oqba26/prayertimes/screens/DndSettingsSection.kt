@file:Suppress("AssignedValueIsNeverRead")

package com.oqba26.prayertimes.screens

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
 * تایم‌پیکر دیجیتال سفارشی
 */
@SuppressLint("DefaultLocale")
@Composable
fun DigitalTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    usePersianNumbers: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    var hourText by remember { mutableStateOf(String.format("%02d", initialHour)) }
    var minuteText by remember { mutableStateOf(String.format("%02d", initialMinute)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "انتخاب زمان",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // دقیقه
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { it.isDigit() }) {
                                minuteText = value
                                value.toIntOrNull()?.let {
                                    if (it in 0..59) minute = it
                                }
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = { Text("دقیقه", style = MaterialTheme.typography.bodySmall) }
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // ساعت
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { it.isDigit() }) {
                                hourText = value
                                value.toIntOrNull()?.let {
                                    if (it in 0..23) hour = it
                                }
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = { Text("ساعت", style = MaterialTheme.typography.bodySmall) }
                    )
                }

                // نمایش زمان انتخاب‌شده
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = DateUtils.formatDisplayTime(
                        String.format("%02d:%02d", hour, minute),
                        true,
                        usePersianNumbers
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // اعتبارسنجی نهایی
                            val finalHour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: hour
                            val finalMinute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: minute
                            onConfirm(finalHour, finalMinute)
                        }
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
        title = { Text("انتخاب روزها") },
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelectedDays) }) {
                Text("تأیید")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
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

    return days.toSortedSet().mapNotNull { shortNames[it] }.joinToString("، ")
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
        DigitalTimePickerDialog(
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
        DigitalTimePickerDialog(
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