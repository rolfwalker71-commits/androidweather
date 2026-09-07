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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ch.rolf.androidweather.MainActivity

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
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primaryContainer)
            .padding(20.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            Text(
                text = snapshot.placeName,
                maxLines = 2,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = snapshot.temperature,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = snapshot.condition,
                maxLines = 2,
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 14.sp)
            )
            snapshot.rainLine?.let { line ->
                Text(
                    text = line,
                    maxLines = 2,
                    style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 13.sp)
                )
            }
        }
        Spacer(GlanceModifier.width(12.dp))
        Text(text = glyphEmoji(snapshot.glyph), style = TextStyle(fontSize = 48.sp))
    }
}

class WideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WideWeatherWidget()
}
