package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.domain.europeanAqi
import ch.rolf.androidweather.domain.pollenLevel
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.adaptive.TabletWidth
import ch.rolf.androidweather.ui.adaptive.isTablet
import ch.rolf.androidweather.ui.components.AirTrendBars
import ch.rolf.androidweather.ui.components.GoodBarColor
import ch.rolf.androidweather.ui.components.WxCard
import ch.rolf.androidweather.ui.components.scaleColors

@Composable
fun LuftScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val bundle = ui.bundle
    val dark = isSystemInDarkTheme()
    TabletWidth {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(if (isTablet()) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (bundle == null) {
            Text("Keine Daten.")
            return@TabletWidth
        }
        val aqiValue = bundle.air?.european_aqi
        val aqi = europeanAqi(aqiValue)
        val trend = bundle.airTrend.filter { it.pm25 != null }
        val cape = bundle.hours.firstOrNull()?.cape
        val hasPollen = bundle.pollen.alder != null || bundle.pollen.birch != null || bundle.pollen.grass != null
        WxCard {
            Text("Luft & Pollen", style = MaterialTheme.typography.titleLarge)
            if (aqiValue != null) {
                val colors = scaleColors(aqi.tone, dark)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.container)
                        .padding(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Luftqualität", style = MaterialTheme.typography.titleMedium, color = colors.content, modifier = Modifier.weight(1f))
                        Text(
                            "${aqiValue.toInt()} · ${aqi.label}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.content
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(aqi.ratio.coerceIn(0.06f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.content)
                        )
                    }
                    val pm = buildList {
                        bundle.air?.pm2_5?.let { add("PM2.5 ${"%.1f".format(it)} µg/m³") }
                        bundle.air?.pm10?.let { add("PM10 ${"%.1f".format(it)} µg/m³") }
                    }
                    if (pm.isNotEmpty()) {
                        Text(
                            pm.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.content.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            if (trend.isNotEmpty()) {
                Text("Feinstaub-Trend", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                AirTrendBars(trend.map { it.pm25 }, color = GoodBarColor)
            }
            if (hasPollen) {
                Text("Pollen", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                listOf(
                    "Erle" to bundle.pollen.alder,
                    "Birke" to bundle.pollen.birch,
                    "Gräser" to bundle.pollen.grass
                ).forEach { (name, value) ->
                    if (value == null) return@forEach
                    val level = pollenLevel(value)
                    val colors = scaleColors(level.tone, dark)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.container)
                            .padding(12.dp)
                    ) {
                        Text(name, style = MaterialTheme.typography.bodyMedium, color = colors.content.copy(alpha = 0.75f))
                        Text(level.label, style = MaterialTheme.typography.titleMedium, color = colors.content)
                    }
                }
            }
            if (cape != null) {
                Text(
                    "CAPE ${cape.toInt()} J/kg · Modell Open-Meteo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            if (aqiValue == null && !hasPollen && trend.isEmpty() && cape == null) {
                Text(
                    "Keine Luft- oder Pollendaten für diesen Ort.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    }
}
