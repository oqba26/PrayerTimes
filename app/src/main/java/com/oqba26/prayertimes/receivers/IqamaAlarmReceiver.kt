@file:Suppress("unused")

package com.oqba26.prayertimes.receivers

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.FontRes
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.services.NotificationService
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PrayerUtils
import com.oqba26.prayertimes.utils.textBitmap
import com.oqba26.prayertimes.viewmodels.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.math.ceil

class IqamaAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_UPDATE_IQAMA = "com.oqba26.prayertimes.UPDATE_IQAMA"
        const val ACTION_CANCEL_IQAMA = "com.oqba26.prayertimes.CANCEL_IQAMA"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_IS_DARK_THEME = "is_dark_theme"
        const val EXTRA_FONT_ID = "font_id"
        const val DEFAULT_NOTIFICATION_ID = 430
        private const val DELETE_IQAMA_REQ_OFFSET = 2000
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent?.action) {
                    ACTION_CANCEL_IQAMA -> {
                        cancelNotification(context, intent)
                    }
                    else -> {
                        val prayerNameEnglish = intent?.getStringExtra("PRAYER_NAME") ?: return@launch
                        val prayerTimeStr = intent.getStringExtra("PRAYER_TIME") ?: return@launch
                        val isDarkTheme = intent.getBooleanExtra(EXTRA_IS_DARK_THEME, false)
                        val fontId = intent.getStringExtra(EXTRA_FONT_ID) ?: "estedad"
                        updateNotification(context, prayerNameEnglish, prayerTimeStr, isDarkTheme, fontId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun cancelNotification(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        if (notificationId != 0) {
            val alarmManager = context.getSystemService<AlarmManager>()!!
            val updateIntent = Intent(context, IqamaAlarmReceiver::class.java).apply {
                action = ACTION_UPDATE_IQAMA
            }
            val updatePi = PendingIntent.getBroadcast(context, notificationId, updateIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            updatePi?.let { alarmManager.cancel(it) }

            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun updateNotification(context: Context, prayerNameEnglish: String, prayerTimeStr: String, isDarkTheme: Boolean, fontId: String) {
        val prayerTime = PrayerUtils.parseTimeSafely(prayerTimeStr) ?: return

        val now = ZonedDateTime.now()
        var prayerDateTime = now.with(prayerTime)
        if (prayerDateTime.isBefore(now)) {
            prayerDateTime = prayerDateTime.plusDays(1)
        }
        val prayerMillis = prayerDateTime.toInstant().toEpochMilli()
        val remainingMillis = prayerMillis - System.currentTimeMillis()
        val notificationId = DEFAULT_NOTIFICATION_ID + prayerNameEnglish.hashCode()

        if (remainingMillis <= 0) {
            NotificationManagerCompat.from(context).cancel(notificationId)
            return
        }

        val remainingMinutes = ceil(remainingMillis.toDouble() / 60000).toLong()

        val preferences = context.dataStore.data.first()
        val usePersianNumbers = preferences[booleanPreferencesKey("use_persian_numbers")] ?: true

        val prayerNameArabic = when (prayerNameEnglish.lowercase()) {
            "fajr" -> "صبح"
            "dhuhr" -> "ظهر"
            "asr" -> "عصر"
            "maghrib" -> "مغرب"
            "isha" -> "عشاء"
            else -> prayerNameEnglish
        }

        val timerText = if (remainingMinutes > 1) {
            "${DateUtils.convertToPersianNumbers(remainingMinutes.toString(), usePersianNumbers)} دقیقه مانده تا نماز $prayerNameArabic"
        } else {
            "کمتر از یک دقیقه مانده تا نماز $prayerNameArabic"
        }

        ensureIqamaChannel(context)

        val typeface = try {
            val fontResId = getFontResId(fontId)
            if (fontResId != 0) {
                ResourcesCompat.getFont(context, fontResId)
            } else {
                Typeface.DEFAULT
            }
        } catch (e: Exception) {
            Typeface.DEFAULT
        } ?: Typeface.DEFAULT

        val textColor: Int
        val titleColor: Int
        val backgroundColor: Int

        if (isDarkTheme) {
            titleColor = Color.WHITE
            textColor = "#80DEEA".toColorInt()
            backgroundColor = "#212121".toColorInt() // Dark gray background
        } else {
            titleColor = Color.BLACK
            textColor = "#0D47A1".toColorInt()
            backgroundColor = Color.WHITE
        }

        val displayMetrics = context.resources.displayMetrics
        val imageWidth = displayMetrics.widthPixels - (2 * 16 * displayMetrics.density).toInt()

        val titleBitmap = textBitmap(context, "اقامه نماز", typeface, 18f, titleColor, imageWidth)
        val contentBitmap = textBitmap(context, timerText, typeface, 16f, textColor, imageWidth)

        val remoteViews = RemoteViews(context.packageName, R.layout.iqama_notification_layout)
        remoteViews.setInt(R.id.iqama_notification_root, "setBackgroundColor", backgroundColor)
        remoteViews.setImageViewBitmap(R.id.iqama_title_image, titleBitmap)
        remoteViews.setImageViewBitmap(R.id.iqama_text_image, contentBitmap)
        remoteViews.setViewVisibility(R.id.iqama_timer_image, View.GONE) // Hide the extra timer view

        val deleteIntent = Intent(context, IqamaAlarmReceiver::class.java).apply {
            action = ACTION_CANCEL_IQAMA
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val deletePi = PendingIntent.getBroadcast(context, notificationId + DELETE_IQAMA_REQ_OFFSET, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, NotificationService.IQAMA_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setCustomContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .setDeleteIntent(deletePi)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())

        // Schedule next action
        val alarmManager = context.getSystemService<AlarmManager>()!!
        if (remainingMillis > 60000) {
            // More than 1 minute left, schedule an update for the next minute.
            val nowMillis = System.currentTimeMillis()
            val nextUpdateMillis = nowMillis - nowMillis % 60000 + 60000
            
            val updateIntent = Intent(context, IqamaAlarmReceiver::class.java).apply {
                action = ACTION_UPDATE_IQAMA
                putExtra("PRAYER_NAME", prayerNameEnglish)
                putExtra("PRAYER_TIME", prayerTimeStr)
                putExtra(EXTRA_IS_DARK_THEME, isDarkTheme)
                putExtra(EXTRA_FONT_ID, fontId)
            }
            val updatePi = PendingIntent.getBroadcast(context, notificationId, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, nextUpdateMillis, updatePi)
        } else {
            // This is the last minute. Schedule the final cancellation to happen exactly at prayer time.
            // We can reuse the same deleteIntent that is used for swiping away the notification.
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, prayerMillis, deletePi)
        }
    }

    private fun ensureIqamaChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = NotificationService.IQAMA_CHANNEL_ID
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                val ch = NotificationChannel(channelId, "اعلان اقامه نماز", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "برای یادآوری زمان اقامه نماز"
                    setSound(null, null)
                    enableVibration(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    @FontRes
    private fun getFontResId(fontId: String): Int {
        return when (fontId) {
            "estedad" -> R.font.estedad_regular
            "byekan" -> R.font.byekan
            "sahel_bold" -> R.font.sahel_bold
            "byekan_bold" -> R.font.byekan_bold
            "iraniansans" -> R.font.iraniansans
            "sahel_black" -> R.font.sahel_black
            "estedad_bold" -> R.font.estedad_bold
            "estedad_black" -> R.font.estedad_black
            "estedad_light" -> R.font.estedad_light
            "estedad_medium" -> R.font.estedad_medium
            "vazirmatn_bold" -> R.font.vazirmatn_bold
            "vazirmatn_thin" -> R.font.vazirmatn_thin
            "estedad_regular" -> R.font.estedad_regular
            "vazirmatn_black" -> R.font.vazirmatn_black
            "vazirmatn_light" -> R.font.vazirmatn_light
            "vazirmatn_medium" -> R.font.vazirmatn_medium
            "vazirmatn_regular" -> R.font.vazirmatn_regular
            else -> R.font.estedad_regular // Fallback to a default font
        }
    }
}
