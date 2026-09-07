# Wetter-Icons: Vergleich und Empfehlung

Vergleichstafel: [`icon-set-vergleich.png`](../icon-set-vergleich.png) (Kopie in diesem Ordner).

## Sets

| Set | Stil | Lizenz | Nacht | Eigen vs. Fallback (14 WMO-Familien) |
|---|---|---|---|---|
| **Native aktuell** | Unicode-Emoji (`glyphEmoji`) | Unicode | nur Klar (`☾`); Teilw. = gleiches `⛅` | **8/14** eigen · Niesel, Starkregen, Schauer, Eisregen, Hagel und Nacht-Teilw. fallen auf Regen/Schnee/Tag-Wolke |
| **PWA Material Symbols** | Rounded outlined (wie `weather.ts`) | Apache 2.0 | Klar + Teilw. bewölkt | **12/14** · Schauer → `rainy`, Eisregen → `rainy` (`weather_mix` liegt ungenutzt im Set) |
| **Material Symbols Filled** | Rounded filled | Apache 2.0 | Klar + Teilw. bewölkt | **12/14** · gleiche Lücken, bessere Lesbarkeit in Widgets |
| **Meteocons Fill** | Farb-Fill | MIT | fast alle Zustände inkl. Niederschlag | **14/14** |
| **Meteocons Line** | Outline mit festen Farben | MIT | wie Fill | **14/14** |
| **Weather Icons** (Erik Flowers) | Mono-Fill | SIL OFL 1.1 | Klar, Teilw., Schauer, Gewitter | **14/14** |

Die App mappt 28 WMO-Codes (0, 1, 2, 3, 45, 48, 51–57, 61–67, 71–77, 80–82, 85–86, 95, 96, 99) auf diese Familien. Nachtvarianten gibt es in der App nur für Codes 0–2.

## Empfehlung

**Material Symbols Rounded Filled** für die native App: Apache-2.0, einfärbbar für Material You 3 Expressive (eine Tinte statt fixer Emoji-Farben), winzig im APK (etwa 13 SVG), und dieselbe Zuordnung wie die PWA — der Abstand zu den heutigen Emoji-Glyphen verschwindet, ohne ein zweites Wetter-Vokabular zu pflegen. Schauer und Eisregen bleiben Fallbacks; `weather_mix` / `rainy_snow` liegen schon im Set und sollten gemappt werden. Meteocons Fill hat die bessere meteorologische Abdeckung inklusive Nacht-Niederschlag, ist aber bunt und damit schwer dynamisch einzutönen; Weather Icons (OFL) wäre die monochrome Vollabdeckung, wirkt aber älter und nicht MY3E-nah.
