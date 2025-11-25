package com.oqba26.prayertimes

enum class PrayerTime(val id: String, val displayName: String) {
    FAJR("fajr", "نماز صبح"),
    DHUHR("dhuhr", "نماز ظهر"),
    ASR("asr", "نماز عصر"),
    MAGHRIB("maghrib", "نماز مغرب"),
    ISHA("isha", "نماز عشاء")
}
