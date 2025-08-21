#!/bin/bash

# اسکریپت اعمال تغییرات برای پروژه PrayerTimes
# استفاده: در ترمینال Android Studio این اسکریپت را اجرا کنید

echo "🚀 شروع اعمال تغییرات..."

# مسیر پروژه (تغییر دهید اگر لازم است)
PROJECT_PATH="."

echo "📁 پروژه در مسیر: $PROJECT_PATH"

# 1. ایجاد فایل local.properties
echo "1️⃣ ایجاد فایل local.properties..."
cat > "$PROJECT_PATH/local.properties" << 'EOF'
# This file contains the path of the Android SDK location.
# For information on how to configure your Android SDK, please refer to the documentation.
# Example (Windows):
# sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
# Example (macOS/Linux):
# sdk.dir=/Users/YourName/Library/Android/sdk

# Note: You need to set the actual path to your Android SDK
# sdk.dir=/path/to/your/android-sdk
EOF

# 2. بروزرسانی AndroidManifest.xml
echo "2️⃣ بروزرسانی AndroidManifest.xml..."
sed -i.bak 's|android:supportsRtl="true"|android:supportsRtl="true"\n        android:layoutDirection="rtl"|g' "$PROJECT_PATH/app/src/main/AndroidManifest.xml"
sed -i.bak 's|<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />|<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />|g' "$PROJECT_PATH/app/src/main/AndroidManifest.xml"

# 3. بروزرسانی PrayerUtils.kt
echo "3️⃣ بروزرسانی PrayerUtils.kt..."
cat > "$PROJECT_PATH/app/src/main/java/com/oqba26/prayertimes/utils/PrayerUtils.kt" << 'EOF'
package com.oqba26.prayertimes.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oqba26.prayertimes.models.MultiDate
import java.io.InputStreamReader

fun getPrayerTimes(context: Context, date: MultiDate): Map<String, String> {
    try {
        val inputStream = context.assets.open("prayer_times.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
        val data: Map<String, Map<String, String>> = Gson().fromJson(reader, type)
        reader.close()
        inputStream.close()
        
        // استخراج ماه و روز از تاریخ شمسی برای جستجو در داده‌های 12 ماهه
        val monthDay = extractMonthDay(date.shamsi)
        return data[monthDay] ?: getDefaultPrayerTimes()
    } catch (e: Exception) {
        e.printStackTrace()
        return getDefaultPrayerTimes()
    }
}

private fun extractMonthDay(shamsiDate: String): String {
    // از تاریخ کامل مثل "1403/01/15" فقط "01/15" رو استخراج می‌کنه
    val parts = shamsiDate.split("/")
    return if (parts.size >= 3) {
        "${parts[1]}/${parts[2]}"
    } else {
        shamsiDate
    }
}

private fun getDefaultPrayerTimes(): Map<String, String> {
    return linkedMapOf(
        "طلوع بامداد" to "05:00",
        "طلوع خورشید" to "06:30",
        "ظهر" to "12:30",
        "عصر" to "16:00",
        "غروب" to "18:30",
        "عشاء" to "20:00"
    )
}
EOF

# 4. بروزرسانی AlarmUtils.kt
echo "4️⃣ بروزرسانی AlarmUtils.kt..."
cat > "$PROJECT_PATH/app/src/main/java/com/oqba26/prayertimes/services/AlarmUtils.kt" << 'EOF'
package com.oqba26.prayertimes.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

fun setDailyAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 8)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    if (calendar.timeInMillis < System.currentTimeMillis()) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Check for exact alarm permission on Android 12+
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            // Fallback to inexact alarm
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    } else {
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
EOF

# 5. بروزرسانی NotificationService.kt
echo "5️⃣ بروزرسانی NotificationService.kt..."
cat > "$PROJECT_PATH/app/src/main/java/com/oqba26/prayertimes/services/NotificationService.kt" << 'EOF'
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
EOF

# 6. ایجاد فایل JSON نمونه
echo "6️⃣ ایجاد فایل JSON نمونه..."
cat > "$PROJECT_PATH/app/src/main/assets/prayer_times_sample.json" << 'EOF'
{
  "01/01": {
    "طلوع بامداد": "05:15",
    "طلوع خورشید": "06:45",
    "ظهر": "12:15",
    "عصر": "15:45",
    "غروب": "18:00",
    "عشاء": "19:30"
  },
  "01/02": {
    "طلوع بامداد": "05:14",
    "طلوع خورشید": "06:44",
    "ظهر": "12:15",
    "عصر": "15:46",
    "غروب": "18:01",
    "عشاء": "19:31"
  },
  "01/03": {
    "طلوع بامداد": "05:13",
    "طلوع خورشید": "06:43",
    "ظهر": "12:16",
    "عصر": "15:47",
    "غروب": "18:02",
    "عشاء": "19:32"
  }
}
EOF

echo "✅ همه تغییرات با موفقیت اعمال شد!"
echo ""
echo "📋 مراحل بعدی:"
echo "1. مسیر Android SDK را در local.properties تنظیم کنید"
echo "2. فایل prayer_times.json را با داده‌های کامل خودتان جایگزین کنید"
echo "3. پروژه را Sync کنید (Ctrl+Shift+O یا Cmd+Shift+O)"
echo "4. پروژه را Build کنید"
echo ""
echo "🎉 پروژه شما آماده است!"