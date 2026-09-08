package ch.rolf.androidweather.domain

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val de = Locale("de", "CH")
private val numberDe: NumberFormat = NumberFormat.getIntegerInstance(de)
private val numberOne: NumberFormat = NumberFormat.getNumberInstance(de).apply { maximumFractionDigits = 1 }
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", de)
private val weekdayFmt = DateTimeFormatter.ofPattern("EEE", de)
private val weekdayLongFmt = DateTimeFormatter.ofPattern("EEEE", de)
private val dayMonthFmt = DateTimeFormatter.ofPattern("d. MMM", de)
private val localDateTimeMinute = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
private val localDateTimeSecond = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

fun zoneIdOf(timeZone: String?): ZoneId =
    timeZone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()

/**
 * Parse Open-Meteo / ISO forecast stamps in the place timezone.
 * Handles `2026-09-07T13:00`, `2026-09-07T13:00:00`, date-only `2026-09-07`, and real instants.
 * Never falls back to [Instant.now] — a failed parse is null so callers cannot reuse "now" on every row.
 */
fun parseForecastInstant(iso: String, timeZone: String? = null): Instant? {
    if (iso.isBlank()) return null
    runCatching { Instant.parse(iso) }.getOrNull()?.let { return it }
    runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()?.let { return it }
    val zone = zoneIdOf(timeZone)
    runCatching { ZonedDateTime.parse(iso).toInstant() }.getOrNull()?.let { return it }
    runCatching { LocalDateTime.parse(iso, localDateTimeSecond).atZone(zone).toInstant() }.getOrNull()?.let { return it }
    runCatching { LocalDateTime.parse(iso, localDateTimeMinute).atZone(zone).toInstant() }.getOrNull()?.let { return it }
    runCatching { LocalDateTime.parse(iso).atZone(zone).toInstant() }.getOrNull()?.let { return it }
    runCatching { LocalDate.parse(iso).atStartOfDay(zone).toInstant() }.getOrNull()?.let { return it }
    return null
}

fun parseForecastEpochMilli(iso: String, timeZone: String? = null): Long =
    parseForecastInstant(iso, timeZone)?.toEpochMilli() ?: 0L

fun parseZoned(iso: String, timeZone: String? = null): ZonedDateTime? {
    val zone = zoneIdOf(timeZone)
    val instant = parseForecastInstant(iso, timeZone) ?: return null
    return instant.atZone(zone)
}

private fun hourFromIso(iso: String): Int? {
    val time = iso.substringAfter('T', "")
    if (time.length >= 2) return time.take(2).toIntOrNull()
    return null
}

fun formatTemp(value: Double): String = "${numberDe.format(kotlin.math.round(value).toInt())}°"
fun formatTempExact(value: Double): String = "${numberOne.format(value)}°"
fun formatMm(value: Double): String = "${numberOne.format(value)} mm"
fun formatPercent(value: Double): String = "${numberDe.format(kotlin.math.round(value).toInt())} %"
fun formatHpa(value: Double): String = "${numberDe.format(kotlin.math.round(value).toInt())} hPa"

fun formatWindValue(valueKmh: Double, unit: WindUnit): String =
    if (unit == WindUnit.Ms) numberOne.format(valueKmh / 3.6)
    else numberDe.format(kotlin.math.round(valueKmh).toInt())

fun formatWind(valueKmh: Double, unit: WindUnit): String =
    if (unit == WindUnit.Ms) "${formatWindValue(valueKmh, unit)} m/s"
    else "${formatWindValue(valueKmh, unit)} km/h"

fun formatTime(iso: String, timeZone: String? = null): String {
    parseZoned(iso, timeZone)?.let { return it.format(timeFmt) }
    val hour = hourFromIso(iso) ?: return iso
    val minute = iso.substringAfter('T', "").drop(3).take(2).toIntOrNull() ?: 0
    return "%02d:%02d".format(hour, minute)
}

fun formatWeekday(iso: String, timeZone: String? = null): String {
    val raw = parseZoned(iso, timeZone)?.format(weekdayFmt) ?: return iso
    return raw.trimEnd('.')
}

fun formatWeekdayLong(iso: String, timeZone: String? = null): String =
    parseZoned(iso, timeZone)?.format(weekdayLongFmt) ?: iso

fun formatDayMonth(iso: String, timeZone: String? = null): String =
    parseZoned(iso, timeZone)?.format(dayMonthFmt) ?: iso

fun formatHour(iso: String, timeZone: String? = null): String = formatTime(iso, timeZone)

fun formatHourLabel(iso: String, timeZone: String? = null): String {
    val hour = parseZoned(iso, timeZone)?.hour ?: hourFromIso(iso) ?: return iso
    return "${hour.toString().padStart(2, '0')} Uhr"
}

fun formatUpdatedRelative(iso: String, now: Long = System.currentTimeMillis()): String {
    val then = parseForecastEpochMilli(iso).takeIf { it > 0 } ?: return ""
    val minutes = ((now - then) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "gerade eben"
        minutes == 1L -> "vor 1 Min."
        minutes < 60 -> "vor $minutes Min."
        minutes < 120 -> "vor 1 Std."
        minutes < 1440 -> "vor ${minutes / 60} Std."
        minutes < 2880 -> "vor 1 Tag"
        else -> "vor ${minutes / 1440} Tagen"
    }
}

fun formatUpdatedAt(iso: String, now: Long = System.currentTimeMillis(), timeZone: String? = null): String {
    val relative = formatUpdatedRelative(iso, now)
    val exact = formatTime(iso, timeZone)
    return if (relative.isNotEmpty()) "$relative · $exact" else exact
}

fun formatRefreshStatus(
    iso: String,
    stale: Boolean,
    now: Long = System.currentTimeMillis(),
    offline: Boolean = false,
    timeZone: String? = null
): String {
    val whenLabel = formatUpdatedAt(iso, now, timeZone)
    return when {
        offline -> "Offline · Stand $whenLabel"
        stale -> "Zwischengespeichert · $whenLabel"
        else -> "Aktualisiert $whenLabel"
    }
}

fun formatStationLine(station: StationObservation, timeZone: String? = null): String {
    val parts = mutableListOf("${station.name} ${formatTempExact(station.temperature)}")
    station.observedAt?.let { parts += "gemessen ${formatTime(it, timeZone)}" }
    return parts.joinToString(" · ")
}

fun formatWidgetStationLine(station: StationObservation, timeZone: String? = null): String {
    val name = station.name.substringBefore(',').trim().ifEmpty { station.name }
    val core = "$name ${formatTempExact(station.temperature)}"
    val time = station.observedAt?.let { formatTime(it, timeZone) } ?: return core
    return "$core · $time"
}

fun placeLabel(place: Place): String = listOfNotNull(
    place.name,
    place.admin1?.takeIf { it != place.name },
    place.country
).joinToString(", ")

fun placeShort(place: Place): String = when {
    place.countryCode != null && place.admin1 != null && place.admin1 != place.name ->
        "${place.name}, ${place.admin1}"
    place.countryCode != null -> "${place.name}, ${place.countryCode}"
    else -> place.name
}

private val COMPASS = listOf(
    "N", "NNO", "NO", "ONO", "O", "OSO", "SO", "SSO",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
)

fun windDirection(degrees: Double): String {
    val index = (kotlin.math.round(degrees / 22.5).toInt() % 16 + 16) % 16
    return COMPASS[index]
}

fun formatAlertValidity(onset: String?, expires: String?, timeZone: String? = null): String? {
    val start = onset?.let { parseForecastInstant(it, timeZone) }?.let {
        "${formatDayMonth(onset, timeZone)} ${formatTime(onset, timeZone)}"
    }.orEmpty()
    val end = expires?.let { parseForecastInstant(it, timeZone) }?.let {
        "${formatDayMonth(expires, timeZone)} ${formatTime(expires, timeZone)}"
    }.orEmpty()
    return when {
        start.isNotEmpty() && end.isNotEmpty() -> "gültig $start – $end"
        end.isNotEmpty() -> "gültig bis $end"
        start.isNotEmpty() -> "gültig ab $start"
        else -> null
    }
}

fun hoursOnDay(hours: List<HourPoint>, date: String): List<HourPoint> =
    hours.filter { it.time.take(10) == date.take(10) }

fun formatUv(value: Double): String = String.format(java.util.Locale.US, "%.1f", value)

fun formatWidgetUpdated(iso: String, timeZone: String? = null, now: Long = System.currentTimeMillis()): String {
    val then = parseForecastEpochMilli(iso, timeZone)
    val minutes = if (then > 0) ((now - then) / 60_000).coerceAtLeast(0) else Long.MAX_VALUE
    val whenLabel = when {
        minutes < 1 -> "gerade eben"
        minutes == 1L -> "vor 1 Minute"
        minutes < 60 -> "vor $minutes Minuten"
        minutes < 120 -> "vor 1 Stunde"
        minutes < 1440 -> "vor ${minutes / 60} Stunden"
        else -> formatTime(iso, timeZone)
    }
    return "(Aktualisiert: $whenLabel)"
}
