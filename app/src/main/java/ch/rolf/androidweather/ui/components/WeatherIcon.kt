package ch.rolf.androidweather.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.R
import ch.rolf.androidweather.domain.getWmo

/** Meteocons Fill (Bas Milius, MIT) — full color, never a single tint. */
@DrawableRes
fun weatherGlyphRes(glyph: String): Int = when (glyph) {
    "sunny" -> R.drawable.wx_clear_day
    "night", "nightlight" -> R.drawable.wx_clear_night
    "partly_cloudy", "partly_cloudy_day" -> R.drawable.wx_partly_cloudy_day
    "partly_cloudy_night" -> R.drawable.wx_partly_cloudy_night
    "cloud" -> R.drawable.wx_overcast
    "foggy" -> R.drawable.wx_fog
    "drizzle", "rainy_light" -> R.drawable.wx_drizzle
    "rainy" -> R.drawable.wx_rain
    "rainy_heavy" -> R.drawable.wx_extreme_rain
    "showers" -> R.drawable.wx_showers
    "showers_night" -> R.drawable.wx_showers_night
    "snowy", "weather_snowy", "snowflake" -> R.drawable.wx_snow
    "sleet" -> R.drawable.wx_sleet
    "thunderstorm" -> R.drawable.wx_thunderstorms
    "hail", "weather_hail" -> R.drawable.wx_hail
    else -> R.drawable.wx_overcast
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

/**
 * Deep tonal wells for light/yellow/white Meteocons; lighter slate for the rare darker glyph.
 * Never mint or lavender — those wash out sun and clouds.
 */
fun weatherIconWellColor(glyph: String, dark: Boolean): Color = when (weatherIconFamily(glyph)) {
    "sun" -> if (dark) Color(0xFF2A2414) else Color(0xFF3E3420)
    "night" -> if (dark) Color(0xFF161428) else Color(0xFF1A2744)
    "rain" -> if (dark) Color(0xFF142433) else Color(0xFF1A3A5C)
    "snow" -> if (dark) Color(0xFF162433) else Color(0xFF1E3A5F)
    "storm" -> if (dark) Color(0xFF221A2C) else Color(0xFF2A2238)
    "fog" -> if (dark) Color(0xFF2A3034) else Color(0xFF37474F)
    else -> if (dark) Color(0xFF24282C) else Color(0xFF37474F)
}

fun weatherIconWellArgb(glyph: String, dark: Boolean): Int {
    val c = weatherIconWellColor(glyph, dark)
    val a = (c.alpha * 255f).toInt().coerceIn(0, 255)
    val r = (c.red * 255f).toInt().coerceIn(0, 255)
    val g = (c.green * 255f).toInt().coerceIn(0, 255)
    val b = (c.blue * 255f).toInt().coerceIn(0, 255)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

@Composable
fun WeatherIcon(
    code: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = null,
    well: Boolean = true
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
    well: Boolean = true
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val image = @Composable {
        Image(
            painter = painterResource(weatherGlyphRes(glyph)),
            contentDescription = contentDescription,
            modifier = Modifier.size(if (well) size * 0.78f else size)
        )
    }
    if (!well) {
        Box(modifier.size(size), contentAlignment = Alignment.Center) { image() }
        return
    }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(weatherIconWellColor(glyph, dark)),
        contentAlignment = Alignment.Center
    ) { image() }
}
