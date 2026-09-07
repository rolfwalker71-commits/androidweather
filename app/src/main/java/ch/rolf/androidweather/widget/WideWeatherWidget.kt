package ch.rolf.androidweather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
    WidgetHeroFrame(mood = snapshot.mood, paddingBottom = 18.dp) {
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snapshot.temperature,
                        style = TextStyle(
                            color = on,
                            fontSize = 41.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    GlanceWeatherIcon(glyph = snapshot.glyph, iconSize = 40.dp, wellSize = 52.dp)
                }
                Text(
                    text = snapshot.condition,
                    maxLines = 1,
                    style = TextStyle(color = on, fontSize = 13.sp)
                )
                snapshot.highLow?.let { range ->
                    Text(
                        text = "Heute $range",
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = 12.sp)
                    )
                }
                snapshot.rainLine?.let { line ->
                    Text(
                        text = line,
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = 12.sp)
                    )
                }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        if (hours.isNotEmpty()) {
            Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 2.dp)) {
                hours.forEach { hour ->
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = hour.time,
                            maxLines = 1,
                            style = TextStyle(color = on, fontSize = 12.sp)
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        GlanceWeatherIcon(glyph = hour.glyph, iconSize = 26.dp, wellSize = 34.dp)
                        Text(
                            text = hour.temperature,
                            style = TextStyle(
                                color = on,
                                fontSize = 17.sp,
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
