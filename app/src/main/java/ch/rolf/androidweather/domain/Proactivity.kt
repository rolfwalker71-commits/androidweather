package ch.rolf.androidweather.domain

import java.time.Instant
import java.time.format.DateTimeFormatter

private const val HORIZON_HOURS = 12
private const val PRECIP_SUM_HOURS = 6

fun buildForecastSnapshot(bundle: WeatherBundle): ForecastSnapshot {
    val hours = bundle.hours.take(HORIZON_HOURS)
    val precipHours = bundle.hours.take(PRECIP_SUM_HOURS)
    val precipOnsetIso = hours.find { it.precipMm >= WET_MM }?.time
        ?: bundle.minutes.find { it.precipMm != null && it.precipMm >= 0.1 }?.time
    val precipNext6Mm = precipHours.sumOf { it.precipMm }
    val maxWind = hours.maxOfOrNull { it.wind }
    val maxGust = hours.mapNotNull { it.gusts }.maxOrNull()
    val today = bundle.days.firstOrNull()
    return ForecastSnapshot(
        dayKey = dayKeyInZone(bundle.current.time.ifBlank { bundle.fetchedAt }, bundle.timezone),
        precipOnsetIso = precipOnsetIso,
        precipNext6Mm = precipNext6Mm,
        maxWindKmh = maxWind,
        maxGustKmh = maxGust,
        todayMax = today?.tMax,
        todayMin = today?.tMin,
        todayCode = today?.code ?: bundle.current.weather_code
    )
}

fun diffForecastSnapshots(
    previous: ForecastSnapshot?,
    current: ForecastSnapshot,
    timeZone: String? = null
): ProactivityChange? {
    if (previous == null || previous.dayKey != current.dayKey) return null
    val changes = mutableListOf<ProactivityChange>()
    if (isWetPlan(previous) && wasDryPlan(current)) {
        changes += ProactivityChange(
            "rainCancel", "rain-cancel-${current.dayKey}",
            "Regen fällt aus", "Wetteränderung",
            "Regen fällt aus — die Prognose ist wieder trocken."
        )
    } else if (wasDryPlan(previous) && isWetPlan(current) && current.precipOnsetIso != null) {
        val whenLabel = formatTime(current.precipOnsetIso, timeZone)
        changes += ProactivityChange(
            "rainNew", "rain-new-${current.precipOnsetIso.take(13)}",
            "Neu: Regen ab $whenLabel", "Wetteränderung",
            "Neu: Regen ab $whenLabel."
        )
    } else if (
        previous.precipOnsetIso != null && current.precipOnsetIso != null &&
        parseMs(current.precipOnsetIso, timeZone) <= parseMs(previous.precipOnsetIso, timeZone) - EARLIER_MS
    ) {
        val whenLabel = formatTime(current.precipOnsetIso, timeZone)
        changes += ProactivityChange(
            "rainEarlier", "rain-earlier-${current.precipOnsetIso.take(13)}",
            "Regen ab $whenLabel (früher als gedacht)", "Wetteränderung",
            "Regen ab $whenLabel — früher als gedacht."
        )
    }
    val gustJump = previous.maxGustKmh != null && current.maxGustKmh != null &&
        current.maxGustKmh - previous.maxGustKmh >= GUST_JUMP && current.maxGustKmh >= GUST_MIN
    val windJump = previous.maxWindKmh != null && current.maxWindKmh != null &&
        current.maxWindKmh - previous.maxWindKmh >= WIND_JUMP && current.maxWindKmh >= WIND_MIN
    if (gustJump || windJump) {
        val peak = if (gustJump) kotlin.math.round(current.maxGustKmh!!).toInt()
        else kotlin.math.round(current.maxWindKmh!!).toInt()
        val label = if (gustJump) "Böen bis $peak km/h" else "Wind bis $peak km/h"
        changes += ProactivityChange(
            "windJump", "wind-${current.dayKey}-$peak",
            "Stärkerer Wind: $label", "Wetteränderung",
            "Stärkerer Wind in der Prognose: $label."
        )
    }
    if (previous.todayMax != null && current.todayMax != null &&
        kotlin.math.abs(current.todayMax - previous.todayMax) >= TEMP_SWING
    ) {
        val dir = if (current.todayMax > previous.todayMax) "höher" else "tiefer"
        changes += ProactivityChange(
            "tempSwing", "tmax-${current.dayKey}-${kotlin.math.round(current.todayMax).toInt()}",
            "Höchsttemperatur neu ${formatTemp(current.todayMax)} ($dir als gedacht)",
            "Wetteränderung",
            "Höchsttemperatur neu ${formatTemp(current.todayMax)} (vorher ${formatTemp(previous.todayMax)})."
        )
    } else if (previous.todayMin != null && current.todayMin != null &&
        kotlin.math.abs(current.todayMin - previous.todayMin) >= TEMP_SWING
    ) {
        val dir = if (current.todayMin > previous.todayMin) "höher" else "tiefer"
        changes += ProactivityChange(
            "tempSwing", "tmin-${current.dayKey}-${kotlin.math.round(current.todayMin).toInt()}",
            "Tiefsttemperatur neu ${formatTemp(current.todayMin)} ($dir als gedacht)",
            "Wetteränderung",
            "Tiefsttemperatur neu ${formatTemp(current.todayMin)} (vorher ${formatTemp(previous.todayMin)})."
        )
    }
    val priority = listOf("rainCancel", "rainEarlier", "rainNew", "windJump", "tempSwing")
    return priority.firstNotNullOfOrNull { kind -> changes.find { it.kind == kind } }
}

fun heroProactivityLine(change: ProactivityChange?): String? =
    change?.let { "Wetteränderung: ${it.detail}" }

private fun wasDryPlan(snap: ForecastSnapshot) =
    snap.precipOnsetIso == null && snap.precipNext6Mm < DRY_PLAN_MM

private fun isWetPlan(snap: ForecastSnapshot) =
    snap.precipOnsetIso != null || snap.precipNext6Mm >= NEW_RAIN_MM

private fun parseMs(iso: String, timeZone: String? = null): Long =
    parseForecastEpochMilli(iso, timeZone)

private fun dayKeyInZone(iso: String, timeZone: String?): String {
    val instant = parseForecastInstant(iso, timeZone) ?: Instant.now()
    return DateTimeFormatter.ISO_LOCAL_DATE.format(instant.atZone(zoneIdOf(timeZone)))
}
