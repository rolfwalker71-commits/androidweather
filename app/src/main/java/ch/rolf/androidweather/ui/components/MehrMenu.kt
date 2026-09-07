package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon

data class MehrItem(val route: String, val label: String, val icon: ImageVector)

data class MehrGroup(val title: String, val items: List<MehrItem>)

val MEHR_GROUPS = listOf(
    MehrGroup(
        "Orte",
        listOf(
            MehrItem("favoriten", "Favoriten", Icons.Outlined.Star),
            MehrItem("vergleich", "Vergleich", Icons.Outlined.CompareArrows)
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        MEHR_GROUPS.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    group.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                group.items.forEach { item ->
                    WxCard(onClick = { onOpen(item.route) }) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(item.label, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
