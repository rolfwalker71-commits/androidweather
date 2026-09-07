package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.clickable
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
import ch.rolf.androidweather.domain.HourPoint
import ch.rolf.androidweather.domain.formatHour
import ch.rolf.androidweather.domain.formatMm
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatWind
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.nowcast30Bars
import ch.rolf.androidweather.domain.windHourBars
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.HourDetail
import ch.rolf.androidweather.ui.components.PrecipBars
import ch.rolf.androidweather.ui.components.WindBarsChart
import ch.rolf.androidweather.ui.components.WxCard
import ch.rolf.androidweather.widget.glyphEmoji

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerlaufScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val unit by vm.windUnit.collectAsStateWithLifecycle()
    val bundle = ui.bundle
    var selected by remember { mutableStateOf<HourPoint?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Verlauf", style = MaterialTheme.typography.headlineSmall)
        if (bundle == null) {
            Text("Keine Daten.")
            return
        }
        val rainBars = nowcast30Bars(bundle.minutes).ifEmpty {
            bundle.hours.take(12).map {
                ch.rolf.androidweather.domain.NowcastBar(it.time, it.precipMm, it.code)
            }
        }
        WxCard {
            Text("Regen 12 h", style = MaterialTheme.typography.titleLarge)
            PrecipBars(rainBars.take(12))
        }
        WxCard {
            Text("Wind 12 h", style = MaterialTheme.typography.titleLarge)
            WindBarsChart(windHourBars(bundle.hours, 12))
        }
        bundle.hours.forEach { hour ->
            WxCard(onClick = { selected = hour }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(formatHour(hour.time), modifier = Modifier.weight(1f))
                    Text(glyphEmoji(getWmo(hour.code, hour.isDay).glyph))
                    Text(formatTemp(hour.temperature))
                    Text(formatMm(hour.precipMm))
                    Text(formatWind(hour.wind, unit))
                }
            }
        }
    }
    selected?.let { hour ->
        ModalBottomSheet(onDismissRequest = { selected = null }) { HourDetail(hour, unit) }
    }
}
