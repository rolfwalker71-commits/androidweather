#!/usr/bin/env python3
"""Rasterize real weather SVGs into a MY3E comparison board PNG."""

from __future__ import annotations

import io
from pathlib import Path

import cairosvg
from PIL import Image, ImageDraw, ImageFont

ROOT = Path("/home/rolf/DEV/androidweather")
ICONS = Path("/tmp/wx-icons")
OUT_ROOT = ROOT / "icon-set-vergleich.png"
OUT_DOCS = ROOT / "docs" / "icon-set-vergleich.png"

# MY3E-ish seed purple surface
BG = (243, 237, 247)
SURFACE = (255, 251, 254)
ON_SURFACE = (29, 27, 32)
ON_VARIANT = (73, 69, 79)
OUTLINE = (231, 224, 236)
PRIMARY = (103, 80, 164)
PRIMARY_CONT = (232, 222, 248)
ON_PRIMARY_CONT = (74, 68, 88)
TERTIARY_CONT = (208, 232, 208)
ON_TERTIARY = (27, 67, 32)
ERROR_CONT = (255, 218, 214)
ON_ERROR = (65, 0, 2)
DASH = (202, 196, 208)

W, H = 2400, 1420
LEFT_W = 300
PAD = 36
HEADER_H = 168
ROW_H = 188
ICON = 72
WELL = (237, 231, 240)

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


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    path = (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
        if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    )
    return ImageFont.truetype(path, size)


def raster_svg(path: Path, size: int = ICON, tint: str | None = None) -> Image.Image | None:
    if not path.exists():
        return None
    svg = path.read_text(encoding="utf-8")
    if tint:
        svg = svg.replace("currentColor", tint)
        if 'fill="' not in svg and "<path" in svg:
            svg = svg.replace("<path", f'<path fill="{tint}"')
    try:
        png = cairosvg.svg2png(
            bytestring=svg.encode("utf-8"),
            output_width=size * 2,
            output_height=size * 2,
            background_color="transparent",
        )
        im = Image.open(io.BytesIO(png)).convert("RGBA")
        return im.resize((size, size), Image.Resampling.LANCZOS)
    except Exception as exc:  # noqa: BLE001
        print(f"raster fail {path}: {exc}")
        return None


def emoji_svg(ch: str) -> str:
    """Simple monochrome stand-ins matching native glyphEmoji() characters."""
    # Drawn as compact 64 viewBox icons so the native row is recognizable, not a collage.
    if ch == "☀":
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
          <circle cx="32" cy="32" r="12" fill="none" stroke="#1d1b20" stroke-width="3"/>
          <g stroke="#1d1b20" stroke-width="3" stroke-linecap="round">
            <path d="M32 6v8M32 50v8M6 32h8M50 32h8M12.5 12.5l5.5 5.5M46 46l5.5 5.5M12.5 51.5l5.5-5.5M46 18l5.5-5.5"/>
          </g></svg>"""
    if ch == "☾":
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
          <path d="M40 10a22 22 0 1 0 14 38A20 20 0 0 1 40 10z" fill="none" stroke="#1d1b20" stroke-width="3"/></svg>"""
    if ch == "⛅":
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
          <circle cx="24" cy="24" r="8" fill="none" stroke="#1d1b20" stroke-width="2.5"/>
          <g stroke="#1d1b20" stroke-width="2.5" stroke-linecap="round">
            <path d="M24 8v5M8 24h5M13 13l3.5 3.5M35 13l-3.5 3.5"/>
          </g>
          <path d="M22 42h24a8 8 0 0 0 0-16 11 11 0 0 0-21-3 9 9 0 0 0-3 19z" fill="none" stroke="#1d1b20" stroke-width="2.5"/>
        </svg>"""
    if ch == "☁":
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
          <path d="M20 44h26a9 9 0 0 0 0-18 12 12 0 0 0-23-3A10 10 0 0 0 20 44z" fill="none" stroke="#1d1b20" stroke-width="3"/></svg>"""
    if ch == "🌫":
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
          <g stroke="#1d1b20" stroke-width="3" stroke-linecap="round">
            <path d="M12 22h32M18 32h34M12 42h28"/>
          </g></svg>"""
    if ch == "🌧":
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
          <path d="M20 34h26a8 8 0 0 0 0-16 11 11 0 0 0-21-3A9 9 0 0 0 20 34z" fill="none" stroke="#1d1b20" stroke-width="2.5"/>
          <g stroke="#1d1b20" stroke-width="2.5" stroke-linecap="round">
            <path d="M24 40v8M32 42v8M40 40v8"/>
          </g></svg>"""
    if ch == "❄":
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
          <g stroke="#1d1b20" stroke-width="3" stroke-linecap="round">
            <path d="M32 10v44M12 21l40 22M12 43l40-22"/>
            <path d="M32 18l-5-4M32 18l5-4M32 46l-5 4M32 46l5 4"/>
          </g></svg>"""
    if ch == "⛈":
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
          <path d="M18 32h26a8 8 0 0 0 0-16 11 11 0 0 0-21-3A9 9 0 0 0 18 32z" fill="none" stroke="#1d1b20" stroke-width="2.5"/>
          <path d="M34 32l-8 12h8l-6 12" fill="none" stroke="#1d1b20" stroke-width="2.5" stroke-linejoin="round"/></svg>"""
    if ch == "🌨":
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
          <path d="M20 32h26a8 8 0 0 0 0-16 11 11 0 0 0-21-3A9 9 0 0 0 20 32z" fill="none" stroke="#1d1b20" stroke-width="2.5"/>
          <g fill="#1d1b20"><circle cx="24" cy="40" r="2"/><circle cx="32" cy="46" r="2"/><circle cx="40" cy="40" r="2"/></g>
        </svg>"""
    return ""


def raster_emoji(ch: str) -> Image.Image | None:
    svg = emoji_svg(ch)
    if not svg:
        return None
    png = cairosvg.svg2png(bytestring=svg.encode(), output_width=ICON * 2, output_height=ICON * 2)
    return Image.open(io.BytesIO(png)).convert("RGBA").resize((ICON, ICON), Image.Resampling.LANCZOS)


def paste_center(dst: Image.Image, src: Image.Image, box: tuple[int, int, int, int]) -> None:
    x0, y0, x1, y1 = box
    x = x0 + (x1 - x0 - src.width) // 2
    y = y0 + (y1 - y0 - src.height) // 2 - 4
    dst.alpha_composite(src, (x, y))


def rounded_rect(draw: ImageDraw.ImageDraw, box, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def chip(draw, xy, text, bg, fg, f):
    x, y = xy
    bbox = f.getbbox(text)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    pad_x, pad_y = 7, 3
    rounded_rect(draw, (x, y, x + tw + pad_x * 2, y + th + pad_y * 2 + 2), 10, bg)
    draw.text((x + pad_x, y + pad_y), text, font=f, fill=fg)
    return tw + pad_x * 2 + 6


MS_O = ICONS / "ms-outline"
MS_F = ICONS / "ms-fill"
MC_F = ICONS / "meteocons-fill"
MC_L = ICONS / "meteocons-line"
WI = ICONS / "weather-icons"

NATIVE = {
    "klar": ("emoji", "☀", False),
    "nacht": ("emoji", "☾", False),
    "teilw_tag": ("emoji", "⛅", False),
    "teilw_nacht": ("emoji", "⛅", True),
    "bedeckt": ("emoji", "☁", False),
    "nebel": ("emoji", "🌫", False),
    "niesel": ("emoji", "🌧", True),
    "regen": ("emoji", "🌧", False),
    "starkregen": ("emoji", "🌧", True),
    "schauer": ("emoji", "🌧", True),
    "schnee": ("emoji", "❄", False),
    "eisregen": ("emoji", "🌧", True),
    "gewitter": ("emoji", "⛈", False),
    "hagel": ("emoji", "🌨", True),
}

PWA_MS = {
    "klar": (MS_O / "sunny.svg", False, True),
    "nacht": (MS_O / "nightlight.svg", False, True),
    "teilw_tag": (MS_O / "partly_cloudy_day.svg", False, True),
    "teilw_nacht": (MS_O / "partly_cloudy_night.svg", False, True),
    "bedeckt": (MS_O / "cloud.svg", False, True),
    "nebel": (MS_O / "foggy.svg", False, True),
    "niesel": (MS_O / "rainy_light.svg", False, True),
    "regen": (MS_O / "rainy.svg", False, True),
    "starkregen": (MS_O / "rainy_heavy.svg", False, True),
    "schauer": (MS_O / "rainy.svg", True, True),
    "schnee": (MS_O / "weather_snowy.svg", False, True),
    "eisregen": (MS_O / "rainy.svg", True, True),
    "gewitter": (MS_O / "thunderstorm.svg", False, True),
    "hagel": (MS_O / "weather_hail.svg", False, True),
}

MS_FILL = {k: (MS_F / p.name, fb, True) for k, (p, fb, _) in PWA_MS.items()}

METEO_FILL = {
    "klar": (MC_F / "clear-day.svg", False, False),
    "nacht": (MC_F / "clear-night.svg", False, False),
    "teilw_tag": (MC_F / "partly-cloudy-day.svg", False, False),
    "teilw_nacht": (MC_F / "partly-cloudy-night.svg", False, False),
    "bedeckt": (MC_F / "overcast.svg", False, False),
    "nebel": (MC_F / "fog.svg", False, False),
    "niesel": (MC_F / "drizzle.svg", False, False),
    "regen": (MC_F / "rain.svg", False, False),
    "starkregen": (MC_F / "extreme-rain.svg", False, False),
    "schauer": (MC_F / "partly-cloudy-day-rain.svg", False, False),
    "schnee": (MC_F / "snow.svg", False, False),
    "eisregen": (MC_F / "sleet.svg", False, False),
    "gewitter": (MC_F / "thunderstorms.svg", False, False),
    "hagel": (MC_F / "hail.svg", False, False),
}

METEO_LINE = {k: (MC_L / p.name, fb, False) for k, (p, fb, _) in METEO_FILL.items()}

WEATHER_ICONS = {
    "klar": (WI / "wi-day-sunny.svg", False, True),
    "nacht": (WI / "wi-night-clear.svg", False, True),
    "teilw_tag": (WI / "wi-day-cloudy.svg", False, True),
    "teilw_nacht": (WI / "wi-night-alt-cloudy.svg", False, True),
    "bedeckt": (WI / "wi-cloudy.svg", False, True),
    "nebel": (WI / "wi-fog.svg", False, True),
    "niesel": (WI / "wi-sprinkle.svg", False, True),
    "regen": (WI / "wi-rain.svg", False, True),
    "starkregen": (WI / "wi-rain-wind.svg", False, True),
    "schauer": (WI / "wi-showers.svg", False, True),
    "schnee": (WI / "wi-snow.svg", False, True),
    "eisregen": (WI / "wi-sleet.svg", False, True),
    "gewitter": (WI / "wi-thunderstorm.svg", False, True),
    "hagel": (WI / "wi-hail.svg", False, True),
}

SETS = [
    ("Native aktuell", "heute", "Unicode-Emoji (glyphEmoji)", "Unicode", "Emoji", "nur Klar", NATIVE, 8),
    ("PWA Material Symbols", "PWA", "Rounded outlined · weather.ts", "Apache 2.0", "Outlined", "Klar + Teilw.", PWA_MS, 12),
    ("Material Symbols Filled", "Empfehlung", "Gleiche Glyphen, filled", "Apache 2.0", "Filled", "Klar + Teilw.", MS_FILL, 12),
    ("Meteocons Fill", "", "Bas Milius · farbig", "MIT", "Color fill", "fast alle", METEO_FILL, 14),
    ("Meteocons Line", "", "Bas Milius · Outline+Farbe", "MIT", "Color line", "wie Fill", METEO_LINE, 14),
    ("Weather Icons", "", "Erik Flowers · meteorologisch", "SIL OFL 1.1", "Mono fill", "Klar/Teilw./Schauer", WEATHER_ICONS, 14),
]


def load_cell(spec):
    if spec[0] == "emoji":
        return raster_emoji(spec[1]), spec[2]
    path, fb, tint = spec
    img = raster_svg(path, ICON, "#1d1b20" if tint else None)
    return img, fb


def main() -> None:
    img = Image.new("RGBA", (W, H), BG + (255,))
    draw = ImageDraw.Draw(img)
    f_title = font(36, True)
    f_lead = font(16)
    f_set = font(18, True)
    f_note = font(12)
    f_chip = font(11, True)
    f_cond = font(13, True)
    f_wmo = font(11)
    f_fb = font(10, True)

    draw.text((PAD, 28), "Wetter-Icons im Vergleich", font=f_title, fill=ON_SURFACE)
    draw.text(
        (PAD, 78),
        "Native Android-App  ·  gleiche WMO-Zustände je Spalte  ·  echte SVG/Unicode-Glyphen",
        font=f_lead,
        fill=ON_VARIANT,
    )
    draw.text(
        (PAD, 102),
        "28 App-Codes (0–99) fallen auf diese Familien.  Fallback = kein eigenes Zeichen, Nachbar-Icon.",
        font=f_lead,
        fill=ON_VARIANT,
    )

    grid_x = PAD + LEFT_W
    grid_w = W - PAD - grid_x
    col_w = grid_w / len(COLS)
    y0 = HEADER_H

    # column headers
    for i, (_k, label, wmo) in enumerate(COLS):
        cx = grid_x + i * col_w + col_w / 2
        bbox = f_cond.getbbox(label)
        draw.text((cx - (bbox[2] - bbox[0]) / 2, 128), label, font=f_cond, fill=ON_VARIANT)
        bbox2 = f_wmo.getbbox(f"WMO {wmo}")
        draw.text((cx - (bbox2[2] - bbox2[0]) / 2, 146), f"WMO {wmo}", font=f_wmo, fill=(*ON_VARIANT, 180))

    for r, (name, badge, note, license_, style, night, mapping, dedicated) in enumerate(SETS):
        top = y0 + r * ROW_H
        bot = top + ROW_H - 12
        rounded_rect(draw, (PAD, top, W - PAD, bot), 28, SURFACE, OUTLINE, 1)

        # left label
        draw.text((PAD + 20, top + 18), name, font=f_set, fill=ON_SURFACE)
        if badge:
            bw = f_chip.getbbox(badge)
            bx = PAD + 20 + f_set.getbbox(name)[2] + 12
            rounded_rect(
                draw,
                (bx, top + 20, bx + bw[2] - bw[0] + 16, top + 20 + 20),
                10,
                PRIMARY,
            )
            draw.text((bx + 8, top + 22), badge, font=f_chip, fill=(255, 255, 255))
        draw.text((PAD + 20, top + 46), note, font=f_note, fill=ON_VARIANT)

        xchip = PAD + 20
        ychip = top + 70
        xchip += chip(draw, (xchip, ychip), license_, PRIMARY_CONT, ON_PRIMARY_CONT, f_chip)
        xchip += chip(draw, (xchip, ychip), style, PRIMARY_CONT, ON_PRIMARY_CONT, f_chip)
        chip(draw, (xchip, ychip), f"{dedicated}/14 eigen", TERTIARY_CONT, ON_TERTIARY, f_chip)
        draw.text((PAD + 20, top + 98), f"Nacht: {night}", font=f_note, fill=ON_VARIANT)

        for i, (key, _label, _wmo) in enumerate(COLS):
            cx0 = int(grid_x + i * col_w)
            cx1 = int(grid_x + (i + 1) * col_w)
            spec = mapping[key]
            cell, fb = load_cell(spec)
            box = (cx0, top + 16, cx1, bot - 22)
            well = (
                int((cx0 + cx1) / 2 - 44),
                top + 22,
                int((cx0 + cx1) / 2 + 44),
                top + 22 + 88,
            )
            rounded_rect(draw, well, 20, WELL)
            if cell is None:
                # dashed empty
                mx0, my0 = cx0 + 18, top + 36
                mx1, my1 = cx1 - 18, bot - 36
                draw.rounded_rectangle((mx0, my0, mx1, my1), 12, outline=DASH, width=2)
                miss = "—"
                mb = f_set.getbbox(miss)
                draw.text(
                    ((mx0 + mx1 - (mb[2] - mb[0])) / 2, (my0 + my1 - 18) / 2),
                    miss,
                    font=f_set,
                    fill=DASH,
                )
            else:
                paste_center(img, cell, box)
            if fb:
                label = "Fallback"
                lb = f_fb.getbbox(label)
                lw = lb[2] - lb[0] + 12
                lx = int((cx0 + cx1 - lw) / 2)
                ly = bot - 28
                rounded_rect(draw, (lx, ly, lx + lw, ly + 16), 8, ERROR_CONT)
                draw.text((lx + 6, ly + 2), label, font=f_fb, fill=ON_ERROR)

    import textwrap

    footer = (
        "Empfehlung: Material Symbols Rounded Filled — Apache 2.0, MY3E-einfärbbar, ~13 SVG im APK, "
        "gleiche Zuordnung wie die PWA. Lücken: Schauer + Eisregen (weather_mix ungenutzt). "
        "Meteocons Fill: beste Abdeckung inkl. Nacht-Niederschlag, aber bunt."
    )
    fy = H - 52
    for line in textwrap.wrap(footer, width=175):
        draw.text((PAD, fy), line, font=f_note, fill=ON_VARIANT)
        fy += 16

    rgb = img.convert("RGB")
    OUT_DOCS.parent.mkdir(parents=True, exist_ok=True)
    rgb.save(OUT_ROOT, "PNG", optimize=True)
    rgb.save(OUT_DOCS, "PNG", optimize=True)
    print(f"wrote {OUT_ROOT} {OUT_ROOT.stat().st_size} bytes {rgb.size}")
    print(f"wrote {OUT_DOCS} {OUT_DOCS.stat().st_size} bytes")


if __name__ == "__main__":
    main()
