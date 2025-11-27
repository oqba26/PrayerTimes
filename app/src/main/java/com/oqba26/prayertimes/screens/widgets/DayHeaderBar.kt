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
import androidx.compose.runtime.remember
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
import com.oqba26.prayertimes.utils.DateUtils

@Composable
fun DayHeaderBar(
    date: MultiDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    isDark: Boolean,
    usePersianNumbers: Boolean,
    useNumericDateMain: Boolean,
    onToggleDateView: () -> Unit
) {
    val weekDay = remember(date) { DateUtils.getWeekDayName(date) }

    val shamsiLong = remember(date, usePersianNumbers) { DateUtils.formatShamsiLong(date, usePersianNumbers) }
    val shamsiNumeric = remember(date, usePersianNumbers) { DateUtils.formatShamsiShort(date, usePersianNumbers) }

    val hijriLong = remember(date, usePersianNumbers) { DateUtils.formatHijriLong(date, usePersianNumbers) }
    val hijriNumeric = remember(date, usePersianNumbers) { DateUtils.formatHijriShort(date, usePersianNumbers) }

    val gregLong = remember(date, usePersianNumbers) { DateUtils.formatGregorianLong(date, usePersianNumbers) }
    val gregNumeric = remember(date, usePersianNumbers) { DateUtils.formatGregorianShort(date, usePersianNumbers) }

    val cardBackgroundBrush = if (isDark) SolidColor(MaterialTheme.colorScheme.surfaceContainer) else SolidColor(Color.White)
    val innerBarBackgroundColor = if (isDark) Color(0xFF4F378B) else Color(0xFF0E7490)
    val innerBarTextColor = MaterialTheme.colorScheme.onPrimary

    val line1 = remember(useNumericDateMain, shamsiLong, shamsiNumeric, weekDay) {
        val weekDayTxt = DateUtils.convertToPersianNumbers(weekDay, usePersianNumbers)
        if (useNumericDateMain) "$weekDayTxt $shamsiNumeric" else "$weekDayTxt $shamsiLong"
    }
    val line2 = remember(useNumericDateMain, hijriLong, gregLong, hijriNumeric, gregNumeric) {
        val hijri = if (useNumericDateMain) hijriNumeric else hijriLong
        val greg = if (useNumericDateMain) gregNumeric else gregLong
        "$hijri | $greg"
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
                .height(52.dp)
                .background(innerBarBackgroundColor, shape = RectangleShape)
                .clickable { onToggleDateView() }
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
