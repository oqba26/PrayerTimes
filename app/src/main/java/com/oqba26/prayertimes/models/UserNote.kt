package com.oqba26.prayertimes.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "user_notes")
data class UserNote(
    @PrimaryKey val id: String, // Usually the date key like "YYYY-MM-DD"
    val content: String,
    val notificationEnabled: Boolean = false,
    val reminderTimeMillis: Long? = null, // Store time as milliseconds since epoch for easier handling
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable