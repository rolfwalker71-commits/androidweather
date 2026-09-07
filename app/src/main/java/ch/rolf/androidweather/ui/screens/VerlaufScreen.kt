package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.domain.HourPoint
import ch.rolf.androidweather.domain.windHourBars
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.adaptive.TabletWidth
import ch.rolf.androidweather.ui.adaptive.isTablet
import ch.rolf.androidweather.ui.adaptive.isTabletLandscape
import ch.rolf.androidweather.ui.components.HourDetailSheet
import ch.rolf.androidweather.ui.components.HourlyForecastStrip
import ch.rolf.androidweather.ui.components.LabeledPrecipChart
import ch.rolf.androidweather.ui.components.WindDirChart
import ch.rolf.androidweather.ui.components.WxCard

@Composable
fun VerlaufScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val unit by vm.windUnit.collectAsStateWithLifecycle()
    val bundle = ui.bundle
    var selected by remember { mutableStateOf<HourPoint?>(null) }
    val tablet = isTablet()
    TabletWidth(max = 1200.dp) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(if (tablet) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (bundle == null) {
            Text("Keine Daten.")
            return@Column
        }
        if (isTabletLandscape()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    WxCard {
                        Text("24 Stunden", style = MaterialTheme.typography.titleLarge)
                        HourlyForecastStrip(
                            hours = bundle.hours,
                            timezone = bundle.timezone,
                            compact = false,
                            onSelect = { selected = it }
                        )
                    }
                }
                Box(Modifier.weight(1f)) {
                    WxCard {
                        Text("Regen + Wind", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Nächste 12 Stunden · stündlich Open-Meteo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Regen", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                        LabeledPrecipChart(bundle.hours.take(12), bundle.timezone)
                        Text("Wind", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                        WindDirChart(windHourBars(bundle.hours, 12), unit, bundle.timezone)
                    }
                }
            }
        } else {
            WxCard {
                Text("24 Stunden", style = MaterialTheme.typography.titleLarge)
                HourlyForecastStrip(
                    hours = bundle.hours,
                    timezone = bundle.timezone,
                    compact = false,
                    onSelect = { selected = it }
                )
            }
            WxCard {
                Text("Regen + Wind", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Nächste 12 Stunden · stündlich Open-Meteo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Regen", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                LabeledPrecipChart(bundle.hours.take(12), bundle.timezone)
                Text("Wind", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                WindDirChart(windHourBars(bundle.hours, 12), unit, bundle.timezone)
            }
        }
    }
    }
    HourDetailSheet(
        hour = selected,
        unit = unit,
        timeZone = bundle?.timezone,
        onDismiss = { selected = null }
    )
}
