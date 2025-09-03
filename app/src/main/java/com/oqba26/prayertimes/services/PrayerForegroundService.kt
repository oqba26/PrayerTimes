package com.oqba26.prayertimes.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.IBinder
import android.text.Html
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import com.oqba26.prayertimes.MainActivity
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PrayerUtils
import kotlinx.coroutines.*
import java.time.LocalTime
import kotlin.math.roundToInt

class PrayerForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "PrayerService"
        private const val UPDATE_INTERVAL = 60_000L
        private const val ACTION_PREV = "PREV_DAY"
        private const val ACTION_NEXT = "NEXT_DAY"
        private const val ACTION_RESTART = "RESTART" // برای رفرش لحظه‌ای از تنظیمات
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var updateJob: Job? = null

    private var cachedIconDay = -1
    private var cachedIcon: IconCompat? = null

    // تاریخ انتخابی (برای دکمه‌ها)
    private var notifSelectedDate: MultiDate? = null

    override fun onCreate() {
        super.onCreate()
        NotificationService.createNotificationChannels(this)
        Log.d(TAG, "Foreground Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START", ACTION_RESTART -> {
                // فوراً یک بار رفرش (تا منتظر حلقه 60 ثانیه نباشیم)
                serviceScope.launch { postOnce() }
                startUpdatingNotification()
            }
            "STOP" -> { stopForegroundCompat(); stopSelf() }
            ACTION_PREV -> {
                val base = notifSelectedDate ?: DateUtils.getCurrentDate()
                notifSelectedDate = DateUtils.getPreviousDate(base)
                serviceScope.launch { postOnce() }
            }
            ACTION_NEXT -> {
                val base = notifSelectedDate ?: DateUtils.getCurrentDate()
                notifSelectedDate = DateUtils.getNextDate(base)
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
                    val baseDate = notifSelectedDate ?: DateUtils.getCurrentDate()
                    val times = PrayerUtils.loadPrayerTimes(applicationContext, baseDate)
                    if (times.isNotEmpty()) {
                        val n = createNotification(baseDate, times)
                        startForeground(NOTIFICATION_ID, n)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "update loop error", e)
                }
                delay(UPDATE_INTERVAL)
            }
        }
    }

    private suspend fun postOnce() {
        try {
            val baseDate = notifSelectedDate ?: DateUtils.getCurrentDate()
            val times = PrayerUtils.loadPrayerTimes(applicationContext, baseDate)
            if (times.isNotEmpty()) {
                val n = createNotification(baseDate, times)
                startForeground(NOTIFICATION_ID, n)
            }
        } catch (e: Exception) {
            Log.e(TAG, "postOnce error", e)
        }
    }

    private fun createNotification(date: MultiDate, prayerTimes: Map<String, String>): Notification {
        val shamsi = date.getShamsiParts()
        val hijri = date.hijriParts()
        val greg = date.gregorianParts()
        val weekDay = DateUtils.getWeekDayName(date)

        val title = buildString {
            append(weekDay).append(" ")
            append(DateUtils.convertToPersianNumbers(shamsi.third.toString())).append(" ")
            append(DateUtils.getPersianMonthName(shamsi.second)).append(" ")
            append(DateUtils.convertToPersianNumbers(shamsi.first.toString()))
        }
        val otherDates = "${hijri.third} ${hijri.second} ${hijri.first} | ${greg.first} ${greg.second} ${greg.third}"

        val now = LocalTime.now()
        val currentPrayer = PrayerUtils.getCurrentPrayerForHighlight(prayerTimes, now)
        val prayerText = createPrayerTimesText(prayerTimes, currentPrayer)

        // RemoteViews برای expanded (دکمه‌ها و فونت)
        val expanded = createExpandedView(title, otherDates, prayerText)

        val darkBlue = Color.parseColor("#0D47A1")
        val day = shamsi.third

        return NotificationCompat.Builder(this, NotificationService.DAILY_CHANNEL_ID)
            .setSmallIcon(getCachedDayIcon(day, usePersianDigits = true))
            // حالت جمع‌شده پیش‌فرض با عنوان/متن (به‌جای setCustomContentView)
            .setContentTitle(title)
            .setContentText(otherDates)
            // فقط حالت باز سفارشی
            .setCustomBigContentView(expanded)
            .setColor(darkBlue)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(createContentIntent())
            .build()
    }


    private fun getCachedDayIcon(day: Int, usePersianDigits: Boolean): IconCompat {
        val safeDay = day.coerceIn(1, 31)
        if (safeDay == cachedIconDay && cachedIcon != null) return cachedIcon!!
        val icon = createDaySmallIconCompat(safeDay, usePersianDigits)
        cachedIconDay = safeDay
        cachedIcon = icon
        return icon
    }

    private fun createDaySmallIconCompat(day: Int, usePersianDigits: Boolean): IconCompat {
        val dm = resources.displayMetrics
        val sizePx = (24f * dm.density).roundToInt().coerceAtLeast(24)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = sizePx * 0.06f
        }

        val raw = day.toString()
        val text = if (usePersianDigits) DateUtils.convertToPersianNumbers(raw) else raw
        p.textSize = if (day < 10) sizePx * 0.90f else sizePx * 0.78f
        val y = sizePx / 2f - (p.descent() + p.ascent()) / 2f
        c.drawText(text, sizePx / 2f, y, p)

        return IconCompat.createWithBitmap(bmp)
    }

    private fun createNextLine(date: MultiDate, now: LocalTime, times: Map<String, String>): String {
        val info = runBlocking {
            PrayerUtils.getNextPrayerNameAndTime(applicationContext, date, now, times)
        }
        return info?.let { "وقت بعدی: ${it.first} ${DateUtils.convertToPersianNumbers(it.second)}" } ?: "وقت بعدی: —"
    }

    private fun createContentIntent(): PendingIntent {
        val i = Intent(this, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(this, 0, i, flags)
    }

    private fun createPrayerTimesText(prayerTimes: Map<String, String>, currentPrayer: String?): String {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
        return order.joinToString(" | ") { name ->
            val time = prayerTimes[name]?.let { DateUtils.convertToPersianNumbers(it) } ?: "--:--"
            if (name == currentPrayer) "<b><font color='#2E7D32'>$name: $time</font></b>"
            else "<font color='#0D47A1'>$name: $time</font>"
        }
    }

    // ---------------- expanded with font bitmaps + nav ----------------
    private fun createExpandedView(title: String, otherDates: String, prayerText: String): RemoteViews {
        val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(prayerText, Html.FROM_HTML_MODE_LEGACY)
        else
            @Suppress("DEPRECATION") Html.fromHtml(prayerText)

        val rv = RemoteViews(packageName, R.layout.notification_expanded).apply {
            // fallback
            setTextViewText(R.id.tv_shamsi_date, title)
            setTextViewText(R.id.tv_other_dates, otherDates)
            setTextViewText(R.id.tv_prayer_times, spanned)

            // دکمه‌ها: چپ = NEXT (جلو) ، راست = PREV (عقب)
            setOnClickPendingIntent(R.id.iv_next_day_exp, pendingService(ACTION_NEXT))
            setOnClickPendingIntent(R.id.iv_prev_day_exp, pendingService(ACTION_PREV))

            val iconGray = 0xFF666666.toInt()
            setInt(R.id.iv_next_day_exp, "setColorFilter", iconGray)
            setInt(R.id.iv_prev_day_exp, "setColorFilter", iconGray)
        }

        // فونت برنامه روی هر سه بخش (Bitmap)
        applyAllFontBitmaps(rv, title, otherDates, spanned)
        return rv
    }

    private fun applyAllFontBitmaps(
        rv: RemoteViews,
        title: String,
        otherDates: String,
        prayerSpanned: CharSequence
    ) {
        val fontId = getSharedPreferences("settings", MODE_PRIVATE)
            .getString("fontId", "system")?.lowercase() ?: "system"

        val tf: Typeface = when (fontId) {
            "byekan"      -> ResourcesCompat.getFont(this, R.font.byekan)
            "estedad"     -> ResourcesCompat.getFont(this, R.font.estedad_regular)
            "vazirmatn"   -> ResourcesCompat.getFont(this, R.font.vazirmatn_regular)
            "iraniansans" -> ResourcesCompat.getFont(this, R.font.iraniansans)
            "sahel"       -> ResourcesCompat.getFont(this, R.font.sahel_bold)
            else          -> Typeface.DEFAULT
        } ?: Typeface.DEFAULT

        val dm = resources.displayMetrics
        val maxW = (dm.widthPixels - dp(16 + 16 + 32 + 32)).coerceAtLeast(dp(200))

        val titleColor = Color.parseColor("#333333")
        val otherColor = Color.parseColor("#555555")
        val defTextColor = Color.parseColor("#333333")

        val bmpTitle = textBitmap(title, tf, 18f, titleColor, maxW)
        val bmpOther = textBitmap(otherDates, tf, 14f, otherColor, maxW)
        val bmpPrayers = spannedBitmap(prayerSpanned, tf, 14f, defTextColor, maxW)

        rv.setImageViewBitmap(R.id.iv_title, bmpTitle)
        rv.setImageViewBitmap(R.id.iv_other, bmpOther)
        rv.setImageViewBitmap(R.id.iv_prayer_times, bmpPrayers)

        rv.setViewVisibility(R.id.iv_title, View.VISIBLE)
        rv.setViewVisibility(R.id.iv_other, View.VISIBLE)
        rv.setViewVisibility(R.id.iv_prayer_times, View.VISIBLE)

        rv.setViewVisibility(R.id.tv_shamsi_date, View.GONE)
        rv.setViewVisibility(R.id.tv_other_dates, View.GONE)
        rv.setViewVisibility(R.id.tv_prayer_times, View.GONE)
    }

    private fun spannedBitmap(
        spanned: CharSequence,
        tf: Typeface,
        sp: Float,
        defaultColor: Int,
        maxWidthPx: Int
    ): Bitmap {
        fun sp2px(v: Float) = v * resources.displayMetrics.scaledDensity
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = defaultColor
            textAlign = Paint.Align.CENTER
            textSize = sp2px(sp)
            typeface = tf
        }

        val layout: StaticLayout =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder
                    .obtain(spanned, 0, spanned.length, paint, maxWidthPx)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(spanned, paint, maxWidthPx, Layout.Alignment.ALIGN_CENTER, 1f, 0f, false)
            }

        val height = layout.height.coerceAtLeast(dp(24))
        val bmp = Bitmap.createBitmap(maxWidthPx, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        layout.draw(c)
        return bmp
    }

    private fun textBitmap(
        text: String,
        tf: Typeface,
        sp: Float,
        color: Int,
        maxWidthPx: Int,
        minSp: Float = 12f
    ): Bitmap {
        fun sp2px(v: Float) = v * resources.displayMetrics.scaledDensity
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textAlign = Paint.Align.CENTER
            this.typeface = tf
        }
        var ts = sp
        var width: Float
        do {
            paint.textSize = sp2px(ts)
            width = paint.measureText(text)
            if (width <= maxWidthPx) break
            ts -= 0.5f
        } while (ts >= minSp)

        val fm = paint.fontMetrics
        val height = (fm.bottom - fm.top).toInt().coerceAtLeast(dp(24))
        val bmp = Bitmap.createBitmap(maxWidthPx, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val cx = maxWidthPx / 2f
        val cy = height / 2f - (fm.ascent + fm.descent) / 2f
        c.drawText(text, cx, cy, paint)
        return bmp
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun pendingService(action: String): PendingIntent {
        val i = Intent(this, PrayerForegroundService::class.java).apply { this.action = action }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getService(this, action.hashCode(), i, flags)
    }

    private fun stopForegroundCompat() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
            else @Suppress("DEPRECATION") stopForeground(true)
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
    }
}