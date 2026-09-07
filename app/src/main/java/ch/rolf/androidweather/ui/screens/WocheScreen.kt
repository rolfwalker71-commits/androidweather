package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.domain.DayPoint
import ch.rolf.androidweather.domain.formatDayMonth
import ch.rolf.androidweather.domain.formatPercent
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatWeekday
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.weatherMood
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.DayDetail
import ch.rolf.androidweather.ui.components.TempRangeBar
import ch.rolf.androidweather.ui.components.WxCard
import ch.rolf.androidweather.ui.components.moodColors
import ch.rolf.androidweather.widget.glyphEmoji

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WocheScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val unit by vm.windUnit.collectAsStateWithLifecycle()
    val bundle = ui.bundle
    var selected by remember { mutableStateOf<DayPoint?>(null) }
    val dark = isSystemInDarkTheme()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (bundle == null) {
            Text("Keine Daten.")
            return
        }
        val days = bundle.days
        val weekMin = days.minOfOrNull { it.tMin } ?: 0.0
        val weekMax = days.maxOfOrNull { it.tMax } ?: 1.0
        WxCard {
            Text(
                if (days.isNotEmpty()) "${days.size}-Tage-Trend" else "Tages-Trend",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Tippen für Stundenverlauf, Minima und Niederschlag",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            days.forEachIndexed { index, day ->
                val mood = moodColors(weatherMood(day.code, true), dark)
                val outlined = selected == day || (selected == null && index == 0)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(mood.container)
                        .then(
                            if (outlined) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                            else Modifier
                        )
                        .clickable { selected = day }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(0.28f)) {
                        Text(
                            if (index == 0) "Heute" else formatWeekday(day.date, bundle.timezone),
                            style = MaterialTheme.typography.titleMedium,
                            color = mood.content
                        )
                        Text(
                            formatDayMonth(day.date, bundle.timezone),
                            style = MaterialTheme.typography.bodyMedium,
                            color = mood.content.copy(alpha = 0.75f)
                        )
                    }
                    Text(glyphEmoji(getWmo(day.code, true).glyph), style = MaterialTheme.typography.titleLarge)
                    TempRangeBar(
                        tMin = day.tMin,
                        tMax = day.tMax,
                        weekMin = weekMin,
                        weekMax = weekMax,
                        modifier = Modifier.weight(1f)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${formatTemp(day.tMin)} / ${formatTemp(day.tMax)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = mood.content
                        )
                        day.precipProb?.let {
                            Text(
                                formatPercent(it),
                                style = MaterialTheme.typography.bodyMedium,
                                color = mood.content.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
    selected?.let { day ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            DayDetail(day, unit, bundle?.timezone)
        }
    }
}
