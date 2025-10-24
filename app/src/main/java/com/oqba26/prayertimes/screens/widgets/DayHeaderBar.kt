package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oqba26.prayertimes.models.MultiDate

@Composable
fun DayHeaderBar(
    date: MultiDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    isDark: Boolean,
    usePersianNumbers: Boolean,
    @Suppress("UNUSED_PARAMETER") use24HourFormat: Boolean
) {
    val weekDay = remember(date) { com.oqba26.prayertimes.utils.DateUtils.getWeekDayName(date) }
    var isNumeric by remember { mutableStateOf(false) }

    fun formatNumericDate(day: Int, month: Int, year: Int): String {
        val dd = String.format(java.util.Locale.US, "%02d", day)
        val mm = String.format(java.util.Locale.US, "%02d", month)
        val yyyy = String.format(java.util.Locale.US, "%04d", year)
        // For RTL context, the string must be built in reverse order (YYYY/MM/DD)
        // to appear correctly as DD/MM/YYYY on the screen.
        //خط زیر مسئول نمایش تاریخ های عدد به شکل صحیح هست ینی : روز ماه سال
        return com.oqba26.prayertimes.utils.DateUtils.convertToPersianNumbers("$yyyy/$mm/$dd", usePersianNumbers)
    }

    val (sy, sm, sd) = date.getShamsiParts()
    val shamsiLong = remember(date, usePersianNumbers) { com.oqba26.prayertimes.utils.DateUtils.formatShamsiLong(date, usePersianNumbers) }
    //نمایش عددی تاریخ شمسی
    val shamsiNumeric = remember(sy, sm, sd, usePersianNumbers) { formatNumericDate(sd, sm, sy) }

    val hijriPartsRaw = remember(date) { date.hijri.split("/") }
    val hY = hijriPartsRaw.getOrNull(0)?.toIntOrNull() ?: 0
    val hM = hijriPartsRaw.getOrNull(1)?.toIntOrNull() ?: 0
    val hD = hijriPartsRaw.getOrNull(2)?.toIntOrNull() ?: 0
    val hijriLong = remember(date, usePersianNumbers) { com.oqba26.prayertimes.utils.DateUtils.formatHijriLong(date, usePersianNumbers) }
    //نمایش عددی تاریخ قمری
    val hijriNumeric = remember(hY, hM, hD, usePersianNumbers) { formatNumericDate(hD, hM, hY) }

    val gregPartsRaw = remember(date) { date.gregorian.split("/") }
    val gY = gregPartsRaw.getOrNull(0)?.toIntOrNull() ?: 0
    val gM = gregPartsRaw.getOrNull(1)?.toIntOrNull() ?: 0
    val gD = gregPartsRaw.getOrNull(2)?.toIntOrNull() ?: 0
    val gregLong = remember(date, usePersianNumbers) { com.oqba26.prayertimes.utils.DateUtils.formatGregorianLong(date, usePersianNumbers) }
    //نمایش عددی تاریخ میلادی
    val gregNumeric = remember(gY, gM, gD, usePersianNumbers) { formatNumericDate(gD, gM, gY) }

    val cardBackgroundBrush = if (isDark) SolidColor(MaterialTheme.colorScheme.surfaceContainer) else SolidColor(Color.White)
    val innerBarBackgroundColor = if (isDark) Color(0xFF4F378B) else Color(0xFF0E7490)
    val innerBarTextColor = MaterialTheme.colorScheme.onPrimary

    val line1 = remember(isNumeric, shamsiLong, shamsiNumeric, weekDay) {
        val weekDayTxt = com.oqba26.prayertimes.utils.DateUtils.convertToPersianNumbers(weekDay, usePersianNumbers)
        if (isNumeric) "$weekDayTxt $shamsiNumeric" else "$weekDayTxt $shamsiLong"
    }
    val line2 = remember(isNumeric, hijriLong, gregLong, hijriNumeric, gregNumeric) {
        if (isNumeric) "$hijriNumeric | $gregNumeric" else "$hijriLong | $gregLong"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 0.dp, shape = RectangleShape)
            .background(brush = cardBackgroundBrush, shape = RectangleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp) // کمی فشرده‌تر
                .background(innerBarBackgroundColor, shape = RectangleShape)
                .clickable { isNumeric = !isNumeric }
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(text = line1, color = innerBarTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(text = line2, color = innerBarTextColor, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }

            IconButton(onClick = onNextDay, modifier = Modifier.align(Alignment.CenterStart).size(40.dp)) {
                Box(
                    modifier = Modifier.size(29.dp).shadow(0.dp, CircleShape).background(if (isDark) Color(0xFF7C5DC7) else Color(0xFF00ACC1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        fontSize = 22.sp,
                        color = Color.White,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
            }
            IconButton(onClick = onPreviousDay, modifier = Modifier.align(Alignment.CenterEnd).size(40.dp)) {
                Box(
                    modifier = Modifier.size(29.dp).shadow(0.dp, CircleShape).background(if (isDark) Color(0xFF7C5DC7) else Color(0xFF00ACC1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "›",
                        fontSize = 22.sp,
                        color = Color.White,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
            }
        }
    }
}
