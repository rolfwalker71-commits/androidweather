package ch.rolf.androidweather.widget

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import ch.rolf.androidweather.ui.components.weatherGlyphRes

/** Same Weather Icons set as the app, tinted to the widget on-color. */
@DrawableRes
fun widgetWeatherGlyphRes(glyph: String): Int = weatherGlyphRes(glyph)

@Composable
fun GlanceWeatherIcon(
    glyph: String,
    iconSize: Dp,
    tint: ColorProvider,
    contentDescription: String? = null
) {
    Image(
        provider = ImageProvider(widgetWeatherGlyphRes(glyph)),
        contentDescription = contentDescription,
        modifier = GlanceModifier.size(iconSize),
        colorFilter = ColorFilter.tint(tint)
    )
}
