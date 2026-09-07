package ch.rolf.androidweather.domain

fun europeanAqi(value: Double?): ScaleLevel {
    if (value == null || value.isNaN()) {
        return ScaleLevel("Keine Daten", "Luftqualität derzeit nicht verfügbar.", 0f)
    }
    return when {
        value <= 20 -> ScaleLevel("Gut", "Frische Luft — ideal für draussen.", (value / 100).toFloat())
        value <= 40 -> ScaleLevel("Mässig", "Für die meisten unproblematisch.", (value / 100).toFloat())
        value <= 60 -> ScaleLevel("Heikel", "Sensible Personen sollten Anstrengung dosieren.", (value / 100).toFloat())
        value <= 80 -> ScaleLevel("Ungesund", "Lange Outdoor-Belastung vermeiden.", (value / 100).toFloat())
        value <= 100 -> ScaleLevel("Sehr ungesund", "Aktivität im Freien einschränken.", 1f)
        else -> ScaleLevel("Extrem", "Draussen nur wenn nötig.", 1f)
    }
}

fun uvLevel(value: Double?): ScaleLevel {
    if (value == null || value.isNaN()) return ScaleLevel("Keine Daten", "", 0f)
    return when {
        value < 3 -> ScaleLevel("Niedrig", "Schutz in der Regel nicht nötig.", (value / 11).toFloat())
        value < 6 -> ScaleLevel("Mässig", "Sonnenschutz um die Mittagszeit.", (value / 11).toFloat())
        value < 8 -> ScaleLevel("Hoch", "Hut, Creme und Schatten empfohlen.", (value / 11).toFloat())
        value < 11 -> ScaleLevel("Sehr hoch", "Mittags möglichst im Schatten bleiben.", (value / 11).toFloat())
        else -> ScaleLevel("Extrem", "Aufenthalt in der Sonne minimieren.", 1f)
    }
}

fun pollenLevel(value: Double?): ScaleLevel {
    if (value == null || value.isNaN()) return ScaleLevel("Keine Daten", "", 0f)
    return when {
        value < 10 -> ScaleLevel("Kein", "Kaum Pollenflug.", 0.08f)
        value < 50 -> ScaleLevel("Schwach", "Leichter Pollenflug.", 0.28f)
        value < 100 -> ScaleLevel("Mässig", "Für Allergiker spürbar.", 0.55f)
        value < 300 -> ScaleLevel("Stark", "Fenster zu, Medikamente bereithalten.", 0.8f)
        else -> ScaleLevel("Sehr stark", "Belastung hoch — draussen vorsichtig.", 1f)
    }
}
