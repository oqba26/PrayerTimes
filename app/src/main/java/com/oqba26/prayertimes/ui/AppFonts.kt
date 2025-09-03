package com.oqba26.prayertimes.ui

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.oqba26.prayertimes.R

object AppFonts {

    data class Entry(
        val id: String,        // دقیقاً مساوی نام فایل فونت در res/font (بدون پسوند)
        val label: String,
        val family: FontFamily
    )

    private fun system() = Entry("system", "پیش‌فرض سیستم", FontFamily.Default)

    private fun familyFromRes(
        ctx: Context,
        base: String,
        weights: List<Pair<Int, FontWeight>>
    ): FontFamily? {
        val fonts = weights.mapNotNull { (resId, w) ->
            runCatching { Font(resId, w) }.getOrNull()
        }
        return if (fonts.isNotEmpty()) FontFamily(fonts) else null
    }

    // 5 فونت واقعی موجود در پروژه‌ات
    fun catalog(ctx: Context): List<Entry> {
        val out = mutableListOf<Entry>()
        out += system()

        // byekan: فقط Regular و Bold داری
        familyFromRes(ctx, "byekan", listOf(
            R.font.byekan to FontWeight.Normal,
            R.font.byekan_bold to FontWeight.Bold
        ))?.let { out += Entry("byekan", "بی‌یکن", it) }

        // estedad: regular, medium, bold, light وجود دارد (براساس لیستت)
        familyFromRes(ctx, "estedad", listOf(
            R.font.estedad_regular to FontWeight.Normal,
            R.font.estedad_medium to FontWeight.Medium,
            R.font.estedad_bold to FontWeight.Bold,
            R.font.estedad_light to FontWeight.Light,
            R.font.estedad_black to FontWeight.Black
        ))?.let { out += Entry("estedad", "استعداد", it) }

        // vazirmatn: مجموعه کامل
        familyFromRes(ctx, "vazirmatn", listOf(
            R.font.vazirmatn_regular to FontWeight.Normal,
            R.font.vazirmatn_medium to FontWeight.Medium,
            R.font.vazirmatn_bold to FontWeight.Bold,
            R.font.vazirmatn_light to FontWeight.Light,
            R.font.vazirmatn_thin to FontWeight.Thin,
            R.font.vazirmatn_black to FontWeight.Black
        ))?.let { out += Entry("vazirmatn", "وزیرمتن", it) }

        // iraniansans: یک فایل
        familyFromRes(ctx, "iraniansans", listOf(
            R.font.iraniansans to FontWeight.Normal
        ))?.let { out += Entry("iraniansans", "ایرانیان‌سنس", it) }

        // sahel: bold و black داری
        familyFromRes(ctx, "sahel", listOf(
            R.font.sahel_bold to FontWeight.Bold,
            R.font.sahel_black to FontWeight.Black
        ))?.let { out += Entry("sahel", "ساحل", it) }

        return out
    }

    fun familyFor(ctx: Context, id: String?): FontFamily {
        if (id.isNullOrBlank() || id == "system") return FontFamily.Default
        val f = catalog(ctx).firstOrNull { it.id == id }?.family
        return f ?: FontFamily.Default
    }
}