package com.oqba26.prayertimes.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.PrayerTime
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.screens.widgets.MinuteDropdown
import com.oqba26.prayertimes.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerAdhanSettingRow(
    prayer: PrayerTime,
    settingsViewModel: SettingsViewModel,
    usePersianNumbers: Boolean,
    adhanSounds: List<Pair<String, String>>,
    onInteraction: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val currentSound by settingsViewModel.getAdhanSoundFlow(prayer.id).collectAsState("off")
    val minutesBefore by settingsViewModel.prayerMinutesBeforeAdhan.collectAsState()
    val currentMinutes = minutesBefore[prayer] ?: 0

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
                AdhanSoundDropdown(
                    label = "صدای اذان",
                    selectedValue = currentSound,
                    onValueChange = { sound ->
                        settingsViewModel.setAdhanSound(prayer.id, sound)
                        if (sound != "off") {
                            isExpanded = false
                        }
                        onInteraction()
                    },
                    options = adhanSounds,
                    onInteraction = onInteraction
                )

                AnimatedVisibility(visible = currentSound != "off") {
                    MinuteDropdown(
                        label = "زمان پخش",
                        selectedValue = currentMinutes,
                        range = 0..60,
                        onValueChange = { minutes ->
                            settingsViewModel.updatePrayerMinutesBeforeAdhan(prayer, minutes)
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
fun AdhanSettingsSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    settingsViewModel: SettingsViewModel,
    usePersianNumbers: Boolean,
    isDark: Boolean,
    lastInteractionTime: Long,
    onInteraction: () -> Unit
) {
    val allAdhansSet by settingsViewModel.allAdhansSet.collectAsState(initial = false)

    LaunchedEffect(allAdhansSet) {
        if (allAdhansSet && expanded) {
            onToggle()
        }
    }

    ExpandableSettingCard(
        title = "اذان",
        expanded = expanded,
        onToggle = onToggle,
        isDark = isDark,
        lastInteractionTime = lastInteractionTime,
        content = {
            val adhanLabels = stringArrayResource(R.array.adhan_sound_labels)
            val adhanIds = stringArrayResource(R.array.adhan_sound_ids)
            val adhanSounds = remember(adhanLabels, adhanIds) {
                adhanIds.zip(adhanLabels).toList()
            }

            Text(
                "برای هر نماز، صدای اذان و زمان پخش را انتخاب کنید.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            PrayerTime.entries.forEach { prayer ->
                PrayerAdhanSettingRow(
                    prayer = prayer,
                    settingsViewModel = settingsViewModel,
                    usePersianNumbers = usePersianNumbers,
                    adhanSounds = adhanSounds,
                    onInteraction = onInteraction
                )
            }
        }
    )
}
