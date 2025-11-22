package com.oqba26.prayertimes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * Receiver برای فعال/غیرفعال کردن سکوت به حالت ویبره (بدون استفاده از DND).
 * هر نماز می‌تونه محدوده‌ای از زمان ویبره خودش رو تنظیم کنه.
 */
class SilentModeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SILENT = "com.oqba26.prayertimes.ACTION_SILENT"
        const val ACTION_UNSILENT = "com.oqba26.prayertimes.ACTION_UNSILENT"

        private const val PREFS = "silent_state"
        private const val KEY_ACTIVE_COUNT = "active_count"
        private const val KEY_PREV_RINGER = "prev_ringer"

        private const val TAG = "SilentModeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val am = ContextCompat.getSystemService(context, AudioManager::class.java) ?: return

        when (action) {
            ACTION_SILENT -> {
                val active = prefs.getInt(KEY_ACTIVE_COUNT, 0) + 1
                prefs.edit { putInt(KEY_ACTIVE_COUNT, active) }
                Log.d(TAG, "🔇 درخواست فعال‌کردن ویبره | شمارنده فعال: $active")

                if (active == 1) {
                    // ذخیره حالت فعلی برای بازگردانی بعداً
                    prefs.edit { putInt(KEY_PREV_RINGER, am.ringerMode) }

                    // بدون نیاز به مجوز، فقط به ویبره برو
                    am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    Log.d(TAG, "✅ گوشی روی ویبره تنظیم شد")
                }
            }

            ACTION_UNSILENT -> {
                val current = (prefs.getInt(KEY_ACTIVE_COUNT, 0) - 1).coerceAtLeast(0)
                prefs.edit { putInt(KEY_ACTIVE_COUNT, current) }
                Log.d(TAG, "🔔 درخواست خروج از ویبره | شمارنده فعال: $current")

                if (current == 0) {
                    // بازگرداندن حالت قبلی (Normal یا هر حالت دیگر)
                    val prev = prefs.getInt(KEY_PREV_RINGER, AudioManager.RINGER_MODE_NORMAL)
                    am.ringerMode = prev
                    Log.d(TAG, "✅ حالت قبلی گوشی بازگردانده شد (prev=$prev)")
                }
            }
        }
    }
}