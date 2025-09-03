package com.oqba26.prayertimes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Stable
private class CalloutShape(
    private val cornerRadius: Dp,
    private val arrowSize: Dp,
    private val arrowFraction: Float,   // 0f..1f نسبت به عرض (از چپ)
    private val arrowOnTop: Boolean
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = with(density) {
        val r = cornerRadius.toPx()
        val aw = arrowSize.toPx()
        val ah = arrowSize.toPx()

        // مستطیل گرد با درنظر گرفتن فضای فلش
        val rectTop = if (arrowOnTop) ah else 0f
        val rectBottom = if (arrowOnTop) size.height else size.height - ah
        val rr = RoundRect(Rect(0f, rectTop, size.width, rectBottom), CornerRadius(r, r))
        val bubble = Path().apply { addRoundRect(rr) }

        // مرکز افقی فلش روی لبه حباب
        val minX = r + aw / 2
        val maxX = size.width - r - aw / 2
        val cx = (size.width * arrowFraction).coerceIn(minX, maxX)

        // مثلث فلش
        val tri = Path().apply {
            if (arrowOnTop) {
                moveTo(cx, 0f)
                lineTo(cx - aw / 2, ah)
                lineTo(cx + aw / 2, ah)
            } else {
                moveTo(cx, size.height)
                lineTo(cx - aw / 2, size.height - ah)
                lineTo(cx + aw / 2, size.height - ah)
            }
            close()
        }

        // اتحاد فلش و بدنه
        val union = Path().apply { op(bubble, tri, PathOperation.Union) }
        Outline.Generic(union)
    }
}

@Composable
fun TooltipBubble(
    titles: List<String>,
    textOnBubble: Color,
    bubbleColor: Color,
    borderColor: Color,
    cornerRadius: Dp,
    arrowFraction: Float,      // 0f..1f
    arrowOnTop: Boolean,
    arrowSize: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    // پدینگ محتوا با درنظر گرفتن فلش یکپارچه
    val pad = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = if (arrowOnTop) arrowSize + 8.dp else 12.dp,
        bottom = if (!arrowOnTop) arrowSize + 8.dp else 12.dp
    )

    val shape = CalloutShape(
        cornerRadius = cornerRadius,
        arrowSize = arrowSize,
        arrowFraction = arrowFraction.coerceIn(0.08f, 0.92f),
        arrowOnTop = arrowOnTop
    )

    Surface(
        shape = shape,
        color = bubbleColor,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier
    ) {
        // متن RTL
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .padding(pad),
                horizontalAlignment = Alignment.Start
            ) {
                titles.forEach { t ->
                    Text(
                        text = t,
                        color = textOnBubble,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}