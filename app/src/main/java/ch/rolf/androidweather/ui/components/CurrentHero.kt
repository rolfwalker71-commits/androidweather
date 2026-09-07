package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.rolf.androidweather.domain.Place
import ch.rolf.androidweather.domain.WeatherBundle
import ch.rolf.androidweather.domain.WindUnit
import ch.rolf.androidweather.domain.clothingLine
import ch.rolf.androidweather.domain.formatHpa
import ch.rolf.androidweather.domain.formatPercent
import ch.rolf.androidweather.domain.formatRefreshStatus
import ch.rolf.androidweather.domain.formatStationLine
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatTime
import ch.rolf.androidweather.domain.formatWind
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.insightLine
import ch.rolf.androidweather.domain.placeLabel
import ch.rolf.androidweather.domain.precipNowSummary
import ch.rolf.androidweather.domain.weatherMood
import ch.rolf.androidweather.domain.windDirection

private val HeroTempStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 70.sp,
    lineHeight = 70.sp,
    letterSpacing = (-1.5).sp,
    fontFeatureSettings = "tnum"
)

private val HeroConditionStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 19.sp,
    lineHeight = 24.sp
)

private val HeroPlaceStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.3).sp
)

private val NowValueStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 25.sp,
    fontFeatureSettings = "tnum"
)

fun heroMoodTopColor(mood: String, dark: Boolean): Color = when {
    dark -> when (mood) {
        "clear" -> Color(0xFF3D3420)
        "night" -> Color(0xFF1C1830)
        "rain" -> Color(0xFF1A2834)
        "snow" -> Color(0xFF1C2834)
        "storm" -> Color(0xFF2A2238)
        else -> Color(0xFF2A2E32)
    }
    else -> when (mood) {
        "clear" -> Color(0xFFFFE082)
        "night" -> Color(0xFF0D47A1)
        "rain" -> Color(0xFF64B5F6)
        "snow" -> Color(0xFF64B5F6)
        "storm" -> Color(0xFF006874)
        else -> Color(0xFF78909C)
    }
}

fun heroMoodBrush(mood: String, dark: Boolean): Brush = when {
    dark -> when (mood) {
        "clear" -> Brush.verticalGradient(listOf(Color(0xFF3D3420), Color(0xFF2C2824), Color(0xFF1E2021)))
        "night" -> Brush.verticalGradient(listOf(Color(0xFF1C1830), Color(0xFF242038), Color(0xFF1E2021)))
        "rain" -> Brush.verticalGradient(listOf(Color(0xFF1A2834), Color(0xFF1E2830), Color(0xFF1E2021)))
        "snow" -> Brush.verticalGradient(listOf(Color(0xFF1C2834), Color(0xFF222830), Color(0xFF1E2021)))
        "storm" -> Brush.verticalGradient(listOf(Color(0xFF2A2238), Color(0xFF241E30), Color(0xFF1E2021)))
        else -> Brush.verticalGradient(listOf(Color(0xFF2A2E32), Color(0xFF242628), Color(0xFF1E2021)))
    }
    else -> when (mood) {
        "clear" -> Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFCC80), Color(0xFFCCE8E9)))
        "night" -> Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF004F58)))
        "rain" -> Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF90CAF9), Color(0xFFBBDEFB)))
        "snow" -> Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF90CAF9), Color(0xFF9FA8DA)))
        "storm" -> Brush.verticalGradient(listOf(Color(0xFF006874), Color(0xFF00838F), Color(0xFF4DD0E1)))
        else -> Brush.verticalGradient(listOf(Color(0xFF78909C), Color(0xFF90A4AE), Color(0xFF80CBC4)))
    }
}

fun heroOnColor(mood: String, dark: Boolean): Color = when {
    dark -> Color(0xFFF4F6F7)
    mood == "clear" -> Color(0xFF3E2723)
    mood == "night" -> Color(0xFFEDE7F6)
    mood == "rain" || mood == "snow" -> Color(0xFF0D47A1)
    mood == "storm" -> Color(0xFF4A148C)
    else -> Color(0xFF263238)
}

@Composable
fun CurrentHero(
    place: Place,
    bundle: WeatherBundle,
    stale: Boolean,
    offline: Boolean,
    favored: Boolean,
    windUnit: WindUnit,
    notice: String?,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null
) {
    val current = bundle.current
    val wmo = getWmo(current.weather_code, current.is_day == 1)
    val mood = weatherMood(current.weather_code, current.is_day == 1)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val onHero = heroOnColor(mood, dark)
    val muted = onHero.copy(alpha = 0.75f)
    val today = bundle.days.firstOrNull()
    val stationLine = bundle.station?.let { formatStationLine(it, bundle.timezone) }
    val insight = insightLine(bundle)
    val clothing = clothingLine(bundle)
    val precip = precipNowSummary(bundle)
    val metricParts = buildList {
        add("Feuchte ${formatPercent(current.relative_humidity_2m)}")
        add("Druck ${formatHpa(current.pressure_msl)}")
        add("Bewölkung ${formatPercent(current.cloud_cover)}")
    }
    val sunLine = today?.takeIf { it.sunrise.isNotBlank() && it.sunset.isNotBlank() }?.let {
        "Sonne ${formatTime(it.sunrise, bundle.timezone)} – ${formatTime(it.sunset, bundle.timezone)}"
    }

    CompositionLocalProvider(LocalContentColor provides onHero) {
        Box(
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(heroMoodBrush(mood, dark))
                .then(
                    if (onOpen != null) {
                        Modifier.clickable(role = Role.Button, onClick = onOpen)
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            formatRefreshStatus(
                                iso = bundle.fetchedAt,
                                stale = stale,
                                offline = offline,
                                timeZone = bundle.timezone
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted
                        )
                        Text(place.name, style = HeroPlaceStyle, color = onHero)
                        Text(
                            placeLabel(place),
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted
                        )
                    }
                    WeatherIcon(
                        code = current.weather_code,
                        isDay = current.is_day == 1,
                        size = 68.dp,
                        contentDescription = wmo.label
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(formatTemp(current.temperature_2m), style = HeroTempStyle, color = onHero)
                        Column {
                            Text(wmo.label, style = HeroConditionStyle, color = onHero)
                            Text(
                                "Gefühlt ${formatTemp(current.apparent_temperature)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = muted
                            )
                        }
                    }
                    if (stationLine != null) {
                        Text(stationLine, style = MaterialTheme.typography.bodyMedium, color = muted)
                    }
                }

                if (notice != null || insight != null || clothing != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (notice != null) {
                            Text(
                                notice,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = onHero,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        if (insight != null) {
                            Text(
                                insight,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = onHero
                            )
                        }
                        if (clothing != null) {
                            Text(clothing, style = MaterialTheme.typography.bodyLarge, color = onHero)
                        }
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(onHero.copy(alpha = 0.09f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NowMetricCell(
                            icon = Icons.Outlined.Thunderstorm,
                            label = "Regen",
                            value = precip.headline,
                            detail = precip.detail,
                            modifier = Modifier.weight(1f)
                        )
                        val gusts = current.wind_gusts_10m
                        NowMetricCell(
                            icon = Icons.Outlined.Air,
                            label = "Wind",
                            value = formatWind(current.wind_speed_10m, windUnit),
                            detail = buildString {
                                append("aus ${windDirection(current.wind_direction_10m)}")
                                append(" · Böen ${formatWind(gusts, windUnit)}")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        metricParts.joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge,
                        color = onHero.copy(alpha = 0.78f)
                    )
                    if (sunLine != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.WbTwilight,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = onHero.copy(alpha = 0.78f)
                            )
                            Text(
                                sunLine,
                                style = MaterialTheme.typography.labelLarge,
                                color = onHero.copy(alpha = 0.78f)
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (today != null) {
                        Text(
                            "Heute ${formatTemp(today.tMin)} bis ${formatTemp(today.tMax)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (favored) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (favored) "Favorit entfernen" else "Als Favorit speichern",
                            modifier = Modifier.size(24.dp),
                            tint = onHero
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowMetricCell(
    icon: ImageVector,
    label: String,
    value: String,
    detail: String?,
    modifier: Modifier = Modifier
) {
    val color = LocalContentColor.current
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color.copy(alpha = 0.78f))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = color.copy(alpha = 0.78f)
            )
        }
        Text(
            value,
            style = NowValueStyle,
            color = color,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (detail != null) {
            Text(
                detail,
                style = MaterialTheme.typography.labelLarge,
                color = color.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
