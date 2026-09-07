package ch.rolf.androidweather.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.domain.PREF_META
import ch.rolf.androidweather.domain.ThemePreference
import ch.rolf.androidweather.domain.WindUnit
import ch.rolf.androidweather.domain.placeShort
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.ChoicePill
import ch.rolf.androidweather.ui.components.CitySearch
import ch.rolf.androidweather.ui.components.SettingsActionPill
import ch.rolf.androidweather.ui.components.SettingsPanel
import ch.rolf.androidweather.ui.components.SettingsToggleRow
import ch.rolf.androidweather.ui.components.ThemeChoiceTile
import ch.rolf.androidweather.widget.CompactWidgetReceiver
import ch.rolf.androidweather.widget.ExtraWideWidgetReceiver
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
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Einstellungen",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        SettingsPanel(
            title = "Home-Ort",
            description = home?.let { "Gesetzt: ${placeShort(it)}" }
                ?: "Kein Home-Ort — bei fehlendem GPS bleibt Bern."
        ) {
            SettingsActionPill(
                label = "Aktuellen Ort als Home",
                onClick = { vm.setHome(ui.place) },
                icon = Icons.Outlined.Place,
                filled = true
            )
            CitySearch(ui.searchResults, vm::search, vm::setHome, placeholder = "Home-Ort suchen")
        }
        SettingsPanel(
            title = "Einheiten",
            description = "Wind. Temperatur bleibt °C, Niederschlag mm."
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoicePill(
                    label = "km/h",
                    selected = unit == WindUnit.Kmh,
                    onClick = { vm.setWindUnit(WindUnit.Kmh) }
                )
                ChoicePill(
                    label = "m/s",
                    selected = unit == WindUnit.Ms,
                    onClick = { vm.setWindUnit(WindUnit.Ms) }
                )
            }
        }
        SettingsPanel(title = "Darstellung") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChoiceTile(
                    label = "System",
                    icon = Icons.Outlined.BrightnessAuto,
                    selected = theme == ThemePreference.System,
                    onClick = { vm.setTheme(ThemePreference.System) }
                )
                ThemeChoiceTile(
                    label = "Hell",
                    icon = Icons.Outlined.LightMode,
                    selected = theme == ThemePreference.Light,
                    onClick = { vm.setTheme(ThemePreference.Light) }
                )
                ThemeChoiceTile(
                    label = "Dunkel",
                    icon = Icons.Outlined.DarkMode,
                    selected = theme == ThemePreference.Dark,
                    onClick = { vm.setTheme(ThemePreference.Dark) }
                )
            }
        }
        SettingsPanel(title = "Meldungen") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    SettingsToggleRow(label = label, hint = hint) {
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
        }
        SettingsPanel(
            title = "Dauerbenachrichtigung",
            description = "Immer sichtbare Benachrichtigung mit Temperatur, Lage und Ort. Tippen öffnet Jetzt. Aktualisierung mit WorkManager."
        ) {
            SettingsToggleRow(label = "Aktiv", hint = "Benachrichtigung dauerhaft anzeigen") {
                Switch(checked = ongoing, onCheckedChange = vm::setOngoing)
            }
        }
        SettingsPanel(title = "Widget anheften") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SettingsActionPill(
                    label = "2×2 Widget anheften",
                    onClick = {
                        val mgr = AppWidgetManager.getInstance(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.isRequestPinAppWidgetSupported) {
                            mgr.requestPinAppWidget(ComponentName(context, CompactWidgetReceiver::class.java), null, null)
                        }
                    },
                    icon = Icons.Outlined.Widgets,
                    filled = true
                )
                SettingsActionPill(
                    label = "4×2 Widget anheften",
                    onClick = {
                        val mgr = AppWidgetManager.getInstance(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.isRequestPinAppWidgetSupported) {
                            mgr.requestPinAppWidget(ComponentName(context, WideWidgetReceiver::class.java), null, null)
                        }
                    },
                    icon = Icons.Outlined.Widgets
                )
                SettingsActionPill(
                    label = "5×2 Widget anheften",
                    onClick = {
                        val mgr = AppWidgetManager.getInstance(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.isRequestPinAppWidgetSupported) {
                            mgr.requestPinAppWidget(ComponentName(context, ExtraWideWidgetReceiver::class.java), null, null)
                        }
                    },
                    icon = Icons.Outlined.Widgets
                )
            }
        }
    }
}
