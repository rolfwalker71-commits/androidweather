package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.domain.DayPoint
import ch.rolf.androidweather.domain.HourPoint
import ch.rolf.androidweather.domain.WindUnit
import ch.rolf.androidweather.domain.formatDayMonth
import ch.rolf.androidweather.domain.formatHourLabel
import ch.rolf.androidweather.domain.formatMm
import ch.rolf.androidweather.domain.formatPercent
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatTime
import ch.rolf.androidweather.domain.formatUv
import ch.rolf.androidweather.domain.formatWeekday
import ch.rolf.androidweather.domain.formatWeekdayLong
import ch.rolf.androidweather.domain.formatWind
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.hoursOnDay
import ch.rolf.androidweather.domain.parseForecastEpochMilli
import ch.rolf.androidweather.domain.uvLevel
import ch.rolf.androidweather.domain.weatherMood
import ch.rolf.androidweather.domain.windDirection
import ch.rolf.androidweather.widget.glyphEmoji

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
fun DayDetail(
    day: DayPoint,
    days: List<DayPoint>,
    hours: List<HourPoint>,
    unit: WindUnit,
    timeZone: String? = null,
    nowIso: String? = null,
    onClose: () -> Unit,
    onSelectDay: (DayPoint) -> Unit,
    onSelectHour: (HourPoint) -> Unit = {}
) {
    val dark = isSystemInDarkTheme()
    val wmo = getWmo(day.code, true)
    val todayDate = days.firstOrNull()?.date
    val weekday = if (day.date == todayDate) "Heute" else formatWeekdayLong(day.date, timeZone)
    val dayHours = hoursOnDay(hours, day.date)
    val nowEpoch = nowIso?.let { parseForecastEpochMilli(it, timeZone) }?.takeIf { it > 0 }
    val precip = buildString {
        day.precipProb?.let { append(formatPercent(it)); append(" · ") }
        append(formatMm(day.precipMm))
    }
    val sun = listOf(day.sunrise, day.sunset)
        .filter { it.isNotBlank() }
        .joinToString(" – ") { formatTime(it, timeZone) }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatDayMonth(day.date, timeZone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Text(
                    "$weekday · ${wmo.label}",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Schliessen")
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.forEachIndexed { index, item ->
                val selected = item.date == day.date
                val label = if (index == 0 || item.date == todayDate) "Heute" else formatWeekday(item.date, timeZone)
                Text(
                    text = label,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable(role = Role.Button) { onSelectDay(item) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(glyphEmoji(wmo.glyph), style = MaterialTheme.typography.displaySmall)
            Text(
                "${formatTemp(day.tMin)} / ${formatTemp(day.tMax)}",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(
                    title = "Niederschlag",
                    value = precip,
                    icon = Icons.Outlined.WaterDrop,
                    colors = moodColors("rain", dark),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Wind max",
                    value = formatWind(day.windMax, unit),
                    icon = Icons.Outlined.Air,
                    colors = moodColors("cloud", dark),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (day.uvMax != null) {
                    MetricTile(
                        title = "UV-Index",
                        value = formatUv(day.uvMax),
                        icon = Icons.Outlined.WbSunny,
                        colors = scaleColors(uvLevel(day.uvMax).tone, dark).let { tone ->
                            if (tone.container.alpha > 0f) tone else moodColors("clear", dark)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (sun.isNotEmpty()) {
                    MetricTile(
                        title = "Sonne",
                        value = sun,
                        icon = Icons.Outlined.WbTwilight,
                        colors = moodColors("clear", dark),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Stundenverlauf", style = MaterialTheme.typography.titleMedium)
            if (dayHours.isEmpty()) {
                Text(
                    "Stundenverlauf nach der nächsten Aktualisierung verfügbar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                HourlyForecastStrip(
                    hours = dayHours,
                    timezone = timeZone.orEmpty(),
                    labelFirstAsNow = false,
                    nowEpochMilli = nowEpoch,
                    onSelect = onSelectHour
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    colors: MoodColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.container)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.content.copy(alpha = 0.8f)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.content.copy(alpha = 0.8f)
            )
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.content)
    }
}
