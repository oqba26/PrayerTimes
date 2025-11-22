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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.oqba26.prayertimes.MainActivity
import com.oqba26.prayertimes.R

/**
 * سرویس foreground برای پخش اذان در زمان مشخص‌شده
 */
class AdhanPlayerService : Service() {

    companion object {
        private const val CHANNEL_ID = "adhan_channel"
        private const val NOTIF_ID = 2245

        const val ACTION_PLAY = "PLAY_ADHAN"
        const val ACTION_STOP = "STOP_ADHAN"
        const val EXTRA_PRAYER_ID = "PRAYER_ID"
        const val EXTRA_ADHAN_SOUND = "ADHAN_SOUND"

        fun playNow(context: Context, prayerId: String, adhanSound: String?) {
            if (adhanSound.isNullOrEmpty() || adhanSound == "off") return
            val intent = Intent(context, AdhanPlayerService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_PRAYER_ID, prayerId)
                putExtra(EXTRA_ADHAN_SOUND, adhanSound)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> startPlayback(intent)
            ACTION_STOP -> stopPlaybackAndSelf()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startPlayback(intent: Intent) {
        stopPlayback() // توقف هر پخش در حال انجام

        val prayerId = intent.getStringExtra(EXTRA_PRAYER_ID) ?: "dhuhr"
        val adhanSound = intent.getStringExtra(EXTRA_ADHAN_SOUND) ?: "makkah"
        val resId = resolveSound(adhanSound)

        if (resId == 0) {
            stopSelf(); return
        }

        startForeground(NOTIF_ID, buildNotification(prayerId))
        player = MediaPlayer().apply {
            try {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                setAudioAttributes(attrs)

                val afd = resources.openRawResourceFd(resId) ?: return
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = false
                setOnCompletionListener { stopPlaybackAndSelf() }
                setOnErrorListener { _, _, _ -> stopPlaybackAndSelf(); true }
                prepare()
                start()
            } catch (_: Exception) {
                stopPlaybackAndSelf()
            }
        }
    }

    private fun buildNotification(prayerId: String): Notification {
        val stopIntent = Intent(this, AdhanPlayerService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 1001, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val launchIntent = Intent(this, MainActivity::class.java)
        val launchPending = PendingIntent.getActivity(
            this, 2001, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (prayerId.lowercase()) {
            "fajr" -> "اذان صبح"
            "dhuhr" -> "اذان ظهر"
            "asr" -> "اذان عصر"
            "maghrib" -> "اذان مغرب"
            "isha" -> "اذان عشاء"
            else -> "اذان"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText("در حال پخش اذان...")
            .setContentIntent(launchPending)
            .addAction(0, "توقف", stopPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .build()
    }

    private fun stopPlaybackAndSelf() {
        stopPlayback()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopPlayback() {
        runCatching { player?.stop(); player?.release() }
        player = null
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(CHANNEL_ID, "اذان", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "پخش اذان در زمان نماز"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(ch)
        }
    }

    private fun resolveSound(name: String): Int {
        return when (name.lowercase()) {
            "makkah" -> R.raw.azan_makkah_4
          //"madina" -> R.raw.azan_madina
          //"abdulbasit" -> R.raw.azan_abdulbasit
            else -> 0
        }
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }
}