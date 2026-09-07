package ch.rolf.androidweather.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ch.rolf.androidweather.data.PrefsStore
import ch.rolf.androidweather.domain.Place
import ch.rolf.androidweather.domain.WeatherBundle
import ch.rolf.androidweather.domain.formatHourLabel
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.nextPrecipLine
import ch.rolf.androidweather.domain.samePlace
import kotlinx.coroutines.flow.first

private val Context.widgetStore by preferencesDataStore("widget_config")

enum class WidgetPlaceSource { Home, Last, Favorite }

data class WidgetHour(
    val time: String,
    val temperature: String,
    val glyph: String
)

data class WidgetSnapshot(
    val placeName: String,
    val temperature: String,
    val condition: String,
    val rainLine: String?,
    val glyph: String,
    val highLow: String? = null,
    val hours: List<WidgetHour> = emptyList()
)

object WidgetPrefs {
    private fun sourceKey(appWidgetId: Int) = stringPreferencesKey("source_$appWidgetId")
    private fun favKey(appWidgetId: Int) = stringPreferencesKey("fav_$appWidgetId")

    suspend fun save(context: Context, appWidgetId: Int, source: WidgetPlaceSource, favoriteName: String? = null) {
        context.widgetStore.edit {
            it[sourceKey(appWidgetId)] = source.name
            if (favoriteName != null) it[favKey(appWidgetId)] = favoriteName else it.remove(favKey(appWidgetId))
        }
    }

    suspend fun load(context: Context, appWidgetId: Int): Pair<WidgetPlaceSource, String?> {
        val prefs = context.widgetStore.data.first()
        val source = prefs[sourceKey(appWidgetId)]?.let { runCatching { WidgetPlaceSource.valueOf(it) }.getOrNull() }
            ?: WidgetPlaceSource.Last
        return source to prefs[favKey(appWidgetId)]
    }
}

suspend fun loadWidgetSnapshot(context: Context, appWidgetId: Int): WidgetSnapshot {
    val prefs = PrefsStore(context)
    val (source, favName) = WidgetPrefs.load(context, appWidgetId)
    val place: Place? = when (source) {
        WidgetPlaceSource.Home -> prefs.home()
        WidgetPlaceSource.Last -> prefs.lastPlace() ?: prefs.home()
        WidgetPlaceSource.Favorite -> prefs.favorites().find { it.name == favName } ?: prefs.lastPlace()
    }
    val bundle = prefs.lastBundle()
    val used = if (bundle != null && place != null && samePlace(bundle.place, place)) bundle
    else bundle
    return snapshotFrom(used, place?.name ?: used?.place?.name ?: "Wetter")
}

fun snapshotFrom(bundle: WeatherBundle?, placeName: String, hourCount: Int = 5): WidgetSnapshot {
    if (bundle == null) {
        return WidgetSnapshot(placeName, "–", "Keine Daten", null, "cloud")
    }
    val wmo = getWmo(bundle.current.weather_code, bundle.current.is_day == 1)
    val today = bundle.days.firstOrNull()
    val hours = bundle.hours.take(hourCount).mapIndexed { index, hour ->
        WidgetHour(
            time = if (index == 0) "Jetzt" else formatHourLabel(hour.time, bundle.timezone),
            temperature = formatTemp(hour.temperature),
            glyph = getWmo(hour.code, hour.isDay).glyph
        )
    }
    return WidgetSnapshot(
        placeName = placeName,
        temperature = formatTemp(bundle.current.temperature_2m),
        condition = wmo.label,
        rainLine = nextPrecipLine(bundle),
        glyph = wmo.glyph,
        highLow = today?.let { "${formatTemp(it.tMin)} / ${formatTemp(it.tMax)}" },
        hours = hours
    )
}
