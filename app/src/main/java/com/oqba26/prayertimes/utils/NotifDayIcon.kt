package com.oqba26.prayertimes.utils

import android.content.Context
import android.graphics.*
import androidx.annotation.ColorInt
import kotlin.math.max

// Bitmap برای largeIcon (دایره رنگی با متن)
fun createDayNumberBitmap(
    context: Context,
    dayText: String,
    @ColorInt bgColor: Int = Color.rgb(27, 94, 32), // سبز تیره
    @ColorInt textColor: Int = Color.WHITE
): Bitmap {
    val dm = context.resources.displayMetrics
    val sizeDp = 48 // توصیه شده برای largeIcon
    val sizePx = max(48, (sizeDp * dm.density).toInt())

    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // پس‌زمینه دایره‌ای
    val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paintBg)

    // متن وسط‌چین
    val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = sizePx * 0.58f
    }
    val y = sizePx / 2f - (paintText.descent() + paintText.ascent()) / 2f
    canvas.drawText(dayText, sizePx / 2f, y, paintText)

    return bmp
}

// نسخهٔ کوچک و تک‌رنگ فقط برای smallIcon «ریسکی»
// نکته: استفاده از این Bitmap به‌عنوان smallIcon ممکنه روی بعضی دستگاه‌ها خطای Bad Notification بده.
// بهتره فقط در صورت نیاز شدید فعالش کنی.
fun createDayNumberGlyphBitmap(context: Context, dayText: String): Bitmap {
    val dm = context.resources.displayMetrics
    val sizeDp = 24
    val sizePx = max(24, (sizeDp * dm.density).toInt())

    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // smallIcon باید سفید و آلفا-بیس باشه
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = sizePx * 0.80f
    }
    val y = sizePx / 2f - (paintText.descent() + paintText.ascent()) / 2f
    // زمینه شفاف، صرفاً متن سفید
    canvas.drawText(dayText, sizePx / 2f, y, paintText)
    return bmp
}