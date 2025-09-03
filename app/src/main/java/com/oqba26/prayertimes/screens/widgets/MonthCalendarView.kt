package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.ui.TooltipBubble
import com.oqba26.prayertimes.utils.DateUtils.convertPersianToGregorian
import com.oqba26.prayertimes.utils.DateUtils.convertToPersianNumbers
import com.oqba26.prayertimes.utils.DateUtils.createMultiDateFromShamsi
import com.oqba26.prayertimes.utils.DateUtils.daysInShamsiMonth
import com.oqba26.prayertimes.utils.DateUtils.getCurrentDate
import com.oqba26.prayertimes.utils.DateUtils.getPersianMonthName


@Composable
fun MonthCalendarView(
    currentDate: MultiDate,
    selectedDate: MultiDate,
    onDateChange: (MultiDate) -> Unit,
    holidays: Map<Int, List<String>> = emptyMap()
) {
    val (year, month, selectedDay) = selectedDate.getShamsiParts()
    val daysInMonth = daysInShamsiMonth(year, month)

    val firstDayOffset = run {
        val (gy, gm, gd) = convertPersianToGregorian(year, month, 1)
        val cal = java.util.Calendar.getInstance().apply { set(gy, gm - 1, gd) }
        cal.get(java.util.Calendar.DAY_OF_WEEK) % 7
    }

    val totalCells = 42
    val days = List(firstDayOffset) { null } +
            (1..daysInMonth).toList() +
            List(totalCells - firstDayOffset - daysInMonth) { null }

    val today = getCurrentDate()
    val (ty, tm, td) = today.getShamsiParts()

    var openHolidayDay by remember { mutableStateOf<Int?>(null) }

    // استایل حباب/فلش
    val bubbleColor = Color(0xFFE8F4F8)
    val textOnBubble = Color(0xFF0E4A5C)
    val borderColor = Color(0xFF0E7490)
    val cornerRadius = 12.dp
    val arrowSize = 10.dp

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() } // عرض واقعی پنجره
    val gapPx = with(density) { 8.dp.roundToPx() } // فاصله حباب از سلول

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // عنوان ماه
            Text(
                text = "${getPersianMonthName(month)} ${convertToPersianNumbers(year.toString())}",
                fontSize = 18.sp,
                color = Color(0xFF0E7490),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                // هدر روزهای هفته
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val headers = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                    headers.forEachIndexed { index, label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = if (index == 6) Color.Red else Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // گرید تقویم
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(210.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    userScrollEnabled = false
                ) {
                    items(days.size) { i ->
                        val day = days[i]
                        if (day == null) {
                            Box(modifier = Modifier.size(34.dp))
                        } else {
                            val isSelected = selectedDay == day
                            val isFriday = (i % 7) == 6
                            val isToday = (year == ty && month == tm && day == td)
                            val titles = holidays[day].orEmpty()
                            val isHoliday = titles.isNotEmpty()

                            val bgColor = if (isSelected) Color(0xFFE0F7FA) else Color.Transparent
                            val textColor = when {
                                isSelected -> Color.Black
                                isHoliday -> Color(0xFFD32F2F)
                                isFriday  -> Color.Red
                                else      -> Color.Black
                            }

                            // bounds دقیق سلول در Window
                            var anchorCenterX by remember { mutableStateOf(0f) }
                            var anchorTop by remember { mutableStateOf(0f) }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .onGloballyPositioned { coords ->
                                        val b = coords.boundsInWindow()
                                        anchorCenterX = b.left + b.width / 2f
                                        anchorTop = b.top
                                    }
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .then(if (isToday) Modifier.border(1.dp, Color(0xFF00ACC1), CircleShape) else Modifier)
                                    .clickable {
                                        onDateChange(createMultiDateFromShamsi(year, month, day))
                                        openHolidayDay = if (isHoliday) {
                                            if (openHolidayDay == day) null else day
                                        } else null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = convertToPersianNumbers(day.toString()),
                                    fontSize = 14.sp,
                                    color = textColor,
                                    textDecoration = if (isHoliday) TextDecoration.Underline else TextDecoration.None
                                )

                                if (isHoliday && openHolidayDay == day && anchorCenterX > 0f) {
                                    // قرار دادن حباب بالای سلول (clamp در لبه‌ها)
                                    val provider = remember(anchorCenterX, anchorTop) {
                                        object : PopupPositionProvider {
                                            override fun calculatePosition(
                                                anchorBounds: IntRect,
                                                windowSize: IntSize,
                                                layoutDirection: LayoutDirection,
                                                popupContentSize: IntSize
                                            ): IntOffset {
                                                // این سمت هم با screenWidthPx یکسانه
                                                var left = (anchorCenterX - popupContentSize.width / 2f).toInt()
                                                val maxLeft = (screenWidthPx - popupContentSize.width).toInt()
                                                if (left < 0) left = 0
                                                if (left > maxLeft) left = maxLeft

                                                val desiredTop = (anchorTop - popupContentSize.height - gapPx).toInt()
                                                val top = desiredTop.coerceAtLeast(0)
                                                return IntOffset(left, top)
                                            }
                                        }
                                    }

                                    Popup(
                                        popupPositionProvider = provider,
                                        onDismissRequest = { openHolidayDay = null },
                                        properties = PopupProperties(
                                            focusable = true,
                                            dismissOnBackPress = true,
                                            dismissOnClickOutside = true
                                        )
                                    ) {
                                        // به‌جای استفاده از مختصات popup (که پنجره‌ی جداست)،
                                        // چپِ حباب را دقیقاً مثل provider دوباره محاسبه می‌کنیم تا در «همان سیستم مختصات» باشد.
                                        var bubbleWidth by remember { mutableStateOf(0f) }

                                        Box(
                                            modifier = Modifier.onGloballyPositioned { coords ->
                                                bubbleWidth = coords.size.width.toFloat()
                                            }
                                        ) {
                                            // محاسبه Left کلَمپ‌شده (همان الگوریتم provider)
                                            val leftClamped = remember(anchorCenterX, bubbleWidth, screenWidthPx) {
                                                if (bubbleWidth <= 0f) 0f
                                                else {
                                                    val raw = anchorCenterX - bubbleWidth / 2f
                                                    raw.coerceIn(0f, screenWidthPx - bubbleWidth)
                                                }
                                            }

                                            // محاسبه fraction دقیقِ فلش بدون نیاز به مختصات popup
                                            val rawFraction =
                                                if (bubbleWidth > 0f)
                                                    ((anchorCenterX - leftClamped) / bubbleWidth)
                                                else 0.5f
                                            val fraction = rawFraction.coerceIn(0.06f, 0.94f)

                                            // تعیین محل فلش (بالا/پایین) بر اساس این‌که حباب بالای سلول افتاده یا نه
                                            val arrowOnTop = false // چون provider همیشه حباب را بالای سلول می‌گذارد مگر در کلَمپ عمودی (در این نسخه بالا می‌ماند)

                                            TooltipBubble(
                                                titles = titles,
                                                textOnBubble = textOnBubble,
                                                bubbleColor = bubbleColor,
                                                borderColor = borderColor,
                                                cornerRadius = cornerRadius,
                                                arrowFraction = fraction,     // فلش دقیقاً زیر عدد
                                                arrowOnTop = arrowOnTop,
                                                arrowSize = arrowSize
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ناوبری ماه (وسط عمودی، فلش‌ها رو به بیرون)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // چپ: ماه بعد
            IconButton(
                onClick = {
                    val (ny, nm) = if (month < 12) year to (month + 1) else (year + 1) to 1
                    val newDay = selectedDay.coerceAtMost(daysInShamsiMonth(ny, nm))
                    onDateChange(createMultiDateFromShamsi(ny, nm, newDay))
                    openHolidayDay = null
                },
                modifier = Modifier.size(32.dp)
            ) { Text("‹", fontSize = 16.sp, color = Color.Gray) }

            // راست: ماه قبل
            IconButton(
                onClick = {
                    val (py, pm) = if (month > 1) year to (month - 1) else (year - 1) to 12
                    val newDay = selectedDay.coerceAtMost(daysInShamsiMonth(py, pm))
                    onDateChange(createMultiDateFromShamsi(py, pm, newDay))
                    openHolidayDay = null
                },
                modifier = Modifier.size(32.dp)
            ) { Text("›", fontSize = 16.sp, color = Color.Gray) }
        }
    }
}