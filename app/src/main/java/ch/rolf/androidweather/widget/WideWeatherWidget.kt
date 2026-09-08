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
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/** Wide 4×2 widget. Fills the real cell height so hours sit on the lower half. */
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
    WideFamilyLayout(snapshot, hourCount = 5)
}

@Composable
fun WideFamilyLayout(snapshot: WidgetSnapshot, hourCount: Int) {
    val hours = snapshot.hours.take(hourCount)
    val on = widgetOnColor(snapshot.mood)
    val roomy = LocalSize.current.height >= 150.dp
    val pad = if (roomy) 12.dp else 8.dp
    WidgetHeroFrame(mood = snapshot.mood, padding = pad, paddingBottom = 12.dp) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snapshot.placeName,
                        maxLines = 1,
                        style = TextStyle(
                            color = on,
                            fontSize = if (roomy) 15.sp else 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    snapshot.updatedAt?.let { stamp ->
                        Text(
                            text = " $stamp",
                            maxLines = 1,
                            style = TextStyle(color = on, fontSize = 9.sp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snapshot.temperature,
                        style = TextStyle(
                            color = on,
                            fontSize = if (roomy) 50.sp else 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    GlanceWeatherIcon(
                        glyph = snapshot.glyph,
                        iconSize = if (roomy) 48.dp else 38.dp,
                        tint = on
                    )
                }
                Text(
                    text = snapshot.condition,
                    maxLines = 1,
                    style = TextStyle(color = on, fontSize = if (roomy) 13.sp else 12.sp),
                    modifier = GlanceModifier.padding(top = (-4).dp)
                )
                snapshot.rainLine?.let { line ->
                    Text(
                        text = line,
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = 12.sp)
                    )
                }
                snapshot.stationLine?.let { line ->
                    Text(
                        text = line,
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = 11.sp)
                    )
                }
            }
            Column(
                modifier = GlanceModifier.width(152.dp).padding(start = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                snapshot.highLow?.let { range ->
                    Text(
                        text = "Heute $range",
                        maxLines = 1,
                        style = TextStyle(
                            color = on,
                            fontSize = if (roomy) 14.sp else 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                snapshot.feelsLike?.let { feels ->
                    Text(
                        text = feels,
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = if (roomy) 13.sp else 12.sp)
                    )
                }
                snapshot.wind?.let { wind ->
                    Text(
                        text = wind,
                        maxLines = 1,
                        style = TextStyle(color = on, fontSize = if (roomy) 13.sp else 12.sp)
                    )
                }
                WidgetSunRow(snapshot, on)
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        if (hours.isNotEmpty()) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
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
                        GlanceWeatherIcon(
                            glyph = hour.glyph,
                            iconSize = if (roomy) 22.dp else 20.dp,
                            tint = on
                        )
                        Text(
                            text = hour.temperature,
                            style = TextStyle(
                                color = on,
                                fontSize = if (roomy) 15.sp else 13.sp,
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
