package ch.rolf.androidweather.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import ch.rolf.androidweather.data.PrefsStore
import ch.rolf.androidweather.domain.Place
import ch.rolf.androidweather.ui.components.WxCard
import ch.rolf.androidweather.ui.theme.WetterTheme
import kotlinx.coroutines.runBlocking

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED)
        setContent {
            WetterTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    WidgetConfigScreen(
                        onPick = { source, favorite ->
                            runBlocking {
                                WidgetPrefs.save(this@WidgetConfigActivity, appWidgetId, source, favorite)
                                CompactWeatherWidget().updateAll(this@WidgetConfigActivity)
                                WideWeatherWidget().updateAll(this@WidgetConfigActivity)
                            }
                            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            setResult(RESULT_OK, result)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetConfigScreen(onPick: (WidgetPlaceSource, String?) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var favorites by remember { mutableStateOf<List<Place>>(emptyList()) }
    LaunchedEffect(Unit) { favorites = PrefsStore(context).favorites() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Widget-Ort", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Home, letzter Ort oder ein Favorit. Das Widget aktualisiert sich mit WorkManager.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        WxCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onPick(WidgetPlaceSource.Home, null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Home-Ort")
                }
                Button(onClick = { onPick(WidgetPlaceSource.Last, null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Letzter Ort")
                }
                favorites.forEach { place ->
                    Button(
                        onClick = { onPick(WidgetPlaceSource.Favorite, place.name) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(place.name) }
                }
            }
        }
    }
}
