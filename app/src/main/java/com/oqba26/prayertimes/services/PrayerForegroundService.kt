package com.oqba26.prayertimes.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.oqba26.prayertimes.MainActivity
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.services.NotificationService.Companion.DAILY_CHANNEL_ID
import com.oqba26.prayertimes.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PrayerForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var updateJob: Job? = null
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        Log.d("PrayerForegroundService", "Service onCreate")

        // تنظیم WakeLock برای جلوگیری از خواب رفتن دستگاه
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PrayerTimes::ForegroundServiceWakeLock"
        )

        // تنظیم کانال نوتیفیکیشن
        createNotificationChannel()

        // حذف نوتیف‌های قدیمی
        NotificationService.clearAllOldNotifications(this)

        // شروع با نوتیفیکیشن پیش‌فرض
        startForeground(NOTIFICATION_ID, createDefaultNotification())
    }

    // ساخت آیکون عدد روز برای نوتیفیکیشن
    private fun createDayNumberIcon(dayText: String): IconCompat {
        val dm = resources.displayMetrics
        val sizeDp = 24
        val sizePx = (sizeDp * dm.density).toInt().coerceAtLeast(24)

        val bmp = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)

        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = sizePx * 0.75f
        }
        val y = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(dayText, sizePx / 2f, y, paint)

        return IconCompat.createWithBitmap(bmp)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("PrayerForegroundService", "onStartCommand - Action: ${intent?.action}")

        when (intent?.action) {
            "START" -> {
                try {
                    // دریافت داده‌ها به صورت جداگانه
                    val dateSerial = intent.getSerializableExtra("date") as? MultiDate
                    @Suppress("UNCHECKED_CAST")
                    val prayerTimes = intent.getSerializableExtra("prayerTimes") as? Map<String, String>

                    if (dateSerial != null && prayerTimes != null) {
                        startForegroundService(dateSerial, prayerTimes)
                    } else {
                        Log.e("PrayerForegroundService", "داده‌های نماز null است")
                    }
                } catch (e: Exception) {
                    Log.e("PrayerForegroundService", "خطا در شروع سرویس", e)
                }
            }
            "STOP" -> {
                stopForegroundService()
                stopSelf()
            }
            "RESTART" -> {
                handleRestart()
            }
        }

        return START_STICKY
    }

    private fun startForegroundService(date: MultiDate, prayerTimes: Map<String, String>) {
        try {
            // تنظیم WakeLock
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(10*60*1000L /*10 minutes*/)
            }

            val notification = createNotification(date, prayerTimes)
            startForeground(NOTIFICATION_ID, notification)

            // شروع بروزرسانی دوره‌ای نماز فعلی
            startPeriodicUpdate(date, prayerTimes)

            Log.d("PrayerForegroundService", "سرویس foreground شروع شد")

        } catch (e: Exception) {
            Log.e("PrayerForegroundService", "خطا در شروع foreground service", e)
        }
    }

    private fun startPeriodicUpdate(date: MultiDate, prayerTimes: Map<String, String>) {
        updateJob?.cancel()
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                try {
                    val updatedNotification = createNotification(date, prayerTimes)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification)

                    // هر 30 ثانیه بروزرسانی کن
                    delay(30000L)
                } catch (e: Exception) {
                    Log.e("PrayerForegroundService", "خطا در بروزرسانی نوتیفیکیشن", e)
                    delay(60000L) // در صورت خطا، 1 دقیقه صبر کن
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("PrayerForegroundService", "Task removed - راه‌اندازی مجدد سرویس")

        // بلافاصله سرویس را دوباره راه‌اندازی کن
        val restartServiceIntent = Intent(applicationContext, PrayerForegroundService::class.java)
        restartServiceIntent.action = "RESTART"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartServiceIntent)
        } else {
            startService(restartServiceIntent)
        }

        scheduleServiceRestart()
    }

    private fun scheduleServiceRestart() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val restartIntent = Intent(this, PrayerForegroundService::class.java).apply {
                action = "RESTART"
            }
            val pendingIntent = PendingIntent.getService(
                this, 0, restartIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                else PendingIntent.FLAG_UPDATE_CURRENT
            )

            val triggerTime = System.currentTimeMillis() + 5000 // 5 ثانیه بعد
            alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)

            Log.d("PrayerForegroundService", "Alarm برای restart تنظیم شد")
        } catch (e: Exception) {
            Log.e("PrayerForegroundService", "خطا در تنظیم alarm restart", e)
        }
    }

    private fun handleRestart() {
        try {
            val today = getCurrentDate()

            CoroutineScope(Dispatchers.IO).launch {
                val prayerTimes = loadPrayerTimes(applicationContext, today)
                if (prayerTimes.isNotEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        startForegroundService(today, prayerTimes)
                    }
                    Log.d("PrayerForegroundService", "سرویس با موفقیت restart شد")
                } else {
                    Log.w("PrayerForegroundService", "اوقات نماز برای restart یافت نشد")
                }
            }
        } catch (e: Exception) {
            Log.e("PrayerForegroundService", "خطا در restart سرویس", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DAILY_CHANNEL_ID,
                "اوقات نماز دائمی",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش دائمی تاریخ و اوقات نماز"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createDefaultNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, DAILY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("تقویم و اوقات نماز")
            .setContentText("در حال بارگیری...")
            .setOngoing(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotification(date: MultiDate, prayerTimes: Map<String, String>): Notification {
        val shamsi = date.getShamsiParts()
        val hijri = date.hijriParts()
        val greg = date.gregorianParts()
        val weekDay = getWeekDayName(date)

        // عنوان: روز و تاریخ شمسی - حالت عادی
        val persianTitle = "$weekDay ${convertToPersianNumbers(shamsi.third.toString())} " +
                "${getPersianMonthName(shamsi.second)} ${convertToPersianNumbers(shamsi.first.toString())}"

        // خط اول: تاریخ قمری | میلادی - حالت عادی
        val hijriLine = "${convertToPersianNumbers(hijri.third)} ${hijri.second} ${convertToPersianNumbers(hijri.first)}"
        val gregorianLine = "${greg.first} ${greg.second} ${greg.third}"
        val dateLineRTL = "$hijriLine قمری، $gregorianLine میلادی"

        // نماز فعلی - حالت عادی
        val currentPrayer = getCurrentPrayerNameFixed(prayerTimes)
        val currentPrayerText = if (currentPrayer.isNotEmpty()) {
            "وضعیت فعلی: $currentPrayer"
        } else {
            "اوقات شرعی امروز"
        }

        // اوقات شرعی - حالت عادی
        val timeLine = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
            .mapNotNull { key -> prayerTimes[key]?.let { "$key ${convertToPersianNumbers(it)}" } }
            .joinToString("  -  ")
        val timeLineRTL = "اوقات شرعی: $timeLine"

        // متن کامل - بدون هیچ Unicode control
        val fullTextRTL = "$dateLineRTL\n$currentPrayerText\n$timeLineRTL"

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        // آیکون عدد روز (فارسی)
        val dayNumber = convertToPersianNumbers(shamsi.third.toString())

        val builder = NotificationCompat.Builder(this, DAILY_CHANNEL_ID)
            .setContentTitle(persianTitle)
            .setContentText(currentPrayerText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(fullTextRTL)
                .setSummaryText("") // خالی کردن summary
            )
            .setColor(Color.rgb(27, 94, 32))
            .setOngoing(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(0) // غیرفعال کردن تمام default ها
            .setGroup("prayer_group") // گروه‌بندی برای RTL
            .setLocalOnly(true) // محلی کردن نوتیفیکیشن

        // تنظیم آیکون روز
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setSmallIcon(createDayNumberIcon(dayNumber))
        } else {
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
        }

        return builder.build()
    }

    private fun stopForegroundService() {
        try {
            updateJob?.cancel()
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            Log.d("PrayerForegroundService", "سرویس foreground متوقف شد")
        } catch (e: Exception) {
            Log.e("PrayerForegroundService", "خطا در متوقف کردن سرویس", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundService()
        Log.d("PrayerForegroundService", "Service onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}