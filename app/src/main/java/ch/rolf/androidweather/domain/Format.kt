package ch.rolf.androidweather.domain

import java.text.NumberFormat
import java.time.Instant
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

private fun parseZoned(iso: String, zone: String? = null): ZonedDateTime {
    val instant = runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { ZonedDateTime.parse(iso).toInstant() }.getOrNull()
        ?: Instant.now()
    val z = zone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
    return instant.atZone(z)
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

fun formatTime(iso: String, timeZone: String? = null): String =
    parseZoned(iso, timeZone).format(timeFmt)

fun formatWeekday(iso: String): String = parseZoned(iso).format(weekdayFmt)
fun formatWeekdayLong(iso: String): String = parseZoned(iso).format(weekdayLongFmt)
fun formatDayMonth(iso: String): String = parseZoned(iso).format(dayMonthFmt)

fun formatHour(iso: String): String = parseZoned(iso).format(timeFmt)
fun formatHourLabel(iso: String): String = "${parseZoned(iso).hour.toString().padStart(2, '0')} Uhr"

fun formatUpdatedRelative(iso: String, now: Long = System.currentTimeMillis()): String {
    val then = runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull() ?: return ""
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
    val start = onset?.takeIf { runCatching { Instant.parse(it) }.isSuccess }?.let {
        "${formatDayMonth(it)} ${formatTime(it, timeZone)}"
    }.orEmpty()
    val end = expires?.takeIf { runCatching { Instant.parse(it) }.isSuccess }?.let {
        "${formatDayMonth(it)} ${formatTime(it, timeZone)}"
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
