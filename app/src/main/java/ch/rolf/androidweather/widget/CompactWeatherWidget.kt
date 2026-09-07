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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ch.rolf.androidweather.MainActivity

/** Compact 2x2 widget. Layout lives here so later visual tweaks stay local. */
class CompactWeatherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val snapshot = loadWidgetSnapshot(context, appWidgetId)
        provideContent {
            GlanceTheme { CompactWidgetLayout(snapshot) }
        }
    }
}

@Composable
fun CompactWidgetLayout(snapshot: WidgetSnapshot) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primaryContainer)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = snapshot.placeName,
            maxLines = 2,
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
                    fontSize = 37.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.width(8.dp))
            GlanceWeatherIcon(glyph = snapshot.glyph, iconSize = 28.dp, wellSize = 36.dp)
        }
        Text(
            text = snapshot.condition,
            maxLines = 2,
            style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 12.sp)
        )
        snapshot.rainLine?.let { line ->
            Text(
                text = line,
                maxLines = 2,
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 11.sp)
            )
        }
    }
}

class CompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWeatherWidget()
}
