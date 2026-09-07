# Wetter / Wetter Schweiz

Native Android-App (Kotlin, Jetpack Compose, Material You 3 Expressive) für das Wetter in der Schweiz und Europa. Alle Abrufe und Warnungen laufen auf dem Gerät (WorkManager). Kein Server, kein WebView, kein Capacitor.

Paket: `ch.rolf.androidweather`

## Funktionen

- **Jetzt** — Ort, Temperatur, WMO, gefühlt, nächster Niederschlag, Proaktivität
- **Verlauf** — Stundenliste plus Regen-/Wind-Balken
- **Woche** — 10-Tage-Prognose, Alpen (800/1500/2500 m), Pässe, Seen, Lawinenbulletin
- **Luft** — Europäischer AQI, PM, Pollen (Erle/Birke/Gräser), 12-h-Trend
- **Mehr** — Radar, Pendeln, Draußen, Wind, Berge, Seen, Favoriten, Vergleich, Einstellungen
- **Radar** — RainViewer-Frames (Abspielen), Infrarot, EUMETSAT-Blitz, Ortsmarker
- **Widgets** — Glance 2×2 und 4×2 (Ort, Temperatur, WMO, nächster Regen). Layouts in eigenen Dateien, konfigurierbar (Home / letzter Ort / Favorit)
- **Dauerbenachrichtigung** — laufende Notification mit Temp + Lage + Ort, Toggle in Einstellungen
- **7 Warnkategorien** — Regen bald, Warnungen, Frost, UV, Luft & Pollen, Morgenbriefing, Wetteränderung

## Lokal bauen

Voraussetzungen: JDK 17, Android SDK 35.

```bash
export JAVA_HOME=…   # z. B. Temurin 17
export ANDROID_HOME=…/Android/Sdk
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release ohne `keystore.properties` wird mit dem Debug-Key signiert. Für einen eigenen Upload-Key:

```
storeFile=upload.jks
storePassword=…
keyAlias=upload
keyPassword=…
```

GitHub Secrets für CI: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## APK aus GHCR holen und sideloaden

```bash
docker pull ghcr.io/rolfwalker71-commits/androidweather:latest
docker create --name wx ghcr.io/rolfwalker71-commits/androidweather:latest
docker cp wx:/apk/wetter.apk ./wetter.apk
docker rm wx
adb install -r wetter.apk
```

Alternativ: GitHub Actions-Artefakt `wetter-apk` oder das Release-Asset `wetter.apk`.

`docker-compose.yml` referenziert nur das GHCR-Image (kein `build:`). Lokales Image: `docker compose -f docker-compose.yml -f docker-compose.build.yml build`.

## Berechtigungen

- Internet / Netzwerkstatus
- Standort (grob + fein), Laufzeit — Fallback Home → letzter Ort → Bern
- Benachrichtigungen (API 33+)
- Boot completed, damit WorkManager neu plant

Kein Hintergrund-GPS. Widgets nutzen Home/letzter Ort/Favorit.

## Widgets später umgestalten

Glance-UI liegt in `app/src/main/java/ch/rolf/androidweather/widget/CompactWeatherWidget.kt` und `WideWeatherWidget.kt`. Datenbindung in `WidgetData.kt`, Konfiguration in `WidgetConfigActivity.kt`. WorkManager ruft `updateAll` auf — Layouts können ohne App-Umbau geändert werden.

## Dauerbenachrichtigung

Kanal `wetter_ongoing`, ID 1001. Toggle unter **Mehr → Einstellungen → Dauerbenachrichtigung**. WorkManager (~15 Min) aktualisiert Text (Temperatur, WMO, Ort). Tippen öffnet Jetzt.

## Datenquellen

Open-Meteo Forecast / Geocoding / Air / Marine, BigDataCloud, RainViewer, Esri, EUMETSAT, BAFU/Existenz, MeteoSwiss SMN/VQHA80, aviationweather.gov METAR, Meteoalarm, SLF.
