package ch.rolf.androidweather.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.domain.PREF_META
import ch.rolf.androidweather.domain.ThemePreference
import ch.rolf.androidweather.domain.WindUnit
import ch.rolf.androidweather.domain.placeShort
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.CitySearch
import ch.rolf.androidweather.ui.components.WxCard
import ch.rolf.androidweather.widget.CompactWidgetReceiver
import ch.rolf.androidweather.widget.WideWidgetReceiver

@Composable
fun EinstellungenScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val home by vm.home.collectAsStateWithLifecycle()
    val unit by vm.windUnit.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val notify by vm.notifyPrefs.collectAsStateWithLifecycle()
    val ongoing by vm.ongoing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineSmall)
        WxCard {
            Text("Home-Ort", style = MaterialTheme.typography.titleLarge)
            Text(
                home?.let { "Gesetzt: ${placeShort(it)}" } ?: "Kein Home-Ort — bei fehlendem GPS bleibt Bern.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { vm.setHome(ui.place) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Aktuellen Ort als Home")
            }
            CitySearch(ui.searchResults, vm::search, vm::setHome, placeholder = "Home-Ort suchen")
        }
        WxCard {
            Text("Einheiten", style = MaterialTheme.typography.titleLarge)
            Text("Wind. Temperatur bleibt °C, Niederschlag mm.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = unit == WindUnit.Kmh, onClick = { vm.setWindUnit(WindUnit.Kmh) }, label = { Text("km/h") })
                FilterChip(selected = unit == WindUnit.Ms, onClick = { vm.setWindUnit(WindUnit.Ms) }, label = { Text("m/s") })
            }
        }
        WxCard {
            Text("Darstellung", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = theme == ThemePreference.System, onClick = { vm.setTheme(ThemePreference.System) }, label = { Text("System") })
                FilterChip(selected = theme == ThemePreference.Light, onClick = { vm.setTheme(ThemePreference.Light) }, label = { Text("Hell") })
                FilterChip(selected = theme == ThemePreference.Dark, onClick = { vm.setTheme(ThemePreference.Dark) }, label = { Text("Dunkel") })
            }
        }
        WxCard {
            Text("Meldungen", style = MaterialTheme.typography.titleLarge)
            PREF_META.forEach { (id, label, hint) ->
                val checked = when (id) {
                    "rainSoon" -> notify.rainSoon
                    "warnings" -> notify.warnings
                    "frost" -> notify.frost
                    "uv" -> notify.uv
                    "air" -> notify.air
                    "dailyBrief" -> notify.dailyBrief
                    else -> notify.forecastChange
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = {
                            vm.setNotifyPrefs(
                                when (id) {
                                    "rainSoon" -> notify.copy(rainSoon = it)
                                    "warnings" -> notify.copy(warnings = it)
                                    "frost" -> notify.copy(frost = it)
                                    "uv" -> notify.copy(uv = it)
                                    "air" -> notify.copy(air = it)
                                    "dailyBrief" -> notify.copy(dailyBrief = it)
                                    else -> notify.copy(forecastChange = it)
                                }
                            )
                        }
                    )
                }
            }
        }
        WxCard {
            Text("Dauerbenachrichtigung", style = MaterialTheme.typography.titleLarge)
            Text(
                "Immer sichtbare Benachrichtigung mit Temperatur, Lage und Ort. Tippen öffnet Jetzt. Aktualisierung mit WorkManager.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Aktiv", Modifier.weight(1f))
                Switch(checked = ongoing, onCheckedChange = vm::setOngoing)
            }
        }
        WxCard {
            Text("Widget anheften", style = MaterialTheme.typography.titleLarge)
            TextButton(
                onClick = {
                    val mgr = AppWidgetManager.getInstance(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.isRequestPinAppWidgetSupported) {
                        mgr.requestPinAppWidget(ComponentName(context, CompactWidgetReceiver::class.java), null, null)
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("2×2 Widget anheften") }
            TextButton(
                onClick = {
                    val mgr = AppWidgetManager.getInstance(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.isRequestPinAppWidgetSupported) {
                        mgr.requestPinAppWidget(ComponentName(context, WideWidgetReceiver::class.java), null, null)
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("4×2 Widget anheften") }
        }
    }
}
