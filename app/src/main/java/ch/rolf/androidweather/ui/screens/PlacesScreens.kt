package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import ch.rolf.androidweather.domain.samePlace
import ch.rolf.androidweather.ui.WetterViewModel
import ch.rolf.androidweather.ui.components.ChoicePill
import ch.rolf.androidweather.ui.components.CitySearch
import ch.rolf.androidweather.ui.components.CurrentHero
import ch.rolf.androidweather.ui.components.PlaceWeatherCard
import ch.rolf.androidweather.ui.components.SettingsActionPill
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VergleichScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val unit by vm.windUnit.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    var slot by remember { mutableStateOf("a") }
    LaunchedEffect(Unit) { vm.loadCompare() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Vergleich", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Zwei Orte nebeneinander — gleiche Karten wie Jetzt, kompakter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoicePill(label = "Ort A", selected = slot == "a", onClick = { slot = "a" })
            ChoicePill(label = "Ort B", selected = slot == "b", onClick = { slot = "b" })
        }
        if (favs.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                favs.forEach { place ->
                    val selected = when (slot) {
                        "a" -> ui.compareA?.place?.let { samePlace(it, place) } == true
                        else -> ui.compareB?.place?.let { samePlace(it, place) } == true
                    }
                    ChoicePill(
                        label = place.name,
                        selected = selected,
                        onClick = {
                            val a = if (slot == "a") place else ui.compareA?.place
                            val b = if (slot == "b") place else ui.compareB?.place
                            vm.compare(a, b)
                        }
                    )
                }
            }
        }
        CitySearch(
            results = ui.searchResults,
            onQuery = vm::search,
            onSelect = { place ->
                val a = if (slot == "a") place else ui.compareA?.place
                val b = if (slot == "b") place else ui.compareB?.place
                vm.compare(a, b)
            },
            placeholder = if (slot == "a") "Ort A suchen" else "Ort B suchen"
        )
        PlaceWeatherCard(
            title = "Ort A",
            bundle = ui.compareA,
            emptyText = "Ort wählen — Favorit oder Suche.",
            windUnit = unit
        )
        PlaceWeatherCard(
            title = "Ort B",
            bundle = ui.compareB,
            emptyText = "Ort wählen — Favorit oder Suche.",
            windUnit = unit
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendelnScreen(vm: WetterViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val destPlace by vm.commutePlace.collectAsStateWithLifecycle()
    val favs by vm.favorites.collectAsStateWithLifecycle()
    val unit by vm.windUnit.collectAsStateWithLifecycle()
    LaunchedEffect(destPlace) { vm.loadCommute() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pendeln", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Wetter am Start und am Ziel — Favoriten oder Suche.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (favs.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                favs.forEach { place ->
                    if (samePlace(place, ui.place)) return@forEach
                    ChoicePill(
                        label = place.name,
                        selected = destPlace?.let { samePlace(it, place) } == true,
                        onClick = { vm.setCommute(place) }
                    )
                }
            }
        }
        CitySearch(
            results = ui.searchResults,
            onQuery = vm::search,
            onSelect = { vm.setCommute(it) },
            placeholder = "Zielort suchen"
        )
        PlaceWeatherCard(
            title = "Start",
            bundle = ui.bundle,
            emptyText = "Aktueller Ort wird geladen…",
            windUnit = unit
        )
        PlaceWeatherCard(
            title = "Ziel",
            bundle = ui.commuteDest,
            emptyText = destPlace?.name?.let { "Wetter für $it wird geladen…" } ?: "Ziel wählen — Favorit oder Suche.",
            windUnit = unit
        )
        val home = ui.bundle
        val dest = ui.commuteDest
        if (home != null && dest != null) {
            commuteHint(home, dest)?.let { hint ->
                WxCard {
                    Text("Hinweis", style = MaterialTheme.typography.titleLarge)
                    Text(hint, style = MaterialTheme.typography.bodyLarge)
                }
            }
            SettingsActionPill(label = "Ziel entfernen", onClick = { vm.setCommute(null) })
        }
    }
}
