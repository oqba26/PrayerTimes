package com.oqba26.prayertimes.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.theme.DefaultAppFontFamily // ایمپورت فونت وزیرمتن با تمام وزن ها

object AppFonts {

    @Immutable
    data class FontEntry(
        val id: String,
        val label: String,
        val family: FontFamily? // برای "system" مقدار null می‌گذاریم
    )

    // فهرست فونت‌ها برای نمایش در تنظیمات
    fun catalog(): List<FontEntry> = listOf(
        FontEntry(
            id = "estedad",
            label = "Estedad (پیش‌فرض برنامه)",
            family = FontFamily(Font(R.font.estedad_regular))
        ),
        FontEntry(
            id = "byekan",
            label = "B Yekan",
            family = FontFamily(Font(R.font.byekan)) // اگر byekan هم وزن های مختلف دارد، مشابه وزیرمتن کامل شود
        ),
        FontEntry(
            id = "vazirmatn",
            label = "Vazirmatn",
            family = DefaultAppFontFamily // <--- تغییر: استفاده از فونت وزیرمتن با تمام وزن ها
        ),
        FontEntry(
            id = "iraniansans",
            label = "Iranian Sans",
            family = FontFamily(Font(R.font.iraniansans))
        ),
        FontEntry(
            id = "sahel",
            label = "Sahel",
            family = FontFamily(Font(R.font.sahel_bold))
        ),

    )

    // فونت انتخابی برای تم برنامه
    // خروجی null یعنی فونت سیستم (بدون override)
    fun familyFor(id: String?): FontFamily = when (id?.lowercase()) {
        "estedad"     -> FontFamily(Font(R.font.estedad_regular))
        "byekan"      -> FontFamily(Font(R.font.byekan))
        "vazirmatn"   -> DefaultAppFontFamily // <--- تغییر: استفاده از فونت وزیرمتن با تمام وزن ها
        "iraniansans" -> FontFamily(Font(R.font.iraniansans))
        "sahel"       -> FontFamily(Font(R.font.sahel_bold))
        else          -> FontFamily.Default // system
    }
}
