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
    val iqamaEnabled by settingsViewModel.iqamaEnabled.collectAsState()
    val minutesBeforeIqama by settingsViewModel.minutesBeforeIqama.collectAsState()
    var iqamaNotificationText by remember { mutableStateOf(settingsViewModel.iqamaNotificationText.value) }

    ExpandableSettingCard(
        title = "اقامه",
        expanded = expanded,
        onToggle = onToggle,
        isDark = isDark,
        lastInteractionTime = lastInteractionTime
    ) {
        SwitchSettingRow(
            title = "فعال کردن اعلان اقامه",
            subtitle = "یک اعلان برای یادآوری اقامه نماز دریافت کنید",
            checked = iqamaEnabled,
            onCheckedChange = { isEnabled ->
                settingsViewModel.updateIqamaEnabled(isEnabled)
                if (isEnabled && expanded) {
                    onToggle()
                }
                onInteraction()
            },
            onInteraction = onInteraction
        )
        AnimatedVisibility(iqamaEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MinuteDropdown(
                    label = "زمان اعلان اقامه",
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
                    placeholder = { Text("متنی که میخواهید برای اعلان اقامه به شما نمایش داده شود را اینجا وارد کنید") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
