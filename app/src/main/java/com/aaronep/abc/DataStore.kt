package com.aaronep.abc

import android.location.Location

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val time: Long,
    val speed: Float,
    val power: Double
)

object DataStore {
    private val points = mutableListOf<TrackPoint>()

    fun addPoint(location: Location, power: Double) {
        points.add(
            TrackPoint(
                location.latitude,
                location.longitude,
                location.altitude,
                location.time,
                location.speed,
                power
            )
        )
    }

    fun getPoints(): List<TrackPoint> = points.toList()

    fun clear() {
        points.clear()
    }
}
