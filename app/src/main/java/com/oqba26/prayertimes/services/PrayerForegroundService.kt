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
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
import java.time.LocalTime
import java.util.*

class PrayerForegroundService : Service() {

    companion object {
        private const val TAG = "PrayerForegroundService"
        private const val NOTIFICATION_ID = 110

        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_UPDATE = "UPDATE"
        const val ACTION_SCHEDULE_ALARMS = "SCHEDULE_ALARMS"
        const val ACTION_SCHEDULE_DND = "SCHEDULE_DND"
        const val ACTION_PREV = "PREVIOUS_DAY"
        const val ACTION_NEXT = "NEXT_DAY"
        const val ACTION_TODAY = "TODAY"
        const val ACTION_TOGGLE_DATE_FORMAT = "TOGGLE_DATE_FORMAT"
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

        fun scheduleDnd(context: Context) {
            startServiceInternal(context, ACTION_SCHEDULE_DND)
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var isForegroundStarted: Boolean = false

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_TIME_TICK) {
                scope.launch {
                    updateAllWidgets(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    updateNotification()
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
        unregisterReceiver(timeTickReceiver)
        Log.d(TAG, "Service Destroyed")
    }

    @SuppressLint("ObsoleteSdkInt")
    @RequiresApi(Build.VERSION_CODES.M)
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PrayerAlarmManager.scheduleMidnightAlarm(this@PrayerForegroundService)
                        }
                        updateNotification()
                    }
                    ACTION_STOP -> stopSelf()
                    ACTION_MIDNIGHT_UPDATE -> handleMidnightUpdate()
                    ACTION_UPDATE -> updateNotification()
                    ACTION_SCHEDULE_ALARMS -> scheduleAlarms()
                    ACTION_SCHEDULE_DND -> scheduleDndAlarms()
                    ACTION_PREV -> changeDate(-1)
                    ACTION_NEXT -> changeDate(1)
                    ACTION_TODAY -> changeDate(0)
                    ACTION_TOGGLE_DATE_FORMAT -> toggleDateFormat()
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

    private fun showErrorNotification(@Suppress("SameParameterValue") title: String, e: Exception) {
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
        updateAllWidgets(ModernWidgetProvider.ACTION_DATE_CHANGED_BY_SERVICE)
        updateNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PrayerAlarmManager.scheduleMidnightAlarm(this)
        }
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
        val now = LocalTime.now()

        val (generalTimes, specificTimes) = PrayerUtils.loadSeparatedPrayerTimes(this, date)

        val settings = dataStore.data.first()
        val showGeneralTimes = settings[booleanPreferencesKey("show_general_times")] ?: true
        val showSpecificTimes = settings[booleanPreferencesKey("show_specific_times")] ?: true

        if ((showGeneralTimes && generalTimes.isEmpty()) || (showSpecificTimes && specificTimes.isEmpty())) {
            Log.e(TAG, "One or both prayer time maps are empty for $date. Aborting notification update.")
            showErrorNotification("خطا در بارگذاری اوقات شرعی", Exception("اطلاعات برای تاریخ $date یافت نشد."))
            return
        }

        // Create a temporary, complete map and order for highlight calculation
        val highlightOrder = listOf("صبح", "طلوع خورشید", "ظهر", "عصر", "مغرب", "عشاء")
        val highlightTimes = specificTimes.toMutableMap().apply {
            generalTimes["طلوع خورشید"]?.let { put("طلوع خورشید", it) }
        }

        // Calculate the highlighted prayer using the complete temporary map
        val calculatedHighlight = PrayerUtils.computeHighlightPrayer(now, highlightTimes, highlightOrder)
        var specificHighlightName = calculatedHighlight

        // Special case: When it's sunrise, highlight Dhuhr in the specific (5-times) list
        if (calculatedHighlight == "طلوع خورشید") {
            specificHighlightName = "ظهر"
        }

        // Convert the specific name (e.g., "صبح") back to a general name (e.g., "طلوع بامداد") for UI matching
        val generalHighlightName = PrayerUtils.getGeneralPrayerName(calculatedHighlight)


        val notification = createPrayerNotification(
            date,
            generalTimes,
            specificTimes,
            generalHighlightName,
            specificHighlightName,
            showGeneralTimes,
            showSpecificTimes
        )
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

    private suspend fun toggleDateFormat() {
        val key = booleanPreferencesKey("use_numeric_date_format_notification")
        dataStore.edit { settings ->
            settings[key] = !(settings[key] ?: false)
        }
        updateNotification()
    }

    @SuppressLint("ScheduleExactAlarm")
    private suspend fun scheduleAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val today = DateUtils.getCurrentDate()
            val times = PrayerUtils.loadDetailedPrayerTimes(this, today)
            if (times.isNotEmpty()) {
                AdhanScheduler.scheduleFromPrayerMap(this, times)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun scheduleDndAlarms() {
        val settings = dataStore.data.first()
        val dndEnabled = settings[booleanPreferencesKey("dnd_enabled")] ?: false
        val dndStartTime = settings[stringPreferencesKey("dnd_start_time")] ?: "22:00"
        val dndEndTime = settings[stringPreferencesKey("dnd_end_time")] ?: "07:00"

        @Suppress("RemoveRedundantQualifierName") val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            if (dndEnabled) {
                val start = LocalTime.parse(dndStartTime)
                val end = LocalTime.parse(dndEndTime)
                DndScheduler.scheduleDnd(this, start, end)
            } else {
                DndScheduler.cancelDnd(this)
            }
        }
    }

    private fun pending(
        req: Int,
        cls: Class<*>,        act: String? = null,
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

    @SuppressLint("RestrictedApi")
    private suspend fun createPrayerNotification(
        date: MultiDate,
        generalTimes: Map<String, String>,
        specificTimes: Map<String, String>,
        generalHighlight: String?,
        specificHighlight: String?,
        showGeneralTimes: Boolean,
        showSpecificTimes: Boolean
    ): Notification {
        val settings = dataStore.data.first()
        val usePersian = settings[booleanPreferencesKey("use_persian_numbers")] ?: true
        val useNumericDate = settings[booleanPreferencesKey("use_numeric_date_format_notification")] ?: false
        val use24h = settings[booleanPreferencesKey("use_24_hour_format")] ?: true
        val fontId = settings[stringPreferencesKey("fontId")] ?: "estedad"
        val dark = settings[booleanPreferencesKey("is_dark_theme")] ?: false

        val tf = font(fontId)

        @Suppress("VariableNeverRead") val primaryTextColor: Int
        val secondaryTextColor: Int
        val prayerTimeColor: Int
        val prayerHighlightColor: Int
        val persianDateColor: Int

        if (dark) {
            @Suppress("AssignedValueIsNeverRead")
            primaryTextColor = Color.WHITE
            secondaryTextColor = Color.LTGRAY
            prayerTimeColor = "#80DEEA".toColorInt()
            prayerHighlightColor = "#FFF59D".toColorInt()
            persianDateColor = prayerTimeColor
        } else {
            @Suppress("AssignedValueIsNeverRead")
            primaryTextColor = Color.BLACK
            secondaryTextColor = Color.DKGRAY
            prayerTimeColor = "#0D47A1".toColorInt()
            prayerHighlightColor = "#2E7D32".toColorInt()
            persianDateColor = prayerTimeColor
        }

        val bgRes = if (dark) R.drawable.widget_background_dark else R.drawable.widget_background_light

        val weekDay = DateUtils.getWeekDayName(date)
        val shamsiStr = if (useNumericDate) DateUtils.formatShamsiShort(date, usePersian) else DateUtils.formatShamsiLong(date, usePersian)
        val hijriStr = if (useNumericDate) DateUtils.formatHijriShort(date, usePersian) else DateUtils.formatHijriLong(date, usePersian)
        val gregStr = if (useNumericDate) DateUtils.formatGregorianShort(date, usePersian) else DateUtils.formatGregorianLong(date, usePersian)

        val shamsi = "$weekDay $shamsiStr"
        val other = "$gregStr | $hijriStr"
        val w = maxWidth()

        val toggleDateFormatIntent = pending(
            ACTION_TOGGLE_DATE_FORMAT.hashCode(),
            PrayerForegroundService::class.java,
            ACTION_TOGGLE_DATE_FORMAT
        )

        val collapsed = RemoteViews(packageName, R.layout.notification_collapsed)
        collapsed.setInt(R.id.collapsed_root_layout, "setBackgroundResource", bgRes)

        val collapsedShamsiSP = findOptimalSP(tf, w, shamsi, pref = 18f)
        collapsed.setImageViewBitmap(
            R.id.iv_title_collapsed,
            textBitmap(this, shamsi, tf, collapsedShamsiSP, persianDateColor, w)
        )
        collapsed.setViewVisibility(R.id.iv_title_collapsed, View.VISIBLE)
        collapsed.setOnClickPendingIntent(R.id.iv_title_collapsed, toggleDateFormatIntent)


        collapsed.setImageViewBitmap(
            R.id.iv_other_collapsed,
            textBitmap(this, other, tf, 14f, secondaryTextColor, w)
        )
        collapsed.setViewVisibility(R.id.iv_other_collapsed, View.VISIBLE)
        collapsed.setOnClickPendingIntent(R.id.iv_other_collapsed, toggleDateFormatIntent)


        val exp = RemoteViews(packageName, R.layout.notification_expanded)
        exp.setInt(R.id.expanded_root_layout, "setBackgroundResource", bgRes)
        val expandedShamsiSP = findOptimalSP(tf, w, shamsi, pref = 20f)
        val expandedOtherSP = findOptimalSP(tf, w, other, pref = 16f)
        exp.setImageViewBitmap(
            R.id.iv_title,
            textBitmap(this, shamsi, tf, expandedShamsiSP, persianDateColor, w)
        )
        exp.setViewVisibility(R.id.iv_title, View.VISIBLE)
        exp.setOnClickPendingIntent(R.id.iv_title, toggleDateFormatIntent)

        exp.setImageViewBitmap(
            R.id.iv_other,
            textBitmap(this, other, tf, expandedOtherSP, secondaryTextColor, w)
        )
        exp.setViewVisibility(R.id.iv_other, View.VISIBLE)
        exp.setOnClickPendingIntent(R.id.iv_other, toggleDateFormatIntent)

        if (showGeneralTimes) {
            val generalBmp = createPrayerTimesBitmapWithHighlight(
                context = this,
                prayerTimes = generalTimes,
                currentPrayerName = generalHighlight,
                tf = tf,
                maxWidthPx = w,
                baseColor = prayerTimeColor,
                highlightColor = prayerHighlightColor,
                use24HourFormat = use24h,
                usePersianNumbers = usePersian
            )
            exp.setImageViewBitmap(R.id.iv_general_times, generalBmp)
            exp.setViewVisibility(R.id.iv_general_times, View.VISIBLE)
        } else {
            exp.setViewVisibility(R.id.iv_general_times, View.GONE)
        }

        if (showGeneralTimes && showSpecificTimes) {
            exp.setViewVisibility(R.id.divider1, View.VISIBLE)
        } else {
            exp.setViewVisibility(R.id.divider1, View.GONE)
        }

        if (showSpecificTimes) {
            val prayBmp = createFivePrayerTimesBitmap(
                context = this,
                prayerTimes = specificTimes,
                currentPrayerName = specificHighlight,
                tf = tf,
                maxWidthPx = w,
                baseColor = prayerTimeColor,
                highlightColor = prayerHighlightColor,
                use24HourFormat = use24h,
                usePersianNumbers = usePersian
            )
            exp.setImageViewBitmap(R.id.iv_prayer_times, prayBmp)
            exp.setViewVisibility(R.id.iv_prayer_times, View.VISIBLE)
        } else {
            exp.setViewVisibility(R.id.iv_prayer_times, View.GONE)
        }

        val navCol = if (dark) "#78909C".toColorInt() else "#546E7A".toColorInt()
        val todayCol = if (dark) "#64B5F6".toColorInt() else "#1976D2".toColorInt()
        val navTextColor = "#FFFFFF".toColorInt()

        exp.setImageViewBitmap(
            R.id.iv_prev_day_exp,
            pillButtonBitmap(this, "روز قبل", tf, navCol, 16, navTextColor)
        )
        exp.setImageViewBitmap(
            R.id.iv_next_day_exp,
            pillButtonBitmap(this, "روز بعد", tf, navCol, 16, navTextColor)
        )

        exp.setImageViewBitmap(
            R.id.btn_today,
            pillButtonBitmap(this, "بازگشت به امروز", tf, todayCol, 20, navTextColor)
        )

        exp.setOnClickPendingIntent(
            R.id.iv_prev_day_exp,
            pending(ACTION_PREV.hashCode(), PrayerForegroundService::class.java, ACTION_PREV)
        )
        exp.setOnClickPendingIntent(
            R.id.iv_next_day_exp,
            pending(ACTION_NEXT.hashCode(), PrayerForegroundService::class.java, ACTION_NEXT)
        )
        exp.setOnClickPendingIntent(
            R.id.btn_today,
            pending(ACTION_TODAY.hashCode(), PrayerForegroundService::class.java, ACTION_TODAY)
        )

        val showToday = notifSelectedDate?.let { it.shamsi != DateUtils.getCurrentDate().shamsi } ?: false
        exp.setViewVisibility(R.id.btn_today, if (showToday) View.VISIBLE else View.INVISIBLE)

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
    private var cachedIconColor = "#000000".toColorInt()
    private var cachedIconTf: Typeface? = null
    private var cachedSmallIcon: IconCompat? = null

    private fun clearIconCache() {
        cachedIconDay = -1
        cachedIconColor = "#000000".toColorInt()
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