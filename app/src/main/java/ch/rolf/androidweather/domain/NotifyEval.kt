package ch.rolf.androidweather.domain

data class NotifyCandidate(
    val category: String,
    val fingerprint: String,
    val cooldownHours: Int,
    val title: String,
    val body: String
)

val PREF_META = listOf(
    Triple("rainSoon", "Regen bald", "Niederschlag in der nächsten Stunde"),
    Triple("warnings", "Warnungen", "Meteoalarm / Unwetter am gespeicherten Ort"),
    Triple("frost", "Frost", "Glatteis und Temperaturen um den Gefrierpunkt"),
    Triple("uv", "UV hoch", "Starke Sonne am Tag"),
    Triple("air", "Luft & Pollen", "Schlechte Luft oder starker Pollenflug"),
    Triple("dailyBrief", "Morgenbriefing", "Kurzer Überblick am Morgen"),
    Triple("forecastChange", "Wetteränderung", "Wenn sich Regen, Wind oder Temperatur gegenüber der letzten Prognose deutlich ändern")
)

fun evaluateNotifications(
    bundle: WeatherBundle,
    prefs: NotifyPrefs,
    alerts: List<AlertItem>,
    change: ProactivityChange?
): List<NotifyCandidate> {
    val notices = mutableListOf<NotifyCandidate>()
    val temps = listOf(bundle.current.temperature_2m, bundle.current.apparent_temperature)
    val nearFrost = bundle.hours.take(6).any { it.temperature <= 1.2 }
    val frostNow = temps.any { it <= 1.2 }
    val nextHourSamples = bundle.minutes.take(4).mapNotNull { it.precipMm }
    val nextHourPrecip = if (nextHourSamples.isNotEmpty()) nextHourSamples.sum() else null
    val nextHourProb = bundle.hours.firstOrNull()?.precipProb

    if (prefs.rainSoon && ((nextHourPrecip != null && nextHourPrecip >= 0.3) || (nextHourProb != null && nextHourProb >= 70))) {
        val body = if (nextHourPrecip != null && nextHourPrecip >= 0.3) {
            "Niederschlag in der nächsten Stunde (${"%.1f".format(nextHourPrecip)} mm)."
        } else {
            "Regenwahrscheinlichkeit ${kotlin.math.round(nextHourProb ?: 0.0).toInt()} % in der nächsten Stunde."
        }
        notices += NotifyCandidate(
            "rainSoon",
            "rain-${bundle.hours.firstOrNull()?.time?.take(13) ?: "now"}",
            3, "Regen bald", body
        )
    }
    if (prefs.warnings) {
        alerts.filter { it.severity in setOf("moderate", "severe", "extreme") }.take(2).forEach { alert ->
            notices += NotifyCandidate(
                "warnings", "warn-${alert.id}", 6,
                alert.event.ifBlank { "Wetterwarnung" },
                alert.headline.ifBlank { alert.area ?: alert.event }
            )
        }
    }
    if (prefs.frost && (frostNow || nearFrost)) {
        notices += NotifyCandidate(
            "frost",
            "frost-${java.time.LocalDate.now()}",
            8, "Frost",
            "Temperatur ${"%.0f".format(bundle.current.temperature_2m)}° (Open-Meteo)."
        )
    }
    val uvNow = bundle.hours.firstOrNull()?.uv ?: bundle.air?.uv_index
    if (prefs.uv && bundle.current.is_day == 1 && uvNow != null && uvNow >= 7) {
        notices += NotifyCandidate(
            "uv", "uv-${java.time.LocalDate.now()}", 12, "UV hoch",
            "UV-Index ${"%.0f".format(uvNow)} — Sonne meiden, Haut schützen."
        )
    }
    val pollenMax = listOfNotNull(bundle.pollen.alder, bundle.pollen.birch, bundle.pollen.grass).maxOrNull()
    val aqi = bundle.air?.european_aqi
    if (prefs.air && ((aqi != null && aqi >= 60) || (pollenMax != null && pollenMax >= 100))) {
        val bits = buildList {
            if (aqi != null && aqi >= 60) add("Luftqualität ${kotlin.math.round(aqi).toInt()}")
            if (pollenMax != null && pollenMax >= 100) add("Pollen ${kotlin.math.round(pollenMax).toInt()}")
        }
        notices += NotifyCandidate("air", "air-${java.time.LocalDate.now()}", 8, "Luft & Pollen", "${bits.joinToString(", ")}.")
    }
    val localHour = runCatching {
        java.time.Instant.parse(bundle.current.time).atZone(
            runCatching { java.time.ZoneId.of(bundle.timezone) }.getOrDefault(java.time.ZoneId.systemDefault())
        ).hour
    }.getOrDefault(12)
    if (prefs.dailyBrief && localHour in 6..9) {
        val body = listOfNotNull(insightLine(bundle), clothingLine(bundle)).joinToString(" · ")
        if (body.isNotBlank()) {
            notices += NotifyCandidate("dailyBrief", "brief-${java.time.LocalDate.now()}", 20, "Morgenbriefing", body)
        }
    }
    if (prefs.forecastChange && change != null) {
        notices += NotifyCandidate("forecastChange", change.fingerprint, 4, change.title, change.body)
    }
    return notices
}
