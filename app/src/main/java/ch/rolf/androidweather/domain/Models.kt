package ch.rolf.androidweather.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val id: Long? = null,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val admin1: String? = null,
    val timezone: String? = null
)

val BERN = Place(
    id = 2661552,
    name = "Bern",
    latitude = 46.948,
    longitude = 7.4474,
    country = "Schweiz",
    countryCode = "CH",
    admin1 = "Bern",
    timezone = "Europe/Zurich"
)

@Serializable
data class ForecastCurrent(
    val time: String,
    val temperature_2m: Double,
    val relative_humidity_2m: Double,
    val apparent_temperature: Double,
    val weather_code: Int,
    val wind_speed_10m: Double,
    val wind_direction_10m: Double,
    val wind_gusts_10m: Double,
    val is_day: Int,
    val precipitation: Double,
    val pressure_msl: Double,
    val cloud_cover: Double
)

@Serializable
data class HourPoint(
    val time: String,
    val temperature: Double,
    val feelsLike: Double,
    val code: Int,
    val precipProb: Double? = null,
    val precipMm: Double = 0.0,
    val wind: Double,
    val humidity: Double,
    val isDay: Boolean,
    val uv: Double? = null,
    val windDir: Double? = null,
    val gusts: Double? = null,
    val cloud: Double? = null,
    val visibility: Double? = null,
    val cape: Double? = null,
    val freezingLevel: Double? = null,
    val snowfall: Double? = null,
    val dewPoint: Double? = null
)

@Serializable
data class MinutePoint(
    val time: String,
    val temperature: Double? = null,
    val precipMm: Double? = null,
    val wind: Double? = null,
    val windDir: Double? = null,
    val gusts: Double? = null,
    val cape: Double? = null,
    val code: Int? = null,
    val snowfall: Double? = null
)

@Serializable
data class DayPoint(
    val date: String,
    val code: Int,
    val tMax: Double,
    val tMin: Double,
    val precipMm: Double,
    val precipProb: Double? = null,
    val sunrise: String,
    val sunset: String,
    val uvMax: Double? = null,
    val windMax: Double
)

@Serializable
data class ElevationSnapshot(
    val elevation: Int,
    val temperature: Double? = null,
    val wind: Double? = null,
    val windDir: Double? = null,
    val humidity: Double? = null
)

@Serializable
data class LakeSnapshot(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val waterTemp: Double? = null,
    val waveHeight: Double? = null,
    val tempSource: String? = null
)

@Serializable
data class AirTrendPoint(
    val time: String,
    val aqi: Double? = null,
    val pm25: Double? = null
)

@Serializable
data class StationObservation(
    val id: String,
    val name: String,
    val temperature: Double,
    val observedAt: String? = null,
    val distanceKm: Double,
    val source: String
)

@Serializable
data class PassObservation(
    val id: String,
    val name: String,
    val temperature: Double? = null,
    val windKmh: Double? = null,
    val windDir: Double? = null,
    val observedAt: String? = null,
    val distanceKm: Double
)

@Serializable
data class AirQualityCurrent(
    val time: String? = null,
    val european_aqi: Double? = null,
    val pm2_5: Double? = null,
    val pm10: Double? = null,
    val uv_index: Double? = null
)

@Serializable
data class PollenValues(
    val alder: Double? = null,
    val birch: Double? = null,
    val grass: Double? = null
)

@Serializable
data class AlertItem(
    val id: String,
    val event: String,
    val headline: String,
    val severity: String,
    val onset: String? = null,
    val expires: String? = null,
    val area: String? = null,
    val source: String
)

@Serializable
data class AvalancheBulletin(
    val available: Boolean,
    val level: Int? = null,
    val label: String,
    val validUntil: String? = null,
    val source: String,
    val note: String
)

@Serializable
data class WeatherBundle(
    val place: Place,
    val timezone: String,
    val current: ForecastCurrent,
    val hours: List<HourPoint>,
    val allHours: List<HourPoint> = emptyList(),
    val minutes: List<MinutePoint> = emptyList(),
    val days: List<DayPoint> = emptyList(),
    val air: AirQualityCurrent? = null,
    val pollen: PollenValues = PollenValues(),
    val airTrend: List<AirTrendPoint> = emptyList(),
    val elevations: List<ElevationSnapshot> = emptyList(),
    val lakes: List<LakeSnapshot> = emptyList(),
    val station: StationObservation? = null,
    val fetchedAt: String
)

@Serializable
data class NotifyPrefs(
    val rainSoon: Boolean = false,
    val warnings: Boolean = false,
    val frost: Boolean = false,
    val uv: Boolean = false,
    val air: Boolean = false,
    val dailyBrief: Boolean = false,
    val forecastChange: Boolean = false
)

enum class ThemePreference { Light, Dark, System }
enum class WindUnit { Kmh, Ms }

@Serializable
data class ForecastSnapshot(
    val dayKey: String,
    val precipOnsetIso: String? = null,
    val precipNext6Mm: Double,
    val maxWindKmh: Double? = null,
    val maxGustKmh: Double? = null,
    val todayMax: Double? = null,
    val todayMin: Double? = null,
    val todayCode: Int? = null
)

data class ProactivityChange(
    val kind: String,
    val fingerprint: String,
    val detail: String,
    val title: String,
    val body: String
)

@Serializable
data class ProactivityNotice(
    val placeKey: String,
    val line: String,
    val fingerprint: String,
    val at: String
)

data class RadarFrame(val time: Long, val path: String, val kind: String)
data class SatelliteFrame(val time: Long, val path: String)
data class RadarCatalog(val host: String, val frames: List<RadarFrame>, val infrared: List<SatelliteFrame>)

data class ScaleLevel(val label: String, val hint: String, val ratio: Float)

const val MAX_FAVORITES = 8
