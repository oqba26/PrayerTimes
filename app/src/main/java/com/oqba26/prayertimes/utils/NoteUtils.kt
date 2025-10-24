package com.oqba26.prayertimes.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.models.UserNote
import com.oqba26.prayertimes.receivers.NoteAlarmReceiver
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

object NoteUtils {

    private const val NOTES_FILE_NAME = "user_notes.json"

    fun formatDateToKey(date: MultiDate): String {
        val (year, month, day) = date.getShamsiParts()
        return "${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    fun loadNotes(context: Context): Map<String, UserNote> {
        val internalFile = File(context.filesDir, NOTES_FILE_NAME)
        val gson = Gson()
        val type = object : TypeToken<Map<String, UserNote>>() {}.type

        return if (internalFile.exists()) {
            try {
                context.openFileInput(NOTES_FILE_NAME).use {
                    gson.fromJson(InputStreamReader(it), type) ?: emptyMap()
                }
            } catch (e: Exception) {
                Log.e("NoteUtils", "Error reading notes", e)
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    fun saveNotesInternal(context: Context, notes: Map<String, UserNote>) {
        val gson = Gson()
        val jsonString = gson.toJson(notes)
        try {
            FileOutputStream(File(context.filesDir, NOTES_FILE_NAME)).use {
                it.write(jsonString.toByteArray())
            }
        } catch (e: IOException) {
            Log.e("NoteUtils", "Error writing notes", e)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleExactAlarm(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w("NoteUtils", "Cannot schedule exact alarms. Permission needed.")
                false
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                true
            }
        } catch (se: SecurityException) {
            Log.e("NoteUtils", "SecurityException: Cannot schedule exact alarm.", se)
            false
        }
    }

    fun scheduleNoteReminder(context: Context, note: UserNote): Boolean {
        if (!note.notificationEnabled || note.reminderTimeMillis == null || note.reminderTimeMillis <= System.currentTimeMillis()) {
            cancelNoteReminder(context, note.id) // Cancel if disabled or past
            return true
        }
        val intent = Intent(context, NoteAlarmReceiver::class.java).apply {
            action = NoteAlarmReceiver.ACTION_SHOW_NOTE_REMINDER
            putExtra(NoteAlarmReceiver.EXTRA_NOTE_ID, note.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, note.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val scheduled = scheduleExactAlarm(context, note.reminderTimeMillis, pendingIntent)
        if (scheduled) {
            Log.d("NoteUtils", "Reminder scheduled for note ID: ${note.id} at ${DateUtils.formatTimeMillis(note.reminderTimeMillis)}")
        }
        return scheduled
    }

    fun cancelNoteReminder(context: Context, noteId: String) {
        val intent = Intent(context, NoteAlarmReceiver::class.java).apply { action = NoteAlarmReceiver.ACTION_SHOW_NOTE_REMINDER }
        val pendingIntent = PendingIntent.getBroadcast(context, noteId.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("NoteUtils", "Reminder cancelled for note ID: $noteId")
        }
    }
}