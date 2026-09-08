package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.domain.HourPoint
import ch.rolf.androidweather.domain.formatHourLabel
import ch.rolf.androidweather.domain.formatPercent
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.parseForecastEpochMilli
import ch.rolf.androidweather.domain.weatherMood

data class MoodColors(val container: Color, val content: Color)

fun moodColors(mood: String, dark: Boolean): MoodColors = when (mood) {
    "clear" -> if (dark) MoodColors(Color(0xFF5D4E1E), Color(0xFFFFE082))
    else MoodColors(Color(0xFFFFE082), Color(0xFF4E342E))
    "night" -> if (dark) MoodColors(Color(0xFF2D2150), Color(0xFFD1C4E9))
    else MoodColors(Color(0xFF283593), Color(0xFFE8EAF6))
    "rain" -> if (dark) MoodColors(Color(0xFF1A3A5C), Color(0xFFBBDEFB))
    else MoodColors(Color(0xFFBBDEFB), Color(0xFF0D47A1))
    "snow" -> if (dark) MoodColors(Color(0xFF1E3A5F), Color(0xFFE3F2FD))
    else MoodColors(Color(0xFFE3F2FD), Color(0xFF0D47A1))
    "storm" -> if (dark) MoodColors(Color(0xFF3D1F4D), Color(0xFFE1BEE7))
    else MoodColors(Color(0xFFE1BEE7), Color(0xFF4A148C))
    "fog" -> if (dark) MoodColors(Color(0xFF2A3034), Color(0xFFCFD8DC))
    else MoodColors(Color(0xFFECEFF1), Color(0xFF37474F))
    else -> if (dark) MoodColors(Color(0xFF2C353A), Color(0xFFECEFF1))
    else MoodColors(Color(0xFFCFD8DC), Color(0xFF263238))
}

fun scaleColors(tone: String, dark: Boolean): MoodColors = when (tone) {
    "good" -> if (dark) MoodColors(Color(0xFF1B4332), Color(0xFFB7EFC5))
    else MoodColors(Color(0xFFC8E6C9), Color(0xFF1B5E20))
    "fair" -> if (dark) MoodColors(Color(0xFF4D3D12), Color(0xFFFFE082))
    else MoodColors(Color(0xFFFFF9C4), Color(0xFFF57F17))
    "warn" -> if (dark) MoodColors(Color(0xFF4E2E12), Color(0xFFFFCC80))
    else MoodColors(Color(0xFFFFE0B2), Color(0xFFE65100))
    "bad" -> if (dark) MoodColors(Color(0xFF4A1C1C), Color(0xFFFFCDD2))
    else MoodColors(Color(0xFFFFCDD2), Color(0xFFB71C1C))
    "extreme" -> if (dark) MoodColors(Color(0xFF4A1530), Color(0xFFF8BBD0))
    else MoodColors(Color(0xFFF8BBD0), Color(0xFF880E4F))
    else -> MoodColors(Color.Transparent, Color.Unspecified)
}

val RainBarColor = Color(0xFF1A73E8)
val WindBarColor = Color(0xFF00897B)
val GustBarColor = Color(0xFF80CBC4)
val GoodBarColor = Color(0xFF43A047)
private val TempBarBrush = Brush.horizontalGradient(
    listOf(Color(0xFF42A5F5), Color(0xFFFFCA28), Color(0xFFEF6C00))
)

@Composable
fun PrecipPill(precipMm: Double, maxPrecip: Double, modifier: Modifier = Modifier) {
    val ratio = if (maxPrecip <= 0) 0f else (precipMm / maxPrecip).toFloat()
    Box(
        modifier
            .width(6.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(ratio.coerceIn(0.08f, 1f))
                .clip(RoundedCornerShape(50))
                .background(RainBarColor)
        )
    }
}

@Composable
fun TempRangeBar(tMin: Double, tMax: Double, weekMin: Double, weekMax: Double, modifier: Modifier = Modifier) {
    val span = (weekMax - weekMin).coerceAtLeast(1.0)
    val start = ((tMin - weekMin) / span).toFloat().coerceIn(0f, 0.92f)
    val widthFrac = ((tMax - tMin) / span).toFloat().coerceAtLeast(0.08f)
    val end = (1f - start - widthFrac).coerceAtLeast(0.001f)
    Row(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (start > 0f) Box(Modifier.weight(start).fillMaxHeight())
        Box(
            Modifier
                .weight(widthFrac)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(TempBarBrush)
        )
        if (end > 0f) Box(Modifier.weight(end).fillMaxHeight())
    }
}

@Composable
fun HourlyForecastStrip(
    hours: List<HourPoint>,
    timezone: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    labelFirstAsNow: Boolean = true,
    nowEpochMilli: Long? = null,
    onSelect: (HourPoint) -> Unit
) {
    val visible = hours
    val maxPrecip = visible.maxOfOrNull { it.precipMm }?.coerceAtLeast(1.0) ?: 1.0
    val dark = isSystemInDarkTheme()
    if (visible.isEmpty()) {
        Text("Keine Stundendaten verfügbar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    if (compact) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            visible.forEachIndexed { index, hour ->
                HourChip(
                    hour = hour,
                    timezone = timezone,
                    index = index,
                    maxPrecip = maxPrecip,
                    dark = dark,
                    labelFirstAsNow = labelFirstAsNow,
                    nowEpochMilli = nowEpochMilli,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
            }
        }
    } else {
        Row(
            modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            visible.forEachIndexed { index, hour ->
                HourChip(
                    hour = hour,
                    timezone = timezone,
                    index = index,
                    maxPrecip = maxPrecip,
                    dark = dark,
                    labelFirstAsNow = labelFirstAsNow,
                    nowEpochMilli = nowEpochMilli,
                    modifier = Modifier.width(72.dp),
                    onSelect = onSelect
                )
            }
        }
    }
}

@Composable
private fun HourChip(
    hour: HourPoint,
    timezone: String,
    index: Int,
    maxPrecip: Double,
    dark: Boolean,
    labelFirstAsNow: Boolean,
    nowEpochMilli: Long?,
    modifier: Modifier,
    onSelect: (HourPoint) -> Unit
) {
    val mood = moodColors(weatherMood(hour.code, hour.isDay), dark)
    val label = if (labelFirstAsNow && index == 0) "Jetzt" else formatHourLabel(hour.time, timezone)
    val past = nowEpochMilli != null &&
        parseForecastEpochMilli(hour.time, timezone) + 60L * 60L * 1000L <= nowEpochMilli
    Column(
        modifier
            .alpha(if (past) 0.55f else 1f)
            .clip(RoundedCornerShape(24.dp))
            .background(mood.container)
            .then(
                if (labelFirstAsNow && index == 0) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                } else Modifier
            )
            .clickable(role = Role.Button) { onSelect(hour) }
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = mood.content)
        CompositionLocalProvider(LocalContentColor provides mood.content) {
            WeatherIcon(code = hour.code, isDay = hour.isDay, size = 28.dp)
        }
        Text(formatTemp(hour.temperature), style = MaterialTheme.typography.titleMedium, color = mood.content)
        PrecipPill(hour.precipMm, maxPrecip)
        hour.precipProb?.let {
            Text(formatPercent(it), style = MaterialTheme.typography.labelMedium, color = mood.content.copy(alpha = 0.8f))
        }
    }
}
