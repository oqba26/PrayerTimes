package com.oqba26.prayertimes.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

class DndReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DND_START = "com.oqba26.prayertimes.ACTION_DND_START"
        const val ACTION_DND_END = "com.oqba26.prayertimes.ACTION_DND_END"
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return
        }

        when (intent.action) {
            ACTION_DND_START -> {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
            ACTION_DND_END -> {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }

        // Reschedule for next day
        val hour = intent.getIntExtra(com.oqba26.prayertimes.utils.DndScheduler.EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(com.oqba26.prayertimes.utils.DndScheduler.EXTRA_MINUTE, -1)

        if (hour != -1 && minute != -1) {
            com.oqba26.prayertimes.utils.DndScheduler.scheduleNext(context, intent.action!!, hour, minute)
        }
    }
}
