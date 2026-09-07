package ch.rolf.androidweather.data

import ch.rolf.androidweather.domain.AlertItem
import ch.rolf.androidweather.domain.AvalancheBulletin
import ch.rolf.androidweather.domain.PASS_STATION_IDS
import ch.rolf.androidweather.domain.PassObservation
import ch.rolf.androidweather.domain.Place
import ch.rolf.androidweather.domain.RadarCatalog
import ch.rolf.androidweather.domain.RadarFrame
import ch.rolf.androidweather.domain.SatelliteFrame
import ch.rolf.androidweather.domain.StationObservation
import ch.rolf.androidweather.domain.haversineKm
import ch.rolf.androidweather.domain.isSwitzerland
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.charset.Charset
import java.time.Instant

class WeatherExtras(private val client: HttpClient) {
    private var smnCache: Pair<Long, List<SmnStation>>? = null

    data class SmnStation(val id: String, val name: String, val latitude: Double, val longitude: Double)
    data class VqhaObs(val temperature: Double?, val windKmh: Double?, val windDir: Double?, val observedAt: String?)

    suspend fun fetchBafuLakeTemps(ids: List<String>): Map<String, Double> {
        val unique = ids.filter { it.isNotBlank() }.distinct()
        if (unique.isEmpty()) return emptyMap()
        runCatching { tempsFromExistenz(unique) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
        return runCatching { tempsFromLindas(unique) }.getOrDefault(emptyMap())
    }

    private suspend fun tempsFromExistenz(ids: List<String>): Map<String, Double> {
        val text = getText("https://api.existenz.ch/apiv1/hydro/latest") {
            parameter("locations", ids.joinToString(","))
            parameter("parameters", "temperature")
            header("Accept", "application/json")
        } ?: return emptyMap()
        val payload = AppJson.parseToJsonElement(text).jsonObject["payload"]?.jsonArray ?: return emptyMap()
        val map = mutableMapOf<String, Double>()
        for (row in payload) {
            val o = row.jsonObject
            if (o.str("par") == "temperature") {
                val loc = o.str("loc") ?: continue
                val value = o.dbl("val") ?: continue
                map[loc] = value
            }
        }
        return map
    }

    private suspend fun tempsFromLindas(ids: List<String>): Map<String, Double> {
        val list = ids.mapNotNull { it.toIntOrNull() }
        if (list.isEmpty()) return emptyMap()
        val query = """
            PREFIX hd: <https://environment.ld.admin.ch/foen/hydro/dimension/>
            PREFIX schema: <http://schema.org/>
            SELECT ?id ?temp WHERE {
              GRAPH <https://lindas.admin.ch/foen/hydro> {
                ?st schema:identifier ?id .
                ?obs hd:station ?st ; hd:waterTemperature ?temp .
                FILTER(?id IN (${list.joinToString(", ")}))
              }
            }
        """.trimIndent()
        val text = getText("https://lindas.admin.ch/query") {
            parameter("query", query)
            header("Accept", "application/sparql-results+json")
        } ?: return emptyMap()
        val bindings = AppJson.parseToJsonElement(text).jsonObject["results"]
            ?.jsonObject?.get("bindings")?.jsonArray ?: return emptyMap()
        val map = mutableMapOf<String, Double>()
        for (row in bindings) {
            val o = row.jsonObject
            val id = o["id"]?.jsonObject?.get("value")?.jsonPrimitive?.content
            val temp = o["temp"]?.jsonObject?.get("value")?.jsonPrimitive?.doubleOrNull
            if (id != null && temp != null) map[id] = temp
        }
        return map
    }

    suspend fun fetchNearestStation(place: Place): StationObservation? {
        if (isSwitzerland(place)) {
            return runCatching { fetchSwissMetNet(place) }.getOrNull()
        }
        return runCatching { fetchMetar(place) }.getOrNull()
    }

    suspend fun fetchPassObservations(lat: Double, lon: Double): List<PassObservation> {
        val stations = loadSmnStations()
        if (stations.isEmpty()) return emptyList()
        val observations = parseVqha80(fetchBytes("https://data.geo.admin.ch/ch.meteoschweiz.messwerte-aktuell/VQHA80.csv") ?: return emptyList())
        val byId = stations.associateBy { it.id }
        return PASS_STATION_IDS.mapNotNull { id ->
            val meta = byId[id] ?: return@mapNotNull null
            val obs = observations[id] ?: return@mapNotNull null
            if (!isFresh(obs.observedAt)) return@mapNotNull null
            if (obs.temperature == null && obs.windKmh == null) return@mapNotNull null
            val distanceKm = haversineKm(lat, lon, meta.latitude, meta.longitude)
            if (distanceKm > 80) return@mapNotNull null
            PassObservation(id, meta.name, obs.temperature, obs.windKmh, obs.windDir, obs.observedAt, distanceKm)
        }.sortedBy { it.distanceKm }.take(5)
    }

    private suspend fun fetchSwissMetNet(place: Place): StationObservation? {
        val stations = loadSmnStations()
        val bytes = fetchBytes("https://data.geo.admin.ch/ch.meteoschweiz.messwerte-aktuell/VQHA80.csv") ?: return null
        val observations = parseVqha80(bytes)
        var best: StationObservation? = null
        for (station in stations) {
            val obs = observations[station.id] ?: continue
            val temp = obs.temperature ?: continue
            if (!isFresh(obs.observedAt)) continue
            val distanceKm = haversineKm(place.latitude, place.longitude, station.latitude, station.longitude)
            if (distanceKm > 80) continue
            if (best == null || distanceKm < best.distanceKm) {
                best = StationObservation(station.id, station.name, temp, obs.observedAt, distanceKm, "meteoswiss")
            }
        }
        return best
    }

    private suspend fun loadSmnStations(): List<SmnStation> {
        val cached = smnCache
        if (cached != null && System.currentTimeMillis() - cached.first < 24 * 60 * 60 * 1000) return cached.second
        val bytes = fetchBytes("https://data.geo.admin.ch/ch.meteoschweiz.ogd-smn/ogd-smn_meta_stations.csv") ?: return emptyList()
        val text = String(bytes, Charset.forName("ISO-8859-1"))
        val stations = parseSmnStations(text)
        if (stations.isNotEmpty()) smnCache = System.currentTimeMillis() to stations
        return stations
    }

    private fun parseSmnStations(text: String): List<SmnStation> {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.size < 2) return emptyList()
        val header = lines.first().split(';').map { it.trim() }
        val idIdx = header.indexOf("station_abbr")
        val nameIdx = header.indexOf("station_name")
        val latIdx = header.indexOf("station_coordinates_wgs84_lat")
        val lonIdx = header.indexOf("station_coordinates_wgs84_lon")
        if (idIdx < 0 || nameIdx < 0 || latIdx < 0 || lonIdx < 0) return emptyList()
        return lines.drop(1).mapNotNull { line ->
            val cells = line.split(';')
            val id = cells.getOrNull(idIdx)?.trim().orEmpty()
            val name = cells.getOrNull(nameIdx)?.trim().orEmpty()
            val lat = cells.getOrNull(latIdx)?.replace(',', '.')?.toDoubleOrNull()
            val lon = cells.getOrNull(lonIdx)?.replace(',', '.')?.toDoubleOrNull()
            if (id.isBlank() || name.isBlank() || lat == null || lon == null) null
            else SmnStation(id, name, lat, lon)
        }
    }

    private fun parseVqha80(bytes: ByteArray): Map<String, VqhaObs> {
        val lines = String(bytes, Charsets.UTF_8).lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.size < 2) return emptyMap()
        val header = lines.first().split(';').map { it.trim() }
        val idIdx = header.indexOf("Station/Location")
        val dateIdx = header.indexOf("Date")
        val tempIdx = header.indexOf("tre200s0")
        val windIdx = header.indexOf("fu3010z0")
        val dirIdx = header.indexOf("dkl010z0")
        if (idIdx < 0 || dateIdx < 0) return emptyMap()
        val map = mutableMapOf<String, VqhaObs>()
        for (line in lines.drop(1)) {
            val cells = line.split(';')
            val id = cells.getOrNull(idIdx)?.trim().orEmpty()
            if (id.isBlank()) continue
            fun num(idx: Int): Double? = cells.getOrNull(idx)?.replace(',', '.')?.toDoubleOrNull()
            val temp = if (tempIdx >= 0) num(tempIdx) else null
            val windMs = if (windIdx >= 0) num(windIdx) else null
            val windDir = if (dirIdx >= 0) num(dirIdx) else null
            if (temp == null && windMs == null) continue
            val rawDate = cells.getOrNull(dateIdx)?.trim().orEmpty()
            val observedAt = if (rawDate.matches(Regex("\\d{12}"))) {
                "${rawDate.substring(0, 4)}-${rawDate.substring(4, 6)}-${rawDate.substring(6, 8)}T${rawDate.substring(8, 10)}:${rawDate.substring(10, 12)}:00Z"
            } else null
            map[id] = VqhaObs(temp, windMs?.times(3.6), windDir, observedAt)
        }
        return map
    }

    private fun isFresh(iso: String?): Boolean {
        if (iso == null) return true
        val then = runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull() ?: return false
        return System.currentTimeMillis() - then <= 3 * 60 * 60 * 1000
    }

    private suspend fun fetchMetar(place: Place): StationObservation? {
        val bbox = "${place.latitude - 0.9},${place.longitude - 0.9},${place.latitude + 0.9},${place.longitude + 0.9}"
        val text = getText("https://aviationweather.gov/api/data/metar") {
            parameter("bbox", bbox)
            parameter("format", "json")
            header("Accept", "application/json")
        } ?: return null
        val rows = AppJson.parseToJsonElement(text)
        if (rows !is JsonArray) return null
        var best: StationObservation? = null
        for (row in rows) {
            val o = row.jsonObject
            val temp = o.dbl("temp") ?: continue
            val lat = o.dbl("lat") ?: continue
            val lon = o.dbl("lon") ?: continue
            val id = o.str("icaoId") ?: continue
            val name = o.str("name") ?: id
            val observedAt = o.str("reportTime")
            val distanceKm = haversineKm(place.latitude, place.longitude, lat, lon)
            if (distanceKm > 80) continue
            if (best == null || distanceKm < best.distanceKm) {
                best = StationObservation(id, name, temp, observedAt, distanceKm, "metar")
            }
        }
        return best
    }

    suspend fun fetchRadarCatalog(): RadarCatalog {
        val text = getText("https://api.rainviewer.com/public/weather-maps.json")
            ?: error("Radar-Katalog nicht verfügbar")
        val data = AppJson.parseToJsonElement(text).jsonObject
        val host = data.str("host") ?: "https://tilecache.rainviewer.com"
        val radar = data["radar"]?.jsonObject
        val past = takeFrames(radar?.get("past")?.jsonArray, "past")
        val nowcast = takeFrames(radar?.get("nowcast")?.jsonArray, "nowcast")
        val infrared = data["satellite"]?.jsonObject?.get("infrared")?.jsonArray.orEmpty().mapNotNull { el ->
            val o = el.jsonObject
            val time = o["time"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
            val path = o.str("path") ?: return@mapNotNull null
            SatelliteFrame(time, path)
        }
        return RadarCatalog(host, (past + nowcast).sortedBy { it.time }, infrared)
    }

    private fun takeFrames(list: JsonArray?, kind: String): List<RadarFrame> {
        val seen = mutableSetOf<Long>()
        return (list ?: JsonArray(emptyList())).mapNotNull { el ->
            val o = el.jsonObject
            val time = o["time"]?.jsonPrimitive?.content?.toLongOrNull() ?: return@mapNotNull null
            val path = o.str("path") ?: return@mapNotNull null
            if (!seen.add(time)) return@mapNotNull null
            RadarFrame(time, path, kind)
        }
    }

    suspend fun fetchMeteoalarm(place: Place): List<AlertItem> {
        val slug = countrySlug(place.countryCode) ?: countryFromBoxes(place.latitude, place.longitude)?.let { countrySlug(it) }
            ?: return emptyList()
        val code = place.countryCode?.uppercase() ?: countryFromBoxes(place.latitude, place.longitude) ?: return emptyList()
        val text = getText("https://feeds.meteoalarm.org/api/v1/warnings/feeds-$slug") {
            header("Accept", "application/json")
        } ?: return emptyList()
        val root = AppJson.parseToJsonElement(text).jsonObject
        val warnings = root["warnings"]?.jsonArray ?: return emptyList()
        val city = place.name
        val admin = place.admin1
        return warnings.mapNotNull { item ->
            val alert = item.jsonObject["alert"]?.jsonObject ?: return@mapNotNull null
            val infos = alert["info"]?.jsonArray ?: return@mapNotNull null
            val preferred = infos.map { it.jsonObject }.firstOrNull { it.str("language")?.startsWith("de", true) == true }
                ?: infos.map { it.jsonObject }.firstOrNull { it.str("language")?.startsWith("en", true) == true }
                ?: infos.first().jsonObject
            val event = preferred.str("event") ?: preferred.str("headline") ?: "Wetterwarnung"
            val severity = severityFrom(preferred.str("severity"), event)
            if (severity == "minor") return@mapNotNull null
            val areaDesc = preferred["area"]?.let { areaEl ->
                when (areaEl) {
                    is JsonArray -> areaEl.firstOrNull()?.jsonObject?.str("areaDesc")
                    is JsonObject -> areaEl.str("areaDesc")
                    else -> null
                }
            }
            val hay = listOfNotNull(areaDesc, event, preferred.str("headline")).joinToString(" ").lowercase()
            val local = listOfNotNull(city, admin).any { token -> token.length >= 3 && hay.contains(token.lowercase()) }
            if (!local && !isSwitzerland(place)) {
                // keep country-level serious alerts when we cannot match a city
            }
            AlertItem(
                id = item.jsonObject.str("uuid") ?: alert.str("identifier") ?: event,
                event = eventLabel(event),
                headline = preferred.str("headline") ?: event,
                severity = severity,
                onset = preferred.str("onset") ?: preferred.str("effective"),
                expires = preferred.str("expires"),
                area = areaDesc ?: city,
                source = "Meteoalarm $code"
            )
        }.filter { alert ->
            val expires = alert.expires?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            expires == null || expires >= System.currentTimeMillis()
        }.sortedByDescending { severityRank(it.severity) }.take(5)
    }

    suspend fun fetchAvalanche(lat: Double, lon: Double): AvalancheBulletin {
        val unavailable = AvalancheBulletin(
            false, null, "nicht verfügbar", null, "SLF",
            "Kein öffentlicher SLF-Feed erreichbar — keine Schätzwerte."
        )
        if (lat !in 45.8..47.85 || lon !in 5.9..10.55) {
            return unavailable.copy(note = "Lawinenbulletin SLF nur für die Schweiz. Ausserhalb nicht verfügbar.")
        }
        val text = getText("https://aws.slf.ch/api/bulletin/caaml/de/json") {
            header("Accept", "application/json")
        } ?: return unavailable
        val payload = AppJson.parseToJsonElement(text).jsonObject
        val bulletins = payload["bulletins"]?.jsonArray.orEmpty()
        if (bulletins.isEmpty()) {
            return AvalancheBulletin(
                false, null, "kein aktuelles Bulletin", null, "SLF",
                "Im Sommer oft kein Bulletin — keine Schätzwerte. Quelle: aws.slf.ch, CC BY 4.0."
            )
        }
        val geoText = getText("https://aws.slf.ch/api/bulletin/caaml/de/geojson") {
            header("Accept", "application/json")
        }
        var matchId: String? = null
        var matchLevel: Int? = null
        var region: String? = null
        if (geoText != null) {
            val features = AppJson.parseToJsonElement(geoText).jsonObject["features"]?.jsonArray.orEmpty()
            for (feature in features) {
                val o = feature.jsonObject
                if (pointInGeometry(lat, lon, o["geometry"]?.jsonObject)) {
                    matchId = o["properties"]?.jsonObject?.str("bulletinID")
                    matchLevel = maxRating(o["properties"]?.jsonObject)
                    region = o["properties"]?.jsonObject?.get("regions")?.jsonArray
                        ?.firstOrNull()?.jsonObject?.str("name")
                    break
                }
            }
        }
        val bulletin = bulletins.map { it.jsonObject }.firstOrNull { it.str("bulletinID") == matchId }
            ?: bulletins.first().jsonObject
        val level = matchLevel ?: maxRating(bulletin)
        val validUntil = bulletin["validTime"]?.jsonObject?.str("endTime")
        if (level == null) {
            return AvalancheBulletin(false, null, "nicht verfügbar", validUntil, "SLF", "Bulletin ohne Stufe — keine Schätzwerte.")
        }
        val labels = mapOf(
            1 to "Stufe 1 · gering",
            2 to "Stufe 2 · mässig",
            3 to "Stufe 3 · erheblich",
            4 to "Stufe 4 · gross",
            5 to "Stufe 5 · sehr gross"
        )
        val label = (labels[level] ?: "Stufe $level") + if (region != null) " · $region" else ""
        return AvalancheBulletin(
            true, level, label, validUntil, "SLF Bulletin API",
            "Offizielle SLF-Daten (CC BY 4.0). Kein Ersatz für das vollständige Bulletin."
        )
    }

    private fun maxRating(obj: JsonObject?): Int? {
        if (obj == null) return null
        val ratings = obj["dangerRatings"]?.jsonArray.orEmpty()
        return ratings.mapNotNull { rating ->
            val main = rating.jsonObject["mainValue"]?.jsonPrimitive
            main?.intOrNull ?: when (main?.content?.lowercase()) {
                "low" -> 1
                "moderate" -> 2
                "considerable" -> 3
                "high" -> 4
                "very_high", "very-high" -> 5
                else -> null
            }
        }.maxOrNull()
    }

    private fun pointInGeometry(lat: Double, lon: Double, geometry: JsonObject?): Boolean {
        if (geometry == null) return false
        return when (geometry.str("type")) {
            "Polygon" -> {
                val coords = geometry["coordinates"]?.jsonArray ?: return false
                val outer = coords.firstOrNull()?.jsonArray ?: return false
                pointInRing(lat, lon, outer)
            }
            "MultiPolygon" -> geometry["coordinates"]?.jsonArray.orEmpty().any { poly ->
                val outer = poly.jsonArray.firstOrNull()?.jsonArray
                outer != null && pointInRing(lat, lon, outer)
            }
            else -> false
        }
    }

    private fun pointInRing(lat: Double, lon: Double, ring: JsonArray): Boolean {
        val pts = ring.mapNotNull { pair ->
            val arr = pair.jsonArray
            val x = arr.getOrNull(0)?.jsonPrimitive?.doubleOrNull
            val y = arr.getOrNull(1)?.jsonPrimitive?.doubleOrNull
            if (x != null && y != null) x to y else null
        }
        var inside = false
        var j = pts.lastIndex
        for (i in pts.indices) {
            val (xi, yi) = pts[i]
            val (xj, yj) = pts[j]
            val intersect = (yi > lat) != (yj > lat) && lon < ((xj - xi) * (lat - yi) / (yj - yi + 1e-12)) + xi
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    private fun countrySlug(code: String?): String? = when (code?.uppercase()) {
        "CH" -> "switzerland"
        "DE" -> "germany"
        "AT" -> "austria"
        "FR" -> "france"
        "IT" -> "italy"
        "LI" -> "liechtenstein"
        else -> null
    }

    private fun countryFromBoxes(lat: Double, lon: Double): String? {
        val boxes = listOf(
            "LI" to listOf(47.04, 9.47, 47.28, 9.64),
            "CH" to listOf(45.8, 5.9, 47.85, 10.55),
            "AT" to listOf(46.4, 9.5, 49.02, 17.2),
            "DE" to listOf(47.27, 5.87, 55.1, 15.04),
            "FR" to listOf(42.33, -5.15, 51.09, 8.23),
            "IT" to listOf(36.6, 6.62, 47.1, 18.52)
        )
        return boxes.filter { (_, box) -> lat in box[0]..box[2] && lon in box[1]..box[3] }
            .minByOrNull { (_, box) -> (box[2] - box[0]) * (box[3] - box[1]) }
            ?.first
    }

    private fun severityFrom(severity: String?, text: String): String {
        val blob = "${severity.orEmpty()} $text".lowercase()
        return when {
            listOf("extreme", "red").any { it in blob } -> "extreme"
            listOf("severe", "orange").any { it in blob } -> "severe"
            listOf("moderate", "yellow", "gelb").any { it in blob } -> "moderate"
            listOf("minor", "green", "grün").any { it in blob } -> "minor"
            else -> "unknown"
        }
    }

    private fun severityRank(s: String) = when (s) {
        "extreme" -> 4
        "severe" -> 3
        "moderate" -> 2
        "minor" -> 1
        else -> 0
    }

    private fun eventLabel(text: String): String {
        val value = text.lowercase()
        return when {
            listOf("thunder", "gewitter").any { it in value } -> "Gewitter"
            listOf("flood", "hochwasser").any { it in value } -> "Hochwasser"
            listOf("rain", "regen").any { it in value } -> "Starkregen"
            listOf("avalanche", "lawine").any { it in value } -> "Lawine"
            listOf("snow", "schnee").any { it in value } -> "Schnee"
            listOf("ice", "frost", "glatt").any { it in value } -> "Glatteis"
            listOf("heat", "hitze").any { it in value } -> "Hitze"
            listOf("wind", "sturm").any { it in value } -> "Wind"
            listOf("fog", "nebel").any { it in value } -> "Nebel"
            else -> text.take(48).ifBlank { "Wetterwarnung" }
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

    private suspend fun fetchBytes(url: String): ByteArray? {
        val response = runCatching { client.get(url) }.getOrNull() ?: return null
        if (response.status.value !in 200..299) return null
        return response.bodyAsText().toByteArray()
    }
}

fun radarTileUrl(host: String, path: String): String = "$host$path/256/{z}/{x}/{y}/4/1_1.png"
fun infraredTileUrl(host: String, path: String): String = "$host$path/256/{z}/{x}/{y}/0/0_0.png"
const val ESRI_BASEMAP = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}"
const val LIGHTNING_WMS = "https://view.eumetsat.int/geoserver/wms"
