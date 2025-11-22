package com.oqba26.prayertimes.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.oqba26.prayertimes.services.AdhanPlayerService
import com.oqba26.prayertimes.utils.PrayerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * دریافت‌کنندهٔ اعلان زمان اذان‌ها
 * بعد از دریافت، سرویس پخش اذان را فعال می‌کند.
 */
class AdhanAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AdhanAlarmReceiver"
        private const val MAX_ALLOWED_DRIFT_MS = 10 * 60 * 1000L // 10 دقیقه، حداکثر انحراف مجاز
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val prayerId = intent.getStringExtra(AdhanPlayerService.EXTRA_PRAYER_ID) ?: return
        val adhanSound = intent.getStringExtra(AdhanPlayerService.EXTRA_ADHAN_SOUND) ?: "off"
        val triggerAt = intent.getLongExtra("TRIGGER_AT", -1L)
        val now = System.currentTimeMillis()

        // 1️⃣ اگر کاربر برای این نماز صدای اذان را "off" کرده باشد، کاری نکن
        if (adhanSound == "off" || adhanSound.isBlank()) {
            Log.d(TAG, "⏩ پخش اذان غیرفعال است: $prayerId")
            return
        }

        // 2️⃣ بررسی خطای زمانی - جلوگیری از تریگر جعلی یا قدیمی
        if (triggerAt > 0 && kotlin.math.abs(now - triggerAt) > MAX_ALLOWED_DRIFT_MS) {
            Log.w(TAG, "⛔ انحراف زمانی زیاد برای اذان $prayerId (${kotlin.math.abs(now - triggerAt) / 1000}s)")
            return
        }

        // 3️⃣ تطبیق prayerId انگلیسی با کلید فارسی داخل JSON (file: prayer_times_24h.json)
        val shouldPlay = runBlocking {
            withContext(Dispatchers.IO) {
                val times = PrayerUtils.loadDetailedPrayerTimes(
                    context,
                    com.oqba26.prayertimes.utils.DateUtils.getCurrentDate()
                )

                val jsonKey = when (prayerId) {
                    "fajr"    -> "صبح"
                    "dhuhr"   -> "ظهر"
                    "asr"     -> "عصر"
                    "maghrib" -> "مغرب"
                    "isha"    -> "عشاء"
                    else      -> null
                }

                val refTime = jsonKey?.let { times[it] }
                Log.d(TAG, "✔️ اذان زمان‌بندی‌شده برای $prayerId با ref = $refTime (key=$jsonKey)")
                refTime != null
            }
        }

        if (!shouldPlay) {
            Log.w(TAG, "🚫 زمان اذان در JSON برای $prayerId پیدا نشد؛ پخش نمی‌شود")
            return
        }

        // 4️⃣ شروع سرویس پخش اذان
        Log.d(TAG, "📢 آغاز پخش اذان برای $prayerId با صدا: $adhanSound")
        AdhanPlayerService.playNow(context.applicationContext, prayerId, adhanSound)
    }
}