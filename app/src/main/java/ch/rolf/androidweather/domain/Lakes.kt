package ch.rolf.androidweather.domain

data class LakeRef(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val tempStationId: String?,
    val tempStationLabel: String?
)

val CH_LAKES = listOf(
    LakeRef("zuerich", "Zürichsee", 47.31, 8.58, "2243", "Limmat, Baden (Auslauf)"),
    LakeRef("vierwald", "Vierwaldstättersee", 47.0, 8.4, "2152", "Reuss, Luzern (Auslauf)"),
    LakeRef("genf", "Genfersee", 46.45, 6.52, "2606", "Rhône, Genf (Auslauf)"),
    LakeRef("boden", "Bodensee", 47.58, 9.42, "2288", "Rhein, Neuhausen (Auslauf)"),
    LakeRef("neuenburg", "Neuenburgersee", 46.9, 6.84, "2029", "Aare, Brügg (nach den Jurarandseen)"),
    LakeRef("thun", "Thunersee", 46.69, 7.72, "2030", "Aare, Thun (Auslauf)"),
    LakeRef("brienzer", "Brienzersee", 46.72, 7.97, "2457", "Aare, Ringgenberg (Auslauf)"),
    LakeRef("maggiore", "Lago Maggiore", 46.15, 8.78, "2068", "Ticino, Riazzino"),
    LakeRef("lugano", "Luganersee", 46.0, 8.97, "2167", "Tresa, Ponte Tresa (Auslauf)"),
    LakeRef("walen", "Walensee", 47.12, 9.2, "2104", "Linth, Weesen (Auslauf)"),
    LakeRef("zug", "Zugersee", 47.15, 8.48, null, null),
    LakeRef("biel", "Bielersee", 47.08, 7.17, "2085", "Aare, Hagneck")
)

data class LakeWithDistance(val lake: LakeRef, val distanceKm: Double)

fun nearestLakes(place: Place, maxKm: Double = 80.0, limit: Int = 2): List<LakeWithDistance> =
    CH_LAKES.map { LakeWithDistance(it, haversineKm(place.latitude, place.longitude, it.latitude, it.longitude)) }
        .filter { it.distanceKm <= maxKm }
        .sortedBy { it.distanceKm }
        .take(limit)

fun toLakeSnapshot(
    lake: LakeWithDistance,
    marineTemp: Double?,
    waveHeight: Double?,
    bafuTemp: Double?
): LakeSnapshot? {
    val waterTemp = bafuTemp ?: marineTemp
    if (waterTemp == null && waveHeight == null) return null
    return LakeSnapshot(
        id = lake.lake.id,
        name = lake.lake.name,
        distanceKm = lake.distanceKm,
        waterTemp = waterTemp,
        waveHeight = waveHeight,
        tempSource = when {
            waterTemp == null -> null
            bafuTemp != null && lake.lake.tempStationLabel != null -> "BAFU ${lake.lake.tempStationLabel}"
            else -> "Open-Meteo Marine"
        }
    )
}

val PASS_STATION_IDS = listOf("BEH", "CDM", "GSB", "JUN", "VAB", "GUE", "GRH", "SIM", "SBE", "BUF")
