package com.oqba26.prayertimes.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.IBinder
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.toColorInt
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.adhan.AdhanScheduler
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PrayerUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

class PrayerForegroundService : Service() {

    data class NotificationColors(
        val primaryText: Int,
        val secondaryText: Int,
        val prayerTime: Int,
        val prayerHighlight: Int,
        val navButton: Int,
        val todayButton: Int,
        val navText: Int
    )

    companion object {
        private const val NOTIFICATION_ID = 1001
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
    private var cachedIconDay = -1
    private var cachedIconColor = Color.WHITE
    private var cachedIcon: IconCompat? = null
    private var cachedUsePersianNumbers: Boolean? = null
    private var notifSelectedDate: MultiDate? = null

    // برای جلوگیری از زمان‌بندی تکراری در طول روز
    private var lastAdhanScheduledForDateKey: String? = null // اضافه شد
    private var lastAdhanScheduledSignature: String? = null

    private fun getNotificationColors(isDarkTheme: Boolean): NotificationColors {
        return if (isDarkTheme) {
            NotificationColors(
                primaryText = Color.WHITE,
                secondaryText = Color.LTGRAY,
                prayerTime = "#80DEEA".toColorInt(),
                prayerHighlight = "#FFF59D".toColorInt(),
                navButton = "#78909C".toColorInt(),
                todayButton = "#64B5F6".toColorInt(),
                navText = Color.WHITE
            )
        } else {
            NotificationColors(
                primaryText = Color.BLACK,
                secondaryText = Color.DKGRAY,
                prayerTime = "#0D47A1".toColorInt(),
                prayerHighlight = "#2E7D32".toColorInt(),
                navButton = "#546E7A".toColorInt(),
                todayButton = "#1976D2".toColorInt(),
                navText = Color.WHITE
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Service creating...")
        try {
            NotificationService.createNotificationChannels(this)
            Log.d(TAG, "onCreate: Notification channels created/ensured.")
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: Error in onCreate while creating notification channels", e)
        }
        Log.d(TAG, "onCreate: Service creation finished.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: Received action: $action, flags: $flags, startId: $startId), intent: $intent")
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val usePersianNumbers = prefs.getBoolean("use_persian_numbers", true)
            DateUtils.setDefaultUsePersianNumbers(usePersianNumbers)

            val isDarkTheme = prefs.getBoolean("is_dark_theme", false)
            Log.d(TAG, "onStartCommand: isDarkTheme=$isDarkTheme, usePersianNumbers=$usePersianNumbers")

            when (action) {
                "START", ACTION_START_FROM_BOOT_OR_UPDATE -> {
                    serviceScope.launch { postOnce(isDarkTheme) }
                    startUpdatingNotification(isDarkTheme)
                }
                ACTION_RESTART -> {
                    // مهم: قفل روزانه را ریست کن تا همین الان دوباره زمان‌بندی شود
                    lastAdhanScheduledForDateKey = null
                    // اگر قبلاً lastAdhanScheduledSignature را طبق پیشنهاد اضافه کرده‌ای، این خط را هم فعال کن:
                    // lastAdhanScheduledSignature = null

                    notifSelectedDate = null // برگرد به امروز
                    serviceScope.launch { postOnce(isDarkTheme) }
                    startUpdatingNotification(isDarkTheme)
                }
                "STOP" -> {
                    stopForegroundCompat()
                    stopSelf()
                }
                ACTION_PREV -> {
                    val base = notifSelectedDate ?: DateUtils.getCurrentDate()
                    notifSelectedDate = DateUtils.getPreviousDate(base)
                    serviceScope.launch { postOnce(isDarkTheme) }
                }
                ACTION_NEXT -> {
                    val base = notifSelectedDate ?: DateUtils.getCurrentDate()
                    notifSelectedDate = DateUtils.getNextDate(base)
                    serviceScope.launch { postOnce(isDarkTheme) }
                }
                ACTION_TODAY -> {
                    notifSelectedDate = null
                    serviceScope.launch { postOnce(isDarkTheme) }
                }
                else -> Log.w(TAG, "onStartCommand: Unknown action: $action")
            }
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: Error in onStartCommand", e)
            if (e is CancellationException) throw e
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "Service stopped from foreground.")
    }

    private fun startUpdatingNotification(isDarkTheme: Boolean) {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            Log.d(TAG, "startUpdatingNotification: Loop started.")
            try {
                if (isActive) {
                    val initialBaseDate = notifSelectedDate ?: DateUtils.getCurrentDate()
                    val initialTimes = PrayerUtils.loadPrayerTimes(applicationContext, initialBaseDate)
                    if (initialTimes.isNotEmpty() && isActive) {
                        val n = withContext(Dispatchers.Default) { createNotification(initialBaseDate, initialTimes, isDarkTheme) }
                        if (isActive) startForeground(NOTIFICATION_ID, n)
                        // زمان‌بندی اذان برای امروز (فقط یک‌بار در روز)
                        maybeScheduleAdhanFor(initialBaseDate, initialTimes) // اضافه شد
                        Log.d(TAG, "Initial notification posted from startUpdatingNotification.")
                    }
                }
            } catch (e: CancellationException) {
                Log.i(TAG, "startUpdatingNotification: Initial post cancelled.", e)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error in initial post from startUpdatingNotification", e)
            }

            while (isActive) {
                try {
                    delay(UPDATE_INTERVAL)
                    if (!isActive) break
                    val baseDate = notifSelectedDate ?: DateUtils.getCurrentDate()
                    val times = PrayerUtils.loadPrayerTimes(applicationContext, baseDate)
                    if (times.isNotEmpty() && isActive) {
                        val n = withContext(Dispatchers.Default) { createNotification(baseDate, times, isDarkTheme) }
                        if (isActive) startForeground(NOTIFICATION_ID, n)
                        // اگر روز عوض شده، برای امروز دوباره آلارم‌های اذان تنظیم می‌شوند
                        maybeScheduleAdhanFor(baseDate, times) // اضافه شد
                        Log.d(TAG, "Notification updated from loop.")
                    }
                } catch (e: CancellationException) {
                    Log.i(TAG, "startUpdatingNotification: Loop cancelled during delay or update.", e)
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Update loop error", e)
                    delay(5000)
                }
            }
            Log.d(TAG, "startUpdatingNotification: Loop finished (isActive is false).")
        }
    }

    // زمان‌بندی اذان برای امروز، فقط یک‌بار در روز
    private fun maybeScheduleAdhanFor(date: MultiDate, prayerTimes: Map<String, String>) {
        val today = DateUtils.getCurrentDate()
        val isToday = try {
            date.gregorianParts() == today.gregorianParts()
        } catch (_: Exception) {
            date.shamsi == today.shamsi
        }
        if (!isToday) {
            Log.d(TAG, "Adhan schedule skipped (date is not today)")
            return
        }

        // یک امضاء پایدار از زمان‌های امروز بسازیم تا اگر عوض شد، دوباره زمان‌بندی کنیم
        fun normalizeDigits(s: String): String {
            val map = mapOf(
                '۰' to '0','١' to '1','۱' to '1','٢' to '2','۲' to '2','٣' to '3','۳' to '3',
                '٤' to '4','۴' to '4','٥' to '5','۵' to '5','٦' to '6','۶' to '6','٧' to '7',
                '۷' to '7','٨' to '8','۸' to '8','٩' to '9','۹' to '9','٫' to ':','،' to ':'
            )
            val sb = StringBuilder()
            s.forEach { ch -> sb.append(map[ch] ?: ch) }
            return sb.toString().trim()
        }

        // فقط کلیدهای مهم رو در نظر بگیریم و اعداد رو لاتین کنیم
        val keys = listOf("فجر","اذان صبح","صبح","طلوع بامداد","ظهر","dhuhr","zuhr","عصر","asr","غروب","مغرب","maghrib","عشاء","عشا","isha")
        val sig = buildString {
            append(today.shamsi)
            append('|')
            keys.forEach { k ->
                val v = prayerTimes.entries.firstOrNull { it.key.contains(k, ignoreCase = true) }?.value
                append(k).append('=').append(if (v != null) normalizeDigits(v) else "--").append(';')
            }
        }

        if (sig == lastAdhanScheduledSignature) {
            Log.d(TAG, "Adhan already scheduled for ${today.shamsi} with same times, skipping.")
            return
        }

        runCatching {
            AdhanScheduler.scheduleFromPrayerMap(this, prayerTimes)
            lastAdhanScheduledForDateKey = today.shamsi
            lastAdhanScheduledSignature = sig
            Log.d(TAG, "Adhan alarms scheduled for ${today.shamsi} (signature changed)")
        }.onFailure {
            Log.e(TAG, "Failed to schedule adhan alarms", it)
        }
    }

    private suspend fun postOnce(isDarkTheme: Boolean) {
        Log.d(TAG, "postOnce: Attempting to post notification.")
        try {
            if (!serviceScope.isActive) return
            val baseDate = notifSelectedDate ?: DateUtils.getCurrentDate()
            val times = PrayerUtils.loadPrayerTimes(applicationContext, baseDate)
            if (times.isNotEmpty() && serviceScope.isActive) {
                val n = createNotification(baseDate, times, isDarkTheme)
                if (serviceScope.isActive) startForeground(NOTIFICATION_ID, n)
                // در پست یک‌باره، زمان‌بندی را به عهده حلقه می‌گذاریم
                Log.d(TAG, "Notification posted successfully from postOnce.")
            }
        } catch (e: CancellationException) {
            Log.i(TAG, "postOnce: Coroutine was cancelled.", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "postOnce error while creating/posting notification", e)
        }
    }

    @SuppressLint("UseKtx")
    private fun createNotification(
        date: MultiDate,
        prayerTimes: Map<String, String>,
        isDarkTheme: Boolean
    ): Notification {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val usePersianNumbers = prefs.getBoolean("use_persian_numbers", true)

        val weekDay = DateUtils.getWeekDayName(date)
        val title = "$weekDay ${DateUtils.formatShamsiLong(date, usePersianNumbers)}"
        val otherDates = "${DateUtils.formatHijriLong(date, usePersianNumbers)} | ${DateUtils.formatGregorianLong(date, usePersianNumbers)}"

        val now = java.time.LocalTime.now()
        val highlightPrayer = PrayerUtils.getCurrentPrayerForHighlight(prayerTimes, now)
        val prayerText = createPrayerTimesText(prayerTimes, highlightPrayer, isDarkTheme)

        val collapsed = createCollapsedView(
            layoutId = R.layout.notification_collapsed,
            title = title,
            otherDates = otherDates,
            date = date,
            now = now,
            prayerTimes = prayerTimes,
            isDarkTheme = isDarkTheme
        )
        val expanded = createExpandedView(
            layoutId = R.layout.notification_expanded,
            title = title,
            otherDates = otherDates,
            prayerText = prayerText,
            prayerTimes = prayerTimes,
            currentPrayer = highlightPrayer,
            showTodayButton = shouldShowTodayButton(notifSelectedDate),
            isDarkTheme = isDarkTheme
        )

        val builder = NotificationCompat.Builder(this, NotificationService.DAILY_CHANNEL_ID)

        val dayOfMonth = date.getShamsiParts().third
        val iconTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        val dynamicIcon = getCachedDayIcon(dayOfMonth, iconTextColor)
        builder.setSmallIcon(dynamicIcon)

        return builder
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .apply {
                setShowWhen(false)
                setUsesChronometer(false)
            }
            .build()
    }

    private fun getCachedDayIcon(day: Int, iconTextColor: Int): IconCompat {
        val usePersianNumbers = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("use_persian_numbers", true)
        return getCachedDayIcon(day, iconTextColor, usePersianNumbers)
    }

    private fun getCachedDayIcon(
        day: Int,
        iconTextColor: Int,
        usePersianNumbers: Boolean
    ): IconCompat {
        val safeDay = day.coerceIn(1, 31)
        if (safeDay == cachedIconDay &&
            iconTextColor == cachedIconColor &&
            cachedUsePersianNumbers == usePersianNumbers &&
            cachedIcon != null
        ) {
            return cachedIcon!!
        }

        val icon = createDaySmallIconCompat(safeDay, iconTextColor, usePersianNumbers)
        cachedIconDay = safeDay
        cachedIconColor = iconTextColor
        cachedUsePersianNumbers = usePersianNumbers
        cachedIcon = icon
        return icon
    }

    private fun createDaySmallIconCompat(
        day: Int,
        iconTextColor: Int,
        usePersianNumbers: Boolean
    ): IconCompat {
        val dm = resources.displayMetrics
        val sizePx = (24f * dm.density).roundToInt().coerceAtLeast(24)

        val bmp = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iconTextColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            style = Paint.Style.FILL
        }

        val textToDraw = DateUtils.convertToPersianNumbers(day.toString(), usePersianNumbers)
        paint.textSize = if (day < 10) sizePx * 0.75f else sizePx * 0.65f

        val textBounds = android.graphics.Rect()
        paint.getTextBounds(textToDraw, 0, textToDraw.length, textBounds)

        val x = canvas.width / 2f
        val y = canvas.height / 2f - textBounds.centerY()

        canvas.drawText(textToDraw, x, y, paint)
        return IconCompat.createWithBitmap(bmp)
    }

    private fun shouldShowTodayButton(selected: MultiDate?): Boolean {
        if (selected == null) return false
        return try {
            val today = DateUtils.getCurrentDate()
            selected.gregorianParts() != today.gregorianParts()
        } catch (_: Exception) {
            runCatching { selected.gregorian != DateUtils.getCurrentDate().gregorian }.getOrDefault(true)
        }
    }

    private fun createCollapsedView(
        layoutId: Int,
        title: String,
        otherDates: String,
        date: MultiDate,
        now: java.time.LocalTime,
        prayerTimes: Map<String, String>,
        isDarkTheme: Boolean
    ): RemoteViews {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val usePersianNumbers = prefs.getBoolean("use_persian_numbers", true)
        val fontId = prefs.getString("fontId", "system")?.lowercase() ?: "system"
        val tf = resolveTypeface(fontId)

        val colors = getNotificationColors(isDarkTheme)
        val backgroundResource = if (isDarkTheme) {
            R.drawable.notification_bg_full_bleed_dark
        } else {
            R.drawable.notification_bg_full_bleed_light
        }

        val titleBmp = rvTextBitmap(
            text = title, tf = tf, sp = 16f, color = colors.prayerHighlight,
            maxWidthPx = dp(300), extraVerticalSpace = 16
        )
        val otherDatesBmp = rvTextBitmap(
            text = otherDates, tf = tf, sp = 13f, color = colors.secondaryText,
            maxWidthPx = dp(300), extraVerticalSpace = 12
        )

        return RemoteViews(packageName, layoutId).apply {
            setInt(R.id.collapsed_root_layout, "setBackgroundResource", backgroundResource)
            setImageViewBitmap(R.id.iv_title_collapsed, titleBmp)
            setViewVisibility(R.id.iv_title_collapsed, View.VISIBLE)
            setViewVisibility(R.id.tv_shamsi_full, View.GONE)

            setImageViewBitmap(R.id.iv_other_collapsed, otherDatesBmp)
            setViewVisibility(R.id.iv_other_collapsed, View.VISIBLE)
            setViewVisibility(R.id.tv_other_dates_collapsed, View.GONE)

            setViewVisibility(R.id.iv_next_collapsed, View.GONE)
            setViewVisibility(R.id.tv_next_prayer, View.GONE)
        }
    }

    private fun createPrayerTimesText(
        prayerTimes: Map<String, String>,
        currentPrayer: String?,
        isDarkTheme: Boolean
    ): String {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val usePersianNumbers = prefs.getBoolean("use_persian_numbers", true)
        val use24HourFormat = prefs.getBoolean("use_24_hour_format", false)

        val colors = getNotificationColors(isDarkTheme)
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")

        val normalColorHex = String.format("#%06X", 0xFFFFFF and colors.prayerTime)
        val highlightColorHex = String.format("#%06X", 0xFFFFFF and colors.prayerHighlight)

        return order.joinToString(" | ") { name ->
            val raw = prayerTimes[name] ?: "--:--"
            val time = DateUtils.formatDisplayTime(raw, use24HourFormat, usePersianNumbers)
            if (name == currentPrayer) "<b><font color='$highlightColorHex'>$name: $time</font></b>"
            else "<font color='$normalColorHex'>$name: $time</font>"
        }
    }

    private fun createExpandedView(
        layoutId: Int,
        title: String,
        otherDates: String,
        prayerText: String,
        prayerTimes: Map<String, String>,
        currentPrayer: String?,
        showTodayButton: Boolean,
        isDarkTheme: Boolean
    ): RemoteViews {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val fontId = prefs.getString("fontId", "system")?.lowercase() ?: "system"
        val tf = resolveTypeface(fontId)
        val usePersianNumbers = prefs.getBoolean("use_persian_numbers", true)
        val use24HourFormat = prefs.getBoolean("use_24_hour_format", false)

        val colors = getNotificationColors(isDarkTheme)
        val backgroundResource = if (isDarkTheme) {
            R.drawable.notification_bg_full_bleed_dark
        } else {
            R.drawable.notification_bg_full_bleed_light
        }

        val titleBmp = rvTextBitmap(
            text = title, tf = tf, sp = 18f, color = colors.primaryText,
            maxWidthPx = dp(320), extraVerticalSpace = 16
        )
        val otherBmp = rvTextBitmap(
            text = otherDates, tf = tf, sp = 14f, color = colors.secondaryText,
            maxWidthPx = dp(320), extraVerticalSpace = 12
        )

        val prayersBmp = createPrayersBitmapWithHighlight(
            prayerTimes = prayerTimes,
            currentPrayerName = currentPrayer,
            tf = tf,
            maxWidthPx = dp(320),
            baseColor = colors.prayerTime,
            highlightColor = colors.prayerHighlight,
            use24HourFormat = use24HourFormat,
            usePersianNumbers = usePersianNumbers
        )

        val bmpPrev = pillButtonBitmap("روز قبل", colors.navButton, colors.navText, 12f)
        val bmpNext = pillButtonBitmap("روز بعد", colors.navButton, colors.navText, 12f)
        val bmpToday = pillButtonBitmap("بازگشت به امروز", colors.todayButton, colors.navText, 12f)

        return RemoteViews(packageName, layoutId).apply {
            setInt(R.id.expanded_root_layout, "setBackgroundResource", backgroundResource)
            setImageViewBitmap(R.id.iv_title, titleBmp)
            setViewVisibility(R.id.iv_title, View.VISIBLE)
            setViewVisibility(R.id.tv_shamsi_date, View.GONE)

            setImageViewBitmap(R.id.iv_other, otherBmp)
            setViewVisibility(R.id.iv_other, View.VISIBLE)
            setViewVisibility(R.id.tv_other_dates, View.GONE)

            setImageViewBitmap(R.id.iv_prayer_times, prayersBmp)
            setViewVisibility(R.id.iv_prayer_times, View.VISIBLE)
            setViewVisibility(R.id.tv_prayer_times, View.GONE)

            setImageViewBitmap(R.id.iv_prev_day_exp, bmpPrev)
            setImageViewBitmap(R.id.iv_next_day_exp, bmpNext)
            setImageViewBitmap(R.id.btn_today, bmpToday)

            setViewVisibility(R.id.iv_prev_day_exp, View.VISIBLE)
            setViewVisibility(R.id.iv_next_day_exp, View.VISIBLE)
            setViewVisibility(R.id.btn_today, if (showTodayButton) View.VISIBLE else View.INVISIBLE)

            setOnClickPendingIntent(R.id.iv_prev_day_exp, pendingService(ACTION_PREV))
            setOnClickPendingIntent(R.id.iv_next_day_exp, pendingService(ACTION_NEXT))
            setOnClickPendingIntent(R.id.btn_today, pendingService(ACTION_TODAY))
        }
    }

    private fun resolveTypeface(fontId: String): Typeface {
        val res = when (fontId.lowercase(Locale.ROOT)) {
            "byekan"      -> R.font.byekan
            "estedad"     -> R.font.estedad_regular
            "vazirmatn"   -> R.font.vazirmatn_regular
            "iraniansans" -> R.font.iraniansans
            "sahel"       -> R.font.sahel_bold
            else -> 0
        }
        return if (res != 0) ResourcesCompat.getFont(this, res) ?: Typeface.DEFAULT else Typeface.DEFAULT
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    private fun spPx(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    private fun rvTextBitmap(
        text: String,
        tf: Typeface,
        sp: Float,
        color: Int,
        maxWidthPx: Int,
        minSp: Float = 11f,
        extraVerticalSpace: Int = 16
    ): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textAlign = Paint.Align.CENTER
            typeface = tf
        }

        var currentSp = sp
        var textWidth: Float
        do {
            paint.textSize = spPx(currentSp)
            textWidth = paint.measureText(text)
            if (textWidth <= maxWidthPx || currentSp <= minSp) break
            currentSp -= 0.5f
        } while (currentSp > minSp)

        val fm = paint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        val extraPx = dp(extraVerticalSpace)
        val totalHeight = (textHeight + extraPx).toInt().coerceAtLeast(dp(32))

        val bmp = createBitmap(maxWidthPx.coerceAtLeast(1), totalHeight)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val cx = maxWidthPx / 2f
        val baseline = totalHeight / 2f + (textHeight / 2f) - fm.descent
        canvas.drawText(text, cx, baseline, paint)
        return bmp
    }

    private fun createPrayersBitmapWithHighlight(
        prayerTimes: Map<String, String>,
        currentPrayerName: String?,
        tf: Typeface,
        maxWidthPx: Int,
        baseColor: Int,
        highlightColor: Int,
        use24HourFormat: Boolean,
        usePersianNumbers: Boolean
    ): Bitmap {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
        val items = order.map { name ->
            val raw = prayerTimes[name] ?: "--:--"
            val time = DateUtils.formatDisplayTime(raw, use24HourFormat, usePersianNumbers)
            name to time
        }

        val cols = 3
        val hPad = dp(8).toFloat()
        val vPad = dp(6).toFloat()
        val colSpacing = dp(8).toFloat()
        val rowSpacing = dp(6).toFloat()
        val contentWidth = maxWidthPx - (2f * hPad)
        val cellWidth = (contentWidth - (cols - 1) * colSpacing) / cols

        fun fitSizePx(text: String, baseSpSize: Float, minSpSize: Float = 10f): Float {
            var currentSpSize = baseSpSize
            val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = tf }
            while (currentSpSize >= minSpSize) {
                tempPaint.textSize = spPx(currentSpSize)
                if (tempPaint.measureText(text) <= cellWidth) return tempPaint.textSize
                currentSpSize -= 0.5f
            }
            return spPx(minSpSize)
        }

        data class LayoutItem(val name: String, val text: String, val textSizePx: Float, val itemHeight: Float)

        fun layoutRow(rowItems: List<Pair<String, String>>): Pair<List<LayoutItem>, Float> {
            val layouts = ArrayList<LayoutItem>(cols)
            var maxH = 0f
            for ((name, timeText) in rowItems) {
                val full = "$name: $timeText"
                val sz = fitSizePx(full, 14f)
                val tmp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = sz; typeface = tf }
                val fm = tmp.fontMetrics
                val h = fm.bottom - fm.top
                if (h > maxH) maxH = h
                layouts.add(LayoutItem(name, full, sz, h))
            }
            return layouts to maxH
        }

        val row1 = items.subList(0, 3)
        val row2 = items.subList(3, 6)

        val (r1Layout, r1H) = layoutRow(row1)
        val (r2Layout, r2H) = layoutRow(row2)
        val totalHeight = (vPad + r1H + rowSpacing + r2H + vPad).toInt().coerceAtLeast(dp(48))

        val bmp = createBitmap(maxWidthPx.coerceAtLeast(1), totalHeight)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = baseColor
            textAlign = Paint.Align.CENTER
            typeface = tf
        }
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = highlightColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(tf, Typeface.BOLD)
        }

        val contentRight = maxWidthPx - hPad
        val y1 = vPad + r1H / 2f
        val y2 = vPad + r1H + rowSpacing + r2H / 2f

        fun drawRow(layouts: List<LayoutItem>, cy: Float) {
            layouts.forEachIndexed { i, it ->
                val cx = contentRight - (cellWidth / 2f) - i * (cellWidth + colSpacing)
                val p = if (it.name == currentPrayerName) highlightPaint else normalPaint
                p.textSize = it.textSizePx
                val fm = p.fontMetrics
                val base = cy - (fm.ascent + fm.descent) / 2f
                canvas.drawText(it.text, cx, base, p)
            }
        }

        drawRow(r1Layout, y1)
        drawRow(r2Layout, y2)
        return bmp
    }

    private fun pillButtonBitmap(
        text: String,
        bgColor: Int,
        textColor: Int,
        sp: Float = 12f,
        minHeightDp: Int = 36,
        hPadDp: Int = 16
    ): Bitmap {
        val h = dp(minHeightDp)
        val padH = dp(hPadDp).toFloat()
        val minW = dp(64)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            textSize = spPx(sp)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textW = paint.measureText(text)
        val w = (textW + 2 * padH).toInt().coerceAtLeast(minW)
        val bmp = createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1))
        val c = Canvas(bmp)
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        val r = h / 2f
        c.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, bg)

        val fm = paint.fontMetrics
        val baseline = h / 2f - (fm.ascent + fm.descent) / 2f
        c.drawText(text, w / 2f, baseline, paint)
        return bmp
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Service destroying...")
        updateJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "onDestroy: Service destroyed, job and scope cancelled.")
    }

    private fun pendingService(action: String): PendingIntent {
        val intent = Intent(this, PrayerForegroundService::class.java).apply { this.action = action }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getService(this, action.hashCode(), intent, flags)
    }
}