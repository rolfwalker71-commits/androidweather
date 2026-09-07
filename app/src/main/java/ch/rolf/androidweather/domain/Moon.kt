package ch.rolf.androidweather.domain

import java.time.Instant
import kotlin.math.cos

private const val SYNODIC = 29.530588853
private val NEW_MOON = Instant.parse("2000-01-06T18:14:00Z").toEpochMilli()

data class MoonInfo(val phase: Double, val illumination: Double, val label: String)

fun moonInfo(date: Instant = Instant.now()): MoonInfo {
    val age = ((date.toEpochMilli() - NEW_MOON) / 86400000.0) % SYNODIC
    val phase = (age + SYNODIC) % SYNODIC
    val illumination = 0.5 * (1 - cos((2 * Math.PI * phase) / SYNODIC))
    val label = when {
        phase < 1.8 -> "Neumond"
        phase < 6.4 -> "Zunehmende Sichel"
        phase < 8.9 -> "Zunehmender Halbmond"
        phase < 13.8 -> "Zunehmender Mond"
        phase < 16.2 -> "Vollmond"
        phase < 21.1 -> "Abnehmender Mond"
        phase < 23.6 -> "Abnehmender Halbmond"
        phase < 27.8 -> "Abnehmende Sichel"
        else -> "Neumond"
    }
    return MoonInfo(phase, illumination, label)
}
