package com.oqba26.prayertimes.screens

import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.viewmodels.SettingsViewModel
import java.util.Locale


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
                val startTimeParts = dndStartTime.split(":").map { it.toInt() }
                val endTimeParts = dndEndTime.split(":").map { it.toInt() }

                val startTimePicker = TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val formattedTime = String.format(Locale.US, "%02d:%02d", hour, minute)
                        settingsViewModel.updateDndStartTime(formattedTime)
                    },
                    startTimeParts[0], startTimeParts[1], true
                )

                val endTimePicker = TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val formattedTime = String.format(Locale.US, "%02d:%02d", hour, minute)
                        settingsViewModel.updateDndEndTime(formattedTime)
                    },
                    endTimeParts[0], endTimeParts[1], true
                )

                ClickableSettingRow(
                    title = "زمان شروع",
                    value = DateUtils.formatDisplayTime(dndStartTime, true, usePersianNumbers),
                    onClick = { startTimePicker.show() }
                )
                HorizontalDivider()
                ClickableSettingRow(
                    title = "زمان پایان",
                    value = DateUtils.formatDisplayTime(dndEndTime, true, usePersianNumbers),
                    onClick = { endTimePicker.show() }
                )
            }
        }
    }
}