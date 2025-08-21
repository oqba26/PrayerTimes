package com.oqba26.prayertimes.widget

import android.app.*
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.os.Build
import android.widget.RemoteViews
import com.oqba26.prayertimes.MainActivity
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.*
import kotlinx.coroutines.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ModernWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // هر دقیقه آپدیت کن، برای ساعت ثانیه‌دار
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ModernWidgetProvider::class.java))
            onUpdate(context, manager, ids)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            scope.launch {
                updateWidget(context, manager, id)
            }
        }
    }

    private suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.modern_widget_layout)

        val now = LocalTime.now()
        val nowText = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

        val date = getCurrentDate()
        val prayers = loadPrayerTimes(context, date)

        // ساعت
        views.setTextViewText(R.id.tv_clock, convertToPersianNumbers(nowText))
        views.setTextColor(R.id.tv_clock, 0xFF004D40.toInt())

        // تاریخ‌ها
        views.setTextViewText(R.id.tv_persian_date, date.getDisplayShamsi())

        val hijri = date.hijriParts()
        val greg = date.gregorianParts()
        val hgText = "${hijri.third} ${hijri.second} ${hijri.first} | ${greg.first} ${greg.second} ${greg.third}"
        views.setTextViewText(R.id.tv_hg_date, hgText)

        // زمان‌های نماز
        views.setTextViewText(R.id.tv_fajr_time, "بامداد: ${prayers["طلوع بامداد"] ?: "--:--"}")
        views.setTextViewText(R.id.tv_sunrise_time, "خورشید: ${prayers["طلوع خورشید"] ?: "--:--"}")
        views.setTextViewText(R.id.tv_dhuhr_time, "ظهر: ${prayers["ظهر"] ?: "--:--"}")
        views.setTextViewText(R.id.tv_asr_time, "عصر: ${prayers["عصر"] ?: "--:--"}")
        views.setTextViewText(R.id.tv_maghrib_time, "غروب: ${prayers["غروب"] ?: "--:--"}")
        views.setTextViewText(R.id.tv_isha_time, "عشاء: ${prayers["عشاء"] ?: "--:--"}")

        // کلیک روی ویجت → باز کردن اپ
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // بروزرسانی نهایی
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    override fun onEnabled(context: Context) {
        val intent = Intent(context, com.oqba26.prayertimes.services.ClockUpdateService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun requestManualWidgetUpdate(context: Context) {
        val intent = Intent(context, ModernWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }

        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, ModernWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }
}