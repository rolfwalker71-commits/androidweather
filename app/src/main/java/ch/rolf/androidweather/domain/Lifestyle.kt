package ch.rolf.androidweather.domain

import java.time.Instant

data class SkyWatch(
    val goldenStart: String?,
    val goldenEnd: String?,
    val goldenLabel: String?,
    val starsLabel: String?,
    val moonLabel: String
)

fun skyWatch(bundle: WeatherBundle): SkyWatch {
    val today = bundle.days.firstOrNull()
    val moon = moonInfo(runCatching { Instant.parse(bundle.current.time) }.getOrDefault(Instant.now()))
    if (today?.sunrise.isNullOrBlank() || today?.sunset.isNullOrBlank()) {
        return SkyWatch(null, null, null, null, moon.label)
    }
    val sunset = Instant.parse(today!!.sunset)
    val eveningStart = sunset.minusSeconds(55 * 60)
    val cloud = bundle.hours.firstOrNull()?.cloud ?: bundle.current.cloud_cover
    val goldOk = cloud <= 55
    val starsOk = cloud <= 30 && moon.illumination < 0.75
    return SkyWatch(
        goldenStart = eveningStart.toString(),
        goldenEnd = sunset.toString(),
        goldenLabel = if (goldOk) "Goldene Stunde gegen ${formatTime(eveningStart.toString(), bundle.timezone)}"
        else "Goldene Stunde hinter Wolken",
        starsLabel = when {
            starsOk -> "Wenig Bewölkung, Mond nicht voll"
            cloud > 45 -> "Bewölkt"
            else -> moon.label
        },
        moonLabel = "${moon.label} · ${kotlin.math.round(moon.illumination * 100).toInt()} % beleuchtet"
    )
}

fun commuteHint(home: WeatherBundle, dest: WeatherBundle): String? {
    val a = clothingLine(home) ?: return null
    val b = clothingLine(dest) ?: return null
    val rainHome = home.hours.take(4).sumOf { it.precipMm }
    val rainDest = dest.hours.take(4).sumOf { it.precipMm }
    return when {
        rainHome < 0.3 && rainDest >= 0.6 -> "Start trocken, Ziel nass — $b"
        rainHome >= 0.6 && rainDest < 0.3 -> "Start nass, Ziel trockener — $a"
        else -> {
            val comfort = comfortAdvice(dest)
            if (comfort != null) "${home.place.name}: $a. ${dest.place.name}: ${comfort.detail}"
            else "$a. $b"
        }
    }
}
