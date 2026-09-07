#!/usr/bin/env python3
"""Build a WMO weather-icon comparison HTML board from local/downloaded SVGs."""

from __future__ import annotations

from pathlib import Path

ROOT = Path("/home/rolf/DEV/androidweather")
ICONS = Path("/tmp/wx-icons")
OUT_HTML = ROOT / "docs" / "icon-set-vergleich.html"

# Representative WMO states the native + PWA apps actually map.
# (28 codes collapse onto these visual families; night only for 0–2.)
COLS = [
    ("klar", "Klar", "0 Tag"),
    ("nacht", "Nacht", "0 Nacht"),
    ("teilw_tag", "Teilw. bewölkt", "2 Tag"),
    ("teilw_nacht", "Teilw. Nacht", "2 Nacht"),
    ("bedeckt", "Bedeckt", "3"),
    ("nebel", "Nebel", "45"),
    ("niesel", "Niesel", "51"),
    ("regen", "Regen", "63"),
    ("starkregen", "Starkregen", "65"),
    ("schauer", "Schauer", "80"),
    ("schnee", "Schnee", "73"),
    ("eisregen", "Eisregen", "67"),
    ("gewitter", "Gewitter", "95"),
    ("hagel", "Hagel", "96"),
]


def read_svg(path: Path) -> str | None:
    if not path.exists():
        return None
    text = path.read_text(encoding="utf-8")
    # Drop XML prolog / comments that break inline embedding.
    if text.startswith("<?xml"):
        text = text.split("?>", 1)[-1]
    text = text.replace("<svg", '<svg class="glyph"', 1)
    return text.strip()


def cell_svg(path: Path, fallback: bool = False) -> str:
    raw = read_svg(path)
    if not raw:
        return '<div class="miss">—</div>'
    mark = '<span class="fb">Fallback</span>' if fallback else ""
    return f'<div class="icon">{raw}{mark}</div>'


def cell_emoji(ch: str, fallback: bool = False) -> str:
    mark = '<span class="fb">Fallback</span>' if fallback else ""
    return f'<div class="icon emoji">{ch}{mark}</div>'


def cell_miss() -> str:
    return '<div class="miss">—</div>'


MS_O = ICONS / "ms-outline"
MS_F = ICONS / "ms-fill"
MC_F = ICONS / "meteocons-fill"
MC_L = ICONS / "meteocons-line"
WI = ICONS / "weather-icons"

# (svg_path or None, is_fallback)
# Native uses Unicode from glyphEmoji() — no dedicated drizzle/showers/sleet/hail.
NATIVE = {
    "klar": ("emoji", "☀", False),
    "nacht": ("emoji", "☾", False),
    "teilw_tag": ("emoji", "⛅", False),
    "teilw_nacht": ("emoji", "⛅", True),  # same ⛅ day+night
    "bedeckt": ("emoji", "☁", False),
    "nebel": ("emoji", "🌫", False),
    "niesel": ("emoji", "🌧", True),
    "regen": ("emoji", "🌧", False),
    "starkregen": ("emoji", "🌧", True),
    "schauer": ("emoji", "🌧", True),
    "schnee": ("emoji", "❄", False),
    "eisregen": ("emoji", "🌧", True),
    "gewitter": ("emoji", "⛈", False),
    "hagel": ("emoji", "🌨", True),  # snow-cloud, not hail
}

PWA_MS = {
    "klar": (MS_O / "sunny.svg", False),
    "nacht": (MS_O / "nightlight.svg", False),
    "teilw_tag": (MS_O / "partly_cloudy_day.svg", False),
    "teilw_nacht": (MS_O / "partly_cloudy_night.svg", False),
    "bedeckt": (MS_O / "cloud.svg", False),
    "nebel": (MS_O / "foggy.svg", False),
    "niesel": (MS_O / "rainy_light.svg", False),
    "regen": (MS_O / "rainy.svg", False),
    "starkregen": (MS_O / "rainy_heavy.svg", False),
    "schauer": (MS_O / "rainy.svg", True),
    "schnee": (MS_O / "weather_snowy.svg", False),
    "eisregen": (MS_O / "rainy.svg", True),  # set has weather_mix unused
    "gewitter": (MS_O / "thunderstorm.svg", False),
    "hagel": (MS_O / "weather_hail.svg", False),
}

MS_FILL = {k: (MS_F / p.name, fb) for k, (p, fb) in PWA_MS.items()}

METEO_FILL = {
    "klar": (MC_F / "clear-day.svg", False),
    "nacht": (MC_F / "clear-night.svg", False),
    "teilw_tag": (MC_F / "partly-cloudy-day.svg", False),
    "teilw_nacht": (MC_F / "partly-cloudy-night.svg", False),
    "bedeckt": (MC_F / "overcast.svg", False),
    "nebel": (MC_F / "fog.svg", False),
    "niesel": (MC_F / "drizzle.svg", False),
    "regen": (MC_F / "rain.svg", False),
    "starkregen": (MC_F / "extreme-rain.svg", False),
    "schauer": (MC_F / "partly-cloudy-day-rain.svg", False),
    "schnee": (MC_F / "snow.svg", False),
    "eisregen": (MC_F / "sleet.svg", False),
    "gewitter": (MC_F / "thunderstorms.svg", False),
    "hagel": (MC_F / "hail.svg", False),
}

METEO_LINE = {k: (MC_L / p.name, fb) for k, (p, fb) in METEO_FILL.items()}

WEATHER_ICONS = {
    "klar": (WI / "wi-day-sunny.svg", False),
    "nacht": (WI / "wi-night-clear.svg", False),
    "teilw_tag": (WI / "wi-day-cloudy.svg", False),
    "teilw_nacht": (WI / "wi-night-alt-cloudy.svg", False),
    "bedeckt": (WI / "wi-cloudy.svg", False),
    "nebel": (WI / "wi-fog.svg", False),
    "niesel": (WI / "wi-sprinkle.svg", False),
    "regen": (WI / "wi-rain.svg", False),
    "starkregen": (WI / "wi-rain-wind.svg", False),
    "schauer": (WI / "wi-showers.svg", False),
    "schnee": (WI / "wi-snow.svg", False),
    "eisregen": (WI / "wi-sleet.svg", False),
    "gewitter": (WI / "wi-thunderstorm.svg", False),
    "hagel": (WI / "wi-hail.svg", False),
}


def render_cell(spec) -> str:
    kind_or_path = spec[0]
    if kind_or_path == "emoji":
        return cell_emoji(spec[1], spec[2])
    path, fb = spec
    return cell_svg(path, fb)


def coverage(mapping: dict) -> tuple[int, int]:
    dedicated = sum(1 for spec in mapping.values() if not spec[-1])
    return dedicated, len(COLS)


SETS = [
    {
        "name": "Native aktuell",
        "badge": "heute",
        "note": "Unicode-Emoji aus glyphEmoji() · kein Icon-Set",
        "license": "Unicode",
        "style": "Emoji / schwach",
        "night": "nur Klar (☾); Teilw. = gleiches ⛅",
        "map": NATIVE,
        "klass": "row-native",
    },
    {
        "name": "PWA Material Symbols",
        "badge": "PWA",
        "note": "Rounded outlined · wie weather.ts",
        "license": "Apache 2.0",
        "style": "Outlined / tonal",
        "night": "Klar + Teilw. bewölkt",
        "map": PWA_MS,
        "klass": "row-mono",
    },
    {
        "name": "Material Symbols Filled",
        "badge": "Empfehlung",
        "note": "Gleiche Glyphen, filled · MY3E / Widgets",
        "license": "Apache 2.0",
        "style": "Filled / tonal",
        "night": "Klar + Teilw. bewölkt",
        "map": MS_FILL,
        "klass": "row-mono",
    },
    {
        "name": "Meteocons Fill",
        "badge": "",
        "note": "Bas Milius · farbig · volle WMO-Familie",
        "license": "MIT",
        "style": "Color fill",
        "night": "fast alle Zustände inkl. Niederschlag",
        "map": METEO_FILL,
        "klass": "row-color",
    },
    {
        "name": "Meteocons Line",
        "badge": "",
        "note": "Bas Milius · Outline, aber feste Farben",
        "license": "MIT",
        "style": "Color line",
        "night": "wie Fill",
        "map": METEO_LINE,
        "klass": "row-color",
    },
    {
        "name": "Weather Icons",
        "badge": "",
        "note": "Erik Flowers · meteorologisch vollständig",
        "license": "SIL OFL 1.1",
        "style": "Mono fill",
        "night": "Klar + Teilw. + Schauer/Gewitter",
        "map": WEATHER_ICONS,
        "klass": "row-mono row-wi",
    },
]


def main() -> None:
    head_cells = "".join(
        f'<th><span class="cond">{label}</span><span class="wmo">WMO {wmo}</span></th>'
        for _key, label, wmo in COLS
    )

    rows = []
    for s in SETS:
        ded, total = coverage(s["map"])
        badge = f'<span class="badge">{s["badge"]}</span>' if s["badge"] else ""
        cells = "".join(f"<td>{render_cell(s['map'][key])}</td>" for key, *_ in COLS)
        rows.append(
            f"""
        <tr class="{s['klass']}">
          <th>
            <div class="set-name">{s['name']} {badge}</div>
            <div class="set-note">{s['note']}</div>
            <div class="meta">
              <span>{s['license']}</span>
              <span>{s['style']}</span>
              <span class="cov">{ded}/{total} eigen</span>
            </div>
            <div class="set-note">Nacht: {s['night']}</div>
          </th>
          {cells}
        </tr>"""
        )

    html = f"""<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="utf-8"/>
<title>Wetter-Icons im Vergleich — Native Android</title>
<style>
  html {{ font-size: 16px; }}
  * {{ box-sizing: border-box; }}
  body {{
    margin: 0;
    background: #f3edf7;
    color: #1d1b20;
    font-family: "Roboto", "DejaVu Sans", ui-sans-serif, sans-serif;
  }}
  .board {{
    width: 2360px;
    padding: 28px 32px 36px;
  }}
  h1 {{
    font-size: 1.85rem;
    font-weight: 700;
    letter-spacing: -0.03em;
    margin: 0 0 0.25rem;
  }}
  .lead {{
    color: #49454f;
    font-size: 0.92rem;
    margin: 0 0 0.15rem;
    max-width: 72rem;
  }}
  table {{
    width: 100%;
    border-collapse: separate;
    border-spacing: 0 10px;
    margin-top: 1rem;
  }}
  thead th {{
    font-size: 0.68rem;
    font-weight: 600;
    color: #49454f;
    text-align: center;
    padding: 0 0.15rem 0.4rem;
    vertical-align: bottom;
  }}
  thead th:first-child {{ text-align: left; width: 17.5rem; }}
  .cond {{ display: block; }}
  .wmo {{ display: block; font-weight: 500; opacity: 0.7; margin-top: 0.1rem; }}
  tbody tr th, tbody tr td {{
    background: #fffbfe;
    border-top: 1px solid #e7e0ec;
    border-bottom: 1px solid #e7e0ec;
  }}
  tbody tr th {{
    border-left: 1px solid #e7e0ec;
    border-radius: 1.4rem 0 0 1.4rem;
    text-align: left;
    width: 17.5rem;
    padding: 0.85rem 1rem;
    vertical-align: middle;
  }}
  tbody tr td:last-child {{
    border-right: 1px solid #e7e0ec;
    border-radius: 0 1.4rem 1.4rem 0;
  }}
  tbody tr td {{
    text-align: center;
    padding: 0.55rem 0.15rem;
    vertical-align: middle;
    min-width: 6.1rem;
  }}
  .set-name {{ font-size: 1.02rem; font-weight: 700; letter-spacing: -0.02em; }}
  .set-note {{ font-size: 0.7rem; color: #49454f; margin-top: 0.18rem; line-height: 1.25; }}
  .meta {{
    display: flex; flex-wrap: wrap; gap: 0.3rem;
    margin-top: 0.4rem;
  }}
  .meta span {{
    font-size: 0.62rem;
    font-weight: 600;
    background: #e8def8;
    color: #4a4458;
    padding: 0.12rem 0.42rem;
    border-radius: 999px;
  }}
  .meta .cov {{ background: #d0e8d0; color: #1b4320; }}
  .badge {{
    display: inline-block;
    background: #6750a4;
    color: #fff;
    font-size: 0.58rem;
    font-weight: 700;
    padding: 0.1rem 0.42rem;
    border-radius: 999px;
    vertical-align: middle;
    letter-spacing: 0.02em;
  }}
  .icon {{ position: relative; min-height: 3.4rem; display: flex; align-items: center; justify-content: center; }}
  svg.glyph {{ width: 52px; height: 52px; display: block; }}
  .row-mono svg.glyph {{ color: #1d1b20; fill: #1d1b20; }}
  .row-wi svg.glyph {{ fill: #1d1b20; }}
  .emoji {{ font-size: 2.35rem; line-height: 1; }}
  .miss {{
    color: #b0aab6;
    font-size: 1.35rem;
    font-weight: 500;
    min-height: 3.4rem;
    display: flex; align-items: center; justify-content: center;
    border: 1.5px dashed #cac4d0;
    border-radius: 0.75rem;
    margin: 0 0.35rem;
    height: 3.4rem;
  }}
  .fb {{
    position: absolute;
    bottom: -0.15rem;
    left: 50%;
    transform: translateX(-50%);
    font-size: 0.52rem;
    font-weight: 700;
    letter-spacing: 0.02em;
    background: #ffdad6;
    color: #410002;
    padding: 0.04rem 0.28rem;
    border-radius: 999px;
    white-space: nowrap;
  }}
  .legend {{
    margin-top: 0.85rem;
    font-size: 0.78rem;
    color: #49454f;
  }}
  .legend strong {{ color: #1d1b20; }}
</style>
</head>
<body>
  <div class="board">
    <h1>Wetter-Icons im Vergleich</h1>
    <p class="lead">Native Android-App · gleiche WMO-Zustände je Spalte · echte Glyphen (SVG / Unicode), keine Collage.</p>
    <p class="lead">28 App-Codes (0–99) fallen auf diese Familien. <strong>Fallback</strong> = Set hat kein eigenes Zeichen, App greift auf ein Nachbar-Icon zurück.</p>
    <table>
      <thead>
        <tr><th>Set · Lizenz · Abdeckung</th>{head_cells}</tr>
      </thead>
      <tbody>
        {''.join(rows)}
      </tbody>
    </table>
    <p class="legend">
      <strong>Empfehlung:</strong> Material Symbols Rounded Filled — Apache 2.0, MY3E-tauglich (einfärbbar),
      winzig im APK (~13 SVG), gleiche Zuordnung wie die PWA. Lücken: Schauer und Eisregen
      (weather_mix liegt ungenutzt im Set). Meteocons Fill hat die beste meteorologische Abdeckung
      inkl. Nacht-Niederschlag, ist aber bunt und schwerer dynamisch einzufärben.
    </p>
  </div>
</body>
</html>
"""
    OUT_HTML.parent.mkdir(parents=True, exist_ok=True)
    OUT_HTML.write_text(html, encoding="utf-8")
    print(f"wrote {OUT_HTML} ({OUT_HTML.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
