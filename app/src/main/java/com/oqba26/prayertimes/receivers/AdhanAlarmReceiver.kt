package com.oqba26.prayertimes.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.oqba26.prayertimes.services.AdhanPlayerService
import com.oqba26.prayertimes.services.PrayerForegroundService

/**
 * دریافت‌کنندهٔ آلارم اذان:
 * - اگر PRAYER_ID = "noop" باشد ⇒ فقط آلارم‌ها را دوباره زمان‌بندی می‌کند (نیمه‌شب).
 * - در غیر این‌صورت ⇒ سرویس پخش اذان را اجرا می‌کند.
 */
class AdhanAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AdhanAlarmReceiver"
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {

        val rawPrayerId = intent.getStringExtra(AdhanPlayerService.EXTRA_PRAYER_ID)
            ?: intent.getStringExtra("PRAYER_ID")

        if (rawPrayerId == null) {
            Log.w(TAG, "onReceive called with no PRAYER_ID, ignoring.")
            return
        }

        if (rawPrayerId == "noop") {
            Log.d(TAG, "Midnight reschedule trigger received (PRAYER_ID=noop)")
            try {
                val svcIntent = Intent(context, PrayerForegroundService::class.java).apply {
                    action = PrayerForegroundService.ACTION_SCHEDULE_ALARMS
                }
                ContextCompat.startForegroundService(context, svcIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting PrayerForegroundService from midnight reschedule", e)
            }
            return
        }

        val prayerId = rawPrayerId

        // صدای اذان انتخاب‌شده را بخوانیم
        val adhanSound = intent.getStringExtra(AdhanPlayerService.EXTRA_ADHAN_SOUND)

        // اگر صدای اذان "off" یا خالی بود، پخش نکن و خارج شو
        if (adhanSound.isNullOrBlank() || adhanSound == "off") {
            Log.i(TAG, "Adhan for $prayerId is set to 'off', skipping playback.")
            return
        }

        // در غیر این صورت، سرویس پخش اذان را اجرا کن
        Log.d(TAG, "📢 Starting Adhan playback for $prayerId with sound='$adhanSound'")
        AdhanPlayerService.playNow(context.applicationContext, prayerId, adhanSound)
    }
}
