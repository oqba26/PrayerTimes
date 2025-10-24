package com.oqba26.prayertimes.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String?,
    val isEnabled: Boolean = true,
    val repeatDays: List<Int> = emptyList(), // Using Calendar constants, e.g., Calendar.MONDAY
    val ringtoneUri: String? = null, // Store as String URI
    val vibrate: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
