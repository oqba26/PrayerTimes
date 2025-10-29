package com.oqba26.prayertimes.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oqba26.prayertimes.models.Alarm
import com.oqba26.prayertimes.receivers.AlarmReceiver
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.Calendar

object AlarmUtils {

    private const val ALARMS_FILE_NAME = "alarms.json"
    private const val TAG = "AlarmUtils"

    fun loadAlarms(context: Context): MutableList<Alarm> {
        val file = File(context.filesDir, ALARMS_FILE_NAME)
        if (!file.exists()) return mutableListOf()

        return try {
            context.openFileInput(ALARMS_FILE_NAME).use { stream ->
                val type = object : TypeToken<MutableList<Alarm>>() {}.type
                Gson().fromJson(InputStreamReader(stream), type) ?: mutableListOf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading alarms", e)
            mutableListOf()
        }
    }

    fun saveAlarms(context: Context, alarms: List<Alarm>) {
        try {
            FileOutputStream(File(context.filesDir, ALARMS_FILE_NAME)).use {
                it.write(Gson().toJson(alarms).toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving alarms", e)
        }
    }

    fun scheduleAlarm(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm) // Ensure it's cancelled if disabled
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextTriggerMillis = calculateNextTrigger(alarm)

        if (nextTriggerMillis == null) {
            Log.w(TAG, "Could not calculate next trigger for alarm ${alarm.id}, not scheduling.")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use modern, permission-respecting scheduling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarm. App needs SCHEDULE_EXACT_ALARM permission.")
            // Optionally, navigate user to settings or show a dialog
            return
        }

        try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(nextTriggerMillis, pendingIntent), pendingIntent)
            Log.i(TAG, "Alarm ${alarm.id} scheduled for ${DateUtils.formatTimeMillis(nextTriggerMillis)}")
        } catch (se: SecurityException) {
            Log.e(TAG, "Failed to schedule alarm due to security exception.", se)
        }
    }

    fun cancelAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Cancelled alarm ${alarm.id}")
        }
    }

    fun calculateNextTrigger(alarm: Alarm): Long? {
        val now = Calendar.getInstance()
        val nextTrigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If no repeat days, it's a one-time alarm for the next upcoming time
        if (alarm.repeatDays.isEmpty()) {
            if (nextTrigger.before(now)) {
                nextTrigger.add(Calendar.DAY_OF_YEAR, 1)
            }
            return nextTrigger.timeInMillis
        }

        // For repeating alarms, find the next valid day
        val today = now.get(Calendar.DAY_OF_WEEK)
        val sortedDays = alarm.repeatDays.sorted()

        // Find the next day in the current week
        for (day in sortedDays) {
            if (day > today || (day == today && nextTrigger.after(now))) {
                nextTrigger.set(Calendar.DAY_OF_WEEK, day)
                return nextTrigger.timeInMillis
            }
        }

        // If not found, it must be for the first valid day of next week
        val firstDayNextWeek = sortedDays.first()
        nextTrigger.set(Calendar.DAY_OF_WEEK, firstDayNextWeek)
        nextTrigger.add(Calendar.WEEK_OF_YEAR, 1)
        return nextTrigger.timeInMillis
    }
}