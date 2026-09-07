package ch.rolf.androidweather.domain

data class NamedWind(
    val name: String,
    val detail: String,
    val confidence: String,
    val uncertain: Boolean
)

private fun dirBetween(degrees: Double, from: Double, to: Double): Boolean {
    val d = ((degrees % 360) + 360) % 360
    return if (from <= to) d in from..to else d >= from || d <= to
}

fun namedWind(place: Place, bundle: WeatherBundle): NamedWind {
    val current = bundle.current
    val dir = current.wind_direction_10m
    val speed = current.wind_speed_10m
    val gusts = current.wind_gusts_10m
    val hour = bundle.hours.firstOrNull()
    val humidity = current.relative_humidity_2m
    if (!isSwitzerland(place)) {
        return NamedWind(
            "kein regionaler Name",
            "Föhn, Bise und Talwind sind Heuristiken für die Schweiz.",
            "niedrig",
            true
        )
    }
    val mittelland = inMittelland(place.latitude, place.longitude)
    val south = southOfAlps(place.latitude)
    if (mittelland && speed >= 14 && dirBetween(dir, 30.0, 90.0)) {
        return NamedWind(
            "Bise",
            "Nordostwind über dem Mittelland — oft kühl und trocken. Heuristik aus Richtung und Stärke.",
            if (speed >= 22) "hoch" else "mittel",
            speed < 22
        )
    }
    if (!south && speed >= 18 && dirBetween(dir, 150.0, 210.0) && (gusts >= 28 || humidity <= 55)) {
        return NamedWind(
            "Südföhn",
            "Südströmung, böig und oft milder. Keine offizielle Föhndiagnose.",
            if (gusts >= 40 && humidity <= 45) "hoch" else "mittel",
            true
        )
    }
    if (south && speed >= 16 && dirBetween(dir, 330.0, 30.0) && humidity <= 60) {
        return NamedWind(
            "Nordföhn",
            "Nordströmung südlich der Alpen — meist klarer und trockener. Heuristik.",
            "mittel",
            true
        )
    }
    val hourOfDay = runCatching { java.time.Instant.parse(current.time).atZone(java.time.ZoneId.systemDefault()).hour }
        .getOrDefault(12)
    val alpine = !mittelland && place.latitude in 45.85..47.1
    if (alpine && speed in 6.0..22.0 && gusts < 35) {
        if (hourOfDay in 10..17 && dirBetween(dir, 160.0, 220.0)) {
            return NamedWind("Talwind", "Tageswind talaufwärts möglich — unsicher ohne lokale Talachse.", "niedrig", true)
        }
        if ((hourOfDay >= 20 || hourOfDay <= 7) && dirBetween(dir, 330.0, 30.0)) {
            return NamedWind("Bergwind", "Nächtlicher Abfluss talabwärts möglich. Heuristik.", "niedrig", true)
        }
    }
    if (hour?.windDir != null && kotlin.math.abs(hour.windDir - dir) > 50 && speed >= 10) {
        return NamedWind("drehender Wind", "Richtung ändert sich — noch keine klare Lage.", "niedrig", true)
    }
    return NamedWind(
        "kein markanter Lagewind",
        "Keine Bise-, Föhn- oder Talwindlage aus Richtung und Stärke ableitbar.",
        "mittel",
        true
    )
}
