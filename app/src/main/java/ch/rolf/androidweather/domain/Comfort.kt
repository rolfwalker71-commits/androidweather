package ch.rolf.androidweather.domain

import kotlin.math.pow

data class ComfortAdvice(
    val apparent: Double,
    val indexName: String,
    val indexValue: Double,
    val recommendation: String,
    val detail: String
)

private fun windChill(tempC: Double, windKmh: Double): Double {
    val v = maxOf(windKmh, 4.8)
    return 13.12 + 0.6215 * tempC - 11.37 * v.pow(0.16) + 0.3965 * tempC * v.pow(0.16)
}

private fun heatIndex(tempC: Double, rh: Double): Double {
    val t = (tempC * 9) / 5 + 32
    val hi = -42.379 + 2.04901523 * t + 10.14333127 * rh - 0.22475541 * t * rh -
        6.83783e-3 * t * t - 5.481717e-2 * rh * rh + 1.22874e-3 * t * t * rh +
        8.5282e-4 * t * rh * rh - 1.99e-6 * t * t * rh * rh
    return ((hi - 32) * 5) / 9
}

fun comfortAdvice(bundle: WeatherBundle): ComfortAdvice? {
    val current = bundle.current
    val hour = bundle.hours.firstOrNull()
    val temp = current.temperature_2m
    val apparent = current.apparent_temperature
    val wind = current.wind_speed_10m
    val rh = current.relative_humidity_2m
    val uv = hour?.uv ?: bundle.days.firstOrNull()?.uvMax
    val precipNow = current.precipitation + (hour?.precipMm ?: 0.0)
    val precipSoon = bundle.hours.take(3).sumOf { it.precipMm }
    val precipProb = bundle.hours.take(3).mapNotNull { it.precipProb }.firstOrNull()
    var indexName = "gefühlte Temperatur"
    var indexValue = apparent
    if (temp <= 10 && wind >= 8) {
        indexName = "Windchill"
        indexValue = windChill(temp, wind)
    } else if (temp >= 27 && rh >= 40) {
        indexName = "Hitzeindex"
        indexValue = heatIndex(temp, rh)
    }
    val recommendation = when {
        precipNow >= 0.4 || (precipSoon >= 0.8 && precipProb != null && precipProb >= 50) -> "Schirm"
        uv != null && uv >= 6 && current.is_day == 1 -> "Sonnencreme"
        indexValue <= 8 || temp <= 10 -> "Jacke"
        indexValue <= 14 || wind >= 28 -> "leichte Jacke"
        else -> "leichte Kleidung reicht"
    }
    return ComfortAdvice(
        apparent, indexName, indexValue, recommendation,
        "$indexName ${kotlin.math.round(indexValue).toInt()}° · $recommendation"
    )
}
