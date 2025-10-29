package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.screens.DarkThemeOnPurpleText
import com.oqba26.prayertimes.screens.DarkThemePurpleBackground
import com.oqba26.prayertimes.ui.TooltipBubble
import com.oqba26.prayertimes.utils.DateUtils

@Composable
fun MonthCalendarView(
    selectedDate: MultiDate,
    onDateChange: (MultiDate) -> Unit,
    holidays: Map<Int, List<String>> = emptyMap(),
    isDark: Boolean,
    usePersianNumbers: Boolean
) {
    val (year, month, selectedDay) = selectedDate.getShamsiParts()
    val daysInMonth = DateUtils.daysInShamsiMonth(year, month)

    val firstDayOffset = run {
        val (gy, gm, gd) = DateUtils.convertPersianToGregorian(year, month, 1)
        val cal = java.util.Calendar.getInstance().apply { set(gy, gm - 1, gd) }
        when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SATURDAY -> 0
            java.util.Calendar.SUNDAY -> 1
            java.util.Calendar.MONDAY -> 2
            java.util.Calendar.TUESDAY -> 3
            java.util.Calendar.WEDNESDAY -> 4
            java.util.Calendar.THURSDAY -> 5
            else -> 6
        }
    }

    val totalCells = 42
    val days = List(firstDayOffset) { null } +
            (1..daysInMonth).toList() +
            List(totalCells - firstDayOffset - daysInMonth) { null }

    val today = DateUtils.getCurrentDate()
    val (ty, tm, td) = today.getShamsiParts()

    var openHolidayDay by remember { mutableStateOf<Int?>(null) }

    val bubbleColor = Color(0xFFE8F4F8)
    val textOnBubble = Color(0xFF0E4A5C)
    val tooltipBorderColor = Color(0xFF0E7490)
    val tooltipCornerRadius = 12.dp
    val tooltipArrowSize = 10.dp

    val density = LocalDensity.current
    val gapPx = with(density) { 8.dp.roundToPx() }

    val grey300 = Color(0xFFE0E0E0)

    val monthTitleBoxHeight = 32.dp
    val weekHeaderHeight = 30.dp
    val gridInternalVPadding = 0.dp
    val contentHorizontalPadding = 40.dp
    val calendarGridHeight = 220.dp

    val navButtonTouchableSize = 36.dp
    val navButtonVisualSize = 28.dp
    val navIconSize = 18.dp

    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val monthTitleBackgroundColor = if (isDark) DarkThemePurpleBackground else Color(0xFF0E7490)
            val monthTitleTextColorValue = if (isDark) DarkThemeOnPurpleText else Color.White
            val monthTitleBorderColorValue = if (isDark) DarkThemePurpleBackground.copy(alpha = 0.7f) else grey300

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(monthTitleBoxHeight)
                    .background(color = monthTitleBackgroundColor, shape = RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${DateUtils.getPersianMonthName(month)} ${DateUtils.convertToPersianNumbers(year.toString(), usePersianNumbers)}",
                    style = MaterialTheme.typography.titleSmall.copy(color = monthTitleTextColorValue, fontWeight = FontWeight.Bold)
                )
            }

            val mainCalendarBackgroundBrush: Brush =
                if (isDark) SolidColor(MaterialTheme.colorScheme.surface)
                else Brush.linearGradient(colors = listOf(Color(0xFFF8F9FA), Color(0xFFE9ECEF)))
            val mainCalendarBorderColor = if (isDark) MaterialTheme.colorScheme.outline else grey300

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = mainCalendarBackgroundBrush, shape = RectangleShape)
                    //.border(BorderStroke(1.dp, mainCalendarBorderColor), RectangleShape)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = contentHorizontalPadding)) {
                        val weekHeaderBackgroundColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFAFAFA)
                        val weekHeaderBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFEEEEEE)
                        val dayHeaderTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF616161)
                        val fridayHeaderTextColor = Color.Red

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(weekHeaderHeight)
                                .background(color = weekHeaderBackgroundColor, shape = RectangleShape)
                                //.border(BorderStroke(1.dp, weekHeaderBorderColor), RectangleShape),
                            //horizontalArrangement = Arrangement.SpaceAround,
                            //verticalAlignment = Alignment.CenterVertically
                        ) {
                            val headers = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                            headers.forEachIndexed { index, label ->
                                Text(
                                    text = label,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    color = if (index == 6) fridayHeaderTextColor else dayHeaderTextColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(calendarGridHeight)
                                .padding(vertical = gridInternalVPadding),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                            userScrollEnabled = false
                        ) {
                            items(days.size) { i ->
                                val day = days[i]
                                if (day == null) {
                                    Box(modifier = Modifier.aspectRatio(1f))
                                } else {
                                    val isSelected = selectedDay == day
                                    val isCurrentDay = (year == ty && month == tm && day == td)
                                    val dayIndexInWeek = (firstDayOffset + day - 1) % 7
                                    val isFriday = (dayIndexInWeek == 6)
                                    val titles = holidays[day].orEmpty()
                                    val isHoliday = titles.isNotEmpty()

                                    val selectedBgColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFB2EBF2)
                                    val currentDayBgColor = if (isDark) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color(0xFFE0E0E0)
                                    val bgColor: Color = when {
                                        isSelected -> selectedBgColor
                                        isCurrentDay -> currentDayBgColor
                                        else -> Color.Transparent
                                    }

                                    val textColor: Color = when {
                                        isFriday || isHoliday -> Color.Red
                                        isSelected -> if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                        isCurrentDay -> MaterialTheme.colorScheme.onSecondaryContainer
                                        isDark -> MaterialTheme.colorScheme.onSurface
                                        else -> Color.Black
                                    }

                                    // بیضی + کادر: فقط برای «روز جاری» همیشه کادر داشته باشد
                                    val pillShape = RoundedCornerShape(50)
                                    val showBorder = isCurrentDay  // فقط روز جاری
                                    val borderColor = if (isDark)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                    else
                                        MaterialTheme.colorScheme.primary

                                    var cellModifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 8.dp)

                                    if (bgColor != Color.Transparent) {
                                        cellModifier = cellModifier.background(bgColor, pillShape)
                                    }
                                    if (showBorder) {
                                        cellModifier = cellModifier.border(2.5.dp, borderColor, pillShape)
                                    }

                                    var anchorCenterX by remember { mutableFloatStateOf(0f) }
                                    var anchorTop by remember { mutableFloatStateOf(0f) }

                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .onGloballyPositioned { coords ->
                                                val b = coords.boundsInWindow()
                                                anchorCenterX = b.left + b.width / 2f
                                                anchorTop = b.top
                                            }
                                            .clickable {
                                                onDateChange(DateUtils.createMultiDateFromShamsi(year, month, day))
                                                openHolidayDay = if (isHoliday) { if (openHolidayDay == day) null else day } else null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(modifier = cellModifier)
                                        Text(
                                            text = DateUtils.convertToPersianNumbers(day.toString(), usePersianNumbers),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor,
                                            fontWeight = if (isCurrentDay && !isHoliday && !isFriday && !isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.align(Alignment.Center),
                                            textAlign = TextAlign.Center,
                                            textDecoration = if (isHoliday) TextDecoration.Underline else null
                                        )

                                        if (isHoliday && openHolidayDay == day && anchorCenterX > 0f) {
                                            val provider = remember(anchorCenterX, anchorTop) {
                                                object : PopupPositionProvider {
                                                    override fun calculatePosition(
                                                        anchorBounds: IntRect,
                                                        windowSize: IntSize,
                                                        layoutDirection: LayoutDirection,
                                                        popupContentSize: IntSize
                                                    ): IntOffset {
                                                        var left = (anchorCenterX - popupContentSize.width / 2f).toInt()
                                                        val maxLeft = windowSize.width - popupContentSize.width
                                                        if (left < 0) left = 0
                                                        if (left > maxLeft) left = maxLeft
                                                        val desiredTop = (anchorTop - popupContentSize.height - gapPx.toFloat()).toInt()
                                                        val top = desiredTop.coerceAtLeast(0)
                                                        return IntOffset(left, top)
                                                    }
                                                }
                                            }
                                            Popup(
                                                popupPositionProvider = provider,
                                                onDismissRequest = { openHolidayDay = null },
                                                properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true)
                                            ) {
                                                TooltipBubble(
                                                    titles = titles.map { DateUtils.convertToPersianNumbers(it, usePersianNumbers) },
                                                    textOnBubble = textOnBubble,
                                                    bubbleColor = bubbleColor,
                                                    borderColor = tooltipBorderColor,
                                                    cornerRadius = tooltipCornerRadius,
                                                    arrowFraction = 0.5f,
                                                    arrowOnTop = false,
                                                    arrowSize = tooltipArrowSize
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
        }

        val navButtonsYOffset = monthTitleBoxHeight + weekHeaderHeight + (calendarGridHeight / 2) - (navButtonTouchableSize / 2)

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).align(Alignment.TopCenter).offset(y = navButtonsYOffset),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // دکمه چپ = یک ماه جلو
                NavButton(onClick = {
                    val (ny, nm) = if (month < 12) year to (month + 1) else (year + 1) to 1
                    val newDay = selectedDay.coerceAtMost(DateUtils.daysInShamsiMonth(ny, nm))
                    onDateChange(DateUtils.createMultiDateFromShamsi(ny, nm, newDay))
                    openHolidayDay = null
                }, isDark = isDark, icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = DateUtils.convertToPersianNumbers("ماه بعد", usePersianNumbers),
                    navButtonTouchableSize = navButtonTouchableSize, navButtonVisualSize = navButtonVisualSize, navIconSize = navIconSize)

                // دکمه راست = یک ماه عقب
                NavButton(onClick = {
                    val (py, pm) = if (month > 1) year to (month - 1) else (year - 1) to 12
                    val newDay = selectedDay.coerceAtMost(DateUtils.daysInShamsiMonth(py, pm))
                    onDateChange(DateUtils.createMultiDateFromShamsi(py, pm, newDay))
                    openHolidayDay = null
                }, isDark = isDark, icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = DateUtils.convertToPersianNumbers("ماه قبل", usePersianNumbers),
                    navButtonTouchableSize = navButtonTouchableSize, navButtonVisualSize = navButtonVisualSize, navIconSize = navIconSize)
            }
        }
    }
}

@Composable
private fun NavButton(
    onClick: () -> Unit,
    isDark: Boolean,
    icon: ImageVector,
    contentDescription: String,
    navButtonTouchableSize: Dp,
    navButtonVisualSize: Dp,
    navIconSize: Dp
) {
    val navButtonColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFF00ACC1)
    val navButtonIconColor = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
    val navButtonShape = CircleShape

    IconButton(onClick = onClick, modifier = Modifier.size(navButtonTouchableSize)) {
        Box(
            modifier = Modifier
                .size(navButtonVisualSize)
                .shadow(elevation = 2.dp, shape = navButtonShape, spotColor = navButtonColor.copy(alpha = 0.5f))
                .background(brush = SolidColor(navButtonColor), shape = navButtonShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = navButtonIconColor, modifier = Modifier.size(navIconSize))
        }
    }
}