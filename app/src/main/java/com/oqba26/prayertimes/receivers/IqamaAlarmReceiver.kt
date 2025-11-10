package com.oqba26.prayertimes.receivers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.oqba26.prayertimes.MainActivity
import com.oqba26.prayertimes.services.NotificationService

class IqamaAlarmReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "نماز"

        // Create an intent to launch the app when the notification is tapped
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, NotificationService.IQAMA_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using a standard Android icon
            .setContentTitle("اقامه نماز")
            .setContentText("زمان اقامه نماز $prayerName فرا رسیده است.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            val notificationId = prayerName.hashCode()
            notify(notificationId, builder.build())
        }
    }
}
