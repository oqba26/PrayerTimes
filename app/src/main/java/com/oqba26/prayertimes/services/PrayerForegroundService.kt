package com.oqba26.prayertimes.services

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.adhan.AdhanScheduler
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.receivers.IqamaAlarmReceiver
import com.oqba26.prayertimes.receivers.SilentModeReceiver
import com.oqba26.prayertimes.screens.PrayerTime
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PrayerUtils
import com.oqba26.prayertimes.viewmodels.dataStore
import com.oqba26.prayertimes.widget.LargeModernWidgetProvider
import com.oqba26.prayertimes.widget.ModernWidgetProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Suppress("DEPRECATION")
class PrayerForegroundService : Service() {

    companion object {
        private const val NOTIF_ID = 1001
        private const val TAG = "PrayerService"
        private const val UPDATE_INTERVAL = 60_000L
        private const val ACTION_PREV = "PREV_DAY"
        private const val ACTION_NEXT = "NEXT_DAY"
        private const val ACTION_RESTART = "RESTART"
        private const val ACTION_TODAY = "TODAY"
        const val ACTION_START_FROM_BOOT_OR_UPDATE = "ACTION_START_FROM_BOOT_OR_UPDATE"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var updateJob: Job? = null
    private var notifSelectedDate: MultiDate? = null
    private var lastScheduleSignature: String? = null

    override fun onCreate() {
        super.onCreate()
        NotificationService.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: "START"
        Log.d(TAG, "onStartCommand: $action")
        when (action) {
            "START", ACTION_START_FROM_BOOT_OR_UPDATE, ACTION_RESTART -> {
                if (action == ACTION_RESTART) lastScheduleSignature = null
                notifSelectedDate = null
                startUpdatingNotification()
            }
            "STOP" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                else stopForeground(true)
                stopSelf()
            }
            ACTION_PREV -> {
                notifSelectedDate =
                    DateUtils.getPreviousDate(notifSelectedDate ?: DateUtils.getCurrentDate())
                serviceScope.launch { postOnce() }
            }
            ACTION_NEXT -> {
                notifSelectedDate =
                    DateUtils.getNextDate(notifSelectedDate ?: DateUtils.getCurrentDate())
                serviceScope.launch { postOnce() }
            }
            ACTION_TODAY -> {
                notifSelectedDate = null
                serviceScope.launch { postOnce() }
            }
        }
        return START_STICKY
    }

    private fun startUpdatingNotification() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                try {
                    val date = notifSelectedDate ?: DateUtils.getCurrentDate()
                    val times = PrayerUtils.loadDetailedPrayerTimes(applicationContext, date)
                    if (times.isNotEmpty()) {
                        val notif = createPrayerNotification(date, times)
                        startForeground(NOTIF_ID, notif)
                        scheduleAllAlarms(date, times)
                        updateAllWidgetsNow()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "loop error", e)
                }
                delay(UPDATE_INTERVAL)
            }
        }
    }

    private suspend fun postOnce() {
        val date = notifSelectedDate ?: DateUtils.getCurrentDate()
        val times = PrayerUtils.loadDetailedPrayerTimes(applicationContext, date)
        if (times.isNotEmpty()) {
            val notif = createPrayerNotification(date, times)
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun updateAllWidgetsNow() {
        val mgr = AppWidgetManager.getInstance(this)
        val providers = listOf(ModernWidgetProvider::class.java, LargeModernWidgetProvider::class.java)
        providers.forEach { providerClass ->
            val component = ComponentName(this, providerClass)
            val ids = mgr.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(this, providerClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                sendBroadcast(intent)
            }
        }
    }

    private suspend fun scheduleAllAlarms(date: MultiDate, prayerTimes: Map<String, String>) {
        val today = DateUtils.getCurrentDate()
        if (date.shamsi != today.shamsi) return
        val settings = applicationContext.dataStore.data.first()
        val sig = today.shamsi + "|" + settings.asMap().toString()
        if (sig == lastScheduleSignature) return
        if (settings[booleanPreferencesKey("adhan_enabled")] ?: true)
            scheduleAdhanAlarms(prayerTimes)
        else AdhanScheduler.cancelAll(this)
        scheduleSilentAndIqama(settings, prayerTimes)
        lastScheduleSignature = sig
    }

    private fun scheduleAdhanAlarms(times: Map<String, String>) {
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        val adj = mapOf("صبح" to 30L, "ظهر" to 20L, "عصر" to 20L, "عشاء" to 20L)

        val adhanScheduleMap = times.toMutableMap()
        adj.forEach { (prayerName, minutesToSubtract) ->
            times[prayerName]?.let { timeStr ->
                PrayerUtils.parseTimeSafely(timeStr)?.let {
                    adhanScheduleMap[prayerName] = it.minusMinutes(minutesToSubtract).format(fmt)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            AdhanScheduler.scheduleFromPrayerMap(this, adhanScheduleMap)
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleSilentAndIqama(settings: Preferences, times: Map<String, String>) {
        val am = getSystemService<AlarmManager>()!!
        val now = LocalDateTime.now()
        val isAutoSilentEnabled = settings[booleanPreferencesKey("auto_silent_enabled")] ?: false
        val isIqamaGloballyEnabled = settings[booleanPreferencesKey("iqama_enabled")] ?: false
        val minutesBeforeIqama = (settings[intPreferencesKey("minutes_before_iqama")] ?: 10).toLong()

        PrayerTime.entries.forEach { p ->
            val base = p.id.hashCode()
            // Always cancel previous alarms for this prayer to avoid conflicts
            am.cancel(pending(base, SilentModeReceiver::class.java, SilentModeReceiver.ACTION_SILENT))
            am.cancel(pending(base + 1, SilentModeReceiver::class.java, SilentModeReceiver.ACTION_UNSILENT))
            am.cancel(pending(base + 2, IqamaAlarmReceiver::class.java))

            val time = PrayerUtils.parseTimeSafely(times[p.displayName] ?: "") ?: return@forEach
            val prayerDateTime = time.atDate(now.toLocalDate())

            // --- Schedule Silent Mode (Per-Prayer) ---
            if (isAutoSilentEnabled) {
                val silentEnabledKey = booleanPreferencesKey("silent_enabled_${p.id}")
                if (settings[silentEnabledKey] ?: false) {
                    val beforeKey = intPreferencesKey("minutes_before_silent_${p.id}")
                    val afterKey = intPreferencesKey("minutes_after_silent_${p.id}")
                    val beforeMinutes = (settings[beforeKey] ?: 10).toLong()
                    val afterMinutes = (settings[afterKey] ?: 10).toLong()

                    val silentTime = prayerDateTime.minusMinutes(beforeMinutes)
                    if (silentTime.isAfter(now)) {
                        setExact(am, silentTime, pending(base, SilentModeReceiver::class.java, SilentModeReceiver.ACTION_SILENT))
                    }

                    val unsilentTime = prayerDateTime.plusMinutes(afterMinutes)
                    if (unsilentTime.isAfter(now)) {
                        setExact(am, unsilentTime, pending(base + 1, SilentModeReceiver::class.java, SilentModeReceiver.ACTION_UNSILENT))
                    }
                }
            }

            // --- Schedule Iqama (Global Setting) ---
            if (isIqamaGloballyEnabled) {
                val iqamaTime = prayerDateTime.minusMinutes(minutesBeforeIqama)
                if (iqamaTime.isAfter(now)) {
                    val pi = pending(base + 2, IqamaAlarmReceiver::class.java) {
                        putExtra("PRAYER_NAME", p.displayName)
                    }
                    setExact(am, iqamaTime, pi)
                }
            }
        }
    }

    private fun pending(
        req: Int,
        cls: Class<*>,
        act: String? = null,
        extraBlock: (Intent.() -> Unit)? = null
    ): PendingIntent {
        val i = Intent(this, cls)
        if (act != null) i.action = act
        extraBlock?.invoke(i)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Service::class.java.isAssignableFrom(cls)) {
            PendingIntent.getService(this, req, i, flags)
        } else {
            PendingIntent.getBroadcast(this, req, i, flags)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun setExact(am: AlarmManager, at: LocalDateTime, pi: PendingIntent) {
        val t = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        else am.setExact(AlarmManager.RTC_WAKEUP, t, pi)
    }

    // -- اندازه dp و sp --
    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()

    private fun sp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    private fun maxWidth(): Int {
        val w = resources.displayMetrics.widthPixels
        return (w - dp(32)).coerceAtLeast(dp(300))
    }

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
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.typeface = tf
        var s = pref
        while (s >= min) {
            p.textSize = sp(s)
            if (p.measureText(txt) <= w) return s
            s -= 0.5f
        }
        return min
    }

    private fun textBitmap(text: String, tf: Typeface, spVal: Float, color: Int, w: Int): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textAlign = Paint.Align.CENTER
            typeface = tf
            textSize = sp(spVal)
        }
        val fm = paint.fontMetrics
        val h = (fm.descent - fm.ascent + dp(8)).toInt()
        val bmp = createBitmap(w.coerceAtLeast(1), h)
        val c = Canvas(bmp)
        val baseline = h / 2f - (fm.ascent + fm.descent) / 2f
        c.drawText(text, w / 2f, baseline, paint)
        return bmp
    }

    private fun pillButtonBitmap(text: String, tf: Typeface, bgColor: Int, hPadDp: Int): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = if (bgColor == Color.TRANSPARENT) Color.TRANSPARENT else Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = sp(12f)
        paint.typeface = tf
        val textW = paint.measureText(text)
        val h = dp(36)
        val padH = dp(hPadDp).toFloat()
        val w = (textW + 2 * padH).toInt().coerceAtLeast(dp(64))
        val bmp = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        bg.color = bgColor
        c.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), h / 2f, h / 2f, bg)
        val fm = paint.fontMetrics
        val base = h / 2f - (fm.ascent + fm.descent) / 2f
        c.drawText(text, w / 2f, base, paint)
        return bmp
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
        val order = listOf("عشاء", "مغرب", "عصر", "ظهر", "صبح")
        val w = dp(320)
        val h = dp(60)
        val bmp = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.typeface = tf
        p.textAlign = Paint.Align.CENTER
        p.textSize = sp(14f)
        val colW = w / order.size
        val offset = (p.descent() - p.ascent()) / 2 - p.descent()
        order.forEachIndexed { i, name ->
            val t = DateUtils.formatDisplayTime(times[name] ?: "--:--", use24h, usePersian)
            p.color = if (name == current) highlightColor else baseColor
            p.isFakeBoldText = name == current
            val x = (i * colW) + (colW / 2f)
            val y1 = h / 2f - (h / 4f) + offset
            val y2 = h / 2f + (h / 4f) + offset
            c.drawText(name, x, y1, p)
            c.drawText(t, x, y2, p)
        }
        return bmp
    }
    @SuppressLint("RestrictedApi")
    @Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
    private suspend fun createPrayerNotification(date: MultiDate, times: Map<String, String>): Notification {
        val settings = applicationContext.dataStore.data.first()
        val usePersian = settings[booleanPreferencesKey("use_persian_numbers")] ?: true
        val use24h = settings[booleanPreferencesKey("use_24_hour_format")] ?: true
        val fontId = settings[stringPreferencesKey("fontId")] ?: "estedad"
        val dark = settings[booleanPreferencesKey("is_dark_theme")] ?: false

        DateUtils.setDefaultUsePersianNumbers(usePersian)
        val tf = font(fontId)

        val (prim, sec, bgRes) = if (dark)
            Triple(Color.WHITE, Color.LTGRAY, R.drawable.widget_background_dark)
        else Triple(Color.BLACK, Color.DKGRAY, R.drawable.widget_background_light)

        val highlightPrayer = PrayerUtils.getCurrentPrayerForHighlight(times, java.time.LocalTime.now())
        val shamsi = "${DateUtils.getWeekDayName(date)} ${DateUtils.formatShamsiLong(date, usePersian)}"
        val other = "${DateUtils.formatHijriLong(date, usePersian)} | ${DateUtils.formatGregorianLong(date, usePersian)}"
        val w = maxWidth()


        // --- Collapsed View ---
        val collapsed = RemoteViews(packageName, R.layout.notification_collapsed).apply {
            setInt(R.id.collapsed_root_layout, "setBackgroundResource", bgRes)

            // Hide text fallbacks
            setViewVisibility(R.id.tv_shamsi_full, View.GONE)
            setViewVisibility(R.id.tv_other_dates_collapsed, View.GONE)

            // Calculate font sizes specifically for collapsed view
            val collapsedShamsiSP = findOptimalSP(tf, w, shamsi, pref = 20f, min = 16f)
            // Using a fixed larger font size for the second line in the collapsed view for better readability.
            val collapsedOtherSP = 16f

            // Show and populate ImageViews
            setViewVisibility(R.id.iv_title_collapsed, View.VISIBLE)
            setViewVisibility(R.id.iv_other_collapsed, View.VISIBLE)
            setImageViewBitmap(R.id.iv_title_collapsed, textBitmap(shamsi, tf, collapsedShamsiSP, prim, w))
            setImageViewBitmap(R.id.iv_other_collapsed, textBitmap(other, tf, collapsedOtherSP, sec, w))
        }

        // --- Expanded View ---
        val exp = RemoteViews(packageName, R.layout.notification_expanded).apply {
            setInt(R.id.expanded_root_layout, "setBackgroundResource", bgRes)

            // Calculate font sizes for expanded view separately (original values)
            val expandedShamsiSP = findOptimalSP(tf, w, shamsi, pref = 20f, min = 16f)
            val expandedOtherSP = findOptimalSP(tf, w, other, pref = 16f, min = 12f)


            // --- Titles ---
            // Hide text fallbacks
            setViewVisibility(R.id.tv_shamsi_date, View.GONE)
            setViewVisibility(R.id.tv_other_dates, View.GONE)

            // Show and populate ImageViews
            setViewVisibility(R.id.iv_title, View.VISIBLE)
            setViewVisibility(R.id.iv_other, View.VISIBLE)
            setImageViewBitmap(R.id.iv_title, textBitmap(shamsi, tf, expandedShamsiSP, prim, w))
            setImageViewBitmap(R.id.iv_other, textBitmap(other, tf, expandedOtherSP, sec, w))

            // --- Prayer Times ---
            val prayBmp = createPrayerTimesBitmap(
                times, highlightPrayer,
                if (dark) "#80DEEA".toColorInt() else "#0D47A1".toColorInt(),
                if (dark) "#FFF59D".toColorInt() else "#2E7D32".toColorInt(),
                tf, usePersian, use24h
            )
            // Hide text fallback
            setViewVisibility(R.id.tv_prayer_times, View.GONE)
            // Show and populate ImageView
            setViewVisibility(R.id.iv_prayer_times, View.VISIBLE)
            setImageViewBitmap(R.id.iv_prayer_times, prayBmp)


            // --- Buttons ---
            val navCol = if (dark) "#78909C".toColorInt() else "#546E7A".toColorInt()
            val todayCol = if (dark) "#64B5F6".toColorInt() else "#1976D2".toColorInt()

            // Create bitmaps for buttons
            setImageViewBitmap(R.id.iv_prev_day_exp, pillButtonBitmap("روز قبل", tf, navCol, 16))
            setImageViewBitmap(R.id.iv_next_day_exp, pillButtonBitmap("روز بعد", tf, navCol, 16))

            // Attach actions to buttons
            setOnClickPendingIntent(
                R.id.iv_prev_day_exp,
                pending(ACTION_PREV.hashCode(), PrayerForegroundService::class.java, ACTION_PREV)
            )
            setOnClickPendingIntent(
                R.id.iv_next_day_exp,
                pending(ACTION_NEXT.hashCode(), PrayerForegroundService::class.java, ACTION_NEXT)
            )

            // Visibility and action for "Today" button
            val showToday = notifSelectedDate?.let { it.shamsi != DateUtils.getCurrentDate().shamsi } ?: false
            if (showToday) {
                val bmpToday = pillButtonBitmap("بازگشت به امروز", tf, todayCol, 20)
                setImageViewBitmap(R.id.btn_today, bmpToday)
                setOnClickPendingIntent(
                    R.id.btn_today,
                    pending(ACTION_TODAY.hashCode(), PrayerForegroundService::class.java, ACTION_TODAY)
                )
            } else {
                val placeholderBmp = pillButtonBitmap("بازگشت به امروز", tf, Color.TRANSPARENT, 20)
                setImageViewBitmap(R.id.btn_today, placeholderBmp)
            }
            setViewVisibility(R.id.btn_today, if (showToday) View.VISIBLE else View.INVISIBLE)
        }

        // --- Build Notification ---
        val builder = NotificationCompat.Builder(this, NotificationService.DAILY_CHANNEL_ID)
            .setCustomContentView(collapsed)
            .setCustomBigContentView(exp)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)

        // --- Dynamic Small Icon ---
        val iconDay = date.getShamsiParts().third
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val smallIcon = getDaySmallIconCached(iconDay, dark, usePersian)
            builder.setSmallIcon(smallIcon)
        } else {
            builder.setSmallIcon(R.drawable.ic_notification_icon)
        }

        return builder.build()
    }

    private var cachedIconDay = -1
    private var cachedIconColor = Color.BLACK
    private var cachedUsePersian = false
    private var cachedIcon: IconCompat? = null

    @Suppress("unused")
    private fun getDaySmallIconCached(day: Int, dark: Boolean, per: Boolean): IconCompat {
        val color = if (dark) Color.WHITE else Color.BLACK
        if (day == cachedIconDay && color == cachedIconColor && cachedUsePersian == per && cachedIcon != null)
            return cachedIcon!!
        val res = createDayIcon(day, dark, per)
        cachedIconDay = day; cachedIconColor = color; cachedUsePersian = per; cachedIcon = res
        return res
    }

    private fun createDayIcon(day: Int, dark: Boolean, per: Boolean): IconCompat {
        val size = dp(24)
        val bmp = createBitmap(size, size)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            color = if (dark) Color.WHITE else Color.BLACK
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        }
        val txt = if (per) DateUtils.convertToPersianNumbers(day.toString()) else day.toString()
        val bnds = Rect()
        var ts = size.toFloat()
        while (ts > 1f) {
            paint.textSize = ts
            paint.getTextBounds(txt, 0, txt.length, bnds)
            if (bnds.width() <= size && bnds.height() <= size) break
            ts -= 0.5f
        }
        val x = size / 2f
        val y = size / 2f - bnds.centerY()
        c.drawText(txt, x, y, paint)
        return IconCompat.createWithBitmap(bmp)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
