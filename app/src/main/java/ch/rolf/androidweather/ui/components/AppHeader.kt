package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.domain.Place
import ch.rolf.androidweather.domain.formatRefreshStatus
import ch.rolf.androidweather.ui.WetterUiState

@Composable
fun AppHeader(
    ui: WetterUiState,
    onMenu: () -> Unit,
    onLocate: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onSelectPlace: (Place) -> Unit,
    modifier: Modifier = Modifier,
    showSearch: Boolean = true,
    searchActive: Boolean = true
) {
    val bundle = ui.bundle
    val updated = when {
        bundle != null -> formatRefreshStatus(
            iso = bundle.fetchedAt,
            stale = ui.stale,
            offline = ui.error?.startsWith("Offline") == true,
            timeZone = bundle.timezone
        )
        else -> "Wetter"
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Menu, contentDescription = "Menü")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    updated,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Schweiz & Welt", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(onClick = onLocate, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.MyLocation, contentDescription = "Standort")
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Aktualisieren")
            }
        }
        if (showSearch) {
            CitySearch(
                results = ui.searchResults,
                onQuery = onSearch,
                onSelect = onSelectPlace,
                placeholder = "Stadt weltweit suchen",
                active = searchActive
            )
        }
    }
}
