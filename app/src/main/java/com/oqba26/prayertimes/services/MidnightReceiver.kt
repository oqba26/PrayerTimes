package com.oqba26.prayertimes.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oqba26.prayertimes.utils.PrayerAlarmManager

class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PrayerAlarmManager.ACTION_MIDNIGHT_ALARM) {
            // Trigger the service to handle the midnight update
            PrayerForegroundService.update(context, PrayerForegroundService.ACTION_MIDNIGHT_UPDATE)
        }
    }
}