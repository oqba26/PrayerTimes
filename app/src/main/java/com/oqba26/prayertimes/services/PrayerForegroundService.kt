package com.oqba26.prayertimes.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.text.Html
import android.util.Log
import androidx.core.app.NotificationCompat
import com.oqba26.prayertimes.MainActivity
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PrayerUtils
import com.oqba26.prayertimes.alarms.NextPrayerScheduler
import com.oqba26.prayertimes.utils.createNotifDayIconBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime

class PrayerForegroundService : Service() {

    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        NotificationService.createNotificationChannels(this)
        Log.d("PrayerService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START", "RESTART" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val currentDate = DateUtils.getCurrentDate()
                    val prayerTimes = PrayerUtils.loadPrayerTimes(applicationContext, currentDate)

                    if (prayerTimes.isNotEmpty()) {
                        val notif = createNotification(currentDate, prayerTimes)
                        startForeground(NOTIFICATION_ID, notif)
                        // تنظیم دوباره برای وقت بعدی
                        NextPrayerScheduler.scheduleForNextPrayer(applicationContext, prayerTimes)
                    } else {
                        Log.w("PrayerService", "No prayer times found for today")
                    }
                }
            }
            "STOP" -> {
                stopForegroundCompat()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotification(date: MultiDate, prayerTimes: Map<String, String>): Notification {
        val shamsi = date.getShamsiParts()
        val hijri = date.hijriParts()
        val greg = date.gregorianParts()

        val weekDay = DateUtils.getWeekDayName(date)
        val persianTitle =
            "$weekDay ${DateUtils.convertToPersianNumbers(shamsi.third.toString())} " +
                    "${DateUtils.getPersianMonthName(shamsi.second)} " +
                    "${DateUtils.convertToPersianNumbers(shamsi.first.toString())}"

        val hijriLine = "${hijri.third} ${hijri.second} ${hijri.first}"
        val gregLine = "${greg.first} ${greg.second} ${greg.third}"

        val nextPrayer = PrayerUtils.getCurrentPrayerForHighlight(prayerTimes, LocalTime.now())
        val order = listOf("طلوع بامداد","طلوع خورشید","ظهر","عصر","غروب","عشاء")

        val prayerText = order.joinToString(" | ") { key ->
            val time = DateUtils.convertToPersianNumbers(prayerTimes[key] ?: "--:--")
            if (key == nextPrayer)
                "<b><font color='#2E7D32'>$key: $time</font></b>"
            else
                "<font color='#0D47A1'>$key: $time</font>"
        }

        val bigText = """
        <div style="text-align:center;">
        <b>$persianTitle</b><br/>
        $hijriLine | $gregLine<br/><br/>
        ⏰ وقت بعدی: $nextPrayer<br/><br/>
        $prayerText
        </div>
    """.trimIndent()

        val openAppIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val dayText = DateUtils.convertToPersianNumbers(shamsi.third.toString())
        val iconBitmap = createNotifDayIconBitmap(
            this,
            dayText,
            bgColor = Color.parseColor("#0D47A1"),
            textColor = Color.WHITE
        )

        return NotificationCompat.Builder(this, NotificationService.DAILY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setLargeIcon(iconBitmap)
            .setContentTitle("📅 تقویم و اوقات شرعی")
            .setContentText("⏰ وقت بعدی: $nextPrayer")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(Html.fromHtml(bigText, Html.FROM_HTML_MODE_LEGACY))
            )
            .setOngoing(true)
            .setColor(Color.parseColor("#0D47A1"))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}