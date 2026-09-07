package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.domain.europeanAqi
import ch.rolf.androidweather.domain.pollenLevel
import ch.rolf.androidweather.domain.uvLevel
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.AirTrendBars
import ch.rolf.androidweather.ui.components.WxCard

@Composable
fun LuftScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val bundle = ui.bundle
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Luft", style = MaterialTheme.typography.headlineSmall)
        if (bundle == null) {
            Text("Keine Daten.")
            return
        }
        val aqi = europeanAqi(bundle.air?.european_aqi)
        WxCard {
            Text("Europäischer AQI", style = MaterialTheme.typography.titleLarge)
            Text(bundle.air?.european_aqi?.toInt()?.toString() ?: "–", style = MaterialTheme.typography.displaySmall)
            Text(aqi.label, style = MaterialTheme.typography.titleMedium)
            Text(aqi.hint)
            LinearProgressIndicator(progress = { aqi.ratio.coerceIn(0f, 1f) })
        }
        WxCard {
            Text("Feinstaub", style = MaterialTheme.typography.titleLarge)
            Text("PM2.5 ${bundle.air?.pm2_5?.let { "${it.toInt()} µg/m³" } ?: "–"}")
            Text("PM10 ${bundle.air?.pm10?.let { "${it.toInt()} µg/m³" } ?: "–"}")
        }
        WxCard {
            Text("Pollen", style = MaterialTheme.typography.titleLarge)
            listOf("Erle" to bundle.pollen.alder, "Birke" to bundle.pollen.birch, "Gräser" to bundle.pollen.grass).forEach { (name, value) ->
                val level = pollenLevel(value)
                Text("$name · ${value?.toInt() ?: "–"} · ${level.label}")
                Text(level.hint, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val uv = uvLevel(bundle.hours.firstOrNull()?.uv ?: bundle.air?.uv_index)
        WxCard {
            Text("UV", style = MaterialTheme.typography.titleLarge)
            Text(uv.label)
            Text(uv.hint)
        }
        if (bundle.airTrend.isNotEmpty()) {
            WxCard {
                Text("Lufttrend 12 h", style = MaterialTheme.typography.titleLarge)
                AirTrendBars(bundle.airTrend.map { it.aqi })
            }
        }
    }
}
