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
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * 5×2 must stay on a two-row height (~110dp). Extra width holds the day details;
 * stacking them above the hours is what clipped the bottom before.
 */
class ExtraWideWeatherWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

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
    val hours = snapshot.hours.take(5)
    val on = widgetOnColor(snapshot.mood)
    WidgetHeroFrame(mood = snapshot.mood, padding = 8.dp, paddingBottom = 8.dp) {
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snapshot.temperature,
                        style = TextStyle(
                            color = on,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    GlanceWeatherIcon(
                        glyph = snapshot.glyph,
                        iconSize = 24.dp,
                        wellSize = 30.dp
                    )
                }
                Text(
                    text = snapshot.condition,
                    maxLines = 1,
                    style = TextStyle(color = on, fontSize = 11.sp)
                )
            }
            Column(
                modifier = GlanceModifier.width(152.dp).padding(start = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                snapshot.highLow?.let { range ->
                    Text(
                        text = "Heute $range",
                        maxLines = 1,
                        style = TextStyle(
                            color = on,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                snapshot.feelsLike?.let { feels ->
                    Text(
                        text = feels,
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = 11.sp)
                    )
                }
                snapshot.wind?.let { wind ->
                    Text(
                        text = wind,
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = 11.sp)
                    )
                }
                WidgetSunRow(snapshot, on)
            }
        }
        ExtraWideHoursRow(hours, on)
    }
}

@Composable
private fun ExtraWideHoursRow(hours: List<WidgetHour>, on: ColorProvider) {
    if (hours.isEmpty()) return
    Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
        hours.forEach { hour ->
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = hour.time,
                    maxLines = 1,
                    style = TextStyle(color = on, fontSize = 10.sp)
                )
                GlanceWeatherIcon(glyph = hour.glyph, iconSize = 16.dp, wellSize = 18.dp)
                Text(
                    text = hour.temperature,
                    style = TextStyle(
                        color = on,
                        fontSize = 13.sp,
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
