package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.domain.NowcastBar
import ch.rolf.androidweather.domain.WindBar

@Composable
fun PrecipBars(bars: List<NowcastBar>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surface
    Canvas(modifier.fillMaxWidth().height(96.dp)) {
        if (bars.isEmpty()) return@Canvas
        val max = maxOf(bars.maxOf { it.precipMm }, 1.0)
        val gap = 6.dp.toPx()
        val barW = (size.width - gap * (bars.size - 1)) / bars.size
        bars.forEachIndexed { i, bar ->
            val h = (bar.precipMm / max).toFloat() * size.height
            val x = i * (barW + gap)
            drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(12f, 12f))
            drawRoundRect(color, Offset(x, size.height - h), Size(barW, h), CornerRadius(12f, 12f))
        }
    }
}

@Composable
fun WindBarsChart(bars: List<WindBar>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surface
    Canvas(modifier.fillMaxWidth().height(96.dp)) {
        if (bars.isEmpty()) return@Canvas
        val max = maxOf(bars.maxOf { it.speed }, 10.0)
        val gap = 6.dp.toPx()
        val barW = (size.width - gap * (bars.size - 1)) / bars.size
        bars.forEachIndexed { i, bar ->
            val h = (bar.speed / max).toFloat() * size.height
            val x = i * (barW + gap)
            drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(12f, 12f))
            drawRoundRect(color, Offset(x, size.height - h), Size(barW, h), CornerRadius(12f, 12f))
        }
    }
}

@Composable
fun AirTrendBars(values: List<Double?>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val track = MaterialTheme.colorScheme.surface
    Canvas(modifier.fillMaxWidth().height(88.dp)) {
        if (values.isEmpty()) return@Canvas
        val nums = values.map { it ?: 0.0 }
        val max = maxOf(nums.max(), 20.0)
        val gap = 6.dp.toPx()
        val barW = (size.width - gap * (nums.size - 1)) / nums.size
        nums.forEachIndexed { i, value ->
            val h = (value / max).toFloat() * size.height
            val x = i * (barW + gap)
            drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(12f, 12f))
            drawRoundRect(color, Offset(x, size.height - h), Size(barW, h), CornerRadius(12f, 12f))
        }
    }
}
