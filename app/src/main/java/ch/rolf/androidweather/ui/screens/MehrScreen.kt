package ch.rolf.androidweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.ui.components.WxCard

@Composable
fun MehrScreen(onOpen: (String) -> Unit) {
    val groups = listOf(
        "Orte" to listOf("favoriten" to "Favoriten", "vergleich" to "Vergleich"),
        "Wetter" to listOf(
            "radar" to "Wetterradar",
            "pendeln" to "Pendeln",
            "draussen" to "Draußen",
            "wind" to "Wind",
            "berge" to "Berge",
            "seen" to "Seen"
        ),
        "App" to listOf("einstellungen" to "Einstellungen")
    )
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Mehr", style = MaterialTheme.typography.headlineSmall)
        groups.forEach { (title, items) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                items.forEach { (route, label) ->
                    WxCard(onClick = { onOpen(route) }) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
