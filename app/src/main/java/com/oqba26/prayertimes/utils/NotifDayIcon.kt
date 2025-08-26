package com.oqba26.prayertimes.utils

import android.content.Context
import android.graphics.*
import androidx.annotation.ColorInt
import kotlin.math.max

fun createNotifDayIconBitmap(
    context: Context,
    dayText: String,
    @ColorInt bgColor: Int,
    @ColorInt textColor: Int
): Bitmap {
    val dm = context.resources.displayMetrics
    val sizeDp = 32
    val sizePx = max(32, (sizeDp * dm.density).toInt())

    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paintBg)

    val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = sizePx * 0.6f
    }
    val y = sizePx / 2f - (paintText.descent() + paintText.ascent()) / 2f
    canvas.drawText(dayText, sizePx / 2f, y, paintText)

    return bmp
}