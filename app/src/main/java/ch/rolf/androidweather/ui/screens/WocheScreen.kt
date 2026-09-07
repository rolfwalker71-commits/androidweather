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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.domain.DayPoint
import ch.rolf.androidweather.domain.HourPoint
import ch.rolf.androidweather.domain.formatDayMonth
import ch.rolf.androidweather.domain.formatPercent
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatWeekday
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.weatherMood
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.DayDetail
import ch.rolf.androidweather.ui.components.HourDetailSheet
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
    var selectedHour by remember { mutableStateOf<HourPoint?>(null) }
    val dark = isSystemInDarkTheme()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                val weekday = if (index == 0) "Heute" else formatWeekday(day.date, bundle.timezone)
                val cardShape = RoundedCornerShape(24.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(cardShape)
                        .background(mood.container)
                        .then(
                            if (outlined) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, cardShape)
                            else Modifier
                        )
                        .clickable { selected = day }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.requiredWidth(84.dp)) {
                        Text(
                            weekday,
                            style = MaterialTheme.typography.titleMedium.copy(
                                lineHeight = 18.sp,
                                hyphens = Hyphens.None,
                                lineBreak = LineBreak.Simple,
                                color = mood.content
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatDayMonth(day.date, bundle.timezone),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 16.sp,
                                hyphens = Hyphens.None,
                                lineBreak = LineBreak.Simple,
                                color = mood.content.copy(alpha = 0.75f)
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        glyphEmoji(getWmo(day.code, true).glyph),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )
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
                            color = mood.content,
                            maxLines = 1,
                            softWrap = false
                        )
                        day.precipProb?.let {
                            Text(
                                formatPercent(it),
                                style = MaterialTheme.typography.bodyMedium,
                                color = mood.content.copy(alpha = 0.75f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
    selected?.let { day ->
        bundle?.let { data ->
            ModalBottomSheet(
                onDismissRequest = { selected = null },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                DayDetail(
                    day = day,
                    days = data.days,
                    hours = data.allHours.ifEmpty { data.hours },
                    unit = unit,
                    timeZone = data.timezone,
                    nowIso = data.current.time,
                    onClose = { selected = null },
                    onSelectDay = { selected = it },
                    onSelectHour = { selectedHour = it }
                )
            }
        }
    }
    HourDetailSheet(
        hour = selectedHour,
        unit = unit,
        timeZone = bundle?.timezone,
        onDismiss = { selectedHour = null }
    )
}
