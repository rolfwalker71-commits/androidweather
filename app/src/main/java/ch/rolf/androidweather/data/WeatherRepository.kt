package ch.rolf.androidweather.data

import ch.rolf.androidweather.domain.AirQualityCurrent
import ch.rolf.androidweather.domain.AirTrendPoint
import ch.rolf.androidweather.domain.AlertItem
import ch.rolf.androidweather.domain.AvalancheBulletin
import ch.rolf.androidweather.domain.DayPoint
import ch.rolf.androidweather.domain.ElevationSnapshot
import ch.rolf.androidweather.domain.ForecastCurrent
import ch.rolf.androidweather.domain.HourPoint
import ch.rolf.androidweather.domain.LakeSnapshot
import ch.rolf.androidweather.domain.MinutePoint
import ch.rolf.androidweather.domain.PassObservation
import ch.rolf.androidweather.domain.Place
import ch.rolf.androidweather.domain.PollenValues
import ch.rolf.androidweather.domain.RadarCatalog
import ch.rolf.androidweather.domain.StationObservation
import ch.rolf.androidweather.domain.WeatherBundle
import ch.rolf.androidweather.domain.nearestLakes
import ch.rolf.androidweather.domain.toLakeSnapshot
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

class WeatherRepository(
    private val client: HttpClient = createHttpClient()
) {
    private val extras = WeatherExtras(client)

    suspend fun searchPlaces(query: String): List<Place> {
        val name = query.trim()
        if (name.length < 2) return emptyList()
        val text = getText("https://geocoding-api.open-meteo.com/v1/search") {
            parameter("name", name)
            parameter("count", 8)
            parameter("language", "de")
            parameter("format", "json")
        } ?: return emptyList()
        val results = AppJson.parseToJsonElement(text).jsonObject["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { el ->
            val o = el.jsonObject
            Place(
                id = o.long("id"),
                name = o.str("name") ?: return@mapNotNull null,
                latitude = o.dbl("latitude") ?: return@mapNotNull null,
                longitude = o.dbl("longitude") ?: return@mapNotNull null,
                country = o.str("country"),
                countryCode = o.str("country_code"),
                admin1 = o.str("admin1"),
                timezone = o.str("timezone")
            )
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Place {
        val text = getText("https://api.bigdatacloud.net/data/reverse-geocode-client") {
            parameter("latitude", lat)
            parameter("longitude", lon)
            parameter("localityLanguage", "de")
        }
        if (text == null) return Place(name = "Aktueller Standort", latitude = lat, longitude = lon)
        val o = AppJson.parseToJsonElement(text).jsonObject
        val name = o.str("city") ?: o.str("locality") ?: "Aktueller Standort"
        return Place(
            name = name,
            latitude = lat,
            longitude = lon,
            admin1 = o.str("principalSubdivision"),
            country = o.str("countryName"),
            countryCode = o.str("countryCode")
        )
    }

    suspend fun fetchWeather(place: Place, lite: Boolean = false): WeatherBundle = coroutineScope {
        val forecastDef = async { fetchForecast(place) }
        val airDef = async { if (lite) null else runCatching { fetchAir(place) }.getOrNull() }
        val forecast = forecastDef.await()
        val extrasJob = async {
            if (lite) Triple(emptyList<ElevationSnapshot>(), emptyList<LakeSnapshot>(), null)
            else Triple(
                runCatching { fetchElevations(place) }.getOrDefault(emptyList()),
                runCatching { fetchLakes(place) }.getOrDefault(emptyList()),
                runCatching { extras.fetchNearestStation(place) }.getOrNull()
            )
        }
        val (elevations, lakes, station) = extrasJob.await()
        buildBundle(place, forecast, airDef.await(), elevations, lakes, station)
    }

    suspend fun fetchAlerts(place: Place): List<AlertItem> =
        runCatching { extras.fetchMeteoalarm(place) }.getOrDefault(emptyList())

    suspend fun fetchAvalanche(place: Place): AvalancheBulletin =
        runCatching { extras.fetchAvalanche(place.latitude, place.longitude) }.getOrDefault(
            AvalancheBulletin(false, null, "nicht verfügbar", null, "SLF", "Kein öffentlicher SLF-Feed erreichbar — keine Schätzwerte.")
        )

    suspend fun fetchPasses(place: Place): List<PassObservation> =
        runCatching { extras.fetchPassObservations(place.latitude, place.longitude) }.getOrDefault(emptyList())

    suspend fun fetchRadar(): RadarCatalog = extras.fetchRadarCatalog()

    private suspend fun fetchForecast(place: Place): JsonObject {
        val url = forecastUrl(place, includeMinutes = true)
        val text = getText(url) ?: error("Forecast fehlgeschlagen")
        val parsed = AppJson.parseToJsonElement(text).jsonObject
        if (parsed["current"] == null) {
            val fallback = getText(forecastUrl(place, includeMinutes = false)) ?: error("Forecast fehlgeschlagen")
            return AppJson.parseToJsonElement(fallback).jsonObject
        }
        return parsed
    }

    private fun forecastUrl(place: Place, includeMinutes: Boolean): String {
        val b = StringBuilder("https://api.open-meteo.com/v1/forecast?")
        b.append("latitude=${place.latitude}&longitude=${place.longitude}")
        b.append("&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,is_day,precipitation,pressure_msl,cloud_cover")
        b.append("&hourly=temperature_2m,weather_code,precipitation_probability,precipitation,wind_speed_10m,wind_direction_10m,wind_gusts_10m,is_day,relative_humidity_2m,apparent_temperature,uv_index,cloud_cover,visibility,cape,freezing_level_height,snowfall,dew_point_2m")
        if (includeMinutes) {
            b.append("&minutely_15=temperature_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,cape,snowfall")
        }
        b.append("&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,sunrise,sunset,uv_index_max,wind_speed_10m_max")
        b.append("&timezone=auto&forecast_days=10&wind_speed_unit=kmh&models=best_match")
        return b.toString()
    }

    private suspend fun fetchAir(place: Place): JsonObject {
        val text = getText("https://air-quality-api.open-meteo.com/v1/air-quality") {
            parameter("latitude", place.latitude)
            parameter("longitude", place.longitude)
            parameter("current", "european_aqi,pm2_5,pm10,uv_index")
            parameter("hourly", "european_aqi,pm2_5,alder_pollen,birch_pollen,grass_pollen")
            parameter("timezone", "auto")
            parameter("forecast_days", 2)
        } ?: error("air")
        return AppJson.parseToJsonElement(text).jsonObject
    }

    private suspend fun fetchElevations(place: Place): List<ElevationSnapshot> {
        val elevations = listOf(800, 1500, 2500)
        val text = getText("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", elevations.joinToString(",") { place.latitude.toString() })
            parameter("longitude", elevations.joinToString(",") { place.longitude.toString() })
            parameter("elevation", elevations.joinToString(","))
            parameter("current", "temperature_2m,wind_speed_10m,wind_direction_10m,relative_humidity_2m")
            parameter("wind_speed_unit", "kmh")
            parameter("models", "best_match")
        } ?: return elevations.map { ElevationSnapshot(it) }
        val root = AppJson.parseToJsonElement(text)
        val points = if (root is JsonArray) root else JsonArray(listOf(root))
        return elevations.mapIndexed { index, elev ->
            val current = points.getOrNull(index)?.jsonObject?.get("current")?.jsonObject
            ElevationSnapshot(
                elevation = elev,
                temperature = current?.dbl("temperature_2m"),
                wind = current?.dbl("wind_speed_10m"),
                windDir = current?.dbl("wind_direction_10m"),
                humidity = current?.dbl("relative_humidity_2m")
            )
        }
    }

    private suspend fun fetchLakes(place: Place): List<LakeSnapshot> {
        val lakes = nearestLakes(place)
        if (lakes.isEmpty()) return emptyList()
        val marineText = getText("https://marine-api.open-meteo.com/v1/marine") {
            parameter("latitude", lakes.joinToString(",") { it.lake.latitude.toString() })
            parameter("longitude", lakes.joinToString(",") { it.lake.longitude.toString() })
            parameter("current", "wave_height,sea_surface_temperature")
            parameter("timezone", "auto")
        }
        val marinePoints = if (marineText != null) {
            val root = AppJson.parseToJsonElement(marineText)
            if (root is JsonArray) root else JsonArray(listOf(root))
        } else JsonArray(emptyList())
        val ids = lakes.mapNotNull { it.lake.tempStationId }
        val bafu = extras.fetchBafuLakeTemps(ids)
        return lakes.mapIndexedNotNull { index, lake ->
            val current = marinePoints.getOrNull(index)?.jsonObject?.get("current")?.jsonObject
            toLakeSnapshot(
                lake,
                current?.dbl("sea_surface_temperature"),
                current?.dbl("wave_height"),
                lake.lake.tempStationId?.let { bafu[it] }
            )
        }
    }

    private fun buildBundle(
        place: Place,
        forecast: JsonObject,
        air: JsonObject?,
        elevations: List<ElevationSnapshot>,
        lakes: List<LakeSnapshot>,
        station: StationObservation?
    ): WeatherBundle {
        val currentObj = forecast["current"]!!.jsonObject
        val current = ForecastCurrent(
            time = currentObj.str("time") ?: Instant.now().toString(),
            temperature_2m = currentObj.dbl("temperature_2m") ?: 0.0,
            relative_humidity_2m = currentObj.dbl("relative_humidity_2m") ?: 0.0,
            apparent_temperature = currentObj.dbl("apparent_temperature") ?: 0.0,
            weather_code = currentObj.int("weather_code") ?: 0,
            wind_speed_10m = currentObj.dbl("wind_speed_10m") ?: 0.0,
            wind_direction_10m = currentObj.dbl("wind_direction_10m") ?: 0.0,
            wind_gusts_10m = currentObj.dbl("wind_gusts_10m") ?: 0.0,
            is_day = currentObj.int("is_day") ?: 1,
            precipitation = currentObj.dbl("precipitation") ?: 0.0,
            pressure_msl = currentObj.dbl("pressure_msl") ?: 0.0,
            cloud_cover = currentObj.dbl("cloud_cover") ?: 0.0
        )
        val hourly = forecast["hourly"]?.jsonObject
        val allHours = mapHours(hourly)
        val now = runCatching { Instant.parse(current.time).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
        var start = allHours.indexOfFirst { parseMs(it.time) >= now }
        if (start < 0) start = 0
        val days = mapDays(forecast["daily"]?.jsonObject)
        val minutes = mapMinutes(forecast["minutely_15"]?.jsonObject, now)
        return WeatherBundle(
            place = place,
            timezone = forecast.str("timezone") ?: "Europe/Zurich",
            current = current,
            hours = allHours.drop(start).take(24),
            allHours = allHours,
            minutes = minutes,
            days = days,
            air = air?.get("current")?.jsonObject?.let {
                AirQualityCurrent(
                    time = it.str("time"),
                    european_aqi = it.dbl("european_aqi"),
                    pm2_5 = it.dbl("pm2_5"),
                    pm10 = it.dbl("pm10"),
                    uv_index = it.dbl("uv_index")
                )
            },
            pollen = currentPollen(air),
            airTrend = mapAirTrend(air),
            elevations = elevations,
            lakes = lakes,
            station = station,
            fetchedAt = Instant.now().toString()
        )
    }

    private fun mapHours(hourly: JsonObject?): List<HourPoint> {
        if (hourly == null) return emptyList()
        val times = hourly.strList("time")
        return times.indices.map { i ->
            HourPoint(
                time = times[i],
                temperature = hourly.dblAt("temperature_2m", i) ?: 0.0,
                feelsLike = hourly.dblAt("apparent_temperature", i) ?: 0.0,
                code = hourly.intAt("weather_code", i) ?: 0,
                precipProb = hourly.dblAt("precipitation_probability", i),
                precipMm = hourly.dblAt("precipitation", i) ?: 0.0,
                wind = hourly.dblAt("wind_speed_10m", i) ?: 0.0,
                humidity = hourly.dblAt("relative_humidity_2m", i) ?: 0.0,
                isDay = hourly.intAt("is_day", i) == 1,
                uv = hourly.dblAt("uv_index", i),
                windDir = hourly.dblAt("wind_direction_10m", i),
                gusts = hourly.dblAt("wind_gusts_10m", i),
                cloud = hourly.dblAt("cloud_cover", i),
                visibility = hourly.dblAt("visibility", i),
                cape = hourly.dblAt("cape", i),
                freezingLevel = hourly.dblAt("freezing_level_height", i),
                snowfall = hourly.dblAt("snowfall", i),
                dewPoint = hourly.dblAt("dew_point_2m", i)
            )
        }
    }

    private fun mapDays(daily: JsonObject?): List<DayPoint> {
        if (daily == null) return emptyList()
        val times = daily.strList("time")
        val limit = minOf(10, times.size)
        return (0 until limit).mapNotNull { i ->
            val date = times.getOrNull(i) ?: return@mapNotNull null
            val tMax = daily.dblAt("temperature_2m_max", i) ?: return@mapNotNull null
            val tMin = daily.dblAt("temperature_2m_min", i) ?: return@mapNotNull null
            val code = daily.intAt("weather_code", i) ?: return@mapNotNull null
            DayPoint(
                date = date,
                code = code,
                tMax = tMax,
                tMin = tMin,
                precipMm = daily.dblAt("precipitation_sum", i) ?: 0.0,
                precipProb = daily.dblAt("precipitation_probability_max", i),
                sunrise = daily.strAt("sunrise", i) ?: "",
                sunset = daily.strAt("sunset", i) ?: "",
                uvMax = daily.dblAt("uv_index_max", i),
                windMax = daily.dblAt("wind_speed_10m_max", i) ?: 0.0
            )
        }
    }

    private fun mapMinutes(minutely: JsonObject?, now: Long): List<MinutePoint> {
        if (minutely == null) return emptyList()
        val times = minutely.strList("time")
        val points = times.indices.map { i ->
            MinutePoint(
                time = times[i],
                temperature = minutely.dblAt("temperature_2m", i),
                precipMm = minutely.dblAt("precipitation", i),
                wind = minutely.dblAt("wind_speed_10m", i),
                windDir = minutely.dblAt("wind_direction_10m", i),
                gusts = minutely.dblAt("wind_gusts_10m", i),
                cape = minutely.dblAt("cape", i),
                code = minutely.intAt("weather_code", i),
                snowfall = minutely.dblAt("snowfall", i)
            )
        }
        var start = points.indexOfFirst { parseMs(it.time) >= now }
        if (start < 0) start = 0
        return points.drop(start).take(24)
    }

    private fun currentPollen(air: JsonObject?): PollenValues {
        val hourly = air?.get("hourly")?.jsonObject ?: return PollenValues()
        val times = hourly.strList("time")
        if (times.isEmpty()) return PollenValues()
        var idx = times.indexOfFirst { parseMs(it) >= System.currentTimeMillis() }
        if (idx < 0) idx = 0
        return PollenValues(
            alder = hourly.dblAt("alder_pollen", idx),
            birch = hourly.dblAt("birch_pollen", idx),
            grass = hourly.dblAt("grass_pollen", idx)
        )
    }

    private fun mapAirTrend(air: JsonObject?): List<AirTrendPoint> {
        val hourly = air?.get("hourly")?.jsonObject ?: return emptyList()
        val times = hourly.strList("time")
        if (times.isEmpty()) return emptyList()
        var idx = times.indexOfFirst { parseMs(it) >= System.currentTimeMillis() }
        if (idx < 0) idx = 0
        return times.drop(idx).take(12).mapIndexed { offset, time ->
            AirTrendPoint(
                time = time,
                aqi = hourly.dblAt("european_aqi", idx + offset),
                pm25 = hourly.dblAt("pm2_5", idx + offset)
            )
        }
    }

    private suspend fun getText(
        url: String,
        block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}
    ): String? {
        val response = runCatching { client.get(url, block) }.getOrNull() ?: return null
        if (response.status.value !in 200..299) return null
        return response.bodyAsText()
    }
}

internal fun parseMs(iso: String): Long =
    runCatching { Instant.parse(iso).toEpochMilli() }.getOrDefault(0L)

internal fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.content
internal fun JsonObject.dbl(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
internal fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
internal fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.content?.toLongOrNull()
internal fun JsonObject.strList(key: String): List<String> =
    this[key]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()

internal fun JsonObject.dblAt(key: String, index: Int): Double? =
    this[key]?.jsonArray?.getOrNull(index)?.jsonPrimitive?.doubleOrNull

internal fun JsonObject.intAt(key: String, index: Int): Int? =
    this[key]?.jsonArray?.getOrNull(index)?.jsonPrimitive?.intOrNull

internal fun JsonObject.strAt(key: String, index: Int): String? =
    this[key]?.jsonArray?.getOrNull(index)?.jsonPrimitive?.content
