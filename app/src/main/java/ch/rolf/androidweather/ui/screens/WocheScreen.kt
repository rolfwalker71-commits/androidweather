package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.domain.DayPoint
import ch.rolf.androidweather.domain.formatDayMonth
import ch.rolf.androidweather.domain.formatMm
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatWeekday
import ch.rolf.androidweather.domain.formatWind
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.windDirection
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.DayDetail
import ch.rolf.androidweather.ui.components.WxCard
import ch.rolf.androidweather.widget.glyphEmoji

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WocheScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val unit by vm.windUnit.collectAsStateWithLifecycle()
    val bundle = ui.bundle
    var selected by remember { mutableStateOf<DayPoint?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Woche", style = MaterialTheme.typography.headlineSmall)
        if (bundle == null) {
            Text("Keine Daten.")
            return
        }
        bundle.days.forEach { day ->
            WxCard(onClick = { selected = day }) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(formatWeekday(day.date), style = MaterialTheme.typography.titleMedium)
                        Text(formatDayMonth(day.date), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(glyphEmoji(getWmo(day.code, true).glyph))
                    Text("${formatTemp(day.tMax)} / ${formatTemp(day.tMin)}")
                    Text(formatMm(day.precipMm))
                }
            }
        }
        if (bundle.elevations.isNotEmpty()) {
            WxCard {
                Text("Alpen", style = MaterialTheme.typography.titleLarge)
                bundle.elevations.forEach { snap ->
                    Text(
                        "${snap.elevation} m · " +
                            (snap.temperature?.let { formatTemp(it) } ?: "–") +
                            (snap.wind?.let { " · ${formatWind(it, unit)}" } ?: "") +
                            (snap.windDir?.let { " ${windDirection(it)}" } ?: "")
                    )
                }
            }
        }
        if (ui.passes.isNotEmpty()) {
            WxCard {
                Text("Pässe", style = MaterialTheme.typography.titleLarge)
                ui.passes.forEach { pass ->
                    Text(
                        "${pass.name} · " +
                            (pass.temperature?.let { formatTemp(it) } ?: "–") +
                            (pass.windKmh?.let { " · ${formatWind(it, unit)}" } ?: "")
                    )
                }
            }
        }
        if (bundle.lakes.isNotEmpty()) {
            WxCard {
                Text("Seen", style = MaterialTheme.typography.titleLarge)
                bundle.lakes.forEach { lake ->
                    Text(
                        "${lake.name} · " +
                            (lake.waterTemp?.let { formatTemp(it) } ?: "–") +
                            (lake.waveHeight?.let { " · Wellen ${"%.1f".format(it)} m" } ?: "")
                    )
                    lake.tempSource?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        ui.avalanche?.let { av ->
            WxCard {
                Text("Lawinenbulletin", style = MaterialTheme.typography.titleLarge)
                Text(av.label)
                Text(av.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    selected?.let { day ->
        ModalBottomSheet(onDismissRequest = { selected = null }) { DayDetail(day, unit) }
    }
}
