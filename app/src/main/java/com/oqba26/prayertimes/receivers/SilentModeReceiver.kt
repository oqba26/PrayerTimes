package com.oqba26.prayertimes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class SilentModeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SILENT = "com.oqba26.prayertimes.ACTION_SILENT"
        const val ACTION_UNSILENT = "com.oqba26.prayertimes.ACTION_UNSILENT"

        private const val PREFS = "silent_state"
        private const val KEY_ACTIVE_COUNT = "active_count"
        private const val KEY_PREV_RINGER_MODE = "prev_ringer_mode"

        private const val TAG = "SilentModeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val am = ContextCompat.getSystemService(context, AudioManager::class.java) ?: return

        when (action) {
            ACTION_SILENT -> {
                val active = prefs.getInt(KEY_ACTIVE_COUNT, 0) + 1
                prefs.edit { putInt(KEY_ACTIVE_COUNT, active) }
                Log.d(TAG, "🔇 ACTION_SILENT | activeCount=$active")

                if (active == 1) {
                    val prevMode = am.ringerMode
                    prefs.edit {
                        putInt(KEY_PREV_RINGER_MODE, prevMode)
                    }

                    try {
                        am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        Log.d(TAG, "✅ Entered prayer silent mode: switched to VIBRATE. Previous mode was $prevMode")
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Cannot change ringer mode to VIBRATE (need DND access?)", e)
                    }
                }
            }

            ACTION_UNSILENT -> {
                val current = (prefs.getInt(KEY_ACTIVE_COUNT, 0) - 1).coerceAtLeast(0)
                prefs.edit { putInt(KEY_ACTIVE_COUNT, current) }
                Log.d(TAG, "🔔 ACTION_UNSILENT | activeCount=$current")

                if (current == 0) {
                    val prevMode = prefs.getInt(
                        KEY_PREV_RINGER_MODE,
                        AudioManager.RINGER_MODE_NORMAL
                    )

                    try {
                        am.ringerMode = prevMode
                        Log.d(TAG, "✅ Exit prayer silent mode: restored ringer mode to $prevMode")
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Cannot restore ringer mode (need DND access?)", e)
                    }
                }
            }
        }
    }
}
