package com.oqba26.prayertimes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PrayerUtils
import com.oqba26.prayertimes.utils.createPrayerTimesBitmapWithHighlight
import com.oqba26.prayertimes.utils.dp
import com.oqba26.prayertimes.utils.pillButtonBitmap
import com.oqba26.prayertimes.utils.textBitmap
import com.oqba26.prayertimes.viewmodels.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale

open class ModernWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        const val ACTION_WIDGET_PREV = "com.oqba26.prayertimes.widget.PREV"
        const val ACTION_WIDGET_NEXT = "com.oqba26.prayertimes.widget.NEXT"
        const val ACTION_WIDGET_TODAY = "com.oqba26.prayertimes.widget.TODAY"

        private const val PREFS = "widget_prefs"
        private fun keySel(id: Int) = "selected_$id"
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scope.launch {
            appWidgetIds.forEach { id ->
                if (!context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(keySel(id))) {
                    setSelectedDate(context, id, DateUtils.getCurrentDate())
                }
                updateOneWidget(context, appWidgetManager, id)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val mgr = AppWidgetManager.getInstance(context)
        val cn = ComponentName(context, this.javaClass)

        val appWidgetIds = mgr.getAppWidgetIds(cn)
        if (appWidgetIds == null || appWidgetIds.isEmpty()) {
            return
        }

        val pendingResult = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    Intent.ACTION_TIME_TICK,
                    AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                        for (appWidgetId in appWidgetIds) {
                            updateOneWidget(context, mgr, appWidgetId)
                        }
                    }

                    ACTION_WIDGET_PREV, ACTION_WIDGET_NEXT, ACTION_WIDGET_TODAY -> {
                        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            val base = getSelectedDate(context, appWidgetId)
                            val newDate = when (intent.action) {
                                ACTION_WIDGET_PREV -> DateUtils.getPreviousDate(base)
                                ACTION_WIDGET_NEXT -> DateUtils.getNextDate(base)
                                ACTION_WIDGET_TODAY -> DateUtils.getCurrentDate()
                                else -> base
                            }
                            setSelectedDate(context, appWidgetId, newDate)
                            updateOneWidget(context, mgr, appWidgetId)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDisabled(context: Context?) {
        job.cancel()
        super.onDisabled(context)
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun updateOneWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Log.d("ModernWidgetProvider", "updateOneWidget - START for ID: $appWidgetId")
        try {
            val appSettings = context.dataStore.data.first()

            val usePersianNumbers = appSettings[booleanPreferencesKey("use_persian_numbers")] ?: true

            val isDarkTheme = appSettings[booleanPreferencesKey("is_dark_theme")] ?: false
            val use24HourFormat = appSettings[booleanPreferencesKey("use_24_hour_format")] ?: true
            val fontId = appSettings[stringPreferencesKey("fontId")] ?: "estedad"

            val primaryTextColor: Int
            val secondaryTextColor: Int
            val prayerTimeColor: Int
            val prayerHighlightColor: Int
            val navButtonColor: Int
            val todayButtonColor: Int
            val separatorColor: Int
            val backgroundResource: Int

            if (isDarkTheme) {
                primaryTextColor = Color.WHITE
                secondaryTextColor = Color.LTGRAY
                prayerTimeColor = "#80DEEA".toColorInt()
                prayerHighlightColor = "#FFF59D".toColorInt()
                navButtonColor = "#78909C".toColorInt()
                todayButtonColor = "#64B5F6".toColorInt()
                separatorColor = "#424242".toColorInt()
                backgroundResource = R.drawable.widget_background_dark
            } else {
                primaryTextColor = Color.BLACK
                secondaryTextColor = Color.DKGRAY
                prayerTimeColor = "#0D47A1".toColorInt()
                prayerHighlightColor = "#2E7D32".toColorInt()
                navButtonColor = "#546E7A".toColorInt()
                todayButtonColor = "#1976D2".toColorInt()
                separatorColor = "#BDBDBD".toColorInt()
                backgroundResource = R.drawable.widget_background_light
            }

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val isLarge = minW >= 200 || minH >= 180

            val date = getSelectedDate(context, appWidgetId)
            val times = PrayerUtils.loadPrayerTimes(context, date)
            val weekDay = DateUtils.getWeekDayName(date)
            val persianDate = "$weekDay ${DateUtils.formatShamsiLong(date, usePersianNumbers)}"
            val hijriDate = DateUtils.formatHijriLong(date, usePersianNumbers)
            val gregDate = DateUtils.formatGregorianLong(date, usePersianNumbers)
            val hijriGregLine = "$hijriDate | $gregDate"

            val now = LocalTime.now()
            val timeNowRaw = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            val timeDisplay = DateUtils.formatDisplayTime(timeNowRaw, use24HourFormat, usePersianNumbers)

            val highlightPrayer = computeHighlightPrayer(now, times)
            val tf = resolveTypeface(context, fontId)

            val layoutId = if (isLarge) R.layout.modern_widget_layout_large else R.layout.modern_widget_layout
            val views = RemoteViews(context.packageName, layoutId).apply {
                setInt(R.id.widget_background_view, "setBackgroundResource", backgroundResource)
                val separatorIds = if (isLarge) listOf(R.id.separator1_large, R.id.separator2_large) else listOf(R.id.separator1_small, R.id.separator2_small)
                separatorIds.forEach { id -> setInt(id, "setBackgroundColor", separatorColor) }

                if (isLarge) {
                    if (fontId == "system") {
                        setTextViewText(R.id.tv_clock, timeDisplay)
                        setTextColor(R.id.tv_clock, primaryTextColor)
                        setTextViewText(R.id.tv_persian_date, persianDate)
                        setTextColor(R.id.tv_persian_date, prayerHighlightColor)
                        setTextViewText(R.id.tv_hg_date, hijriGregLine)
                        setTextColor(R.id.tv_hg_date, secondaryTextColor)

                        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
                        val prayersLine = order.joinToString(" | ") { name ->
                            val raw = times[name] ?: "--:--"
                            val t = DateUtils.formatDisplayTime(raw, use24HourFormat, usePersianNumbers)
                            val colorHex = String.format("#%06X", 0xFFFFFF and if (name == highlightPrayer) prayerHighlightColor else prayerTimeColor)
                            if (name == highlightPrayer) "<b><font color=''$colorHex''>$name: $t</font></b>" else "<font color=''$colorHex''>$name: $t</font>"
                        }
                        val spanned = Html.fromHtml(prayersLine, Html.FROM_HTML_MODE_LEGACY)
                        setTextViewText(R.id.tv_prayers_line, spanned)

                        setViewVisibility(R.id.tv_clock, View.VISIBLE); setViewVisibility(R.id.iv_clock, View.GONE)
                        setViewVisibility(R.id.tv_persian_date, View.VISIBLE); setViewVisibility(R.id.iv_persian_date, View.GONE)
                        setViewVisibility(R.id.tv_hg_date, View.VISIBLE); setViewVisibility(R.id.iv_hg_date, View.GONE)
                        setViewVisibility(R.id.tv_prayers_line, View.VISIBLE); setViewVisibility(R.id.iv_prayers, View.GONE)
                    } else {
                        setImageViewBitmap(R.id.iv_clock, textBitmap(context, timeDisplay, tf, 26f, primaryTextColor, dp(context, 260)))
                        setImageViewBitmap(R.id.iv_persian_date, textBitmap(context, persianDate, tf, 16f, prayerHighlightColor, dp(context, 260)))
                        setImageViewBitmap(R.id.iv_hg_date, textBitmap(context, hijriGregLine, tf, 13f, secondaryTextColor, dp(context, 260)))
                        val bmpPrayers = createPrayerTimesBitmapWithHighlight(context, times, highlightPrayer, tf, dp(context, 300), prayerTimeColor, prayerHighlightColor, use24HourFormat, usePersianNumbers)
                        setImageViewBitmap(R.id.iv_prayers, bmpPrayers)

                        setViewVisibility(R.id.iv_clock, View.VISIBLE); setViewVisibility(R.id.tv_clock, View.GONE)
                        setViewVisibility(R.id.iv_persian_date, View.VISIBLE); setViewVisibility(R.id.tv_persian_date, View.GONE)
                        setViewVisibility(R.id.iv_hg_date, View.VISIBLE); setViewVisibility(R.id.tv_hg_date, View.GONE)
                        setViewVisibility(R.id.iv_prayers, View.VISIBLE); setViewVisibility(R.id.tv_prayers_line, View.GONE)
                    }
                    applyNavButtonsBitmaps(context, this, tf, showTodayButton = !isToday(date), navButtonColor, todayButtonColor)
                    setOnClickPendingIntent(R.id.iv_prev_day, pending(context, ACTION_WIDGET_PREV, appWidgetId, this@ModernWidgetProvider.javaClass))
                    setOnClickPendingIntent(R.id.iv_next_day, pending(context, ACTION_WIDGET_NEXT, appWidgetId, this@ModernWidgetProvider.javaClass))
                    setOnClickPendingIntent(R.id.btn_today,  pending(context, ACTION_WIDGET_TODAY, appWidgetId, this@ModernWidgetProvider.javaClass))
                } else {
                    if (fontId == "system") {
                        setTextViewText(R.id.tv_widget_time, timeDisplay); setTextColor(R.id.tv_widget_time, primaryTextColor)
                        setTextViewText(R.id.tv_widget_date_shamsi, persianDate); setTextColor(R.id.tv_widget_date_shamsi, prayerHighlightColor)
                        setTextViewText(R.id.tv_widget_date_hijri_gregorian, hijriGregLine); setTextColor(R.id.tv_widget_date_hijri_gregorian, secondaryTextColor)
                        val nextInfo = PrayerUtils.getNextPrayerNameAndTime(context, date, now, times)
                        val nextRaw = nextInfo?.second ?: "--:--"
                        val nextText = nextInfo?.let { "${it.first} - ${DateUtils.formatDisplayTime(nextRaw, use24HourFormat, usePersianNumbers)}" } ?: "—"
                        setTextViewText(R.id.tv_widget_next_prayer, nextText); setTextColor(R.id.tv_widget_next_prayer, prayerHighlightColor)

                        setViewVisibility(R.id.tv_widget_time, View.VISIBLE); setViewVisibility(R.id.iv_widget_time, View.GONE)
                        setViewVisibility(R.id.tv_widget_date_shamsi, View.VISIBLE); setViewVisibility(R.id.iv_widget_shamsi, View.GONE)
                        setViewVisibility(R.id.tv_widget_date_hijri_gregorian, View.VISIBLE); setViewVisibility(R.id.iv_widget_hg, View.GONE)
                        setViewVisibility(R.id.tv_widget_next_prayer, View.VISIBLE); setViewVisibility(R.id.iv_widget_next, View.GONE)
                    } else {
                        setImageViewBitmap(R.id.iv_widget_time, textBitmap(context, timeDisplay, tf, 22f, primaryTextColor, dp(context, 240)))
                        setImageViewBitmap(R.id.iv_widget_shamsi, textBitmap(context, persianDate, tf, 14f, prayerHighlightColor, dp(context, 240)))
                        setImageViewBitmap(R.id.iv_widget_hg, textBitmap(context, hijriGregLine, tf, 12f, secondaryTextColor, dp(context, 240)))
                        val nextInfo = PrayerUtils.getNextPrayerNameAndTime(context, date, now, times)
                        val nextRaw = nextInfo?.second ?: "--:--"
                        val nextText = nextInfo?.let { "${it.first} - ${DateUtils.formatDisplayTime(nextRaw, use24HourFormat, usePersianNumbers)}" } ?: "—"
                        setImageViewBitmap(R.id.iv_widget_next, textBitmap(context, nextText, tf, 14f, prayerHighlightColor, dp(context, 240)))

                        setViewVisibility(R.id.iv_widget_time, View.VISIBLE); setViewVisibility(R.id.tv_widget_time, View.GONE)
                        setViewVisibility(R.id.iv_widget_shamsi, View.VISIBLE); setViewVisibility(R.id.tv_widget_date_shamsi, View.GONE)
                        setViewVisibility(R.id.iv_widget_hg, View.VISIBLE); setViewVisibility(R.id.tv_widget_date_hijri_gregorian, View.GONE)
                        setViewVisibility(R.id.iv_widget_next, View.VISIBLE); setViewVisibility(R.id.tv_widget_next_prayer, View.GONE)
                    }
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d("ModernWidgetProvider", "updateOneWidget - END for ID: $appWidgetId")
        } catch (e: Exception) {
            Log.e("ModernWidgetProvider", "updateOneWidget - error", e)
        }
    }

    private fun pending(context: Context, action: String, appWidgetId: Int, providerClass: Class<out AppWidgetProvider>): PendingIntent {
        val i = Intent(context, providerClass).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, (action.hashCode() + appWidgetId), i, flags)
    }

    private fun getSelectedDate(context: Context, id: Int): MultiDate {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val g = p.getString(keySel(id), null)
        return if (g.isNullOrEmpty()) DateUtils.getCurrentDate() else parseMultiDateFromGregorian(g)
    }

    private fun setSelectedDate(context: Context, id: Int, date: MultiDate) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(keySel(id), date.gregorian) }
    }

    private fun parseMultiDateFromGregorian(gregorianDate: String): MultiDate {
        val parts = gregorianDate.split("/")
        if (parts.size != 3) return DateUtils.getCurrentDate()
        return try {
            val y = parts[0].toInt()
            val m = parts[1].toInt()
            val d = parts[2].toInt()
            val sh = DateUtils.gregorianToJalali(y, m, d)
            val hj = DateUtils.convertToHijri(y, m, d)
            MultiDate(sh, hj, gregorianDate)
        } catch (_: NumberFormatException) {
            DateUtils.getCurrentDate()
        }
    }

    private fun isToday(date: MultiDate): Boolean = runCatching { date.gregorian == DateUtils.getCurrentDate().gregorian }.getOrDefault(false)

    private fun resolveTypeface(context: Context, fontId: String): Typeface {
        val fontResId = when (fontId.lowercase(Locale.ROOT)) {
            "byekan" -> R.font.byekan
            "estedad" -> R.font.estedad_regular
            "vazirmatn" -> R.font.vazirmatn_regular
            "iraniansans" -> R.font.iraniansans
            "sahel" -> R.font.sahel_bold
            else -> 0
        }
        return if (fontResId != 0) ResourcesCompat.getFont(context, fontResId) ?: Typeface.DEFAULT else Typeface.DEFAULT
    }

    private fun applyNavButtonsBitmaps(
        context: Context, rv: RemoteViews, tf: Typeface,
        showTodayButton: Boolean, navColor: Int, todayColor: Int
    ) {
        val navTextColor = Color.WHITE // Always white for better contrast on colored buttons
        val bmpPrev = pillButtonBitmap(context, "روز قبل", tf, navColor, 16, navTextColor)
        val bmpNext = pillButtonBitmap(context, "روز بعد", tf, navColor, 16, navTextColor)
        val bmpToday = pillButtonBitmap(context, "بازگشت به امروز", tf, todayColor, 20, navTextColor)

        rv.setImageViewBitmap(R.id.iv_prev_day, bmpPrev)
        rv.setImageViewBitmap(R.id.iv_next_day, bmpNext)
        rv.setImageViewBitmap(R.id.btn_today, bmpToday)
        rv.setViewVisibility(R.id.btn_today, if (showTodayButton) View.VISIBLE else View.INVISIBLE)
    }

    private fun computeHighlightPrayer(now: LocalTime, times: Map<String, String>): String? {
        val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
        val stickMinutes = 30L

        fun toAsciiDigits(persianDigits: String): String {
            val map = mapOf('۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
                '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9')
            return persianDigits.map { map[it] ?: it }.joinToString("")
        }

        fun parsePrayerTime(prayerName: String, timeString: String): LocalTime? {
            return try {
                val asciiTime = toAsciiDigits(timeString.trim())
                val parts = asciiTime.split(":")
                if (parts.size != 2) return null
                val hour = parts[0].toIntOrNull() ?: return null
                val minute = parts[1].toIntOrNull() ?: return null
                LocalTime.of(hour % 24, minute)
            } catch (e: Exception) {
                Log.w("ModernWidgetProvider", "Could not parse time for $prayerName: $timeString", e)
                null
            }
        }

        val parsedPrayerTimes = order.mapNotNull { name ->
            times[name]?.let { timeStr ->
                val parsedTime = parsePrayerTime(name, timeStr)
                parsedTime?.let { Pair(name, it) }
            }
        }.sortedBy { it.second }

        if (parsedPrayerTimes.isEmpty()) return null

        for ((name, prayerTime) in parsedPrayerTimes) {
            val prayerTimePlusStick = prayerTime.plusMinutes(stickMinutes)
            if (!now.isAfter(prayerTimePlusStick)) return name
        }
        return parsedPrayerTimes.firstOrNull()?.first
    }
}
