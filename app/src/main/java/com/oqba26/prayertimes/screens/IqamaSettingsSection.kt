package com.oqba26.prayertimes.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.PrayerTime
import com.oqba26.prayertimes.screens.widgets.MinuteDropdown
import com.oqba26.prayertimes.viewmodels.SettingsViewModel

@Composable
private fun PrayerIqamaSettingRow(
    prayer: PrayerTime,
    settingsViewModel: SettingsViewModel,
    usePersianNumbers: Boolean,
    onInteraction: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val prayerIqamaEnabled by settingsViewModel.prayerIqamaEnabled.collectAsState()
    val isEnabled = prayerIqamaEnabled[prayer] ?: false
    val minutesBeforeIqama by settingsViewModel.minutesBeforeIqama.collectAsState()

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
                SwitchSettingRow(
                    title = "فعال کردن اعلان اقامه",
                    subtitle = null,
                    checked = isEnabled,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updatePrayerIqamaEnabled(prayer, enabled)
                        // اگر غیرفعال شد، بخش بسته شود
                        if (!enabled) {
                            isExpanded = false
                        }
                        onInteraction()
                    },
                    onInteraction = onInteraction
                )

                AnimatedVisibility(visible = isEnabled) {
                    MinuteDropdown(
                        label = "زمان اعلان اقامه",
                        selectedValue = minutesBeforeIqama,
                        range = 0..30,
                        onValueChange = { minutes ->
                            settingsViewModel.updateMinutesBeforeIqama(minutes)
                            // این آخرین فیلد است، پس بخش بسته شود
                            isExpanded = false
                            onInteraction()
                        },
                        usePersianNumbers = usePersianNumbers,
                        onInteraction = onInteraction
                    )
                }
            }
        }
        HorizontalDivider()
    }
}

@Composable
fun IqamaSettingsSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    settingsViewModel: SettingsViewModel,
    usePersianNumbers: Boolean,
    isDark: Boolean,
    lastInteractionTime: Long,
    onInteraction: () -> Unit
) {
    val prayerIqamaEnabled by settingsViewModel.prayerIqamaEnabled.collectAsState()
    val anyIqamaEnabled = prayerIqamaEnabled.values.any { it }
    var iqamaNotificationText by remember { mutableStateOf(settingsViewModel.iqamaNotificationText.value) }
    val focusManager = LocalFocusManager.current

    // سوئیچ کلی - اگر حداقل یکی فعال باشه، سوئیچ کلی فعاله
    var masterSwitchEnabled by remember { mutableStateOf(anyIqamaEnabled) }

    ExpandableSettingCard(
        title = "اقامه",
        expanded = expanded,
        onToggle = onToggle,
        isDark = isDark,
        lastInteractionTime = lastInteractionTime
    ) {
        // سوئیچ کلی
        SwitchSettingRow(
            title = "فعال کردن اعلان اقامه",
            subtitle = "نمایش اعلان قبل از وقت نماز",
            checked = masterSwitchEnabled,
            onCheckedChange = { enabled ->
                masterSwitchEnabled = enabled
                if (!enabled) {
                    // غیرفعال کردن همه نمازها
                    PrayerTime.entries.forEach { prayer ->
                        settingsViewModel.updatePrayerIqamaEnabled(prayer, false)
                    }
                }
                onInteraction()
            },
            onInteraction = onInteraction
        )

        AnimatedVisibility(visible = masterSwitchEnabled) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    "برای هر نماز، زمان اعلان اقامه را تنظیم کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PrayerTime.entries.forEach { prayer ->
                    PrayerIqamaSettingRow(
                        prayer = prayer,
                        settingsViewModel = settingsViewModel,
                        usePersianNumbers = usePersianNumbers,
                        onInteraction = onInteraction
                    )
                }

                // متن اعلان اقامه (مشترک)
                OutlinedTextField(
                    value = iqamaNotificationText,
                    onValueChange = {
                        iqamaNotificationText = it
                        settingsViewModel.updateIqamaNotificationText(it)
                        onInteraction()
                    },
                    label = { Text("متن اعلان اقامه") },
                    placeholder = { Text("مثال: الان زمان اقامه {prayer} است.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true
                )
            }
        }
    }
}