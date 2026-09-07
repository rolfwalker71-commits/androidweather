package ch.rolf.androidweather.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return 6371.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun isSwitzerland(place: Place): Boolean {
    if (place.countryCode?.uppercase() == "CH") return true
    return place.latitude in 45.8..47.85 && place.longitude in 5.9..10.55
}

fun inMittelland(lat: Double, lon: Double): Boolean =
    lat in 46.7..47.65 && lon in 6.4..9.7

fun southOfAlps(lat: Double): Boolean = lat < 46.35

fun samePlace(a: Place, b: Place): Boolean =
    kotlin.math.abs(a.latitude - b.latitude) < 0.01 &&
        kotlin.math.abs(a.longitude - b.longitude) < 0.01

fun placeKey(place: Place): String =
    "${"%.3f".format(place.latitude)},${"%.3f".format(place.longitude)}"
