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
import com.oqba26.prayertimes.R

class AdhanPlayerService : Service() {

    companion object {
        private const val CHANNEL_ID = "adhan_channel"
        private const val NOTIF_ID = 2245

        const val ACTION_PLAY = "PLAY_ADHAN"
        const val ACTION_STOP = "STOP_ADHAN"
        const val EXTRA_PRAYER_ID = "PRAYER_ID" // fajr/dhuhr/etc. for notification title
        const val EXTRA_ADHAN_SOUND = "ADHAN_SOUND" // The sound name, e.g., "makkah"

        fun playNow(context: Context, prayerId: String, adhanSound: String?) {
            if (adhanSound.isNullOrEmpty() || adhanSound == "off") return

            val intent = Intent(context, AdhanPlayerService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_PRAYER_ID, prayerId)
                putExtra(EXTRA_ADHAN_SOUND, adhanSound)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdhanPlayerService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
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
            ACTION_STOP -> {
                stopPlayback()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                stopSelf()
            }
            ACTION_PLAY -> {
                val prayerId = intent.getStringExtra(EXTRA_PRAYER_ID) ?: "dhuhr"
                val adhanSound = intent.getStringExtra(EXTRA_ADHAN_SOUND) ?: "makkah"
                val resId = resolveSelectedVoiceRes(adhanSound)

                if (resId != 0) {
                    startForeground(NOTIF_ID, buildNotification(prayerId))
                    startPlayback(resId)
                } else {
                    // If sound is not found, just stop
                    stopSelf()
                }
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startPlayback(resId: Int) {
        stopPlayback()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            try {
                val afd = resources.openRawResourceFd(resId) ?: return
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = false
                setOnCompletionListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                    stopSelf()
                }
                setOnErrorListener { _, _, _ ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                    stopSelf(); true
                }
                prepare()
                start()
            } catch (_: Exception) {
                // Handle exceptions, e.g., file not found
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                stopSelf()
            }
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
            .setSmallIcon(R.drawable.ic_notification_icon) // Changed for compatibility
            .setContentTitle(title)
            .setContentText("برای قطع پخش، توقف را بزنید")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .addAction(0, "توقف", stopPending)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID, "اذان", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)      // Sound is played by MediaPlayer
                description = "پخش اذان در زمان نماز"
                enableVibration(false)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(ch)
        }
    }

    private fun resolveSelectedVoiceRes(soundName: String): Int {
        return when (soundName) {
            "makkah" -> R.raw.azan_makkah_4 // Assuming this is the default
            //"madina" -> R.raw.azan_madina // Assuming you have this file
            //"abdulbasit" -> R.raw.azan_abdulbasit // Assuming you have this file
            // Add other adhan sounds here
            else -> R.raw.azan_makkah_4 // Default fallback
        }
    }
}
