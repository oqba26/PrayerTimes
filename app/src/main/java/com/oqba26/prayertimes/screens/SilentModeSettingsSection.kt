package com.oqba26.prayertimes.screens

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oqba26.prayertimes.PrayerTime
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.viewmodels.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SilentStatusDropdown(
    selectedValue: Boolean,
    onValueChange: (Boolean) -> Unit,
    onInteraction: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(true to "فعال", false to "غیرفعال")
    val selectedLabel = if (selectedValue) "فعال" else "غیرفعال"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = it
            onInteraction()
        },
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selectedLabel,
            onValueChange = {},
            label = { Text("وضعیت سکوت") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.medium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(value)
                        onInteraction()
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SilentMinuteDropdown(
    label: String,
    selectedValue: Int,
    range: IntProgression,
    onValueChange: (Int) -> Unit,
    usePersianNumbers: Boolean,
    timingLabel: String,
    zeroLabel: String,
    onInteraction: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = if (selectedValue == 0) zeroLabel else "${DateUtils.convertToPersianNumbers(selectedValue.toString(), usePersianNumbers)} دقیقه $timingLabel"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = it
            onInteraction()
        },
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selectedLabel,
            onValueChange = { },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.medium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            range.forEach { minute ->
                DropdownMenuItem(
                    text = { Text(if (minute == 0) zeroLabel else "${DateUtils.convertToPersianNumbers(minute.toString(), usePersianNumbers)} دقیقه $timingLabel") },
                    onClick = {
                        onValueChange(minute)
                        onInteraction()
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
private fun PrayerSilentSettingRow(
    prayer: PrayerTime,
    settingsViewModel: SettingsViewModel,
    usePersianNumbers: Boolean,
    onInteraction: () -> Unit
) {
    val prayerSilentEnabled by settingsViewModel.prayerSilentEnabled.collectAsState()
    val isEnabled = prayerSilentEnabled[prayer] ?: false

    val prayerMinutesBefore by settingsViewModel.prayerMinutesBefore.collectAsState()
    val before = prayerMinutesBefore[prayer] ?: 10

    val prayerMinutesAfter by settingsViewModel.prayerMinutesAfter.collectAsState()
    val after = prayerMinutesAfter[prayer] ?: 10

    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isExpanded = !isExpanded
                    onInteraction()
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(prayer.displayName, style = MaterialTheme.typography.bodyLarge)
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "بستن" else "باز کردن"
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SilentStatusDropdown(
                    selectedValue = isEnabled,
                    onValueChange = { enabled ->
                        settingsViewModel.updatePrayerSilentEnabled(prayer, enabled)
                        if (enabled) {
                            isExpanded = false
                        }
                        onInteraction()
                    },
                    onInteraction = onInteraction
                )

                AnimatedVisibility(visible = isEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SilentMinuteDropdown(
                            label = "شروع سکوت (دقیقه قبل از نماز)",
                            selectedValue = before,
                            range = 0..60,
                            onValueChange = { v ->
                                settingsViewModel.updatePrayerMinutesBefore(
                                    prayer,
                                    v
                                )
                                onInteraction()
                            },
                            usePersianNumbers = usePersianNumbers,
                            timingLabel = "قبل",
                            zeroLabel = "همزمان با نماز",
                            onInteraction = onInteraction
                        )
                        SilentMinuteDropdown(
                            label = "پایان سکوت (دقیقه بعد از نماز)",
                            selectedValue = after,
                            range = 0..60,
                            onValueChange = { v ->
                                settingsViewModel.updatePrayerMinutesAfter(
                                    prayer,
                                    v
                                )
                                onInteraction()
                            },
                            usePersianNumbers = usePersianNumbers,
                            timingLabel = "بعد",
                            zeroLabel = "بلافاصله بعد از اتمام",
                            onInteraction = onInteraction
                        )
                    }
                }
            }
        }
        HorizontalDivider()
    }
}


@Composable
fun SilentModeSettingsSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    settingsViewModel: SettingsViewModel,
    usePersianNumbers: Boolean,
    hasExactAlarmPermission: Boolean,
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

    val allSilentEnabled by settingsViewModel.allSilentEnabled.collectAsState(initial = false)

    LaunchedEffect(allSilentEnabled) {
        if (allSilentEnabled && expanded) {
            onToggle()
        }
    }


    val autoSilentEnabled by settingsViewModel.autoSilentEnabled.collectAsState()

    ExpandableSettingCard(
        title = "سکوت خودکار",
        expanded = expanded,
        onToggle = onToggle,
        isDark = isDark,
        lastInteractionTime = lastInteractionTime
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasDndPermission) {
            @Suppress("KotlinConstantConditions")
            PermissionRequestRow(
                title = "مجوز حالت ویبره",
                subtitle = "برای فعال کردن حالت ویبره در زمان نماز، این مجوز لازم است.",
                permissionGranted = hasDndPermission
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        SwitchSettingRow(
            title = "فعال کردن سکوت خودکار",
            subtitle = "گوشی در زمان‌های مشخص شده برای نمازها به حالت ویبره می‌رود",
            checked = autoSilentEnabled,
            onCheckedChange = { 
                settingsViewModel.updateAutoSilentEnabled(it)
                onInteraction()
            },
            enabled = hasExactAlarmPermission && hasDndPermission,
            onInteraction = onInteraction
        )

        AnimatedVisibility(visible = autoSilentEnabled && hasExactAlarmPermission && hasDndPermission) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PrayerTime.entries.forEach { prayer ->
                    PrayerSilentSettingRow(
                        prayer = prayer,
                        settingsViewModel = settingsViewModel,
                        usePersianNumbers = usePersianNumbers,
                        onInteraction = onInteraction
                    )
                }
            }
        }
    }
}
