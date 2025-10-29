package com.oqba26.prayertimes.screens.alarm

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.Alarm
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    onAddAlarm: () -> Unit,
    onAlarmClick: (Alarm) -> Unit,
    onToggleAlarm: (Alarm, Boolean) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { AlarmListTopBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlarm,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_alarm))
            }
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(id = R.string.no_alarms_set),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(alarms, key = { it.id }) {
                    AlarmItem(
                        alarm = it,
                        onClick = { onAlarmClick(it) },
                        onToggle = { enabled -> onToggleAlarm(it, enabled) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmListTopBar() {
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
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.alarms_title),
                color = fg,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmItem(
    alarm: Alarm,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = com.oqba26.prayertimes.utils.DateUtils.convertToPersianNumbers(
                        String.format(java.util.Locale.US, "%02d:%02d", alarm.hour, alarm.minute),
                        enabled = true
                    ),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(alarm.label ?: stringResource(id = R.string.alarm), style = MaterialTheme.typography.bodyMedium)
                Text(getRepeatDaysString(alarm.repeatDays), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun getRepeatDaysString(days: List<Int>): String {
    if (days.isEmpty()) return stringResource(id = R.string.one_time_alarm)
    if (days.size == 7) return stringResource(id = R.string.daily)
    val dayMap = mapOf(
        Calendar.SATURDAY to stringResource(id = R.string.saturday_short),
        Calendar.SUNDAY to stringResource(id = R.string.sunday_short),
        Calendar.MONDAY to stringResource(id = R.string.monday_short),
        Calendar.TUESDAY to stringResource(id = R.string.tuesday_short),
        Calendar.WEDNESDAY to stringResource(id = R.string.wednesday_short),
        Calendar.THURSDAY to stringResource(id = R.string.thursday_short),
        Calendar.FRIDAY to stringResource(id = R.string.friday_short)
    )
    return days.sorted().mapNotNull { dayMap[it] }.joinToString(", ")
}
