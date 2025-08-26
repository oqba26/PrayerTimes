package com.oqba26.prayertimes.services

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.*
import com.oqba26.prayertimes.widget.ModernWidgetProvider
import kotlinx.coroutines.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ClockUpdateService : Service() {

    private val handler = Handler()
    private lateinit var runnable: Runnable
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createSilentNotification())
        startLiveUpdates()
        return START_STICKY
    }

    private fun startLiveUpdates() {
        runnable = object : Runnable {
            override fun run() {
                try {
                    // فقط یک سیگنال آپدیت به Provider ارسال کن
                    val intent = Intent(this@ClockUpdateService, ModernWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    }
                    val ids = AppWidgetManager.getInstance(applicationContext)
                        .getAppWidgetIds(ComponentName(applicationContext, ModernWidgetProvider::class.java))

                    if (ids.isNotEmpty()) {
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        sendBroadcast(intent)
                    }
                } catch (e: Exception) {
                    Log.e("ClockUpdateService", "Error in widget update runnable", e)
                }

                // هر 1 ثانیه تکرار کن
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadPrayerTimesSync(context: Context, date: MultiDate): Map<String, String> {
        return runBlocking {
            loadPrayerTimes(context, date)
        }
    }

    private fun createSilentNotification(): Notification {
        return NotificationCompat.Builder(this, "clock_channel_id")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("PrayerTimes Widget")
            .setContentText("در حال به‌روزرسانی ویجت")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "clock_channel_id",
                "PrayerTimes Background",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "کانال بروزرسانی ساعت ویجت"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildHijriGregorianText(date: MultiDate): String {
        val hijri = date.hijriParts()
        val greg = date.gregorianParts()

        return "${hijri.third} ${hijri.second} ${hijri.first} | ${greg.first} ${greg.second} ${greg.third}"
    }
}