package com.oqba26.prayertimes.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.utils.getCurrentDate
import com.oqba26.prayertimes.utils.getPrayerTimes

fun sendNotification(context: Context) {
    val date = getCurrentDate()
    val prayerTimes = getPrayerTimes(context, date)

    val channelId = "prayer_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Prayer Times", NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val content = "تقویم امروز:\nشمسی: ${date.shamsi}\nقمری: ${date.hijri}\nمیلادی: ${date.gregorian}\n\nاوقات نماز:\n" +
            prayerTimes.entries.joinToString("\n") { "${it.key}: ${it.value}" }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("تقویم و اوقات نماز")
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    // Check notification permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(1, notification)
        }
    } else {
        NotificationManagerCompat.from(context).notify(1, notification)
    }
}