package com.oqba26.prayertimes.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.util.TypedValue
import androidx.core.graphics.createBitmap

fun dp(context: Context, value: Int): Int = 
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

fun textBitmap(
    context: Context,
    text: String,
    tf: Typeface,
    sp: Float,
    color: Int,
    maxWidthPx: Int,
    minSp: Float = 11f
): Bitmap {
    fun sp2px(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, context.resources.displayMetrics)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textAlign = Paint.Align.CENTER
        typeface = tf
    }
    var currentSp = sp
    var textWidth: Float
    do {
        paint.textSize = sp2px(currentSp)
        textWidth = paint.measureText(text)
        if (textWidth <= maxWidthPx || currentSp <= minSp) break
        currentSp -= 0.5f
    } while (currentSp > minSp)

    val fm = paint.fontMetrics
    val height = (fm.bottom - fm.top).toInt().coerceAtLeast(dp(context, 24))
    val safeMaxWidthPx = maxWidthPx.coerceAtLeast(1)
    val bmp = createBitmap(safeMaxWidthPx, height, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    val cx = safeMaxWidthPx / 2f
    val cy = height / 2f - (fm.ascent + fm.descent) / 2f
    c.drawText(text, cx, cy, paint)
    return bmp
}

fun createDayIconBitmap(context: Context, day: Int, color: Int, usePersian: Boolean, tf: Typeface): Bitmap {
    fun sp(v: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, context.resources.displayMetrics)
    val text = DateUtils.convertToPersianNumbers(day.toString(), usePersian)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        typeface = Typeface.create(tf, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        textSize = sp(22f)
    }
    val size = dp(context, 32)
    val bmp = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawText(text, size / 2f, size / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f, paint)
    return bmp
}

fun createPrayerTimesBitmapWithHighlight(
    context: Context,
    prayerTimes: Map<String, String>,
    currentPrayerName: String?,
    tf: Typeface,
    maxWidthPx: Int,
    baseColor: Int,
    highlightColor: Int,
    use24HourFormat: Boolean,
    usePersianNumbers: Boolean
): Bitmap {
    val sp = 14f
    fun sp2px(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, context.resources.displayMetrics)

    val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = baseColor
        textAlign = Paint.Align.CENTER
        textSize = sp2px(sp)
        typeface = tf
    }
    val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = highlightColor
        textAlign = Paint.Align.CENTER
        textSize = sp2px(sp)
        typeface = Typeface.create(tf, Typeface.BOLD)
    }

    val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
    val items = order.map { name ->
        val raw = prayerTimes[name] ?: "--:--"
        val time = DateUtils.formatDisplayTime(raw, use24HourFormat, usePersianNumbers)
        name to time
    }

    val cols = 3
    val hPad = dp(context, 8).toFloat()
    val vPad = dp(context, 6).toFloat()
    val colSpacing = dp(context, 8).toFloat()
    val rowSpacing = dp(context, 6).toFloat()

    val contentWidth = maxWidthPx - (2f * hPad)
    val cellWidth = (contentWidth - (cols - 1) * colSpacing) / cols

    fun fitSizePx(text: String, baseSpSize: Float, minSpSize: Float = 10f): Float {
        var currentSpSize = baseSpSize
        val tempPaint = Paint(normalPaint)
        while (currentSpSize >= minSpSize) {
            tempPaint.textSize = sp2px(currentSpSize)
            if (tempPaint.measureText(text) <= cellWidth) return tempPaint.textSize
            currentSpSize -= 0.5f
        }
        return sp2px(minSpSize)
    }

    data class LayoutItem(val name: String, val text: String, val textSizePx: Float, val itemHeight: Float)

    fun layoutRowItems(rowItems: List<Pair<String, String>>): Pair<List<LayoutItem>, Float> {
        val layouts = ArrayList<LayoutItem>(cols)
        var maxRowHeight = 0f
        for ((name, timeText) in rowItems) {
            val fullText = "$name: $timeText"
            val fittedSizePx = fitSizePx(fullText, sp)
            val tempPaint = Paint(normalPaint).apply { textSize = fittedSizePx }
            val fm = tempPaint.fontMetrics
            val height = fm.bottom - fm.top
            if (height > maxRowHeight) maxRowHeight = height
            layouts.add(LayoutItem(name, fullText, fittedSizePx, height))
        }
        return layouts to maxRowHeight
    }

    val firstRowItems = items.subList(0, 3)
    val secondRowItems = items.subList(3, 6)

    val (row1Layouts, row1Height) = layoutRowItems(firstRowItems)
    val (row2Layouts, row2Height) = layoutRowItems(secondRowItems)

    val totalHeight = (vPad + row1Height + rowSpacing + row2Height + vPad).toInt().coerceAtLeast(dp(context, 48))
    val safeMaxWidthPx = maxWidthPx.coerceAtLeast(1)
    val bmp = createBitmap(safeMaxWidthPx, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

    val contentRightEdge = safeMaxWidthPx - hPad
    val row1CenterY = vPad + row1Height / 2f
    val row2CenterY = vPad + row1Height + rowSpacing + row2Height / 2f

    fun drawRowOnCanvas(layoutItems: List<LayoutItem>, centerY: Float) {
        for (i in layoutItems.indices) {
            val centerX = contentRightEdge - (cellWidth / 2f) - i * (cellWidth + colSpacing)
            val item = layoutItems[i]
            val paintToUse = if (item.name == currentPrayerName) highlightPaint else normalPaint
            paintToUse.textSize = item.textSizePx
            val fm = paintToUse.fontMetrics
            val baseline = centerY - (fm.ascent + fm.descent) / 2f
            canvas.drawText(item.text, centerX, baseline, paintToUse)
        }
    }

    drawRowOnCanvas(row1Layouts, row1CenterY)
    drawRowOnCanvas(row2Layouts, row2CenterY)
    return bmp
}

fun pillButtonBitmap(
    context: Context,
    text: String,
    tf: Typeface,
    bgColor: Int,
    hPadDp: Int,
    textColor: Int
): Bitmap {
    val sp = 12f
    val minHeightDp = 36

    fun sp2px(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, context.resources.displayMetrics)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = textColor
        textAlign = Paint.Align.CENTER
        textSize = sp2px(sp)
        typeface = tf
    }
    val textW = paint.measureText(text)
    val h = dp(context, minHeightDp)
    val padH = dp(context, hPadDp).toFloat()
    val minW = dp(context, 64)
    val w = (textW + 2 * padH).toInt().coerceAtLeast(minW)
    val safeW = w.coerceAtLeast(1)
    val safeH = h.coerceAtLeast(1)

    val bmp = createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val r = safeH / 2f
    c.drawRoundRect(0f, 0f, safeW.toFloat(), safeH.toFloat(), r, r, bgPaint)

    val fm = paint.fontMetrics
    val baseline = safeH / 2f - (fm.ascent + fm.descent) / 2f
    c.drawText(text, safeW / 2f, baseline, paint)
    return bmp
}
