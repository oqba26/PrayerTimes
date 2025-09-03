package com.oqba26.prayertimes.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PrayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ModernWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_CLOCK_TICK_UPDATE = "com.oqba26.prayertimes.ACTION_CLOCK_TICK_UPDATE"
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            scope.launch {
                val today = getTodayMultiDate()
                val times = PrayerUtils.loadPrayerTimes(context, today)
                updateAppWidgetWithTimes(context, appWidgetManager, appWidgetId, today, times)
            }
        }
        scheduleNextUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CLOCK_TICK_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, ModernWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            allWidgetIds.forEach { appWidgetId ->
                scope.launch {
                    val today = getTodayMultiDate()
                    val times = PrayerUtils.loadPrayerTimes(context, today)
                    updateAppWidgetWithTimes(context, appWidgetManager, appWidgetId, today, times)
                }
            }
            scheduleNextUpdate(context)
        }
    }

    private fun updateAppWidgetWithTimes(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        today: MultiDate,
        times: Map<String, String>
    ) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val isLarge = minWidth >= 200 || minHeight >= 200

        val shamsiParts = today.getShamsiParts()
        val hijriParts = today.hijriParts()
        val gregParts = today.gregorianParts()
        val weekDay = DateUtils.getWeekDayName(today)

        val persianDate =
            "$weekDay ${DateUtils.convertToPersianNumbers(shamsiParts.third.toString())} " +
                    "${DateUtils.getPersianMonthName(shamsiParts.second)} " +
                    DateUtils.convertToPersianNumbers(shamsiParts.first.toString())

        val hijriDate = "${hijriParts.third} ${hijriParts.second} ${hijriParts.first}"
        val gregDate = "${gregParts.first} ${gregParts.second} ${gregParts.third}"
        val hijriGregLine = "$hijriDate | $gregDate"

        val views = if (isLarge) {
            val remoteViews = RemoteViews(context.packageName, R.layout.modern_widget_layout_large)

            val time = SimpleDateFormat("HH:mm", Locale("fa")).format(Date())
            remoteViews.setTextViewText(R.id.tv_clock, DateUtils.convertToPersianNumbers(time))

            // تاریخ‌ها
            remoteViews.setTextViewText(R.id.tv_persian_date, persianDate)
            remoteViews.setTextViewText(R.id.tv_hg_date, hijriGregLine)

            // هایلایت نماز بعدی
            val nextPrayer = PrayerUtils.getCurrentPrayerForHighlight(times, java.time.LocalTime.now())
            val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
            val prayersLine = order.joinToString(" | ") { name ->
                val raw = DateUtils.convertToPersianNumbers(times[name] ?: "--:--")
                if (name == nextPrayer) "<b><font color='#2E7D32'>$name: $raw</font></b>"
                else "<font color='#0D47A1'>$name: $raw</font>"
            }

            remoteViews.setTextViewText(
                R.id.tv_prayers_line,
                android.text.Html.fromHtml(prayersLine, android.text.Html.FROM_HTML_MODE_LEGACY)
            )
            remoteViews
        } else {
            RemoteViews(context.packageName, R.layout.modern_widget_layout).apply {
                val time = SimpleDateFormat("HH:mm", Locale("fa")).format(Date())
                setTextViewText(R.id.tv_widget_time, DateUtils.convertToPersianNumbers(time))
                setTextViewText(R.id.tv_widget_date_shamsi, persianDate)
                setTextViewText(R.id.tv_widget_date_hijri_gregorian, hijriGregLine)
            }
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun scheduleNextUpdate(context: Context) {
        val intent = Intent(context, ModernWidgetProvider::class.java).apply {
            action = ACTION_CLOCK_TICK_UPDATE
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervalMillis = 60_000L
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        intervalMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("ModernWidgetProvider", "scheduleNextUpdate error: ${e.message}")
        }
    }

    private fun getTodayMultiDate(): MultiDate {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val gregorian = String.format("%d/%02d/%02d", year, month, day)
        val shamsi = DateUtils.gregorianToJalali(year, month, day)
        val hijri = DateUtils.convertToHijri(year, month, day)
        return MultiDate(shamsi, hijri, gregorian)
    }
}