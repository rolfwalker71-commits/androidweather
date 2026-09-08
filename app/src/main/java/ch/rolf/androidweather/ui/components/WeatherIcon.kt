package ch.rolf.androidweather.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.R
import ch.rolf.androidweather.domain.getWmo

/** Weather Icons (Erik Flowers, SIL OFL 1.1) — monochrome, tinted to the current text color. */
@DrawableRes
fun weatherGlyphRes(glyph: String): Int = when (glyph) {
    "sunny" -> R.drawable.wi_day_sunny
    "night", "nightlight" -> R.drawable.wi_night_clear
    "partly_cloudy", "partly_cloudy_day" -> R.drawable.wi_day_cloudy
    "partly_cloudy_night" -> R.drawable.wi_night_alt_cloudy
    "cloud" -> R.drawable.wi_cloudy
    "foggy" -> R.drawable.wi_fog
    "drizzle", "rainy_light" -> R.drawable.wi_sprinkle
    "rainy" -> R.drawable.wi_rain
    "rainy_heavy" -> R.drawable.wi_rain_wind
    "showers" -> R.drawable.wi_showers
    "showers_night" -> R.drawable.wi_night_alt_showers
    "snowy", "weather_snowy", "snowflake" -> R.drawable.wi_snow
    "sleet" -> R.drawable.wi_sleet
    "thunderstorm" -> R.drawable.wi_thunderstorm
    "hail", "weather_hail" -> R.drawable.wi_hail
    else -> R.drawable.wi_cloudy
}

fun weatherIconFamily(glyph: String): String = when (glyph) {
    "sunny", "partly_cloudy", "partly_cloudy_day" -> "sun"
    "night", "nightlight", "partly_cloudy_night" -> "night"
    "drizzle", "rainy_light", "rainy", "rainy_heavy", "showers", "showers_night" -> "rain"
    "snowy", "weather_snowy", "snowflake", "sleet" -> "snow"
    "thunderstorm", "hail", "weather_hail" -> "storm"
    "foggy" -> "fog"
    else -> "cloud"
}

@Composable
fun WeatherIcon(
    code: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = null,
    @Suppress("UNUSED_PARAMETER") well: Boolean = false
) {
    val wmo = getWmo(code, isDay)
    WeatherGlyph(
        glyph = wmo.glyph,
        modifier = modifier,
        size = size,
        contentDescription = contentDescription ?: wmo.label,
        well = well
    )
}

@Composable
fun WeatherGlyph(
    glyph: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = null,
    @Suppress("UNUSED_PARAMETER") well: Boolean = false
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(weatherGlyphRes(glyph)),
            contentDescription = contentDescription,
            modifier = Modifier.size(size),
            colorFilter = ColorFilter.tint(LocalContentColor.current)
        )
    }
}
