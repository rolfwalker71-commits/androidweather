package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.domain.DayPoint
import ch.rolf.androidweather.domain.HourPoint
import ch.rolf.androidweather.domain.WindUnit
import ch.rolf.androidweather.domain.formatHourLabel
import ch.rolf.androidweather.domain.formatMm
import ch.rolf.androidweather.domain.formatPercent
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatTime
import ch.rolf.androidweather.domain.formatWeekdayLong
import ch.rolf.androidweather.domain.formatWind
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.windDirection

@Composable
fun HourDetail(hour: HourPoint, unit: WindUnit, timeZone: String? = null) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(formatHourLabel(hour.time, timeZone), style = MaterialTheme.typography.headlineSmall)
        Text(getWmo(hour.code, hour.isDay).label, style = MaterialTheme.typography.titleMedium)
        Text("Temperatur ${formatTemp(hour.temperature)} · gefühlt ${formatTemp(hour.feelsLike)}")
        Text("Wind ${formatWind(hour.wind, unit)}" + (hour.windDir?.let { " ${windDirection(it)}" } ?: ""))
        hour.gusts?.let { Text("Böen ${formatWind(it, unit)}") }
        Text("Luftfeuchtigkeit ${formatPercent(hour.humidity)}")
        hour.precipProb?.let { Text("Niederschlag ${formatPercent(it)} · ${formatMm(hour.precipMm)}") }
        hour.uv?.let { Text("UV ${it.toInt()}") }
        hour.cloud?.let { Text("Bewölkung ${formatPercent(it)}") }
        hour.visibility?.let { Text("Sicht ${it.toInt()} m") }
        hour.cape?.let { Text("CAPE ${it.toInt()} J/kg") }
        hour.freezingLevel?.let { Text("Nullgradgrenze ${it.toInt()} m") }
        hour.snowfall?.let { Text("Schnee ${formatMm(it)}") }
        hour.dewPoint?.let { Text("Taupunkt ${formatTemp(it)}") }
    }
}

@Composable
fun DayDetail(day: DayPoint, unit: WindUnit, timeZone: String? = null) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(formatWeekdayLong(day.date, timeZone), style = MaterialTheme.typography.headlineSmall)
        Text(getWmo(day.code, true).label, style = MaterialTheme.typography.titleMedium)
        Text("${formatTemp(day.tMax)} / ${formatTemp(day.tMin)}")
        Text("Niederschlag ${formatMm(day.precipMm)}" + (day.precipProb?.let { " · ${formatPercent(it)}" } ?: ""))
        Text("Wind max ${formatWind(day.windMax, unit)}")
        day.uvMax?.let { Text("UV max ${it.toInt()}") }
        if (day.sunrise.isNotBlank()) Text("Sonnenaufgang ${formatTime(day.sunrise, timeZone)}")
        if (day.sunset.isNotBlank()) Text("Sonnenuntergang ${formatTime(day.sunset, timeZone)}")
    }
}
