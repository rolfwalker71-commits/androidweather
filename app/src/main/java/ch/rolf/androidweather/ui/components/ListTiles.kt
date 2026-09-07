package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val Stadium = RoundedCornerShape(50)
private val SoftPanel = RoundedCornerShape(28.dp)
private val SoftRow = RoundedCornerShape(20.dp)

private val MintCardLight = Color(0xFFECF4F4)
private val MintWellLight = Color(0xFFCCE8E9)
private val MintCardDark = Color(0xFF1E2021)
private val MintWellDark = Color(0xFF26292B)
private val MintOnLight = Color(0xFF161D1D)
private val MintOnDark = Color(0xFFECEFF1)
private val MintPrimaryLight = Color(0xFF006874)
private val MintPrimaryDark = Color(0xFF4DD8E8)
private val MintSecondaryLight = Color(0xFFCCE8E9)
private val MintSecondaryDark = Color(0xFF2A2E30)

@Composable
private fun mintSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
fun MehrListTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val dark = mintSurface()
    val tile = when {
        selected && dark -> MintSecondaryDark
        selected -> MintSecondaryLight
        dark -> MintCardDark
        else -> MintCardLight
    }
    val well = if (dark) MintWellDark else MintWellLight
    val on = when {
        selected && dark -> MintPrimaryDark
        selected -> MintPrimaryLight
        dark -> MintOnDark
        else -> MintOnLight
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(Stadium)
            .background(tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(well),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = on, modifier = Modifier.size(20.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = on,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SettingsPanel(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = mintSurface()
    Column(
        modifier
            .fillMaxWidth()
            .clip(SoftPanel)
            .background(if (dark) MintCardDark else MintCardLight)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = if (dark) MintOnDark else MintOnLight
        )
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = (if (dark) MintOnDark else MintOnLight).copy(alpha = 0.7f)
            )
        }
        content()
    }
}

@Composable
fun ChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val dark = mintSurface()
    val bg = when {
        selected && dark -> MintSecondaryDark
        selected -> MintSecondaryLight
        dark -> MintWellDark
        else -> Color.White
    }
    val on = when {
        selected && dark -> MintPrimaryDark
        selected -> MintPrimaryLight
        dark -> MintOnDark
        else -> MintOnLight
    }
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(Stadium)
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = on, modifier = Modifier.size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = on)
    }
}

@Composable
fun ThemeChoiceTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MehrListTile(label = label, icon = icon, onClick = onClick, modifier = modifier, selected = selected)
}

@Composable
fun SettingsToggleRow(
    label: String,
    hint: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit
) {
    val dark = mintSurface()
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(SoftRow)
            .background(if (dark) MintWellDark else MintWellLight)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (dark) MintOnDark else MintOnLight
            )
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = (if (dark) MintOnDark else MintOnLight).copy(alpha = 0.7f)
            )
        }
        trailing()
    }
}

@Composable
fun SettingsActionPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false
) {
    val dark = mintSurface()
    val bg = when {
        filled && dark -> MintSecondaryDark
        filled -> MintSecondaryLight
        dark -> MintWellDark
        else -> MintWellLight
    }
    val on = when {
        filled && dark -> MintPrimaryDark
        filled -> MintPrimaryLight
        dark -> MintOnDark
        else -> MintPrimaryLight
    }
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(Stadium)
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = on, modifier = Modifier.size(18.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = on)
    }
}
