package ch.rolf.androidweather.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ch.rolf.androidweather.domain.BERN
import ch.rolf.androidweather.domain.ForecastSnapshot
import ch.rolf.androidweather.domain.NotifyPrefs
import ch.rolf.androidweather.domain.Place
import ch.rolf.androidweather.domain.ProactivityNotice
import ch.rolf.androidweather.domain.ThemePreference
import ch.rolf.androidweather.domain.WeatherBundle
import ch.rolf.androidweather.domain.WindUnit
import ch.rolf.androidweather.domain.placeKey
import ch.rolf.androidweather.domain.samePlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wetter")

class PrefsStore(private val context: Context) {
    private val json = AppJson

    private val homeKey = stringPreferencesKey("home_place")
    private val lastKey = stringPreferencesKey("last_place")
    private val favKey = stringPreferencesKey("favorites")
    private val commuteKey = stringPreferencesKey("commute_dest")
    private val windKey = stringPreferencesKey("wind_unit")
    private val themeKey = stringPreferencesKey("theme")
    private val notifyKey = stringPreferencesKey("notify_prefs")
    private val ongoingKey = booleanPreferencesKey("ongoing_notification")
    private val snapshotKey = stringPreferencesKey("forecast_snapshots")
    private val noticeKey = stringPreferencesKey("proactivity_notice")
    private val lastBundleKey = stringPreferencesKey("last_bundle")
    private val cooldownKey = stringPreferencesKey("notify_cooldowns")
    private val compareAKey = stringPreferencesKey("compare_a")
    private val compareBKey = stringPreferencesKey("compare_b")

    val homeFlow: Flow<Place?> = context.dataStore.data.map { it[homeKey]?.let(::decodePlace) }
    val lastPlaceFlow: Flow<Place?> = context.dataStore.data.map { it[lastKey]?.let(::decodePlace) }
    val favoritesFlow: Flow<List<Place>> = context.dataStore.data.map { it[favKey]?.let(::decodePlaces) ?: emptyList() }
    val commuteFlow: Flow<Place?> = context.dataStore.data.map { it[commuteKey]?.let(::decodePlace) }
    val windUnitFlow: Flow<WindUnit> = context.dataStore.data.map {
        if (it[windKey] == "ms") WindUnit.Ms else WindUnit.Kmh
    }
    val themeFlow: Flow<ThemePreference> = context.dataStore.data.map {
        when (it[themeKey]) {
            "light" -> ThemePreference.Light
            "dark" -> ThemePreference.Dark
            else -> ThemePreference.System
        }
    }
    val notifyPrefsFlow: Flow<NotifyPrefs> = context.dataStore.data.map {
        it[notifyKey]?.let { raw -> runCatching { json.decodeFromString<NotifyPrefs>(raw) }.getOrNull() }
            ?: NotifyPrefs()
    }
    val ongoingFlow: Flow<Boolean> = context.dataStore.data.map { it[ongoingKey] ?: false }
    val lastBundleFlow: Flow<WeatherBundle?> = context.dataStore.data.map {
        it[lastBundleKey]?.let { raw -> runCatching { json.decodeFromString<WeatherBundle>(raw) }.getOrNull() }
    }

    suspend fun resolveActivePlace(): Place {
        val prefs = context.dataStore.data.first()
        return prefs[lastKey]?.let(::decodePlace)
            ?: prefs[homeKey]?.let(::decodePlace)
            ?: BERN
    }

    suspend fun saveHome(place: Place) {
        context.dataStore.edit { it[homeKey] = json.encodeToString(place) }
    }

    suspend fun saveLastPlace(place: Place) {
        context.dataStore.edit { it[lastKey] = json.encodeToString(place) }
    }

    suspend fun saveFavorites(places: List<Place>) {
        context.dataStore.edit { it[favKey] = json.encodeToString(places.take(8)) }
    }

    suspend fun toggleFavorite(place: Place): List<Place> {
        val current = context.dataStore.data.first()[favKey]?.let(::decodePlaces) ?: emptyList()
        val next = if (current.any { samePlace(it, place) }) {
            current.filterNot { samePlace(it, place) }
        } else {
            (listOf(place) + current).take(8)
        }
        saveFavorites(next)
        return next
    }

    suspend fun saveCommute(place: Place?) {
        context.dataStore.edit {
            if (place == null) it.remove(commuteKey) else it[commuteKey] = json.encodeToString(place)
        }
    }

    suspend fun saveWindUnit(unit: WindUnit) {
        context.dataStore.edit { it[windKey] = if (unit == WindUnit.Ms) "ms" else "kmh" }
    }

    suspend fun saveTheme(theme: ThemePreference) {
        context.dataStore.edit {
            it[themeKey] = when (theme) {
                ThemePreference.Light -> "light"
                ThemePreference.Dark -> "dark"
                ThemePreference.System -> "system"
            }
        }
    }

    suspend fun saveNotifyPrefs(prefs: NotifyPrefs) {
        context.dataStore.edit { it[notifyKey] = json.encodeToString(prefs) }
    }

    suspend fun saveOngoing(enabled: Boolean) {
        context.dataStore.edit { it[ongoingKey] = enabled }
    }

    suspend fun saveLastBundle(bundle: WeatherBundle) {
        context.dataStore.edit { it[lastBundleKey] = json.encodeToString(bundle) }
    }

    suspend fun loadSnapshot(place: Place): ForecastSnapshot? {
        val map = decodeSnapshotMap()
        return map[placeKey(place)]
    }

    suspend fun saveSnapshot(place: Place, snapshot: ForecastSnapshot) {
        val map = decodeSnapshotMap().toMutableMap()
        map[placeKey(place)] = snapshot
        val keys = map.keys.toList()
        if (keys.size > 12) keys.take(keys.size - 12).forEach { map.remove(it) }
        context.dataStore.edit { it[snapshotKey] = json.encodeToString(map) }
    }

    suspend fun loadNotice(place: Place): ProactivityNotice? {
        val raw = context.dataStore.data.first()[noticeKey] ?: return null
        val notice = runCatching { json.decodeFromString<ProactivityNotice>(raw) }.getOrNull() ?: return null
        if (notice.placeKey != placeKey(place)) return null
        val age = System.currentTimeMillis() - runCatching {
            java.time.Instant.parse(notice.at).toEpochMilli()
        }.getOrDefault(0L)
        return notice.takeIf { age in 0..(6 * 60 * 60 * 1000) }
    }

    suspend fun saveNotice(notice: ProactivityNotice) {
        context.dataStore.edit { it[noticeKey] = json.encodeToString(notice) }
    }

    suspend fun loadCooldowns(): Map<String, Long> {
        val raw = context.dataStore.data.first()[cooldownKey] ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Long>>(raw) }.getOrDefault(emptyMap())
    }

    suspend fun saveCooldowns(map: Map<String, Long>) {
        context.dataStore.edit { it[cooldownKey] = json.encodeToString(map) }
    }

    suspend fun saveCompare(a: Place?, b: Place?) {
        context.dataStore.edit {
            if (a != null) it[compareAKey] = json.encodeToString(a) else it.remove(compareAKey)
            if (b != null) it[compareBKey] = json.encodeToString(b) else it.remove(compareBKey)
        }
    }

    suspend fun loadCompare(): Pair<Place?, Place?> {
        val prefs = context.dataStore.data.first()
        return prefs[compareAKey]?.let(::decodePlace) to prefs[compareBKey]?.let(::decodePlace)
    }

    suspend fun home(): Place? = context.dataStore.data.first()[homeKey]?.let(::decodePlace)
    suspend fun lastPlace(): Place? = context.dataStore.data.first()[lastKey]?.let(::decodePlace)
    suspend fun favorites(): List<Place> = context.dataStore.data.first()[favKey]?.let(::decodePlaces) ?: emptyList()
    suspend fun commute(): Place? = context.dataStore.data.first()[commuteKey]?.let(::decodePlace)
    suspend fun windUnit(): WindUnit =
        if (context.dataStore.data.first()[windKey] == "ms") WindUnit.Ms else WindUnit.Kmh
    suspend fun theme(): ThemePreference = when (context.dataStore.data.first()[themeKey]) {
        "light" -> ThemePreference.Light
        "dark" -> ThemePreference.Dark
        else -> ThemePreference.System
    }
    suspend fun notifyPrefs(): NotifyPrefs =
        context.dataStore.data.first()[notifyKey]?.let { runCatching { json.decodeFromString<NotifyPrefs>(it) }.getOrNull() }
            ?: NotifyPrefs()
    suspend fun ongoing(): Boolean = context.dataStore.data.first()[ongoingKey] ?: false
    suspend fun lastBundle(): WeatherBundle? =
        context.dataStore.data.first()[lastBundleKey]?.let { runCatching { json.decodeFromString<WeatherBundle>(it) }.getOrNull() }

    private suspend fun decodeSnapshotMap(): Map<String, ForecastSnapshot> {
        val raw = context.dataStore.data.first()[snapshotKey] ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, ForecastSnapshot>>(raw) }.getOrDefault(emptyMap())
    }

    private fun decodePlace(raw: String): Place? =
        runCatching { json.decodeFromString<Place>(raw) }.getOrNull()

    private fun decodePlaces(raw: String): List<Place> =
        runCatching { json.decodeFromString<List<Place>>(raw) }.getOrDefault(emptyList())
}
