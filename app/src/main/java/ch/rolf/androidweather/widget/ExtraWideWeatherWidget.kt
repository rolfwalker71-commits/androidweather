package ch.rolf.androidweather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ch.rolf.androidweather.R

/**
 * Extra-wide widget. Glance does not shrink a tall layout to a 2-row cell —
 * overflow is clipped. 5×2 is ~110dp; the rich day-column + hours need ~220dp.
 * [SizeMode.Exact] picks a compact 5×2 or the full 5×3 layout from real height.
 */
class ExtraWideWeatherWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val snapshot = loadWidgetSnapshot(context, appWidgetId)
        provideContent {
            GlanceTheme { ExtraWideWidgetLayout(snapshot) }
        }
    }
}

@Composable
fun ExtraWideWidgetLayout(snapshot: WidgetSnapshot) {
    val tall = LocalSize.current.height >= 190.dp
    if (tall) ExtraWideTallLayout(snapshot) else ExtraWideShortLayout(snapshot)
}

@Composable
private fun ExtraWideShortLayout(snapshot: WidgetSnapshot) {
    val hours = snapshot.hours.take(5)
    val on = widgetOnColor(snapshot.mood)
    WidgetHeroFrame(mood = snapshot.mood, padding = 8.dp, paddingBottom = 10.dp) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = snapshot.placeName,
                    maxLines = 1,
                    style = TextStyle(
                        color = on,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snapshot.temperature,
                        style = TextStyle(
                            color = on,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    GlanceWeatherIcon(
                        glyph = snapshot.glyph,
                        iconSize = 26.dp,
                        wellSize = 32.dp
                    )
                }
            }
            Column(
                modifier = GlanceModifier.width(148.dp).padding(start = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                snapshot.highLow?.let { range ->
                    Text(
                        text = range,
                        maxLines = 1,
                        style = TextStyle(
                            color = on,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                ExtraWideSunRow(snapshot, on)
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        ExtraWideHoursRow(hours, on, compact = true)
    }
}

@Composable
private fun ExtraWideTallLayout(snapshot: WidgetSnapshot) {
    val hours = snapshot.hours.take(5)
    val on = widgetOnColor(snapshot.mood)
    WidgetHeroFrame(mood = snapshot.mood, padding = 12.dp) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = snapshot.placeName,
                    maxLines = 1,
                    style = TextStyle(
                        color = on,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snapshot.temperature,
                        style = TextStyle(
                            color = on,
                            fontSize = 39.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    GlanceWeatherIcon(
                        glyph = snapshot.glyph,
                        iconSize = 38.dp,
                        wellSize = 50.dp
                    )
                }
                Text(
                    text = snapshot.condition,
                    maxLines = 1,
                    style = TextStyle(color = on, fontSize = 12.sp)
                )
            }
            Column(
                modifier = GlanceModifier.width(156.dp).padding(start = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                snapshot.highLow?.let { range ->
                    Text(
                        text = "Heute",
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = 12.sp)
                    )
                    Text(
                        text = range,
                        maxLines = 1,
                        style = TextStyle(
                            color = on,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                snapshot.feelsLike?.let { feels ->
                    Text(
                        text = feels,
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = 13.sp)
                    )
                }
                snapshot.wind?.let { wind ->
                    Text(
                        text = wind,
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = 13.sp)
                    )
                }
                ExtraWideSunRow(snapshot, on)
                snapshot.rainLine?.let { line ->
                    Text(
                        text = line,
                        maxLines = 2,
                        style = TextStyle(color = on, fontSize = 12.sp)
                    )
                }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        ExtraWideHoursRow(hours, on, compact = false)
    }
}

@Composable
private fun ExtraWideSunRow(snapshot: WidgetSnapshot, on: ColorProvider) {
    if (snapshot.sunrise == null || snapshot.sunset == null) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_sunrise),
            contentDescription = "Sonnenaufgang",
            modifier = GlanceModifier.size(13.dp),
            colorFilter = ColorFilter.tint(on)
        )
        Text(
            text = " ${snapshot.sunrise} – ",
            maxLines = 1,
            style = TextStyle(color = on, fontSize = 12.sp)
        )
        Image(
            provider = ImageProvider(R.drawable.ic_widget_sunset),
            contentDescription = "Sonnenuntergang",
            modifier = GlanceModifier.size(13.dp),
            colorFilter = ColorFilter.tint(on)
        )
        Text(
            text = " ${snapshot.sunset}",
            maxLines = 1,
            style = TextStyle(color = on, fontSize = 12.sp)
        )
    }
}

@Composable
private fun ExtraWideHoursRow(
    hours: List<WidgetHour>,
    on: ColorProvider,
    compact: Boolean
) {
    if (hours.isEmpty()) return
    val timeSize = if (compact) 11.sp else 12.sp
    val tempSize = if (compact) 14.sp else 17.sp
    val icon = if (compact) 18.dp else 26.dp
    val well = if (compact) 22.dp else 34.dp
    val gap = if (compact) 1.dp else 4.dp
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        hours.forEach { hour ->
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = hour.time,
                    maxLines = 1,
                    style = TextStyle(color = on, fontSize = timeSize)
                )
                Spacer(GlanceModifier.height(gap))
                GlanceWeatherIcon(glyph = hour.glyph, iconSize = icon, wellSize = well)
                Text(
                    text = hour.temperature,
                    style = TextStyle(
                        color = on,
                        fontSize = tempSize,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class ExtraWideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExtraWideWeatherWidget()
}
