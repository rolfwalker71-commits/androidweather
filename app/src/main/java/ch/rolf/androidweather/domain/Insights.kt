package ch.rolf.androidweather.domain

const val WET_MM = 0.4
const val DRY_PLAN_MM = 0.35
const val NEW_RAIN_MM = 0.8
const val EARLIER_MS = 60 * 60 * 1000L
const val TEMP_SWING = 3.5
const val WIND_JUMP = 15.0
const val GUST_JUMP = 20.0
const val WIND_MIN = 35.0
const val GUST_MIN = 45.0
private const val WET_HOUR_MM = 0.4
private const val WET_MINUTE_MM = 0.1
private const val DRY_MM = 0.25

fun nextPrecipLine(bundle: WeatherBundle): String? {
    val wet = bundle.hours.find { it.precipMm >= 0.3 } ?: return null
    val now = bundle.hours.firstOrNull()
    return if (now != null && now.precipMm >= 0.3) "Niederschlag jetzt"
    else "Niederschlag ab ${formatTime(wet.time, bundle.timezone)}"
}

fun insightLine(bundle: WeatherBundle): String? {
    val now = bundle.hours.firstOrNull()
        ?: return getWmo(bundle.current.weather_code, bundle.current.is_day == 1).label
    val later = bundle.hours.getOrNull(2) ?: bundle.hours.getOrNull(1)
    val phrases = mutableListOf<String>()
    val laterPrecip = later?.precipMm
    val laterProb = later?.precipProb
    if (now.precipMm >= 0.3 && laterPrecip != null && laterPrecip < now.precipMm * 0.45) {
        phrases += "Regen lässt nach"
    } else if (now.precipMm < 0.15 && laterPrecip != null && laterPrecip >= 0.5 && (laterProb == null || laterProb >= 45)) {
        phrases += "ab ${formatTime(later.time, bundle.timezone)} Regen"
    } else if ((now.snowfall ?: 0.0) >= 0.2) {
        phrases += "Schnee im Gang"
    }
    val clearHour = bundle.hours.find {
        it.cloud != null && it.cloud <= 25 && it.code <= 1 &&
            parseForecastEpochMilli(it.time, bundle.timezone) > System.currentTimeMillis()
    }
    val cloudNow = now.cloud ?: bundle.current.cloud_cover
    if (clearHour != null && cloudNow >= 55) {
        phrases += "ab ${formatTime(clearHour.time, bundle.timezone)} klar"
    } else if (cloudNow <= 25 && now.code <= 1) {
        phrases += "weiterhin klar"
    }
    if (now.code >= 95) phrases += getWmo(now.code, now.isDay).label
    if (later != null) {
        if (later.temperature - now.temperature >= 3) phrases += "es wird milder"
        else if (now.temperature - later.temperature >= 3) phrases += "es kühlt ab"
    }
    val unique = phrases.distinct().take(2)
    return if (unique.isNotEmpty()) unique.joinToString(" · ")
    else "${getWmo(now.code, now.isDay).label} bleibt vorerst ähnlich"
}

fun precipOnsetTime(bundle: WeatherBundle): String? {
    val hourOnset = bundle.hours.find { it.precipMm >= WET_HOUR_MM }
    val minuteOnset = bundle.minutes.find { it.precipMm != null && it.precipMm >= WET_MINUTE_MM }
    return when {
        hourOnset == null && minuteOnset == null -> null
        minuteOnset == null -> hourOnset!!.time
        hourOnset == null -> minuteOnset.time
        else -> if (minuteOnset.time <= hourOnset.time) minuteOnset.time else hourOnset.time
    }
}

fun clothingLine(bundle: WeatherBundle): String? {
    val comfort = comfortAdvice(bundle) ?: return null
    val nowMm = bundle.hours.firstOrNull()?.precipMm ?: 0.0
    val rainingNow = nowMm >= WET_HOUR_MM ||
        (bundle.minutes.firstOrNull()?.precipMm != null && bundle.minutes.first().precipMm!! >= WET_MINUTE_MM)
    val rec = if (comfort.recommendation == "Schirm") "Schirm einpacken" else comfort.recommendation
    val extra = when {
        nowMm < DRY_MM && !rainingNow -> {
            val onset = precipOnsetTime(bundle)
            if (onset != null) "trocken bis ${formatTime(onset, bundle.timezone)}" else "trocken"
        }
        rainingNow -> "jetzt nass"
        else -> null
    }
    return listOfNotNull(rec, extra).joinToString(", ")
}

data class NowcastBar(val time: String, val precipMm: Double, val code: Int?)
data class WindBar(val time: String, val speed: Double, val direction: Double, val gusts: Double?, val intervalMin: Int)

fun nowcast30Bars(minutes: List<MinutePoint>, horizonMin: Int = 360): List<NowcastBar> {
    val bars = mutableListOf<NowcastBar>()
    var i = 0
    while (i + 1 < minutes.size && bars.size * 30 < horizonMin) {
        val first = minutes[i]
        val second = minutes[i + 1]
        if (first.precipMm == null || second.precipMm == null) {
            i += 1
            continue
        }
        bars += NowcastBar(first.time, first.precipMm + second.precipMm, maxOfNullable(first.code, second.code))
        i += 2
    }
    return bars
}

fun windHourBars(hours: List<HourPoint>, count: Int = 12): List<WindBar> =
    hours.filter { it.windDir != null }.take(count).map {
        WindBar(it.time, it.wind, it.windDir!!, it.gusts, 60)
    }

private fun maxOfNullable(a: Int?, b: Int?): Int? = when {
    a == null -> b
    b == null -> a
    else -> maxOf(a, b)
}
