package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.domain.HourPoint
import ch.rolf.androidweather.domain.clothingLine
import ch.rolf.androidweather.domain.formatAlertValidity
import ch.rolf.androidweather.domain.formatHour
import ch.rolf.androidweather.domain.formatPercent
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatUpdatedRelative
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.insightLine
import ch.rolf.androidweather.domain.nextPrecipLine
import ch.rolf.androidweather.domain.placeShort
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.CitySearch
import ch.rolf.androidweather.ui.components.HourDetail
import ch.rolf.androidweather.ui.components.WxCard
import ch.rolf.androidweather.widget.glyphEmoji

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JetztScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val unit by vm.windUnit.collectAsStateWithLifecycle()
    val bundle = ui.bundle
    var selectedHour by remember { mutableStateOf<HourPoint?>(null) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CitySearch(ui.searchResults, vm::search, vm::selectPlace)
            WxCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(placeShort(ui.place), style = MaterialTheme.typography.headlineSmall)
                        bundle?.let {
                            Text(
                                formatUpdatedRelative(it.fetchedAt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { vm.toggleFavorite(ui.place) }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (vm.isFavorite(ui.place)) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorit"
                        )
                    }
                }
                if (bundle != null) {
                    val wmo = getWmo(bundle.current.weather_code, bundle.current.is_day == 1)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(formatTemp(bundle.current.temperature_2m), style = MaterialTheme.typography.displayMedium)
                        Text(glyphEmoji(wmo.glyph), style = MaterialTheme.typography.displaySmall)
                    }
                    Text(wmo.label, style = MaterialTheme.typography.titleMedium)
                    Text("Gefühlt ${formatTemp(bundle.current.apparent_temperature)}")
                    nextPrecipLine(bundle)?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    insightLine(bundle)?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    clothingLine(bundle)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    ui.notice?.let { Text(it.line, color = MaterialTheme.colorScheme.primary) }
                    bundle.station?.let {
                        Text(
                            "${it.name} ${formatTemp(it.temperature)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (ui.loading) {
                    Text("Wetter wird geladen…")
                } else {
                    Text(ui.error ?: "Keine aktuellen Daten.")
                }
            }
            if (bundle != null) {
                Text("Nächste Stunden", style = MaterialTheme.typography.titleLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bundle.hours.take(4).forEach { hour ->
                        WxCard(modifier = Modifier.width(132.dp), onClick = { selectedHour = hour }) {
                            Text(formatHour(hour.time), style = MaterialTheme.typography.labelLarge)
                            Text(formatTemp(hour.temperature), style = MaterialTheme.typography.headlineSmall)
                            Text(glyphEmoji(getWmo(hour.code, hour.isDay).glyph))
                            hour.precipProb?.let { Text(formatPercent(it)) }
                        }
                    }
                }
            }
            if (ui.alerts.isNotEmpty()) {
                WxCard {
                    Text("Warnungen", style = MaterialTheme.typography.titleLarge)
                    ui.alerts.forEach { alert ->
                        Text(alert.event, style = MaterialTheme.typography.titleMedium)
                        if (alert.headline.isNotBlank()) Text(alert.headline)
                        formatAlertValidity(alert.onset, alert.expires, bundle?.timezone)?.let {
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        alert.area?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = vm::locate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(64.dp),
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Outlined.MyLocation, contentDescription = "Standort")
        }
    }
    selectedHour?.let { hour ->
        ModalBottomSheet(onDismissRequest = { selectedHour = null }) {
            HourDetail(hour, unit)
        }
    }
}
