package com.oqba26.prayertimes.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.core.content.edit

class SilentModeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SILENT = "com.oqba26.prayertimes.ACTION_SILENT"
        const val ACTION_UNSILENT = "com.oqba26.prayertimes.ACTION_UNSILENT"

        private const val PREFS = "silent_state"
        private const val KEY_ACTIVE_COUNT = "active_count"
        private const val KEY_PREV_RINGER = "prev_ringer"
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onReceive(context: Context, intent: Intent) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        when (intent.action) {
            ACTION_SILENT -> {
                val active = prefs.getInt(KEY_ACTIVE_COUNT, 0) + 1
                prefs.edit { putInt(KEY_ACTIVE_COUNT, active) }

                if (active == 1) {
                    // Store the previous ringer mode
                    prefs.edit { putInt(KEY_PREV_RINGER, am.ringerMode) }
                    // Apply vibrate mode
                    am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
            }

            ACTION_UNSILENT -> {
                val active = (prefs.getInt(KEY_ACTIVE_COUNT, 0) - 1).coerceAtLeast(0)
                prefs.edit { putInt(KEY_ACTIVE_COUNT, active) }

                if (active == 0) {
                    // Restore the original ringer mode
                    val prevRingerMode = prefs.getInt(KEY_PREV_RINGER, AudioManager.RINGER_MODE_NORMAL)
                    am.ringerMode = prevRingerMode
                }
            }
        }
    }
}
