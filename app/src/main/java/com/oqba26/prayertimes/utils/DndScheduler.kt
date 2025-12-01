package com.oqba26.prayertimes.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.oqba26.prayertimes.receivers.DndReceiver
import java.time.LocalTime
import java.util.Calendar

object DndScheduler {

    private const val DND_START_REQ_CODE = 9001
    private const val DND_END_REQ_CODE = 9002

    const val EXTRA_HOUR = "extra_hour"
    const val EXTRA_MINUTE = "extra_minute"

    @RequiresApi(Build.VERSION_CODES.M)
    fun scheduleDnd(context: Context, startTime: LocalTime, endTime: LocalTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startIntent = Intent(context, DndReceiver::class.java).apply {
            action = DndReceiver.ACTION_DND_START
            putExtra(EXTRA_HOUR, startTime.hour)
            putExtra(EXTRA_MINUTE, startTime.minute)
        }
        val startPendingIntent = PendingIntent.getBroadcast(context, DND_START_REQ_CODE, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val endIntent = Intent(context, DndReceiver::class.java).apply {
            action = DndReceiver.ACTION_DND_END
            putExtra(EXTRA_HOUR, endTime.hour)
            putExtra(EXTRA_MINUTE, endTime.minute)
        }
        val endPendingIntent = PendingIntent.getBroadcast(context, DND_END_REQ_CODE, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val now = Calendar.getInstance()

        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startTime.hour)
            set(Calendar.MINUTE, startTime.minute)
            set(Calendar.SECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endTime.hour)
            set(Calendar.MINUTE, endTime.minute)
            set(Calendar.SECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startCalendar.timeInMillis, startPendingIntent)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endCalendar.timeInMillis, endPendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startCalendar.timeInMillis, startPendingIntent)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endCalendar.timeInMillis, endPendingIntent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun scheduleNext(context: Context, action: String, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reqCode = if (action == DndReceiver.ACTION_DND_START) DND_START_REQ_CODE else DND_END_REQ_CODE

        val intent = Intent(context, DndReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1) // Schedule for tomorrow
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancelDnd(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val startIntent = Intent(context, DndReceiver::class.java).apply {
            action = DndReceiver.ACTION_DND_START
        }
        val startPendingIntent = PendingIntent.getBroadcast(context, DND_START_REQ_CODE, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val endIntent = Intent(context, DndReceiver::class.java).apply {
            action = DndReceiver.ACTION_DND_END
        }
        val endPendingIntent = PendingIntent.getBroadcast(context, DND_END_REQ_CODE, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        alarmManager.cancel(startPendingIntent)
        alarmManager.cancel(endPendingIntent)
    }
}
