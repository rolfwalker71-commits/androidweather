package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import ch.rolf.androidweather.domain.formatAlertValidity
import ch.rolf.androidweather.domain.insightLine
import ch.rolf.androidweather.domain.samePlace
import ch.rolf.androidweather.domain.snowFrost
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.CurrentHero
import ch.rolf.androidweather.ui.components.HourDetail
import ch.rolf.androidweather.ui.components.HourlyForecastStrip
import ch.rolf.androidweather.ui.components.WxCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JetztScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val unit by vm.windUnit.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val bundle = ui.bundle
    var selectedHour by remember { mutableStateOf<HourPoint?>(null) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (bundle != null) {
                CurrentHero(
                    place = ui.place,
                    bundle = bundle,
                    stale = ui.stale,
                    offline = ui.error?.startsWith("Offline") == true,
                    favored = favorites.any { samePlace(it, ui.place) },
                    windUnit = unit,
                    notice = ui.notice?.line,
                    onToggleFavorite = { vm.toggleFavorite(ui.place) }
                )
            } else if (ui.loading) {
                WxCard { Text("Wetter wird geladen…") }
            } else {
                WxCard { Text(ui.error ?: "Keine aktuellen Daten.") }
            }
            if (bundle != null) {
                val hourSubtitle = listOfNotNull(
                    insightLine(bundle),
                    snowFrost(bundle).snowLabel
                ).joinToString(" · ")
                WxCard {
                    Text("Nächste 4 Stunden", style = MaterialTheme.typography.titleLarge)
                    if (hourSubtitle.isNotBlank()) {
                        Text(
                            hourSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HourlyForecastStrip(
                        hours = bundle.hours.take(4),
                        timezone = bundle.timezone,
                        compact = true,
                        modifier = Modifier.padding(top = 12.dp),
                        onSelect = { selectedHour = it }
                    )
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
            HourDetail(hour, unit, bundle?.timezone)
        }
    }
}
