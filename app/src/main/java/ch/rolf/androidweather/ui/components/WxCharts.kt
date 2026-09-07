package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.domain.HourPoint
import ch.rolf.androidweather.domain.NowcastBar
import ch.rolf.androidweather.domain.WindBar
import ch.rolf.androidweather.domain.WindUnit
import ch.rolf.androidweather.domain.formatHour
import ch.rolf.androidweather.domain.formatMm
import ch.rolf.androidweather.domain.formatPercent
import ch.rolf.androidweather.domain.formatWindValue

@Composable
fun PrecipBars(bars: List<NowcastBar>, modifier: Modifier = Modifier) {
    val color = RainBarColor
    val track = MaterialTheme.colorScheme.surface
    Canvas(modifier.fillMaxWidth().height(96.dp)) {
        if (bars.isEmpty()) return@Canvas
        val max = maxOf(bars.maxOf { it.precipMm }, 0.2)
        val gap = 6.dp.toPx()
        val barW = (size.width - gap * (bars.size - 1)) / bars.size
        bars.forEachIndexed { i, bar ->
            val h = (bar.precipMm / max).toFloat().coerceAtLeast(0.1f) * size.height
            val x = i * (barW + gap)
            drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(12f, 12f))
            drawRoundRect(color, Offset(x, size.height - h), Size(barW, h), CornerRadius(12f, 12f))
        }
    }
}

@Composable
fun LabeledPrecipChart(hours: List<HourPoint>, timezone: String, modifier: Modifier = Modifier) {
    if (hours.isEmpty()) {
        Text("Keine Niederschlagsdaten.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val max = maxOf(hours.maxOf { it.precipMm }, 0.2)
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            hours.forEach { hour ->
                val frac = (hour.precipMm / max).toFloat().coerceAtLeast(0.1f)
                Column(
                    Modifier.width(44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(frac)
                                .clip(RoundedCornerShape(50))
                                .background(RainBarColor)
                        )
                    }
                    Text(
                        formatHour(hour.time, timezone),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        val first = hours.first()
        Text(
            buildString {
                append("Nächste Stunde ${formatMm(first.precipMm)}")
                first.precipProb?.let { append(" · Wahrscheinlichkeit ${formatPercent(it)}") }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun WindBarsChart(bars: List<WindBar>, modifier: Modifier = Modifier) {
    val color = WindBarColor
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
fun WindDirChart(bars: List<WindBar>, unit: WindUnit, timezone: String, modifier: Modifier = Modifier) {
    if (bars.isEmpty()) {
        Text("Keine Winddaten.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val max = maxOf(bars.maxOf { maxOf(it.speed, it.gusts ?: 0.0) }, 1.0)
    val hasGusts = bars.any { it.gusts != null }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                val speedFrac = (bar.speed / max).toFloat().coerceAtLeast(0.08f)
                Column(
                    Modifier.width(44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WindArrow(degrees = bar.direction.toFloat())
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        bar.gusts?.let { gusts ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight((gusts / max).toFloat().coerceIn(0.08f, 1f))
                                    .clip(RoundedCornerShape(50))
                                    .background(GustBarColor)
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(speedFrac)
                                .clip(RoundedCornerShape(50))
                                .background(WindBarColor)
                        )
                    }
                    Text(formatWindValue(bar.speed, unit), style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatHour(bar.time, timezone),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            "Zahl Wind${if (hasGusts) " · kleiner Böen" else ""} · ${if (unit == WindUnit.Ms) "m/s" else "km/h"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun WindArrow(degrees: Float, modifier: Modifier = Modifier) {
    val color = WindBarColor
    Canvas(modifier.size(12.dp, 16.dp).rotate(degrees)) {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width * 0.88f, size.height)
            lineTo(size.width / 2f, size.height * 0.72f)
            lineTo(size.width * 0.12f, size.height)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun AirTrendBars(values: List<Double?>, modifier: Modifier = Modifier, color: Color = GoodBarColor) {
    val track = MaterialTheme.colorScheme.surface
    Canvas(modifier.fillMaxWidth().height(48.dp)) {
        if (values.isEmpty()) return@Canvas
        val nums = values.map { it ?: 0.0 }
        val max = maxOf(nums.max(), 8.0)
        val gap = 4.dp.toPx()
        val barW = (size.width - gap * (nums.size - 1)) / nums.size
        nums.forEachIndexed { i, value ->
            val h = (value / max).toFloat().coerceAtLeast(0.1f) * size.height
            val x = i * (barW + gap)
            drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(16f, 16f))
            drawRoundRect(color, Offset(x, size.height - h), Size(barW, h), CornerRadius(16f, 16f))
        }
    }
}
