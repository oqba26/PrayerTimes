package com.oqba26.prayertimes.activities

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.Alarm
import com.oqba26.prayertimes.receivers.AlarmReceiver
import com.oqba26.prayertimes.theme.PrayerTimesTheme
import com.oqba26.prayertimes.utils.AlarmUtils

class AlarmRingingActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOnLockScreen()

        val alarmId = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_ID)
        val alarm = alarmId?.let { AlarmUtils.loadAlarms(this).find { a -> a.id == it } }

        if (alarm == null) {
            finish()
            return
        }

        startAlarmSoundAndVibration(alarm)

        val onDismiss: () -> Unit = {
            stopAlarmSoundAndVibration()
            cancelNotification(alarm)
            finish()
        }

        val onSnooze: () -> Unit = {
            stopAlarmSoundAndVibration()
            cancelNotification(alarm)
            snoozeAlarm(alarm)
            finish()
        }

        setContent {
            PrayerTimesTheme {
                AlarmRingingScreen(
                    alarm = alarm,
                    onDismiss = onDismiss,
                    onSnooze = onSnooze
                )
            }
        }
    }

    private fun cancelNotification(alarm: Alarm) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarm.id.hashCode())
    }

    private fun showOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAlarmSoundAndVibration(alarm: Alarm) {
        if (alarm.vibrate) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
        }

        try {
            val soundUri = alarm.ringtoneUri?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmRingingActivity, soundUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@AlarmRingingActivity, defaultSoundUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private fun stopAlarmSoundAndVibration() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    private fun snoozeAlarm(alarm: Alarm) {
        val snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000 // 10 minutes

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(this, alarm.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Handle missing permission
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSoundAndVibration()
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun AlarmRingingScreen(alarm: Alarm, onDismiss: () -> Unit, onSnooze: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = String.format("%02d:%02d", alarm.hour, alarm.minute), fontSize = 60.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = alarm.label ?: stringResource(id = R.string.alarm), fontSize = 22.sp)

            Spacer(modifier = Modifier.height(64.dp))

            Row {
                Button(onClick = onSnooze) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(text = stringResource(id = R.string.snooze), fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(text = stringResource(id = R.string.dismiss), fontSize = 18.sp)
                }
            }
        }
    }
}
