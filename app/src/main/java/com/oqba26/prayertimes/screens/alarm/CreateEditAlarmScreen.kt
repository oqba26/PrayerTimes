package com.oqba26.prayertimes.screens.alarm

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.Alarm
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditAlarmScreen(
    existingAlarm: Alarm?,
    onSave: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val timePickerState = rememberTimePickerState(
        initialHour = existingAlarm?.hour ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = existingAlarm?.minute ?: Calendar.getInstance().get(Calendar.MINUTE),
        is24Hour = true
    )

    var label by remember { mutableStateOf(existingAlarm?.label ?: "") }
    var repeatDays by remember { mutableStateOf(existingAlarm?.repeatDays?.toSet() ?: emptySet()) }
    var vibrate by remember { mutableStateOf(existingAlarm?.vibrate ?: true) }
    var ringtoneUri by remember { mutableStateOf(existingAlarm?.ringtoneUri) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                // Take persistable permission to access the file across reboots
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ringtoneUri = it.toString()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = if (existingAlarm == null) R.string.add_alarm else R.string.edit_alarm), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back)) } },
                actions = {
                    if (existingAlarm != null) {
                        IconButton(onClick = { onDelete(existingAlarm) }) {
                            Icon(Icons.Default.Delete, stringResource(id = R.string.delete))
                        }
                    }
                    IconButton(onClick = {
                        val newOrUpdatedAlarm = (existingAlarm ?: Alarm(hour = 0, minute = 0, label = null)).copy(
                            hour = timePickerState.hour,
                            minute = timePickerState.minute,
                            label = label.ifBlank { null },
                            repeatDays = repeatDays.toList().sorted(),
                            vibrate = vibrate,
                            ringtoneUri = ringtoneUri
                        )
                        onSave(newOrUpdatedAlarm)
                    }) { Icon(Icons.Default.Check, stringResource(id = R.string.save)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
            TimePicker(state = timePickerState)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(id = R.string.label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Text(stringResource(id = R.string.repeat), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Right)
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

            Button(onClick = { ringtonePickerLauncher.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) {
                Text("${stringResource(id = R.string.ringtone)}: ${getRingtoneTitle(context, ringtoneUri)}")
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
    
    val uri = Uri.parse(uriString)
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
        } catch (e: Exception) {
            // Could be a SecurityException or others, fallback to last path segment
            title = uri.lastPathSegment ?: context.getString(R.string.unknown_ringtone)
        }
    } else {
        // Fallback for non-content URIs
        title = uri.lastPathSegment ?: context.getString(R.string.unknown_ringtone)
    }
    return title
}
