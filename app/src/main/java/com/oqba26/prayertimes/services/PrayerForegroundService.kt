package com.oqba26.prayertimes.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.adhan.AdhanScheduler
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.*
import com.oqba26.prayertimes.viewmodels.dataStore
import com.oqba26.prayertimes.widget.LargeModernWidgetProvider
import com.oqba26.prayertimes.widget.ModernWidgetProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*

class PrayerForegroundService : Service() {

    companion object {
        private const val TAG = "PrayerForegroundService"
        private const val NOTIFICATION_ID = 110

        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_UPDATE = "UPDATE"
        const val ACTION_SCHEDULE_ALARMS = "SCHEDULE_ALARMS"
        const val ACTION_PREV = "PREVIOUS_DAY"
        const val ACTION_NEXT = "NEXT_DAY"
        const val ACTION_TODAY = "TODAY"
        const val ACTION_MIDNIGHT_UPDATE = "MIDNIGHT_UPDATE"

        private var isRunning = false
        var notifSelectedDate: MultiDate? = null

        private fun startServiceInternal(context: Context, action: String) {
            val intent = Intent(context, PrayerForegroundService::class.java).apply {
                this.action = action
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun start(context: Context) {
            if (isRunning) return
            startServiceInternal(context, ACTION_START)
        }

        @Suppress("unused")
        fun stop(context: Context) {
            if (!isRunning) return
            startServiceInternal(context, ACTION_STOP)
        }

        fun update(context: Context, action: String = ACTION_UPDATE) {
            startServiceInternal(context, action)
        }

        fun scheduleAlarms(context: Context) {
            startServiceInternal(context, ACTION_SCHEDULE_ALARMS)
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var isForegroundStarted: Boolean = false

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_TIME_TICK) {
                scope.launch {
                    // Send a standard update to widgets every minute to update the time
                    updateAllWidgets(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        isForegroundStarted = false
        clearIconCache()
        NotificationService.createNotificationChannels(this)
        // Register the receiver for time ticks to update the widget clock
        registerReceiver(timeTickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        isRunning = false
        notifSelectedDate = null
        isForegroundStarted = false
        clearIconCache()
        PrayerAlarmManager.cancelMidnightAlarm(this)
        // Unregister the time tick receiver
        unregisterReceiver(timeTickReceiver)
        Log.d(TAG, "Service Destroyed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand with action: ${intent?.action}")
        val action = intent?.action

        if (!isForegroundStarted) {
            val loadingNotification = createLoadingNotification()
            startForeground(NOTIFICATION_ID, loadingNotification)
            isForegroundStarted = true
        }

        scope.launch {
            try {
                when (action) {
                    null -> updateNotification()
                    ACTION_START -> {
                        PrayerAlarmManager.scheduleMidnightAlarm(this@PrayerForegroundService)
                        updateNotification()
                    }
                    ACTION_STOP -> stopSelf()
                    ACTION_MIDNIGHT_UPDATE -> handleMidnightUpdate()
                    ACTION_UPDATE -> updateNotification()
                    ACTION_SCHEDULE_ALARMS -> scheduleAlarms()
                    ACTION_PREV -> changeDate(-1)
                    ACTION_NEXT -> changeDate(1)
                    ACTION_TODAY -> changeDate(0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "FATAL: Unhandled exception in onStartCommand's coroutine", e)
                showErrorNotification("خطا در پردازش سرویس", e)
            }
        }

        return START_STICKY
    }

    private fun createLoadingNotification(): Notification {
        NotificationService.createNotificationChannels(this)
        return NotificationCompat.Builder(this, NotificationService.DAILY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("در حال آماده‌سازی...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun showErrorNotification(title: String, e: Exception) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val errorNotification = NotificationCompat.Builder(this, NotificationService.DAILY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText("یک خطای غیرمنتظره رخ داد. لطفاً گزارش دهید.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(e.message))
            .setOngoing(true)
            .build()
        nm.notify(NOTIFICATION_ID, errorNotification)
    }

    private suspend fun handleMidnightUpdate() {
        Log.d(TAG, "Handling Midnight Update...")
        notifSelectedDate = DateUtils.getCurrentDate()
        PrayerUtils.loadDetailedPrayerTimes(this, notifSelectedDate!!)
        // Send our custom, allowed action to widgets to notify them of the date change
        updateAllWidgets(ModernWidgetProvider.ACTION_DATE_CHANGED_BY_SERVICE)
        updateNotification()
        PrayerAlarmManager.scheduleMidnightAlarm(this)
        Log.d(TAG, "Midnight Update Finished.")
    }

    private fun updateAllWidgets(action: String) {
        updateWidget(this, ModernWidgetProvider::class.java, action)
        updateWidget(this, LargeModernWidgetProvider::class.java, action)
    }

    private fun updateWidget(context: Context, widgetClass: Class<*>, action: String) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, widgetClass)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isNotEmpty()) {
            val updateIntent = Intent(context, widgetClass).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(updateIntent)
        }
    }

    private suspend fun updateNotification() {
        val date = notifSelectedDate ?: DateUtils.getCurrentDate()
        val times = PrayerUtils.loadDetailedPrayerTimes(this, date)
        val notification = createPrayerNotification(date, times)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private suspend fun changeDate(dayOffset: Int) {
        val currentCal = Calendar.getInstance()
        notifSelectedDate?.let {
            val parts = it.gregorian.split("/")
            currentCal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }

        if (dayOffset == 0) {
            notifSelectedDate = DateUtils.getCurrentDate()
        } else {
            currentCal.add(Calendar.DAY_OF_MONTH, dayOffset)
            notifSelectedDate = DateUtils.convertCalendarToMultiDate(currentCal)
        }
        updateNotification()
    }

    @SuppressLint("ScheduleExactAlarm")
    private suspend fun scheduleAlarms() {
        val today = DateUtils.getCurrentDate()
        val times = PrayerUtils.loadDetailedPrayerTimes(this, today)
        if (times.isNotEmpty()) {
            AdhanScheduler.scheduleFromPrayerMap(this, times)
        }
    }

    private fun pending(
        req: Int,
        cls: Class<*>,
        act: String? = null,
        extraBlock: (Intent.() -> Unit)? = null
    ): PendingIntent {
        val i = Intent(this, cls).apply { action = act }
        extraBlock?.invoke(i)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Service::class.java.isAssignableFrom(cls)) {
            PendingIntent.getService(this, req, i, flags)
        } else {
            PendingIntent.getBroadcast(this, req, i, flags)
        }
    }

    private fun sp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    private fun maxWidth(): Int =
        (resources.displayMetrics.widthPixels - dp(this, 32)).coerceAtLeast(dp(this, 300))

    private fun font(f: String): Typeface {
        val id = when (f.lowercase(Locale.ROOT)) {
            "byekan" -> R.font.byekan
            "estedad" -> R.font.estedad_regular
            "vazirmatn" -> R.font.vazirmatn_regular
            "iraniansans" -> R.font.iraniansans
            "sahel" -> R.font.sahel_bold
            else -> 0
        }
        return if (id != 0) ResourcesCompat.getFont(this, id) ?: Typeface.DEFAULT
        else Typeface.DEFAULT
    }

    private fun findOptimalSP(
        tf: Typeface,
        w: Int,
        txt: String,
        pref: Float = 22f,
        min: Float = 14f
    ): Float {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tf }
        var s = pref
        while (s >= min) {
            p.textSize = sp(s)
            if (p.measureText(txt) <= w) return s
            s -= 0.5f
        }
        return min
    }

    private fun createPrayerTimesBitmap(
        times: Map<String, String>,
        current: String?,
        baseColor: Int,
        highlightColor: Int,
        tf: Typeface,
        usePersian: Boolean,
        use24h: Boolean
    ): Bitmap {
        val prayerOrder = listOf(
            "نماز عشاء" to "عشاء",
            "نماز مغرب" to "مغرب",
            "نماز عصر" to "عصر",
            "نماز ظهر" to "ظهر",
            "نماز صبح" to "صبح"
        )
        val w = dp(this, 320)
        val h = dp(this, 60)
        val bmp = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textAlign = Paint.Align.CENTER
            textSize = sp(14f)
        }
        val colW = w / prayerOrder.size
        val offset = (p.descent() - p.ascent()) / 2 - p.descent()
        prayerOrder.forEachIndexed { i, (displayName, internalName) ->
            val t = DateUtils.formatDisplayTime(
                times[internalName] ?: "--:--",
                use24h,
                usePersian
            )
            val isCurrent = displayName == "نماز $current"
            p.color = if (isCurrent) highlightColor else baseColor
            p.isFakeBoldText = isCurrent
            val x = (i * colW) + (colW / 2f)
            c.drawText(displayName, x, h / 2f - (h / 4f) + offset, p)
            c.drawText(t, x, h / 2f + (h / 4f) + offset, p)
        }
        return bmp
    }

    @SuppressLint("RestrictedApi")
    private suspend fun createPrayerNotification(
        date: MultiDate,
        times: Map<String, String>
    ): Notification {
        val settings = dataStore.data.first()
        val usePersian = settings[booleanPreferencesKey("use_persian_numbers")] ?: true
        val use24h = settings[booleanPreferencesKey("use_24_hour_format")] ?: true
        val fontId = settings[stringPreferencesKey("fontId")] ?: "estedad"
        val dark = settings[booleanPreferencesKey("is_dark_theme")] ?: false

        val tf = font(fontId)

        val (prim, sec, bgRes) = if (dark)
            Triple(Color.WHITE, Color.LTGRAY, R.drawable.widget_background_dark)
        else
            Triple(Color.BLACK, Color.DKGRAY, R.drawable.widget_background_light)

        val highlightPrayer =
            PrayerUtils.getCurrentPrayerForHighlight(times, java.time.LocalTime.now())
        val shamsi =
            "${DateUtils.getWeekDayName(date)} ${DateUtils.formatShamsiLong(date, usePersian)}"
        val other =
            "${DateUtils.formatHijriLong(date, usePersian)} | ${DateUtils.formatGregorianLong(date, usePersian)}"
        val w = maxWidth()

        val collapsed = RemoteViews(packageName, R.layout.notification_collapsed).apply {
            setInt(R.id.collapsed_root_layout, "setBackgroundResource", bgRes)
            setViewVisibility(R.id.tv_shamsi_full, View.GONE)
            setViewVisibility(R.id.tv_other_dates_collapsed, View.GONE)

            val collapsedShamsiSP = findOptimalSP(tf, w, shamsi, pref = 20f, min = 16f)
            setImageViewBitmap(
                R.id.iv_title_collapsed,
                textBitmap(this@PrayerForegroundService, shamsi, tf, collapsedShamsiSP, prim, w)
            )
            setViewVisibility(R.id.iv_title_collapsed, View.VISIBLE)

            setImageViewBitmap(
                R.id.iv_other_collapsed,
                textBitmap(this@PrayerForegroundService, other, tf, 16f, sec, w)
            )
            setViewVisibility(R.id.iv_other_collapsed, View.VISIBLE)
        }

        val exp = RemoteViews(packageName, R.layout.notification_expanded).apply {
            setInt(R.id.expanded_root_layout, "setBackgroundResource", bgRes)
            val expandedShamsiSP = findOptimalSP(tf, w, shamsi, pref = 20f, min = 16f)
            val expandedOtherSP = findOptimalSP(tf, w, other, pref = 16f, min = 12f)
            setImageViewBitmap(
                R.id.iv_title,
                textBitmap(this@PrayerForegroundService, shamsi, tf, expandedShamsiSP, prim, w)
            )
            setViewVisibility(R.id.iv_title, View.VISIBLE)

            setImageViewBitmap(
                R.id.iv_other,
                textBitmap(this@PrayerForegroundService, other, tf, expandedOtherSP, sec, w)
            )
            setViewVisibility(R.id.iv_other, View.VISIBLE)

            val prayBmp = createPrayerTimesBitmap(
                times,
                highlightPrayer,
                if (dark) "#80DEEA".toColorInt() else "#0D47A1".toColorInt(),
                if (dark) "#FFF59D".toColorInt() else "#2E7D32".toColorInt(),
                tf,
                usePersian,
                use24h
            )
            setImageViewBitmap(R.id.iv_prayer_times, prayBmp)
            setViewVisibility(R.id.iv_prayer_times, View.VISIBLE)

            val navCol = if (dark) "#78909C".toColorInt() else "#546E7A".toColorInt()
            val todayCol = if (dark) "#64B5F6".toColorInt() else "#1976D2".toColorInt()
            setImageViewBitmap(
                R.id.iv_prev_day_exp,
                pillButtonBitmap(this@PrayerForegroundService, "روز قبل", tf, navCol, 16, Color.WHITE)
            )
            setImageViewBitmap(
                R.id.iv_next_day_exp,
                pillButtonBitmap(this@PrayerForegroundService, "روز بعد", tf, navCol, 16, Color.WHITE)
            )
            setOnClickPendingIntent(
                R.id.iv_prev_day_exp,
                pending(ACTION_PREV.hashCode(), PrayerForegroundService::class.java, ACTION_PREV)
            )
            setOnClickPendingIntent(
                R.id.iv_next_day_exp,
                pending(ACTION_NEXT.hashCode(), PrayerForegroundService::class.java, ACTION_NEXT)
            )

            val showToday =
                notifSelectedDate?.let { it.shamsi != DateUtils.getCurrentDate().shamsi } ?: false
            if (showToday) {
                setImageViewBitmap(
                    R.id.btn_today,
                    pillButtonBitmap(
                        this@PrayerForegroundService,
                        "بازگشت به امروز",
                        tf,
                        todayCol,
                        20,
                        Color.WHITE
                    )
                )
                setOnClickPendingIntent(
                    R.id.btn_today,
                    pending(ACTION_TODAY.hashCode(), PrayerForegroundService::class.java, ACTION_TODAY)
                )
            } else {
                val placeholder = pillButtonBitmap(
                    this@PrayerForegroundService,
                    "بازگشت به امروز",
                    tf,
                    Color.TRANSPARENT,
                    20,
                    Color.TRANSPARENT
                )
                setImageViewBitmap(R.id.btn_today, placeholder)
            }
            setViewVisibility(R.id.btn_today, if (showToday) View.VISIBLE else View.INVISIBLE)
        }

        val builder = NotificationCompat.Builder(this, NotificationService.DAILY_CHANNEL_ID)
            .setCustomContentView(collapsed)
            .setCustomBigContentView(exp)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setShowWhen(false)

        val iconDay = date.getShamsiParts().third
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setSmallIcon(getDaySmallIconCached(iconDay, dark, usePersian, tf))
        } else {
            builder.setSmallIcon(R.drawable.ic_notification_icon)
        }

        return builder.build()
    }

    private var cachedIconDay = -1
    private var cachedIconColor = Color.BLACK
    private var cachedIconTf: Typeface? = null
    private var cachedSmallIcon: IconCompat? = null

    private fun clearIconCache() {
        cachedIconDay = -1
        cachedIconColor = Color.BLACK
        cachedIconTf = null
        cachedSmallIcon = null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getDaySmallIconCached(
        day: Int,
        isDark: Boolean,
        usePersian: Boolean,
        tf: Typeface
    ): IconCompat {
        val color = if (isDark) Color.WHITE else Color.BLACK
        if (cachedIconDay == day &&
            cachedIconColor == color &&
            cachedIconTf == tf &&
            cachedSmallIcon != null
        ) {
            return cachedSmallIcon!!
        }

        val bmp = createDayIconBitmap(this, day, color, usePersian, tf)

        cachedIconDay = day
        cachedIconColor = color
        cachedIconTf = tf
        cachedSmallIcon = IconCompat.createWithBitmap(bmp)
        return cachedSmallIcon!!
    }
}