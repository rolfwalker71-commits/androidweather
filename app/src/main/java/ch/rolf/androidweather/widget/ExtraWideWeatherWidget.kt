package ch.rolf.androidweather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ch.rolf.androidweather.MainActivity

/** Extra-wide 5x2 widget. Dedicated file so size/layout tweaks stay cheap. */
class ExtraWideWeatherWidget : GlanceAppWidget() {
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
    val metrics = listOfNotNull(snapshot.wind, snapshot.pressure, snapshot.humidity).joinToString(" · ")
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primaryContainer)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = snapshot.placeName,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snapshot.temperature,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(text = "  ${glyphEmoji(snapshot.glyph)}", style = TextStyle(fontSize = 24.sp))
                }
                Text(
                    text = snapshot.condition,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 10.sp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                snapshot.highLow?.let { range ->
                    Text(
                        text = range,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                snapshot.rainLine?.let { line ->
                    Text(
                        text = line,
                        maxLines = 2,
                        style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 10.sp)
                    )
                }
            }
        }
        if (metrics.isNotEmpty()) {
            Text(
                text = metrics,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 10.sp),
                modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
        if (hours.isNotEmpty()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                hours.forEach { hour ->
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = hour.time,
                            maxLines = 1,
                            style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 10.sp)
                        )
                        Text(text = glyphEmoji(hour.glyph), style = TextStyle(fontSize = 14.sp))
                        Text(
                            text = hour.temperature,
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
        snapshot.updatedAt?.let { stamp ->
            Text(
                text = stamp,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 10.sp),
                modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}

class ExtraWideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExtraWideWeatherWidget()
}
