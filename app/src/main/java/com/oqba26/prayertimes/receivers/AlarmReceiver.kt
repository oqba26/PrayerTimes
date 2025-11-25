package com.oqba26.prayertimes.receivers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.activities.AlarmRingingActivity
import com.oqba26.prayertimes.utils.AlarmUtils

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.oqba26.prayertimes.ACTION_TRIGGER_ALARM"
        const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"
        private const val ALARM_CHANNEL_ID = "alarm_channel"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRIGGER_ALARM) return

        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        if (alarmId == null) {
            Log.e(TAG, "Alarm ID is null, cannot proceed.")
            return
        }

        val alarms = AlarmUtils.loadAlarms(context)
        val alarm = alarms.find { it.id == alarmId }

        if (alarm == null) {
            Log.e(TAG, "Alarm with ID $alarmId not found.")
            return
        }

        if (!alarm.isEnabled) {
            Log.w(TAG, "Alarm with ID $alarmId is disabled. Cancelling.")
            AlarmUtils.cancelAlarm(context, alarm)
            return
        }

        Log.i(TAG, "Triggering alarm: ${alarm.label ?: alarm.id}")
        showAlarmNotification(context, alarm)

        if (alarm.repeatDays.isNotEmpty()) {
            AlarmUtils.scheduleAlarm(context, alarm)
        } else {
            val updatedAlarm = alarm.copy(isEnabled = false)
            val updatedAlarms = alarms.map { if (it.id == alarmId) updatedAlarm else it }
            AlarmUtils.saveAlarms(context, updatedAlarms)
        }
    }

    @SuppressLint("DefaultLocale", "FullScreenIntentPolicy")
    private fun showAlarmNotification(context: Context, alarm: com.oqba26.prayertimes.models.Alarm) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val fullScreenIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, alarm.id.hashCode(), fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(alarm.label ?: "Alarm")
            .setContentText(String.format("%02d:%02d", alarm.hour, alarm.minute))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        notificationManager.notify(alarm.id.hashCode(), notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(ALARM_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    ALARM_CHANNEL_ID,
                    "Alarms",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for alarms"
                    setSound(null, null)
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
