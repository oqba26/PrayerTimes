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
import android.graphics.*
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

        private var isRunning = false
        var notifSelectedDate: MultiDate? = null

        fun start(context: Context) {
            if (isRunning) return
            val intent = Intent(context, PrayerForegroundService::class.java).apply { action = ACTION_START }
            context.startService(intent)
        }

        @Suppress("unused")
        fun stop(context: Context) {
            val intent = Intent(context, PrayerForegroundService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }

        @Suppress("unused")
        fun update(context: Context) {
            val intent = Intent(context, PrayerForegroundService::class.java).apply { action = ACTION_UPDATE }
            context.startService(intent)
        }

        fun scheduleAlarms(context: Context) {
            val intent = Intent(context, PrayerForegroundService::class.java).apply { action = ACTION_SCHEDULE_ALARMS }
            context.startService(intent)
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_TIME_TICK) {
                updateWidget(context, ModernWidgetProvider::class.java)
                updateWidget(context, LargeModernWidgetProvider::class.java)
                // Send update command to the service itself
                val updateIntent = Intent(context, PrayerForegroundService::class.java).apply {
                    action = ACTION_UPDATE
                }
                context.startService(updateIntent)
            }
        }

        private fun updateWidget(context: Context, widgetClass: Class<*>) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, widgetClass)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val updateIntent = Intent(context, widgetClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(updateIntent)
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        registerReceiver(timeTickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        isRunning = false
        notifSelectedDate = null
        unregisterReceiver(timeTickReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand with action: ${intent?.action}")
        scope.launch {
            when (intent?.action) {
                ACTION_START -> startForegroundWithNotification()
                ACTION_UPDATE -> updateNotification()
                ACTION_SCHEDULE_ALARMS -> scheduleAlarms()
                ACTION_STOP -> stopSelf()
                ACTION_PREV -> changeDate(-1)
                ACTION_NEXT -> changeDate(1)
                ACTION_TODAY -> changeDate(0)
            }
        }
        return START_STICKY
    }

    private suspend fun startForegroundWithNotification() {
        val date = DateUtils.getCurrentDate()
        notifSelectedDate = date
        val times = PrayerUtils.loadDetailedPrayerTimes(this, date)
        if (times.isEmpty()) {
            Log.e(TAG, "Prayer times are empty, cannot create notification.")
            return
        }
        val notification = createPrayerNotification(date, times)
        startForeground(NOTIFICATION_ID, notification)
    }

    private suspend fun updateNotification() {
        val date = notifSelectedDate ?: DateUtils.getCurrentDate()
        val times = PrayerUtils.loadDetailedPrayerTimes(this, date)
        if (times.isEmpty()) return

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val today = DateUtils.getCurrentDate()
            val times = PrayerUtils.loadDetailedPrayerTimes(this, today)
            if (times.isNotEmpty()) {
                AdhanScheduler.scheduleFromPrayerMap(this, times)
            }
        }
    }

    private fun pending(req: Int, cls: Class<*>, act: String? = null, extraBlock: (Intent.() -> Unit)? = null): PendingIntent {
        val i = Intent(this, cls).apply { action = act }
        extraBlock?.invoke(i)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Service::class.java.isAssignableFrom(cls)) {
            PendingIntent.getService(this, req, i, flags)
        } else {
            PendingIntent.getBroadcast(this, req, i, flags)
        }
    }

    private fun sp(v: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    private fun maxWidth(): Int = (resources.displayMetrics.widthPixels - dp(this, 32)).coerceAtLeast(dp(this, 300))

    private fun font(f: String): Typeface {
        val id = when (f.lowercase(Locale.ROOT)) {
            "byekan" -> R.font.byekan
            "estedad" -> R.font.estedad_regular
            "vazirmatn" -> R.font.vazirmatn_regular
            "iraniansans" -> R.font.iraniansans
            "sahel" -> R.font.sahel_bold
            else -> 0
        }
        return if (id != 0) ResourcesCompat.getFont(this, id) ?: Typeface.DEFAULT else Typeface.DEFAULT
    }

    private fun findOptimalSP(tf: Typeface, w: Int, txt: String, pref: Float = 22f, min: Float = 14f): Float {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tf }
        var s = pref
        while (s >= min) {
            p.textSize = sp(s)
            if (p.measureText(txt) <= w) return s
            s -= 0.5f
        }
        return min
    }

    private fun createPrayerTimesBitmap(times: Map<String, String>, current: String?, baseColor: Int, highlightColor: Int, tf: Typeface, usePersian: Boolean, use24h: Boolean): Bitmap {
        val order = listOf("عشاء", "مغرب", "عصر", "ظهر", "صبح")
        val w = dp(this, 320)
        val h = dp(this, 60)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textAlign = Paint.Align.CENTER
            textSize = sp(14f)
        }
        val colW = w / order.size
        val offset = (p.descent() - p.ascent()) / 2 - p.descent()
        order.forEachIndexed { i, name ->
            val t = DateUtils.formatDisplayTime(times[name] ?: "--:--", use24h, usePersian)
            p.color = if (name == current) highlightColor else baseColor
            p.isFakeBoldText = name == current
            val x = (i * colW) + (colW / 2f)
            c.drawText(name, x, h / 2f - (h / 4f) + offset, p)
            c.drawText(t, x, h / 2f + (h / 4f) + offset, p)
        }
        return bmp
    }

    @SuppressLint("RestrictedApi")
    private suspend fun createPrayerNotification(date: MultiDate, times: Map<String, String>): Notification {
        val settings = dataStore.data.first()
        val usePersian = settings[booleanPreferencesKey("use_persian_numbers")] ?: true
        val use24h = settings[booleanPreferencesKey("use_24_hour_format")] ?: true
        val fontId = settings[stringPreferencesKey("fontId")] ?: "estedad"
        val dark = settings[booleanPreferencesKey("is_dark_theme")] ?: false

        val tf = font(fontId)

        val (prim, sec, bgRes) = if (dark) Triple(Color.WHITE, Color.LTGRAY, R.drawable.widget_background_dark) else Triple(Color.BLACK, Color.DKGRAY, R.drawable.widget_background_light)

        val highlightPrayer = PrayerUtils.getCurrentPrayerForHighlight(times, java.time.LocalTime.now())
        val shamsi = "${DateUtils.getWeekDayName(date)} ${DateUtils.formatShamsiLong(date, usePersian)}"
        val other = "${DateUtils.formatHijriLong(date, usePersian)} | ${DateUtils.formatGregorianLong(date, usePersian)}"
        val w = maxWidth()

        val collapsed = RemoteViews(packageName, R.layout.notification_collapsed).apply {
            setInt(R.id.collapsed_root_layout, "setBackgroundResource", bgRes)
            setViewVisibility(R.id.tv_shamsi_full, View.GONE)
            setViewVisibility(R.id.tv_other_dates_collapsed, View.GONE)

            val collapsedShamsiSP = findOptimalSP(tf, w, shamsi, pref = 20f, min = 16f)
            setImageViewBitmap(R.id.iv_title_collapsed, textBitmap(this@PrayerForegroundService, shamsi, tf, collapsedShamsiSP, prim, w))
            setViewVisibility(R.id.iv_title_collapsed, View.VISIBLE)

            setImageViewBitmap(R.id.iv_other_collapsed, textBitmap(this@PrayerForegroundService, other, tf, 16f, sec, w))
            setViewVisibility(R.id.iv_other_collapsed, View.VISIBLE)
        }

        val exp = RemoteViews(packageName, R.layout.notification_expanded).apply {
            setInt(R.id.expanded_root_layout, "setBackgroundResource", bgRes)
            val expandedShamsiSP = findOptimalSP(tf, w, shamsi, pref = 20f, min = 16f)
            val expandedOtherSP = findOptimalSP(tf, w, other, pref = 16f, min = 12f)
            setImageViewBitmap(R.id.iv_title, textBitmap(this@PrayerForegroundService, shamsi, tf, expandedShamsiSP, prim, w))
            setViewVisibility(R.id.iv_title, View.VISIBLE)

            setImageViewBitmap(R.id.iv_other, textBitmap(this@PrayerForegroundService, other, tf, expandedOtherSP, sec, w))
            setViewVisibility(R.id.iv_other, View.VISIBLE)

            val prayBmp = createPrayerTimesBitmap(times, highlightPrayer, if (dark) "#80DEEA".toColorInt() else "#0D47A1".toColorInt(), if (dark) "#FFF59D".toColorInt() else "#2E7D32".toColorInt(), tf, usePersian, use24h)
            setImageViewBitmap(R.id.iv_prayer_times, prayBmp)
            setViewVisibility(R.id.iv_prayer_times, View.VISIBLE)

            val navCol = if (dark) "#78909C".toColorInt() else "#546E7A".toColorInt()
            val todayCol = if (dark) "#64B5F6".toColorInt() else "#1976D2".toColorInt()
            setImageViewBitmap(R.id.iv_prev_day_exp, pillButtonBitmap(this@PrayerForegroundService, "روز قبل", tf, navCol, 16, Color.WHITE))
            setImageViewBitmap(R.id.iv_next_day_exp, pillButtonBitmap(this@PrayerForegroundService, "روز بعد", tf, navCol, 16, Color.WHITE))
            setOnClickPendingIntent(R.id.iv_prev_day_exp, pending(ACTION_PREV.hashCode(), PrayerForegroundService::class.java, ACTION_PREV))
            setOnClickPendingIntent(R.id.iv_next_day_exp, pending(ACTION_NEXT.hashCode(), PrayerForegroundService::class.java, ACTION_NEXT))
            val showToday = notifSelectedDate?.let { it.shamsi != DateUtils.getCurrentDate().shamsi } ?: false
            if (showToday) {
                setImageViewBitmap(R.id.btn_today, pillButtonBitmap(this@PrayerForegroundService, "بازگشت به امروز", tf, todayCol, 20, Color.WHITE))
                setOnClickPendingIntent(R.id.btn_today, pending(ACTION_TODAY.hashCode(), PrayerForegroundService::class.java, ACTION_TODAY))
            } else {
                val placeholder = pillButtonBitmap(this@PrayerForegroundService, "بازگشت به امروز", tf, Color.TRANSPARENT, 20, Color.TRANSPARENT)
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
            builder.setSmallIcon(getDaySmallIconCached(iconDay, dark, usePersian))
        } else {
            builder.setSmallIcon(R.drawable.ic_notification_icon)
        }

        return builder.build()
    }

    private var cachedIconDay = -1
    private var cachedIconColor = Color.BLACK
    private var cachedSmallIcon: IconCompat? = null

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getDaySmallIconCached(day: Int, isDark: Boolean, usePersian: Boolean): IconCompat {
        val color = if (isDark) Color.WHITE else Color.BLACK
        if (cachedIconDay == day && cachedIconColor == color && cachedSmallIcon != null) {
            return cachedSmallIcon!!
        }

        val text = DateUtils.convertToPersianNumbers(day.toString(), usePersian)
        val tf = font("sans-serif-medium")
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            typeface = tf
            textAlign = Paint.Align.CENTER
            textSize = sp(18f)
        }
        val size = dp(this, 24)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawText(text, size / 2f, size / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f, paint)

        cachedIconDay = day
        cachedIconColor = color
        cachedSmallIcon = IconCompat.createWithBitmap(bmp)
        return cachedSmallIcon!!
    }
}
