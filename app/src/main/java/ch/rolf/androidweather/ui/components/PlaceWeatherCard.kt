package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.domain.WeatherBundle
import ch.rolf.androidweather.domain.WindUnit
import ch.rolf.androidweather.domain.formatPercent
import ch.rolf.androidweather.domain.formatStationLine
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatWind
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.nextPrecipLine
import ch.rolf.androidweather.domain.placeShort
import ch.rolf.androidweather.domain.weatherMood
import ch.rolf.androidweather.domain.windDirection

@Composable
fun PlaceWeatherCard(
    title: String,
    bundle: WeatherBundle?,
    emptyText: String,
    windUnit: WindUnit,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null
) {
    if (bundle == null) {
        WxCard(modifier = modifier, onClick = onOpen) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val current = bundle.current
    val wmo = getWmo(current.weather_code, current.is_day == 1)
    val mood = weatherMood(current.weather_code, current.is_day == 1)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val onHero = heroOnColor(mood, dark)
    val muted = onHero.copy(alpha = 0.75f)
    CompositionLocalProvider(LocalContentColor provides onHero) {
        Box(
            modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(heroMoodBrush(mood, dark))
                .then(
                    if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen)
                    else Modifier
                )
        ) {
            HeroIllustration(
                res = weatherIllustrationRes(mood),
                dark = dark,
                night = mood == "night",
                modifier = Modifier.matchParentSize()
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = muted)
            Text(bundle.place.name, style = MaterialTheme.typography.headlineSmall, color = onHero)
            Text(placeShort(bundle.place), style = MaterialTheme.typography.bodyMedium, color = muted)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    formatTemp(current.temperature_2m),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = onHero
                )
                WeatherIcon(
                    code = current.weather_code,
                    isDay = current.is_day == 1,
                    size = 52.dp,
                    contentDescription = wmo.label
                )
            }
            Text(wmo.label, style = MaterialTheme.typography.titleMedium, color = onHero)
            Text(
                nextPrecipLine(bundle) ?: "Kein Niederschlag in den nächsten Stunden",
                style = MaterialTheme.typography.bodyLarge,
                color = onHero
            )
            Text(
                bundle.station?.let { formatStationLine(it, bundle.timezone) }
                    ?: "Keine Stationsmessung in der Nähe",
                style = MaterialTheme.typography.bodyMedium,
                color = muted
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(onHero.copy(alpha = 0.09f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Wind ${formatWind(current.wind_speed_10m, windUnit)} · ${windDirection(current.wind_direction_10m)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = onHero
                )
                Text(
                    "Feuchte ${formatPercent(current.relative_humidity_2m)} · Gefühlte ${formatTemp(current.apparent_temperature)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted
                )
            }
            }
        }
    }
}
