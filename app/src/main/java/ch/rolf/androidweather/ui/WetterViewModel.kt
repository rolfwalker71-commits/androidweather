package ch.rolf.androidweather.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.rolf.androidweather.data.LocationClient
import ch.rolf.androidweather.data.PrefsStore
import ch.rolf.androidweather.data.WeatherRepository
import ch.rolf.androidweather.domain.AlertItem
import ch.rolf.androidweather.domain.AvalancheBulletin
import ch.rolf.androidweather.domain.BERN
import ch.rolf.androidweather.domain.NotifyPrefs
import ch.rolf.androidweather.domain.PassObservation
import ch.rolf.androidweather.domain.Place
import ch.rolf.androidweather.domain.ProactivityNotice
import ch.rolf.androidweather.domain.RadarCatalog
import ch.rolf.androidweather.domain.ThemePreference
import ch.rolf.androidweather.domain.WeatherBundle
import ch.rolf.androidweather.domain.WindUnit
import ch.rolf.androidweather.domain.buildForecastSnapshot
import ch.rolf.androidweather.domain.diffForecastSnapshots
import ch.rolf.androidweather.domain.heroProactivityLine
import ch.rolf.androidweather.domain.placeKey
import ch.rolf.androidweather.domain.samePlace
import ch.rolf.androidweather.notify.WetterNotifications
import ch.rolf.androidweather.work.WeatherRefreshWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class WetterUiState(
    val place: Place = BERN,
    val bundle: WeatherBundle? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val alerts: List<AlertItem> = emptyList(),
    val avalanche: AvalancheBulletin? = null,
    val passes: List<PassObservation> = emptyList(),
    val notice: ProactivityNotice? = null,
    val searchResults: List<Place> = emptyList(),
    val radar: RadarCatalog? = null,
    val compareA: WeatherBundle? = null,
    val compareB: WeatherBundle? = null,
    val commuteDest: WeatherBundle? = null,
    val favoriteBundles: List<WeatherBundle> = emptyList()
)

class WetterViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = PrefsStore(app)
    private val repo = WeatherRepository()
    private val location = LocationClient(app)

    private val _state = MutableStateFlow(WetterUiState())
    val state: StateFlow<WetterUiState> = _state

    val favorites = prefs.favoritesFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val home = prefs.homeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val windUnit = prefs.windUnitFlow.stateIn(viewModelScope, SharingStarted.Eagerly, WindUnit.Kmh)
    val theme = prefs.themeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.System)
    val notifyPrefs = prefs.notifyPrefsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, NotifyPrefs())
    val ongoing = prefs.ongoingFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val commutePlace = prefs.commuteFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        val cached = prefs.lastBundle()
        val place = prefs.resolveActivePlace()
        if (cached != null) {
            _state.value = _state.value.copy(place = cached.place, bundle = cached, loading = false)
        }
        refresh(place)
    }

    fun refresh(place: Place = _state.value.place) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, place = place)
            runCatching { repo.fetchWeather(place) }
                .onSuccess { bundle ->
                    prefs.saveLastPlace(place)
                    prefs.saveLastBundle(bundle)
                    val previous = prefs.loadSnapshot(place)
                    val snap = buildForecastSnapshot(bundle)
                    val change = diffForecastSnapshots(previous, snap, bundle.timezone)
                    prefs.saveSnapshot(place, snap)
                    val notice = if (change != null) {
                        val n = ch.rolf.androidweather.domain.ProactivityNotice(
                            placeKey(place), heroProactivityLine(change) ?: change.detail, change.fingerprint, Instant.now().toString()
                        )
                        prefs.saveNotice(n)
                        n
                    } else prefs.loadNotice(place)
                    _state.value = _state.value.copy(bundle = bundle, loading = false, notice = notice, error = null)
                    launch { loadSwissExtras(place) }
                    if (prefs.ongoing()) WetterNotifications.showOngoing(getApplication(), bundle)
                    WeatherRefreshWorker.enqueue(getApplication())
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Wetter konnte nicht geladen werden."
                    )
                }
        }
    }

    private suspend fun loadSwissExtras(place: Place) {
        val alerts = runCatching { repo.fetchAlerts(place) }.getOrDefault(emptyList())
        val avalanche = runCatching { repo.fetchAvalanche(place) }.getOrNull()
        val passes = runCatching { repo.fetchPasses(place) }.getOrDefault(emptyList())
        _state.value = _state.value.copy(alerts = alerts, avalanche = avalanche, passes = passes)
    }

    fun search(query: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(searchResults = runCatching { repo.searchPlaces(query) }.getOrDefault(emptyList()))
        }
    }

    fun selectPlace(place: Place) {
        _state.value = _state.value.copy(searchResults = emptyList())
        refresh(place)
    }

    fun locate() {
        viewModelScope.launch {
            val loc = location.lastOrCurrent() ?: return@launch
            val place = repo.reverseGeocode(loc.latitude, loc.longitude)
            selectPlace(place)
        }
    }

    fun hasLocationPermission(): Boolean = location.hasPermission()

    fun toggleFavorite(place: Place) {
        viewModelScope.launch { prefs.toggleFavorite(place) }
    }

    fun isFavorite(place: Place): Boolean = favorites.value.any { samePlace(it, place) }

    fun setHome(place: Place) {
        viewModelScope.launch { prefs.saveHome(place) }
    }

    fun setWindUnit(unit: WindUnit) {
        viewModelScope.launch { prefs.saveWindUnit(unit) }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { prefs.saveTheme(theme) }
    }

    fun setNotifyPrefs(prefsValue: NotifyPrefs) {
        viewModelScope.launch { this@WetterViewModel.prefs.saveNotifyPrefs(prefsValue) }
    }

    fun setOngoing(enabled: Boolean) {
        viewModelScope.launch {
            prefs.saveOngoing(enabled)
            val bundle = _state.value.bundle
            if (enabled && bundle != null) WetterNotifications.showOngoing(getApplication(), bundle)
            else WetterNotifications.cancelOngoing(getApplication())
        }
    }

    fun setCommute(place: Place?) {
        viewModelScope.launch {
            prefs.saveCommute(place)
            if (place != null) {
                _state.value = _state.value.copy(
                    commuteDest = runCatching { repo.fetchWeather(place, lite = true) }.getOrNull()
                )
            } else {
                _state.value = _state.value.copy(commuteDest = null)
            }
        }
    }

    fun loadRadar() {
        viewModelScope.launch {
            _state.value = _state.value.copy(radar = runCatching { repo.fetchRadar() }.getOrNull())
        }
    }

    fun loadFavoritesWeather() {
        viewModelScope.launch {
            val places = prefs.favorites()
            val bundles = places.mapNotNull { runCatching { repo.fetchWeather(it, lite = true) }.getOrNull() }
            _state.value = _state.value.copy(favoriteBundles = bundles)
        }
    }

    fun compare(a: Place?, b: Place?) {
        viewModelScope.launch {
            prefs.saveCompare(a, b)
            _state.value = _state.value.copy(
                compareA = a?.let { runCatching { repo.fetchWeather(it, lite = true) }.getOrNull() },
                compareB = b?.let { runCatching { repo.fetchWeather(it, lite = true) }.getOrNull() }
            )
        }
    }

    fun loadCommute() {
        viewModelScope.launch {
            val dest = prefs.commute() ?: return@launch
            _state.value = _state.value.copy(
                commuteDest = runCatching { repo.fetchWeather(dest, lite = true) }.getOrNull()
            )
        }
    }
}
