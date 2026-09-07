package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import ch.rolf.androidweather.domain.clothingLine
import ch.rolf.androidweather.domain.comfortAdvice
import ch.rolf.androidweather.domain.formatPercent
import ch.rolf.androidweather.domain.formatHpa
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatTime
import ch.rolf.androidweather.domain.formatWind
import ch.rolf.androidweather.domain.namedWind
import ch.rolf.androidweather.domain.skyWatch
import ch.rolf.androidweather.domain.snowFrost
import ch.rolf.androidweather.domain.windDirection
import ch.rolf.androidweather.domain.windHourBars
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.InsetPanel
import ch.rolf.androidweather.ui.components.WeatherIcon
import ch.rolf.androidweather.ui.components.WindDirChart
import ch.rolf.androidweather.ui.components.WxCard

object TopicScreens {
    @Composable
    fun Wind(vm: WetterViewModel) {
        val ui by vm.state.collectAsStateWithLifecycle()
        val unit by vm.windUnit.collectAsStateWithLifecycle()
        val bundle = ui.bundle
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Wind", style = MaterialTheme.typography.headlineSmall)
            if (bundle == null) {
                WxCard { Text("Keine Daten.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                return
            }
            val named = namedWind(ui.place, bundle)
            val current = bundle.current
            WxCard {
                Text("Lage", style = MaterialTheme.typography.titleLarge)
                Text(named.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
                Text(named.detail, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Sicherheit ${named.confidence}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            WxCard {
                Text("Wind jetzt", style = MaterialTheme.typography.titleLarge)
                Text(
                    formatWind(current.wind_speed_10m, unit),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "aus ${windDirection(current.wind_direction_10m)} · Böen ${formatWind(current.wind_gusts_10m, unit)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                bundle.hours.firstOrNull()?.cape?.let { cape ->
                    Text(
                        "CAPE ${cape.toInt()} J/kg · Modell Open-Meteo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InsetPanel(Modifier.weight(1f)) {
                        Text("Feuchte", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatPercent(current.relative_humidity_2m), style = MaterialTheme.typography.titleMedium)
                    }
                    InsetPanel(Modifier.weight(1f)) {
                        Text("Luftdruck", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatHpa(current.pressure_msl), style = MaterialTheme.typography.titleMedium)
                    }
                }
                InsetPanel(Modifier.padding(top = 12.dp)) {
                    Text("Bewölkung", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatPercent(current.cloud_cover), style = MaterialTheme.typography.titleMedium)
                }
            }
            WxCard {
                Text("Nächste Stunden", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Wie im Verlauf — Balken und Richtung",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WindDirChart(windHourBars(bundle.hours, 12), unit, bundle.timezone)
            }
        }
    }

    @Composable
    fun Berge(vm: WetterViewModel) {
        val ui by vm.state.collectAsStateWithLifecycle()
        val unit by vm.windUnit.collectAsStateWithLifecycle()
        val bundle = ui.bundle
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Berge", style = MaterialTheme.typography.headlineSmall)
            if (bundle == null) {
                WxCard { Text("Keine Daten.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                return
            }
            val frost = snowFrost(bundle)
            val bands = bundle.elevations.filter { it.temperature != null || it.wind != null }
            WxCard {
                Text("Höhen", style = MaterialTheme.typography.titleLarge)
                frost.snowLabel?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                }
                if (bands.isEmpty()) {
                    Text(
                        "Keine Höhenbänder für diesen Ort.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    bands.chunked(3).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { snap ->
                                InsetPanel(Modifier.weight(1f)) {
                                    Text(
                                        "${snap.elevation} m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        snap.temperature?.let { formatTemp(it) } ?: "–",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    snap.wind?.let { wind ->
                                        Text(
                                            buildString {
                                                append(formatWind(wind, unit))
                                                snap.windDir?.let { append(" ${windDirection(it)}") }
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Text(
                        "Open-Meteo für diese Höhen, nicht Stationsmessung.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            ui.avalanche?.let { bulletin ->
                WxCard {
                    Text("Schneebericht", style = MaterialTheme.typography.titleLarge)
                    Text(bulletin.source, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(bulletin.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    if (bulletin.note.isNotBlank()) {
                        Text(bulletin.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (ui.passes.isNotEmpty()) {
                WxCard {
                    Text("Pässe", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "SwissMetNet, im Umkreis von 80 km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ui.passes.forEach { pass ->
                        InsetPanel(Modifier.padding(top = 12.dp)) {
                            Text(pass.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${pass.distanceKm.toInt()} km entfernt · ${pass.id}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                pass.temperature?.let { formatTemp(it) } ?: "–",
                                style = MaterialTheme.typography.titleLarge
                            )
                            pass.windKmh?.let { wind ->
                                Text(
                                    buildString {
                                        append(formatWind(wind, unit))
                                        pass.windDir?.let { append(" ${windDirection(it)}") }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            pass.observedAt?.let {
                                Text(
                                    formatTime(it, bundle.timezone),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Seen(vm: WetterViewModel) {
        val ui by vm.state.collectAsStateWithLifecycle()
        val bundle = ui.bundle
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Seen", style = MaterialTheme.typography.headlineSmall)
            val lakes = bundle?.lakes.orEmpty()
            if (lakes.isEmpty()) {
                WxCard {
                    Text("Keine Seen in der Nähe", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Im Umkreis von 80 km liegen keine Seen mit Wassertemperatur oder Wellen.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return
            }
            WxCard {
                Text("Seen in der Nähe", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Wassertemperatur und Wellen, soweit vorhanden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                lakes.forEach { lake ->
                    InsetPanel(Modifier.padding(top = 12.dp)) {
                        Text(lake.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${lake.distanceKm.toInt()} km entfernt",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Wassertemperatur ${lake.waterTemp?.let { formatTemp(it) } ?: "–"}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        lake.waveHeight?.let {
                            Text("Wellen ${"%.1f".format(it)} m", style = MaterialTheme.typography.bodyLarge)
                        }
                        lake.tempSource?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Draussen(vm: WetterViewModel) {
        val ui by vm.state.collectAsStateWithLifecycle()
        val bundle = ui.bundle
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Draußen", style = MaterialTheme.typography.headlineSmall)
            if (bundle == null) {
                WxCard { Text("Keine Daten.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                return
            }
            val clothing = clothingLine(bundle)
            val comfort = comfortAdvice(bundle)
            val sky = skyWatch(bundle)
            WxCard {
                Text("Kleidung", style = MaterialTheme.typography.titleLarge)
                Row(
                    Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeatherIcon(
                        code = bundle.current.weather_code,
                        isDay = bundle.current.is_day == 1,
                        size = 48.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            clothing ?: comfort?.recommendation ?: "Keine Empfehlung",
                            style = MaterialTheme.typography.titleLarge
                        )
                        comfort?.let {
                            Text(it.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            comfort?.let {
                WxCard {
                    Text("Komfort", style = MaterialTheme.typography.titleLarge)
                    InsetPanel(Modifier.padding(top = 12.dp)) {
                        Text(it.indexName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${it.indexValue.toInt()}°", style = MaterialTheme.typography.headlineSmall)
                        Text(it.recommendation, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            WxCard {
                Text("Himmel", style = MaterialTheme.typography.titleLarge)
                sky.goldenLabel?.let {
                    InsetPanel(Modifier.padding(top = 12.dp)) {
                        Text("Goldene Stunde", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(it, style = MaterialTheme.typography.titleMedium)
                    }
                }
                sky.starsLabel?.let {
                    InsetPanel(Modifier.padding(top = 12.dp)) {
                        Text("Sterne", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(it, style = MaterialTheme.typography.titleMedium)
                    }
                }
                InsetPanel(Modifier.padding(top = 12.dp)) {
                    Text("Mond", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sky.moonLabel, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
