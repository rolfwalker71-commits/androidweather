package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class TopicMood { Draussen, Wind, Berge, Seen }

fun topicHeroBrush(mood: TopicMood, dark: Boolean): Brush = when (mood) {
    TopicMood.Draussen -> if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF4A3420), Color(0xFF3A2C22), Color(0xFF2A2420)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFCC80), Color(0xFFFFB74D)))
    }
    TopicMood.Wind -> if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF0E3A3E), Color(0xFF164248), Color(0xFF1E2C2E)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF4DD0E1), Color(0xFF26C6DA), Color(0xFF80CBC4)))
    }
    TopicMood.Berge -> if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF3E2A22), Color(0xFF3A2828), Color(0xFF2A2220)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFCCBC), Color(0xFFFFAB91), Color(0xFFE8A598)))
    }
    TopicMood.Seen -> if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF0A3D3A), Color(0xFF0E4A44), Color(0xFF1A2E2C)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF80DEEA), Color(0xFF4DB6AC), Color(0xFF26A69A)))
    }
}

fun topicOnColor(mood: TopicMood, dark: Boolean): Color = when {
    dark -> Color(0xFFF7F3EE)
    mood == TopicMood.Draussen -> Color(0xFF3E2723)
    mood == TopicMood.Wind -> Color(0xFF00363D)
    mood == TopicMood.Berge -> Color(0xFF3E2723)
    else -> Color(0xFF003731)
}

data class TopicChipColors(val container: Color, val content: Color, val muted: Color)

@Composable
fun topicChipColors(mood: TopicMood): TopicChipColors {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val container = when (mood) {
        TopicMood.Draussen -> if (dark) Color(0xFF4A3A24) else Color(0xFFFFF3C4)
        TopicMood.Wind -> if (dark) Color(0xFF1A3C40) else Color(0xFFB2EBF2)
        TopicMood.Berge -> if (dark) Color(0xFF4A322C) else Color(0xFFFFE0D4)
        TopicMood.Seen -> if (dark) Color(0xFF144440) else Color(0xFFB2DFDB)
    }
    val content = when {
        dark -> Color(0xFFF4EEE6)
        mood == TopicMood.Draussen -> Color(0xFF4E342E)
        mood == TopicMood.Wind -> Color(0xFF004D54)
        mood == TopicMood.Berge -> Color(0xFF4E342E)
        else -> Color(0xFF004D40)
    }
    return TopicChipColors(container, content, content.copy(alpha = 0.72f))
}

@Composable
fun TopicHero(
    mood: TopicMood,
    title: String,
    icon: ImageVector,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val onHero = topicOnColor(mood, dark)
    val muted = onHero.copy(alpha = 0.78f)
    CompositionLocalProvider(LocalContentColor provides onHero) {
        Column(
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(topicHeroBrush(mood, dark))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(onHero.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = onHero, modifier = Modifier.size(26.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = muted,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, color = onHero)
            if (detail.isNotBlank()) {
                Text(detail, style = MaterialTheme.typography.bodyLarge, color = muted)
            }
            extra()
        }
    }
}

@Composable
fun TopicInset(
    mood: TopicMood,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = topicChipColors(mood)
    InsetPanel(
        modifier = modifier,
        containerColor = colors.container,
        contentColor = colors.content,
        content = content
    )
}
