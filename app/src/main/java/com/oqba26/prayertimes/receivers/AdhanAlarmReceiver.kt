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

        // ۱) شناسه نماز را از اینتنت بگیریم
        //    (هم از EXTRA_PRAYER_ID و هم از "PRAYER_ID" پشتیبانی می‌کنیم)
        val rawPrayerId = intent.getStringExtra(AdhanPlayerService.EXTRA_PRAYER_ID)
            ?: intent.getStringExtra("PRAYER_ID")

        if (rawPrayerId == null) {
            Log.w(TAG, "onReceive called with no PRAYER_ID, ignoring.")
            return
        }

        // ۲) اگر "noop" باشد یعنی آلارم نیمه‌شب برای reschedule
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

        // ۳) صدای اذان انتخاب‌شده را بخوانیم
        val adhanSoundExtra = intent.getStringExtra(AdhanPlayerService.EXTRA_ADHAN_SOUND)

        // ⚠ برای این‌که مطمئن شویم فعلاً اذان حتماً پخش می‌شود،
        //   اگر مقدار تهی یا "off" بود، به صورت پیش‌فرض "makkah" را می‌گذاریم.
        val soundToPlay = if (adhanSoundExtra.isNullOrBlank() || adhanSoundExtra == "off") {
            Log.w(
                TAG,
                "Adhan sound was null/blank/off for $prayerId; using default 'makkah' for debugging."
            )
            "makkah"
        } else {
            adhanSoundExtra
        }

        Log.d(TAG, "📢 Starting Adhan playback for $prayerId with sound='$soundToPlay'")
        AdhanPlayerService.playNow(context.applicationContext, prayerId, soundToPlay)
    }
}