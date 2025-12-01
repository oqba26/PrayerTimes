package com.oqba26.prayertimes.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.PrayerTime
import com.oqba26.prayertimes.screens.widgets.MinuteDropdown
import com.oqba26.prayertimes.viewmodels.SettingsViewModel

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
    val minutesBeforeIqama by settingsViewModel.minutesBeforeIqama.collectAsState()
    var iqamaNotificationText by remember { mutableStateOf(settingsViewModel.iqamaNotificationText.value) }

    ExpandableSettingCard(
        title = "اقامه",
        expanded = expanded,
        onToggle = onToggle,
        isDark = isDark,
        lastInteractionTime = lastInteractionTime
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PrayerTime.entries.forEach { prayer ->
                SwitchSettingRow(
                    title = "اعلان اقامه برای ${prayer.displayName}",
                    subtitle = null,
                    checked = prayerIqamaEnabled[prayer] ?: false,
                    onCheckedChange = { isEnabled ->
                        settingsViewModel.updatePrayerIqamaEnabled(prayer, isEnabled)
                        onInteraction()
                    },
                    onInteraction = onInteraction
                )
            }
        }

        AnimatedVisibility(anyIqamaEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MinuteDropdown(
                    label = "زمان اعلان اقامه",
                    subtitle = "اعلان اقامه چند دقیقه قبل از وقت نماز نمایش داده شود",
                    selectedValue = minutesBeforeIqama,
                    range = 0..30,
                    onValueChange = { 
                        settingsViewModel.updateMinutesBeforeIqama(it)
                        onInteraction()
                    },
                    usePersianNumbers = usePersianNumbers,
                    onInteraction = onInteraction
                )
                OutlinedTextField(
                    value = iqamaNotificationText,
                    onValueChange = {
                        iqamaNotificationText = it
                        settingsViewModel.updateIqamaNotificationText(it)
                        onInteraction()
                    },
                    label = { Text("متن اعلان اقامه") },
                    placeholder = { Text("مثال: اکنون زمان اقامه {prayer} است.") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
