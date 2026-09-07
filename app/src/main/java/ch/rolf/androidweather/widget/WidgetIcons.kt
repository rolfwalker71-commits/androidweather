package ch.rolf.androidweather.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size
import ch.rolf.androidweather.ui.components.weatherGlyphRes
import ch.rolf.androidweather.ui.components.weatherIconFamily

/** Deep well so colorful Meteocons stay readable on lavender/purple widget chrome. */
fun glanceIconWellColor(glyph: String): Color = when (weatherIconFamily(glyph)) {
    "sun" -> Color(0xFF3E3420)
    "night" -> Color(0xFF1A2744)
    "rain" -> Color(0xFF1A3A5C)
    "snow" -> Color(0xFF1E3A5F)
    "storm" -> Color(0xFF2A2238)
    "fog" -> Color(0xFF37474F)
    else -> Color(0xFF2A2E32)
}

@Composable
fun GlanceWeatherIcon(
    glyph: String,
    iconSize: Dp,
    wellSize: Dp = iconSize + 8.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = GlanceModifier
            .size(wellSize)
            .cornerRadius(wellSize / 2)
            .background(glanceIconWellColor(glyph)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(weatherGlyphRes(glyph)),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(iconSize)
        )
    }
}
