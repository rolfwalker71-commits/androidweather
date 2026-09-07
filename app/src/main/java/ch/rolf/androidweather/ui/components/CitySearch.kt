package ch.rolf.androidweather.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.rolf.androidweather.domain.Place
import ch.rolf.androidweather.domain.placeLabel

@Composable
fun CitySearch(
    results: List<Place>,
    onQuery: (String) -> Unit,
    onSelect: (Place) -> Unit,
    placeholder: String = "Stadt weltweit suchen",
    active: Boolean = true,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onQuery(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )
        if (active && query.isNotBlank()) {
            results.take(8).forEach { place ->
                Text(
                    text = placeLabel(place),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable {
                            onSelect(place)
                            query = ""
                            onQuery("")
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
