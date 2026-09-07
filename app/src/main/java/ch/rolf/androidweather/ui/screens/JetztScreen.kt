package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import ch.rolf.androidweather.ui.adaptive.isTablet
import ch.rolf.androidweather.ui.components.CurrentHero
import ch.rolf.androidweather.ui.components.HourDetailSheet
import ch.rolf.androidweather.ui.components.HourlyForecastStrip
import ch.rolf.androidweather.ui.components.WxCard

@Composable
fun JetztScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val unit by vm.windUnit.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val bundle = ui.bundle
    var selectedHour by remember { mutableStateOf<HourPoint?>(null) }
    val tablet = isTablet()
    val hourSubtitle = bundle?.let {
        listOfNotNull(insightLine(it), snowFrost(it).snowLabel).joinToString(" · ")
    }.orEmpty()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (tablet) 24.dp else 16.dp)
                .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(if (tablet) 16.dp else 12.dp)
        ) {
            if (bundle != null && tablet) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(Modifier.weight(1.15f)) {
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
                    }
                    Column(
                        Modifier.weight(0.85f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                        if (ui.alerts.isNotEmpty()) {
                            WxCard {
                                Text("Warnungen", style = MaterialTheme.typography.titleLarge)
                                ui.alerts.forEach { alert ->
                                    Text(alert.event, style = MaterialTheme.typography.titleMedium)
                                    if (alert.headline.isNotBlank()) Text(alert.headline)
                                    formatAlertValidity(alert.onset, alert.expires, bundle.timezone)?.let {
                                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    alert.area?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                }
                            }
                        }
                    }
                }
            } else if (bundle != null) {
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
                if (ui.alerts.isNotEmpty()) {
                    WxCard {
                        Text("Warnungen", style = MaterialTheme.typography.titleLarge)
                        ui.alerts.forEach { alert ->
                            Text(alert.event, style = MaterialTheme.typography.titleMedium)
                            if (alert.headline.isNotBlank()) Text(alert.headline)
                            formatAlertValidity(alert.onset, alert.expires, bundle.timezone)?.let {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            alert.area?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            } else if (ui.loading) {
                WxCard { Text("Wetter wird geladen…") }
            } else {
                WxCard { Text(ui.error ?: "Keine aktuellen Daten.") }
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
    HourDetailSheet(
        hour = selectedHour,
        unit = unit,
        timeZone = bundle?.timezone,
        onDismiss = { selectedHour = null }
    )
}
