package com.oqba26.prayertimes.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.oqba26.prayertimes.R

class AdhanPlayerService : Service() {

    companion object {
        private const val CHANNEL_ID = "adhan_channel"
        private const val NOTIF_ID = 2245

        const val ACTION_PLAY = "PLAY_ADHAN"
        const val ACTION_STOP = "STOP_ADHAN"
        const val EXTRA_PRAYER_ID = "PRAYER_ID" // fajr/dhuhr/asr/maghrib/isha

        fun playNow(context: Context, prayerId: String = "dhuhr") {
            val intent = Intent(context, AdhanPlayerService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_PRAYER_ID, prayerId)
            }
            // استفاده از ContextCompat برای سازگاری با همه API ها
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdhanPlayerService::class.java).apply { action = ACTION_STOP }
            // برای توقف لازم نیست foreground service استارت کنیم
            context.startService(intent)
        }
    }

    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_PLAY -> {
                val prayerId = intent.getStringExtra(EXTRA_PRAYER_ID) ?: "dhuhr"
                startForeground(NOTIF_ID, buildNotification(prayerId))
                startPlayback(resolveSelectedVoiceRes())
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun startPlayback(resId: Int) {
        stopPlayback()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            val afd = resources.openRawResourceFd(resId)!!
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            isLooping = false
            setOnCompletionListener {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            setOnErrorListener { _, _, _ ->
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(); true
            }
            prepare()
            start()
        }
    }

    private fun stopPlayback() {
        runCatching { player?.stop(); player?.release() }
        player = null
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun buildNotification(prayerId: String): Notification {
        val stopIntent = Intent(this, AdhanPlayerService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 1001, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = when (prayerId) {
            "fajr" -> "زمان اذان صبح"
            "dhuhr" -> "زمان اذان ظهر"
            "asr" -> "زمان نماز عصر"
            "maghrib" -> "زمان اذان مغرب"
            "isha" -> "زمان نماز عشاء"
            else -> "زمان اذان"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("برای قطع پخش، توقف را بزنید")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(0, "توقف", stopPending)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID, "اذان", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)      // صدا از MediaPlayer پخش می‌شود
                description = "پخش اذان در زمان نماز"
                enableVibration(false)
                setBypassDnd(true)        // کمک می‌کند در حالت DND هم دیده شود
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(ch)
        }
    }

    private fun resolveSelectedVoiceRes(): Int = R.raw.azan_makkah_4
}