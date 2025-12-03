package com.oqba26.prayertimes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.oqba26.prayertimes.services.PrayerForegroundService

class BootAndTimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootAndTimeChangeRcvr", "onReceive: ${intent.action}")

        // Schedule prayer alarms
        val alarmsSvc = Intent(context, PrayerForegroundService::class.java).apply {
            action = PrayerForegroundService.ACTION_SCHEDULE_ALARMS
        }
        ContextCompat.startForegroundService(context, alarmsSvc)

        // Schedule DND alarms
        val dndSvc = Intent(context, PrayerForegroundService::class.java).apply {
            action = PrayerForegroundService.ACTION_SCHEDULE_DND
        }
        ContextCompat.startForegroundService(context, dndSvc)
    }
}