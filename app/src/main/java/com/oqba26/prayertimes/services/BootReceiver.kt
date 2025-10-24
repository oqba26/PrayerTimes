package com.oqba26.prayertimes.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.wtf("BootReceiver", "ON_RECEIVE_CALLED_SIMPLE_LOG_WTF") // لاگ با بالاترین اولویت و بسیار ساده

        val action = intent.action

        if (Intent.ACTION_BOOT_COMPLETED == action ||
            "android.intent.action.MY_PACKAGE_REPLACED" == action || // برای بروزرسانی اپ
            "android.intent.action.QUICKBOOT_POWERON" == action // برای برخی دستگاه‌ها
        ) {
            Log.i("BootReceiver", "Action '$action' received. Attempting to start PrayerForegroundService.")
            val serviceIntent = Intent(context, PrayerForegroundService::class.java).apply {
                this.action = PrayerForegroundService.ACTION_START_FROM_BOOT_OR_UPDATE
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                    Log.i("BootReceiver", "PrayerForegroundService started using startForegroundService.")
                } else {
                    context.startService(serviceIntent)
                    Log.i("BootReceiver", "PrayerForegroundService started using startService.")
                }
            } catch (e: Exception) {
                Log.wtf("BootReceiver", "Error starting PrayerForegroundService", e) // تغییر به wtf برای مشاهده خطا
            }
        } else {
            // Log.d("BootReceiver", "Received action is not relevant: $action") // موقتا کامنت شد
        }
    }
}
