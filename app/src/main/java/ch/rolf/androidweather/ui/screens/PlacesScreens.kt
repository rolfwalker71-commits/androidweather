package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.rolf.androidweather.domain.Place
import ch.rolf.androidweather.domain.commuteHint
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.placeShort
import ch.rolf.androidweather.domain.samePlace
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.CitySearch
import ch.rolf.androidweather.ui.components.CurrentHero
import ch.rolf.androidweather.ui.components.WxCard

@Composable
fun FavoritenScreen(vm: WetterViewModel, onOpenPlace: (Place) -> Unit) {
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val ui by vm.state.collectAsStateWithLifecycle()
    val unit by vm.windUnit.collectAsStateWithLifecycle()
    LaunchedEffect(favs) { vm.loadFavoritesWeather() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Favoriten", style = MaterialTheme.typography.headlineSmall)
        if (favs.isEmpty()) {
            WxCard {
                Text("Keine Favoriten", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Noch keine Orte gespeichert. Öffne Jetzt und tippe auf den Stern unten rechts in der Hauptkarte.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            favs.forEach { place ->
                val bundle = ui.favoriteBundles.find { samePlace(it.place, place) }
                if (bundle != null) {
                    CurrentHero(
                        place = place,
                        bundle = bundle,
                        stale = false,
                        offline = false,
                        favored = true,
                        windUnit = unit,
                        notice = null,
                        onToggleFavorite = { vm.toggleFavorite(place) },
                        onOpen = { onOpenPlace(place) }
                    )
                } else {
                    WxCard(onClick = { onOpenPlace(place) }) {
                        Text(place.name, style = MaterialTheme.typography.headlineSmall)
                        Text("Wetter wird geladen…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun VergleichScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    var slot by remember { mutableStateOf("a") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Vergleich", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = slot == "a", onClick = { slot = "a" }, label = { Text("Ort A") })
            FilterChip(selected = slot == "b", onClick = { slot = "b" }, label = { Text("Ort B") })
        }
        CitySearch(
            results = ui.searchResults,
            onQuery = vm::search,
            onSelect = { place ->
                val a = if (slot == "a") place else ui.compareA?.place
                val b = if (slot == "b") place else ui.compareB?.place
                vm.compare(a, b)
            }
        )
        listOf("A" to ui.compareA, "B" to ui.compareB).forEach { (label, bundle) ->
            WxCard {
                Text("Ort $label", style = MaterialTheme.typography.titleLarge)
                if (bundle == null) Text("Ort wählen")
                else {
                    Text(placeShort(bundle.place))
                    Text(formatTemp(bundle.current.temperature_2m), style = MaterialTheme.typography.headlineMedium)
                    Text(getWmo(bundle.current.weather_code, bundle.current.is_day == 1).label)
                    Text("Wind ${bundle.current.wind_speed_10m.toInt()} km/h · ${bundle.current.relative_humidity_2m.toInt()} %")
                }
            }
        }
    }
}

@Composable
fun PendelnScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val destPlace by vm.commutePlace.collectAsStateWithLifecycle()
    LaunchedEffect(destPlace) { vm.loadCommute() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pendeln", style = MaterialTheme.typography.headlineSmall)
        Text("Zielort mit Lite-Prognose gegenüber dem aktuellen Ort.")
        CitySearch(
            results = ui.searchResults,
            onQuery = vm::search,
            onSelect = { vm.setCommute(it) },
            placeholder = "Ziel suchen"
        )
        destPlace?.let { Text("Ziel: ${placeShort(it)}") }
        val home = ui.bundle
        val dest = ui.commuteDest
        if (home != null) {
            WxCard {
                Text("Start · ${placeShort(home.place)}", style = MaterialTheme.typography.titleLarge)
                Text("${formatTemp(home.current.temperature_2m)} · ${getWmo(home.current.weather_code, home.current.is_day == 1).label}")
            }
        }
        if (dest != null) {
            WxCard {
                Text("Ziel · ${placeShort(dest.place)}", style = MaterialTheme.typography.titleLarge)
                Text("${formatTemp(dest.current.temperature_2m)} · ${getWmo(dest.current.weather_code, dest.current.is_day == 1).label}")
            }
        }
        if (home != null && dest != null) {
            commuteHint(home, dest)?.let { WxCard { Text(it) } }
        }
    }
}
