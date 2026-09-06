package com.example.findmycafe.data

data class Coordinate(val latitude: Double, val longitude: Double)

data class Cafe(
    val id: String = "",
    val name: String = "",
    val rating: Double = 0.0,
    val address: String = "",
    val description: String = "",
    val location: Coordinate? = null
)

object DistanceCalculator {
    fun kilometers(from: Coordinate?, to: Coordinate?): Double? {
        if (from == null || to == null) return null
        val earthRadiusKm = 6371.0
        val latitudeDifference = Math.toRadians(to.latitude - from.latitude)
        val longitudeDifference = Math.toRadians(to.longitude - from.longitude)
        val a = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2) +
            Math.cos(Math.toRadians(from.latitude)) * Math.cos(Math.toRadians(to.latitude)) *
            Math.sin(longitudeDifference / 2) * Math.sin(longitudeDifference / 2)
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    fun label(from: Coordinate?, to: Coordinate?): String = kilometers(from, to)?.let { km ->
        if (km < 1) "${(km * 1000).toInt()} m" else "%.1f km".format(km)
    } ?: "Lokasi tidak tersedia"
}
