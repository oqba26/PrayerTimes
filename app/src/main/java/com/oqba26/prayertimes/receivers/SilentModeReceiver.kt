package com.oqba26.prayertimes.receivers

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit

class SilentModeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SILENT = "com.oqba26.prayertimes.ACTION_SILENT"
        const val ACTION_UNSILENT = "com.oqba26.prayertimes.ACTION_UNSILENT"

        // extra برای تعیین مدت سایلنت (دقیقه)
        const val EXTRA_DURATION_MINUTES = "duration_min"

        private const val DEFAULT_SILENT_DURATION_MINUTES = 20

        private const val PREFS = "silent_state"
        private const val KEY_ACTIVE_COUNT = "active_count"
        private const val KEY_PREV_RINGER = "prev_ringer"
        private const val KEY_PREV_FILTER = "prev_filter"
        private const val KEY_NEXT_ID = "next_id"
    }

    @SuppressLint("ObsoleteSdkInt", "ScheduleExactAlarm")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        when (intent.action) {
            ACTION_SILENT -> {
                val active = prefs.getInt(KEY_ACTIVE_COUNT, 0) + 1
                prefs.edit { putInt(KEY_ACTIVE_COUNT, active) }

                if (active == 1) {
                    // ذخیره حالت قبلی
                    prefs.edit {
                        putInt(KEY_PREV_RINGER, am.ringerMode)
                            .putInt(KEY_PREV_FILTER, safeCurrentFilter(nm))
                    }
                    // اعمال سایلنت (اولویت با DND)
                    applySilent(nm, am)
                }

                val minutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, DEFAULT_SILENT_DURATION_MINUTES)
                val triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L

                // برای پشتیبانی از چند بازه هم‌پوشان، requestCode یکتا
                val requestCode = prefs.getInt(KEY_NEXT_ID, 1)
                prefs.edit { putInt(KEY_NEXT_ID, requestCode + 1) }

                val unsilentIntent = Intent(context, SilentModeReceiver::class.java).apply {
                    action = ACTION_UNSILENT
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    unsilentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                }
            }

            ACTION_UNSILENT -> {
                val active = (prefs.getInt(KEY_ACTIVE_COUNT, 0) - 1).coerceAtLeast(0)
                prefs.edit { putInt(KEY_ACTIVE_COUNT, active) }

                if (active == 0) {
                    restoreSound(nm, am, prefs)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun safeCurrentFilter(nm: NotificationManager): Int =
        runCatching { nm.currentInterruptionFilter }
            .getOrDefault(NotificationManager.INTERRUPTION_FILTER_ALL)

    @SuppressLint("ObsoleteSdkInt")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun applySilent(nm: NotificationManager, am: AudioManager) {
        val dndGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || nm.isNotificationPolicyAccessGranted
        if (dndGranted) {
            runCatching { nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS) }
                .onFailure { am.ringerMode = AudioManager.RINGER_MODE_SILENT }
        } else {
            am.ringerMode = AudioManager.RINGER_MODE_SILENT
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun restoreSound(nm: NotificationManager, am: AudioManager, prefs: android.content.SharedPreferences) {
        val dndGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || nm.isNotificationPolicyAccessGranted
        if (dndGranted) {
            val prev = prefs.getInt(KEY_PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
            runCatching { nm.setInterruptionFilter(prev) }
                .onFailure { am.ringerMode = AudioManager.RINGER_MODE_NORMAL }
        } else {
            am.ringerMode = prefs.getInt(KEY_PREV_RINGER, AudioManager.RINGER_MODE_NORMAL)
        }
    }

    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}