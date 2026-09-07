package ch.rolf.androidweather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/** Wide 4x2 widget. Dedicated file so size/layout tweaks stay cheap. */
class WideWeatherWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val snapshot = loadWidgetSnapshot(context, appWidgetId)
        provideContent {
            GlanceTheme { WideWidgetLayout(snapshot) }
        }
    }
}

@Composable
fun WideWidgetLayout(snapshot: WidgetSnapshot) {
    val hours = snapshot.hours.take(4)
    val on = widgetOnColor(snapshot.mood)
    val compact = LocalSize.current.height < 190.dp
    val pad = if (compact) 8.dp else 16.dp
    WidgetHeroFrame(mood = snapshot.mood, padding = pad, paddingBottom = if (compact) 10.dp else 12.dp) {
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
                        fontSize = if (compact) 13.sp else 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snapshot.temperature,
                        style = TextStyle(
                            color = on,
                            fontSize = if (compact) 30.sp else 41.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.width(if (compact) 4.dp else 8.dp))
                    GlanceWeatherIcon(
                        glyph = snapshot.glyph,
                        iconSize = if (compact) 26.dp else 40.dp,
                        wellSize = if (compact) 32.dp else 52.dp
                    )
                }
                if (!compact) {
                    Text(
                        text = snapshot.condition,
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = 13.sp)
                    )
                }
                snapshot.highLow?.let { range ->
                    Text(
                        text = if (compact) range else "Heute $range",
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = 12.sp)
                    )
                }
                if (!compact) {
                    snapshot.rainLine?.let { line ->
                        Text(
                            text = line,
                            maxLines = 1,
                            style = TextStyle(color = on, fontSize = 12.sp)
                        )
                    }
                }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        if (hours.isNotEmpty()) {
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
    }
}

class WideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WideWeatherWidget()
}
