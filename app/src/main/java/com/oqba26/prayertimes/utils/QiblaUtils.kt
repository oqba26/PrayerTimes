package com.oqba26.prayertimes.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object QiblaUtils {

    // مختصات تقریبی کعبه
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262

    /**
     * زاویه قبله نسبت به شمال حقیقی (درجه، 0..360، ساعتگرد)
     */
    fun bearingToQibla(userLat: Double, userLng: Double): Float {
        val lat1 = Math.toRadians(userLat)
        val lon1 = Math.toRadians(userLng)
        val lat2 = Math.toRadians(KAABA_LAT)
        val lon2 = Math.toRadians(KAABA_LNG)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val brng = Math.toDegrees(atan2(y, x))

        return (((brng + 360) % 360).toFloat())
    }
}