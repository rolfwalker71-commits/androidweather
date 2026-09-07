package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    WxCard(modifier = modifier, onClick = onOpen) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (bundle == null) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@WxCard
        }
        val current = bundle.current
        val wmo = getWmo(current.weather_code, current.is_day == 1)
        Text(
            bundle.place.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            placeShort(bundle.place),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                formatTemp(current.temperature_2m),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
            )
            WeatherIcon(
                code = current.weather_code,
                isDay = current.is_day == 1,
                size = 52.dp,
                contentDescription = wmo.label
            )
        }
        Text(wmo.label, style = MaterialTheme.typography.titleMedium)
        nextPrecipLine(bundle)?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }
        bundle.station?.let {
            Text(
                formatStationLine(it, bundle.timezone),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        InsetPanel {
            Text(
                "Wind ${formatWind(current.wind_speed_10m, windUnit)} · ${windDirection(current.wind_direction_10m)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Feuchte ${formatPercent(current.relative_humidity_2m)} · Gefühlte ${formatTemp(current.apparent_temperature)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
