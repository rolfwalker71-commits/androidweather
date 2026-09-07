package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class MehrItem(val route: String, val label: String, val icon: ImageVector)

data class MehrGroup(val title: String, val items: List<MehrItem>)

val MEHR_GROUPS = listOf(
    MehrGroup(
        "Orte",
        listOf(
            MehrItem("favoriten", "Favoriten", Icons.Outlined.Star),
            MehrItem("vergleich", "Vergleich", Icons.AutoMirrored.Outlined.CompareArrows)
        )
    ),
    MehrGroup(
        "Wetter",
        listOf(
            MehrItem("radar", "Wetterradar", Icons.Outlined.Radar),
            MehrItem("pendeln", "Pendeln", Icons.Outlined.DirectionsBus),
            MehrItem("draussen", "Draußen", Icons.Outlined.Checkroom),
            MehrItem("wind", "Wind", Icons.Outlined.Air),
            MehrItem("berge", "Berge", Icons.Outlined.Terrain),
            MehrItem("seen", "Seen", Icons.Outlined.Water)
        )
    ),
    MehrGroup(
        "App",
        listOf(MehrItem("einstellungen", "Einstellungen", Icons.Outlined.Settings))
    )
)

@Composable
fun MehrMenu(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        MEHR_GROUPS.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    group.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                group.items.forEach { item ->
                    MehrListTile(
                        label = item.label,
                        icon = item.icon,
                        onClick = { onOpen(item.route) }
                    )
                }
            }
        }
    }
}
