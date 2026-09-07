package ch.rolf.androidweather.domain

data class WmoInfo(val label: String, val glyph: String)

private val WMO = mapOf(
    0 to WmoInfo("Klarer Himmel", "sunny"),
    1 to WmoInfo("Überwiegend klar", "sunny"),
    2 to WmoInfo("Teilweise bewölkt", "partly_cloudy"),
    3 to WmoInfo("Bedeckt", "cloud"),
    45 to WmoInfo("Nebel", "foggy"),
    48 to WmoInfo("Reifnebel", "foggy"),
    51 to WmoInfo("Leichter Nieselregen", "drizzle"),
    53 to WmoInfo("Nieselregen", "drizzle"),
    55 to WmoInfo("Starker Nieselregen", "drizzle"),
    56 to WmoInfo("Leichter Eisniesel", "sleet"),
    57 to WmoInfo("Gefrierender Nieselregen", "sleet"),
    61 to WmoInfo("Leichter Regen", "rainy"),
    63 to WmoInfo("Regen", "rainy"),
    65 to WmoInfo("Starker Regen", "rainy_heavy"),
    66 to WmoInfo("Leichter Eisregen", "sleet"),
    67 to WmoInfo("Eisregen", "sleet"),
    71 to WmoInfo("Leichter Schneefall", "snowy"),
    73 to WmoInfo("Schneefall", "snowy"),
    75 to WmoInfo("Starker Schneefall", "snowy"),
    77 to WmoInfo("Schneegriesel", "snowy"),
    80 to WmoInfo("Leichte Regenschauer", "showers"),
    81 to WmoInfo("Regenschauer", "showers"),
    82 to WmoInfo("Heftige Regenschauer", "rainy_heavy"),
    85 to WmoInfo("Leichte Schneeschauer", "snowy"),
    86 to WmoInfo("Schneeschauer", "snowy"),
    95 to WmoInfo("Gewitter", "thunderstorm"),
    96 to WmoInfo("Gewitter mit Hagel", "hail"),
    99 to WmoInfo("Schweres Gewitter mit Hagel", "hail")
)

fun getWmo(code: Int, isDay: Boolean = true): WmoInfo {
    WMO[code]?.let { info ->
        if (!isDay && code <= 1) return WmoInfo(info.label, "night")
        if (!isDay && code == 2) return WmoInfo(info.label, "partly_cloudy_night")
        if (!isDay && code in 80..81) return WmoInfo(info.label, "showers_night")
        return info
    }
    val inferred = when {
        code <= 3 -> WMO[code] ?: WMO[2]!!
        code <= 19 -> WmoInfo("Dunst oder Nebel", "foggy")
        code <= 29 -> WmoInfo("Niederschlag in der Nähe", "rainy")
        code <= 39 -> WmoInfo("Schneeverwehung", "snowy")
        code <= 49 -> WmoInfo("Nebel", "foggy")
        code <= 59 -> WmoInfo("Nieselregen", "drizzle")
        code <= 69 -> WmoInfo("Regen", "rainy")
        code <= 79 -> WmoInfo("Schnee", "snowy")
        code <= 84 -> WmoInfo("Regenschauer", if (isDay) "showers" else "showers_night")
        code <= 94 -> WmoInfo("Schneeschauer", "snowy")
        else -> WmoInfo("Gewitter", "thunderstorm")
    }
    return inferred
}

fun weatherMood(code: Int, isDay: Boolean): String {
    if (!isDay && code <= 2) return "night"
    if (code <= 1) return "clear"
    if (code <= 3) return "cloud"
    if (code >= 95) return "storm"
    if ((code in 71..77) || code == 85 || code == 86) return "snow"
    if ((code in 10..19) || (code in 40..49) || code == 45 || code == 48) return "fog"
    if (code >= 51) return "rain"
    return "cloud"
}
