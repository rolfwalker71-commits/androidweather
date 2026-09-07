package ch.rolf.androidweather.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.R
import ch.rolf.androidweather.domain.getWmo

@DrawableRes
fun weatherGlyphRes(glyph: String): Int = when (glyph) {
    "sunny" -> R.drawable.wx_sunny
    "night", "nightlight" -> R.drawable.wx_nightlight
    "partly_cloudy", "partly_cloudy_day" -> R.drawable.wx_partly_cloudy_day
    "partly_cloudy_night" -> R.drawable.wx_partly_cloudy_night
    "cloud" -> R.drawable.wx_cloud
    "foggy" -> R.drawable.wx_foggy
    "rainy_light" -> R.drawable.wx_rainy_light
    "rainy" -> R.drawable.wx_rainy
    "rainy_heavy" -> R.drawable.wx_rainy_heavy
    "snowy", "weather_snowy" -> R.drawable.wx_weather_snowy
    "snowflake" -> R.drawable.wx_snowflake
    "thunderstorm" -> R.drawable.wx_thunderstorm
    "hail", "weather_hail" -> R.drawable.wx_weather_hail
    else -> R.drawable.wx_cloud
}

@Composable
fun WeatherIcon(
    code: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = null
) {
    val wmo = getWmo(code, isDay)
    Icon(
        painter = painterResource(weatherGlyphRes(wmo.glyph)),
        contentDescription = contentDescription ?: wmo.label,
        modifier = modifier.size(size),
        tint = LocalContentColor.current
    )
}
