package com.oqba26.prayertimes.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.oqba26.prayertimes.utils.getCurrentDate
import com.oqba26.prayertimes.utils.loadPrayerTimes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d("BootReceiver", "دستگاه restart شد - راه‌اندازی مجدد سرویس‌ها")
                // راه‌اندازی مجدد سرویس بعد از ریبوت
                restartPrayerForegroundService(context)
            }
        }
    }

    private fun restartPrayerForegroundService(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ایجاد کانال نوتیفیکیشن
                NotificationService.createNotificationChannels(context)

                val currentDate = getCurrentDate()
                val prayerTimes = loadPrayerTimes(context, currentDate)

                if (prayerTimes.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val serviceIntent = Intent(context, PrayerForegroundService::class.java)
                        serviceIntent.action = "START"
                        serviceIntent.putExtra("date", currentDate)
                        serviceIntent.putExtra("prayerTimes", HashMap(prayerTimes))

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }

                        Log.d("BootReceiver", "سرویس foreground بعد از restart راه‌اندازی شد")
                    }
                } else {
                    Log.w("BootReceiver", "اوقات نماز برای امروز پیدا نشد")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "خطا در راه‌اندازی مجدد سرویس foreground", e)
            }
        }
    }
}