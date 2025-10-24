package com.oqba26.prayertimes.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.oqba26.prayertimes.MainActivity
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.UserNote
import com.oqba26.prayertimes.utils.NoteUtils

class NoteAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_NOTE_ID = "EXTRA_NOTE_ID"
        const val ACTION_SHOW_NOTE_REMINDER = "com.oqba26.prayertimes.ACTION_SHOW_NOTE_REMINDER"
        private const val REMINDER_CHANNEL_ID = "note_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW_NOTE_REMINDER) return

        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        if (noteId == null) {
            Log.e("NoteAlarmReceiver", "Note ID is null.")
            return
        }

        val note = NoteUtils.loadNotes(context)[noteId]
        if (note == null) {
            Log.e("NoteAlarmReceiver", "Note not found for ID: $noteId")
            return
        }

        handleReminder(context, note)
    }

    private fun handleReminder(context: Context, note: UserNote) {
        if (!note.notificationEnabled) return

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, note.id.hashCode(), openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle("یادآوری: ${note.content.take(20)}...")
            .setContentText(note.content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(note.id.hashCode(), notification)
        Log.d("NoteAlarmReceiver", "Reminder notification shown for ID: ${note.id}")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(REMINDER_CHANNEL_ID, "یادآوری‌ها", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Channel for note reminders"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
