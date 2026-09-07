package ch.rolf.androidweather.widget

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import ch.rolf.androidweather.MainActivity
import ch.rolf.androidweather.ui.components.heroMoodGradientArgb
import ch.rolf.androidweather.ui.components.heroOnColor

@Composable
fun widgetOnColor(mood: String): ColorProvider {
    val context = LocalContext.current
    val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
    return ColorProvider(heroOnColor(mood, dark))
}

private fun moodGradientBitmap(mood: String, dark: Boolean): Bitmap {
    val width = 720
    val height = 448
    val colors = heroMoodGradientArgb(mood, dark)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            colors,
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    return bitmap
}

@Composable
fun WidgetHeroFrame(
    mood: String,
    padding: Dp = 16.dp,
    paddingBottom: Dp = padding,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
    val background = remember(mood, dark) { ImageProvider(moodGradientBitmap(mood, dark)) }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .clickable(actionStartActivity<MainActivity>())
            .padding(start = padding, top = padding, end = padding, bottom = paddingBottom)
    ) {
        content()
    }
}
