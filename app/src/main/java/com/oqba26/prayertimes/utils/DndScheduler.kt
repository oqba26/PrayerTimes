package com.oqba26.prayertimes.utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oqba26.prayertimes.receivers.DndReceiver
import com.oqba26.prayertimes.viewmodels.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.util.Calendar

object DndScheduler {

    private const val TAG = "DndScheduler"
    private const val DND_START_REQ_CODE = 9001
    private const val DND_END_REQ_CODE = 9002

    const val EXTRA_HOUR = "extra_hour"
    const val EXTRA_MINUTE = "extra_minute"

    private fun getSelectedDays(context: Context): Set<Int> {
        return runBlocking {
            val settings = context.dataStore.data.first()
            val daysString = settings[stringPreferencesKey("dnd_days")] ?: "0,1,2,3,4,5,6"
            daysString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    private fun getTodayIndex(): Int {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    private fun isTodaySelected(context: Context): Boolean {
        val selectedDays = getSelectedDays(context)
        return selectedDays.contains(getTodayIndex())
    }

    /**
     * بررسی می‌کنه که آیا الان در بازه زمانی DND هستیم یا نه
     */
    private fun isCurrentlyInDndPeriod(startTime: LocalTime, endTime: LocalTime): Boolean {
        val now = LocalTime.now()

        return if (startTime.isBefore(endTime)) {
            // بازه عادی: مثلاً ۰۸:۰۰ تا ۱۲:۰۰
            now.isAfter(startTime) && now.isBefore(endTime)
        } else {
            // بازه شبانه: مثلاً ۲۲:۰۰ تا ۰۷:۰۰
            now.isAfter(startTime) || now.isBefore(endTime)
        }
    }

    /**
     * فعال کردن فوری حالت DND
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun enableDndNow(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            Log.d(TAG, "DND enabled immediately")
        } else {
            Log.w(TAG, "Cannot enable DND - permission not granted")
        }
    }

    /**
     * غیرفعال کردن فوری حالت DND
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun disableDndNow(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            Log.d(TAG, "DND disabled immediately")
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun scheduleDnd(context: Context, startTime: LocalTime, endTime: LocalTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // بررسی مجوز در Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarms - permission not granted")
            return
        }

        // بررسی اینکه امروز در لیست روزهای انتخاب‌شده هست یا نه
        if (!isTodaySelected(context)) {
            Log.d(TAG, "Today is not in selected days, skipping DND schedule")
            cancelDnd(context)
            disableDndNow(context)
            return
        }

        // ✅ اگه الان در بازه زمانی هستیم، فوری DND رو فعال کن
        if (isCurrentlyInDndPeriod(startTime, endTime)) {
            Log.d(TAG, "Currently in DND period, enabling DND now")
            enableDndNow(context)
        }

        // تنظیم آلارم‌ها برای شروع و پایان
        val startIntent = Intent(context, DndReceiver::class.java).apply {
            action = DndReceiver.ACTION_DND_START
            putExtra(EXTRA_HOUR, startTime.hour)
            putExtra(EXTRA_MINUTE, startTime.minute)
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context, DND_START_REQ_CODE, startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = Intent(context, DndReceiver::class.java).apply {
            action = DndReceiver.ACTION_DND_END
            putExtra(EXTRA_HOUR, endTime.hour)
            putExtra(EXTRA_MINUTE, endTime.minute)
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            context, DND_END_REQ_CODE, endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = Calendar.getInstance()

        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startTime.hour)
            set(Calendar.MINUTE, startTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endTime.hour)
            set(Calendar.MINUTE, endTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                startCalendar.timeInMillis,
                startPendingIntent
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                endCalendar.timeInMillis,
                endPendingIntent
            )
            Log.d(TAG, "DND alarms scheduled: start=${startTime}, end=${endTime}")
            Log.d(TAG, "Start alarm at: ${startCalendar.time}")
            Log.d(TAG, "End alarm at: ${endCalendar.time}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling DND", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun scheduleNext(context: Context, action: String, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // بررسی مجوز در Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule next alarm - permission not granted")
            return
        }

        // پیدا کردن روز بعدی که در لیست انتخاب‌شده هست
        val selectedDays = getSelectedDays(context)
        if (selectedDays.isEmpty()) {
            Log.d(TAG, "No days selected, not scheduling next DND")
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }

        // پیدا کردن نزدیک‌ترین روز انتخاب‌شده
        var daysChecked = 0
        while (daysChecked < 7) {
            val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> 0
                Calendar.SUNDAY -> 1
                Calendar.MONDAY -> 2
                Calendar.TUESDAY -> 3
                Calendar.WEDNESDAY -> 4
                Calendar.THURSDAY -> 5
                Calendar.FRIDAY -> 6
                else -> 0
            }
            if (selectedDays.contains(dayOfWeek)) {
                break
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            daysChecked++
        }

        if (daysChecked >= 7) {
            Log.w(TAG, "No valid day found in next 7 days")
            return
        }

        val reqCode = if (action == DndReceiver.ACTION_DND_START) DND_START_REQ_CODE else DND_END_REQ_CODE

        val intent = Intent(context, DndReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Next DND scheduled for ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling next DND", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun cancelDnd(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startIntent = Intent(context, DndReceiver::class.java).apply {
            action = DndReceiver.ACTION_DND_START
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context, DND_START_REQ_CODE, startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = Intent(context, DndReceiver::class.java).apply {
            action = DndReceiver.ACTION_DND_END
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            context, DND_END_REQ_CODE, endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(startPendingIntent)
        alarmManager.cancel(endPendingIntent)

        // غیرفعال کردن فوری DND
        disableDndNow(context)

        Log.d(TAG, "DND alarms cancelled and DND disabled")
    }
}