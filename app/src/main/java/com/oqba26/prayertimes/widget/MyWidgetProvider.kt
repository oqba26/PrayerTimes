package com.oqba26.prayertimes.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.utils.getCurrentDate
import com.oqba26.prayertimes.utils.getPrayerTimes

class MyWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val date = getCurrentDate()
            val prayerTimes = getPrayerTimes(context, date)

            views.setTextViewText(R.id.tv_date, "شمسی: ${date.shamsi}\nقمری: ${date.hijri}\nمیلادی: ${date.gregorian}")
            views.setTextViewText(R.id.tv_prayers, prayerTimes.entries.joinToString("\n") { "${it.key}: ${it.value}" })

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}