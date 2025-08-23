package com.oqba26.prayertimes.widget

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters

class WidgetUpdateWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext
        val intent = Intent(context, ModernWidgetProvider::class.java).apply {
            action = ModernWidgetProvider.ACTION_UPDATE_CLOCK
        }
        context.sendBroadcast(intent)
        return Result.success()
    }
}