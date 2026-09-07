package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.domain.comfortAdvice
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatWind
import ch.rolf.androidweather.domain.namedWind
import ch.rolf.androidweather.domain.skyWatch
import ch.rolf.androidweather.domain.windDirection
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.WxCard

object TopicScreens {
    @Composable
    fun Wind(vm: WetterViewModel) {
        val ui by vm.state.collectAsStateWithLifecycle()
        val unit by vm.windUnit.collectAsStateWithLifecycle()
        val bundle = ui.bundle
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Wind", style = MaterialTheme.typography.headlineSmall)
            if (bundle == null) { Text("Keine Daten."); return }
            val named = namedWind(ui.place, bundle)
            WxCard {
                Text(named.name, style = MaterialTheme.typography.titleLarge)
                Text(named.detail)
                Text("Sicherheit ${named.confidence}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            WxCard {
                Text("Aktuell", style = MaterialTheme.typography.titleLarge)
                Text(formatWind(bundle.current.wind_speed_10m, unit) + " ${windDirection(bundle.current.wind_direction_10m)}")
                Text("Böen ${formatWind(bundle.current.wind_gusts_10m, unit)}")
            }
        }
    }

    @Composable
    fun Berge(vm: WetterViewModel) {
        val ui by vm.state.collectAsStateWithLifecycle()
        val unit by vm.windUnit.collectAsStateWithLifecycle()
        val bundle = ui.bundle
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Berge", style = MaterialTheme.typography.headlineSmall)
            if (bundle == null) { Text("Keine Daten."); return }
            WxCard {
                Text("Höhen", style = MaterialTheme.typography.titleLarge)
                bundle.elevations.forEach { snap ->
                    Text("${snap.elevation} m · ${snap.temperature?.let { formatTemp(it) } ?: "–"} · ${snap.wind?.let { formatWind(it, unit) } ?: ""}")
                }
            }
            ui.avalanche?.let {
                WxCard {
                    Text("Lawinenbulletin", style = MaterialTheme.typography.titleLarge)
                    Text(it.label)
                    Text(it.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (ui.passes.isNotEmpty()) {
                WxCard {
                    Text("Pässe", style = MaterialTheme.typography.titleLarge)
                    ui.passes.forEach { Text("${it.name} · ${it.temperature?.let { t -> formatTemp(t) } ?: "–"}") }
                }
            }
        }
    }

    @Composable
    fun Seen(vm: WetterViewModel) {
        val ui by vm.state.collectAsStateWithLifecycle()
        val bundle = ui.bundle
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Seen", style = MaterialTheme.typography.headlineSmall)
            if (bundle?.lakes.isNullOrEmpty()) Text("Keine Seen in der Nähe (80 km).")
            bundle?.lakes?.forEach { lake ->
                WxCard {
                    Text(lake.name, style = MaterialTheme.typography.titleLarge)
                    Text("Wassertemperatur ${lake.waterTemp?.let { formatTemp(it) } ?: "–"}")
                    lake.waveHeight?.let { Text("Wellen ${"%.1f".format(it)} m") }
                    lake.tempSource?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }

    @Composable
    fun Draussen(vm: WetterViewModel) {
        val ui by vm.state.collectAsStateWithLifecycle()
        val bundle = ui.bundle
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Draußen", style = MaterialTheme.typography.headlineSmall)
            if (bundle == null) { Text("Keine Daten."); return }
            comfortAdvice(bundle)?.let {
                WxCard {
                    Text(it.recommendation, style = MaterialTheme.typography.titleLarge)
                    Text(it.detail)
                }
            }
            val sky = skyWatch(bundle)
            WxCard {
                Text("Himmel", style = MaterialTheme.typography.titleLarge)
                sky.goldenLabel?.let { Text(it) }
                sky.starsLabel?.let { Text(it) }
                Text(sky.moonLabel)
            }
        }
    }
}
