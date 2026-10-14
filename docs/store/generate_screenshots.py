#!/usr/bin/env python3
"""Generate Play Store phone screenshots (1080x1920) for N Player.

The frames reproduce the 1.1.0 UI: the brand near-black/blue palette, rounded
poster cards with a duration pill, section headers with counts, the player's
+/-10s buttons plus the 20/40-minute quick-jump row, and a selected-label-only
bottom bar. Output: docs/store/screenshots/phone-*.png
"""

import pathlib
import subprocess

W, H = 1080, 1920
BG = "#08080C"
SURFACE = "#0F0F15"
SURFACE_VARIANT = "#1B1B23"
ON_SURFACE = "#E4E2E9"
ON_SURFACE_VARIANT = "#BFC6D4"
PRIMARY = "#9ECAFF"
ON_PRIMARY = "#002A55"
PRIMARY_CONTAINER = "#123B66"
SECONDARY_CONTAINER = "#1B2531"
OUTLINE_VARIANT = "#262A33"

# Grid geometry, scaled from the app's 116 dp cards at roughly 3x.
PAD = 42
GAP = 36
CARD_W = 300
CARD_H_VIDEO = int(CARD_W * 9 / 16)
CARD_H_AUDIO = int(CARD_W * 3 / 2)
ROW_TOP = 210
BAR_H = 210

FONT = "system-ui, -apple-system, 'DejaVu Sans', 'Noto Sans', sans-serif"


def text(x, y, s, size=28, fill=ON_SURFACE, weight=400, anchor="start", opacity=1.0):
    return (
        f'<text x="{x}" y="{y}" font-family="{FONT}" font-size="{size}" '
        f'font-weight="{weight}" fill="{fill}" text-anchor="{anchor}" '
        f'opacity="{opacity}">{s}</text>'
    )


def poster(x, y, w, h, kind, title, subtitle, badge):
    """A rounded card: artwork, scrim, duration pill, title, subtitle."""
    top = f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="44" fill="{SURFACE_VARIANT}"/>'
    if kind == "video":
        art = (
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="44" fill="url(#art)"/>'
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="44" fill="url(#scrim)"/>'
        )
    else:
        art = (
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="44" fill="url(#audio)"/>'
            f'<g transform="translate({x + w // 2 - 34},{y + h // 2 - 34})">'
            f'<circle cx="34" cy="34" r="34" fill="{PRIMARY}" opacity="0.16"/>'
            f'<path d="M40,16 v30 a8,8 0 1 1 -6,-8 v-22 h18 v8 h-12" fill="none" '
            f'stroke="{ON_SURFACE_VARIANT}" stroke-width="4" stroke-linejoin="round"/>'
            f'</g>'
        )
    pill = ""
    if badge:
        bw = 26 + len(badge) * 15
        pill = (
            f'<rect x="{x + w - bw - 18}" y="{y + h - 54}" width="{bw}" height="38" '
            f'rx="14" fill="#000000" opacity="0.62"/>'
            + text(x + w - bw // 2 - 18, y + h - 27, badge, size=22, fill="#FFFFFF",
                   anchor="middle", weight=500)
        )
    label = (
        text(x + 6, y + h + 44, title, size=28, weight=500)
        + text(x + 6, y + h + 82, subtitle, size=23, fill=ON_SURFACE_VARIANT)
    )
    return top + art + pill + label


def bottom_bar(selected_index, labels):
    """Icon row with the selected destination highlighted and labelled."""
    y = H - BAR_H
    parts = [f'<rect x="0" y="{y}" width="{W}" height="{BAR_H}" fill="{SURFACE}"/>']
    n = len(labels)
    step = W / n
    for i in range(n):
        cx = int(step * i + step / 2)
        sel = i == selected_index
        if sel:
            parts.append(
                f'<rect x="{cx - 58}" y="{y + 34}" width="116" height="66" rx="33" '
                f'fill="{PRIMARY_CONTAINER}"/>'
            )
        col = PRIMARY if sel else ON_SURFACE_VARIANT
        glyph = {
            0: f'<path d="M{cx-18},{y+52} l18,-15 l18,15 v26 h-36 z" fill="none" stroke="{col}" stroke-width="3.4" stroke-linejoin="round"/>',
            1: f'<circle cx="{cx-4}" cy="{y+60}" r="13" fill="none" stroke="{col}" stroke-width="3.4"/><path d="M{cx+7},{y+71} l14,14" stroke="{col}" stroke-width="3.4" stroke-linecap="round"/>',
            2: f'<path d="M{cx-19},{y+54} h13 l5,-5 h16 a3,3 0 0 1 3,3 v24 h-37 z" fill="none" stroke="{col}" stroke-width="3.4" stroke-linejoin="round"/>',
            3: f'<path d="M{cx},{y+78} c-20,-14 -30,-24 -30,-34 a14,14 0 0 1 30,-6 a14,14 0 0 1 30,6 c0,10 -10,20 -30,34 z" fill="none" stroke="{col}" stroke-width="3.4"/>',
            4: f'<path d="M{cx-16},{y+54} h32 M{cx-16},{y+66} h32 M{cx-16},{y+78} h20" stroke="{col}" stroke-width="3.4" stroke-linecap="round"/>',
            5: f'<circle cx="{cx}" cy="{y+66}" r="16" fill="none" stroke="{col}" stroke-width="3.4"/><circle cx="{cx}" cy="{y+66}" r="5" fill="{col}"/>',
        }[i]
        parts.append(glyph)
        if sel:
            parts.append(text(cx, y + 132, labels[i], size=24, fill=PRIMARY,
                              anchor="middle", weight=600))
    return "".join(parts)


def header(title, sub=None):
    out = text(PAD, 128, title, size=52, weight=700)
    if sub:
        out += text(PAD, 172, sub, size=26, fill=ON_SURFACE_VARIANT)
    return out


def section(y, label, count):
    return (
        f'<rect x="{PAD}" y="{y - 22}" width="9" height="40" rx="4" fill="{PRIMARY}"/>'
        + text(PAD + 26, y + 6, label, size=34, weight=600)
        + f'<rect x="{W - PAD - 92}" y="{y - 26}" width="92" height="48" rx="24" fill="{SECONDARY_CONTAINER}"/>'
        + text(W - PAD - 46, y + 6, str(count), size=24, fill=ON_SURFACE_VARIANT,
               anchor="middle", weight=600)
    )


def toggle_row(y, title, sub, on):
    col = PRIMARY if on else "#4A4F5C"
    knob_x = 880 if on else 812
    return (
        text(PAD, y, title, size=30)
        + text(PAD, y + 40, sub, size=24, fill=ON_SURFACE_VARIANT)
        + f'<rect x="800" y="{y-30}" width="120" height="64" rx="32" fill="{col}"/>'
        + f'<circle cx="{knob_x}" cy="{y+2}" r="26" fill="#FFFFFF"/>'
    )


def frame(inner, title, desc, w=W, h=H):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}" role="img" aria-labelledby="t d">
<title id="t">{title}</title><desc id="d">{desc}</desc>
<defs>
<linearGradient id="art" x1="0" y1="0" x2="1" y2="1">
<stop offset="0%" stop-color="#2A3F63"/><stop offset="55%" stop-color="#16203A"/><stop offset="100%" stop-color="#0A0E1A"/>
</linearGradient>
<linearGradient id="audio" x1="0" y1="0" x2="1" y2="1">
<stop offset="0%" stop-color="#23233A"/><stop offset="100%" stop-color="#141420"/>
</linearGradient>
<linearGradient id="scrim" x1="0" y1="0" x2="0" y2="1">
<stop offset="55%" stop-color="#000000" stop-opacity="0"/><stop offset="100%" stop-color="#000000" stop-opacity="0.45"/>
</linearGradient>
<linearGradient id="hero" x1="0" y1="0" x2="0" y2="1">
<stop offset="60%" stop-color="{BG}" stop-opacity="0"/><stop offset="100%" stop-color="{BG}"/>
</linearGradient>
<radialGradient id="glow" cx="0.5" cy="0.5" r="0.6">
<stop offset="0%" stop-color="{PRIMARY}" stop-opacity="0.35"/><stop offset="100%" stop-color="{PRIMARY}" stop-opacity="0"/>
</radialGradient>
</defs>
<rect width="{w}" height="{h}" fill="{BG}"/>
{inner}
</svg>'''


def screen_home():
    parts = [header("N Player", "Your media, on your device")]
    parts.append(section(ROW_TOP + 40, "Videos", 128))
    y = ROW_TOP + 110
    videos = [("Trip 2024", "1920x1080", "1:12:04"), ("Sunset clip", "3840x2160", "0:48"),
              ("Skate run", "1280x720", "12:31")]
    for i, (t, s, b) in enumerate(videos):
        parts.append(poster(PAD + i * (CARD_W + GAP), y, CARD_W, CARD_H_VIDEO, "video", t, s, b))
    y2 = y + CARD_H_VIDEO + 150
    parts.append(section(y2, "Music", 64))
    y2 += 70
    for i, (t, s, b) in enumerate([("Nightdrive", "Aurora", "4:12"), ("Bloom", "Kite", "3:38"),
                                   ("Low Tide", "Mira", "5:01")]):
        parts.append(poster(PAD + i * (CARD_W + GAP), y2, CARD_W, CARD_H_AUDIO, "audio", t, s, b))
    parts.append(bottom_bar(0, ["Home", "Search", "Folders", "Favourites", "Playlists", "Settings"]))
    return frame("".join(parts), "N Player home",
                 "Home screen: a Videos grid of rounded thumbnail cards with duration pills, "
                 "a Music grid of audio cards, and a bottom bar with the Home destination selected.")


def screen_player():
    parts = [
        f'<rect width="{W}" height="{H}" fill="#000"/>',
        f'<rect width="{W}" height="{H}" fill="url(#art)"/>',
        f'<rect width="{W}" height="{H}" fill="#000" opacity="0.38"/>',
    ]
    parts.append(f'<circle cx="{PAD + 26}" cy="96" r="42" fill="#000" opacity="0.55"/>')
    parts.append(f'<path d="M{PAD+36},80 l-22,16 l22,16" fill="none" stroke="#FFF" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>')
    parts.append(text(PAD + 92, 108, "Trip 2024", size=34, weight=600, fill="#FFFFFF"))
    parts.append(f'<rect x="{W-PAD-64}" y="82" width="34" height="26" rx="6" fill="none" stroke="#FFF" stroke-width="4"/>')
    parts.append(f'<path d="M{W-PAD-56},82 v-10 a13,13 0 0 1 18,0 v10" fill="none" stroke="#FFF" stroke-width="4"/>')
    cy = 800
    parts.append(f'<g transform="translate({W//2-300},{cy-52})"><path d="M26,0 A26,26 0 1 0 52,26" fill="none" stroke="#FFF" stroke-width="5" stroke-linecap="round"/><path d="M26,0 l0,13 l-13,-6 z" fill="#FFF"/>{text(26,44,"10",size=20,fill="#FFF",anchor="middle")}</g>')
    parts.append(f'<circle cx="{W//2}" cy="{cy}" r="72" fill="{PRIMARY}"/>')
    parts.append(f'<rect x="{W//2-24}" y="{cy-26}" width="14" height="52" rx="5" fill="{ON_PRIMARY}"/>')
    parts.append(f'<rect x="{W//2+10}" y="{cy-26}" width="14" height="52" rx="5" fill="{ON_PRIMARY}"/>')
    parts.append(f'<g transform="translate({W//2+248},{cy-52})"><path d="M26,0 A26,26 0 1 1 0,26" fill="none" stroke="#FFF" stroke-width="5" stroke-linecap="round"/><path d="M26,0 l0,13 l13,-6 z" fill="#FFF"/>{text(26,44,"10",size=20,fill="#FFF",anchor="middle")}</g>')
    qy = cy + 150
    labels = ["+20m", "+40m", "\u221220m", "\u221240m"]
    step = (W - 2 * PAD) / len(labels)
    for i, lab in enumerate(labels):
        cx = int(PAD + step * i + step / 2)
        parts.append(f'<rect x="{cx-92}" y="{qy-42}" width="184" height="84" rx="42" fill="#FFFFFF" opacity="0.10"/>')
        parts.append(text(cx, qy + 12, lab, size=36, weight=600, fill="#FFFFFF", anchor="middle"))
    sy = qy + 210
    parts.append(text(PAD, sy, "24:18", size=28, fill="#FFFFFF"))
    parts.append(text(W - PAD, sy, "\u22121:12:04", size=28, fill="#FFFFFF", anchor="end"))
    parts.append(f'<rect x="{PAD}" y="{sy+26}" width="{W-2*PAD}" height="14" rx="7" fill="#33363F"/>')
    parts.append(f'<rect x="{PAD}" y="{sy+26}" width="{(W-2*PAD)//3}" height="14" rx="7" fill="{PRIMARY}"/>')
    parts.append(f'<circle cx="{PAD+(W-2*PAD)//3}" cy="{sy+33}" r="20" fill="{PRIMARY}"/>')
    chips = ["1x", "Fit", "Repeat", "Shuffle", "A\u2013B", "Sleep", "Night", "PiP", "Equalizer", "Subtitle"]
    cx = PAD
    for chip in chips:
        w = 40 + len(chip) * 20
        parts.append(f'<rect x="{cx}" y="{sy+90}" width="{w}" height="76" rx="38" fill="#FFFFFF" opacity="0.10"/>')
        parts.append(text(cx + w // 2, sy + 140, chip, size=26, fill="#FFFFFF", anchor="middle"))
        cx += w + 18
    return frame("".join(parts), "N Player player",
                 "Player screen: full-bleed video under an overlay with back, title and lock, "
                 "ten-second rewind and forward around a large pause button, a row of "
                 "+20m, +40m, -20m and -40m quick jumps, a scrubber showing elapsed and "
                 "remaining time, and a row of speed, aspect, repeat, shuffle, A-B, sleep, "
                 "night, PiP, equalizer and subtitle controls.")


def screen_detail():
    parts = [
        f'<rect x="0" y="0" width="{W}" height="620" fill="url(#art)"/>',
        f'<rect x="0" y="0" width="{W}" height="620" fill="url(#hero)"/>',
        f'<circle cx="{PAD+30}" cy="96" r="44" fill="#000" opacity="0.45"/>',
        f'<path d="M{PAD+42},80 l-24,16 l24,16" fill="none" stroke="#FFF" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>',
        f'<circle cx="{W//2}" cy="310" r="66" fill="#FFFFFF" opacity="0.18"/>',
        f'<path d="M{W//2-18},276 l52,34 l-52,34 z" fill="#FFFFFF"/>',
        text(PAD, 700, "Trip 2024", size=56, weight=700),
        text(PAD, 756, "1920x1080  \u2022  1:12:04  \u2022  1.4 GB", size=28, fill=ON_SURFACE_VARIANT),
    ]
    parts.append(f'<rect x="{PAD}" y="796" width="360" height="62" rx="20" fill="{PRIMARY_CONTAINER}"/>')
    parts.append(text(PAD + 22, 838, "Resumes at 24:18", size=26, fill="#D3E4FF"))
    by = 900
    parts.append(f'<rect x="{PAD}" y="{by}" width="290" height="96" rx="28" fill="{PRIMARY}"/>')
    parts.append(f'<path d="M{PAD+44},{by+30} l36,18 l-36,18 z" fill="{ON_PRIMARY}"/>')
    parts.append(text(PAD + 104, by + 62, "Resume", size=32, weight=600, fill=ON_PRIMARY))
    for i, glyph in enumerate(["heart", "add", "hide"]):
        cx = PAD + 330 + i * 116
        parts.append(f'<circle cx="{cx+44}" cy="{by+48}" r="48" fill="{SECONDARY_CONTAINER}"/>')
        if glyph == "heart":
            parts.append(f'<path d="M{cx+44},{by+72} c-26,-18 -38,-30 -38,-44 a18,18 0 0 1 38,-8 a18,18 0 0 1 38,8 c0,14 -12,26 -38,44 z" fill="{PRIMARY}"/>')
        elif glyph == "add":
            parts.append(f'<path d="M{cx+44},{by+28} v40 M{cx+24},{by+48} h40" stroke="{PRIMARY}" stroke-width="6" stroke-linecap="round"/>')
        else:
            parts.append(f'<path d="M{cx+24},{by+40} h40 M{cx+44},{by+40} l-10,-10 M{cx+44},{by+40} l-10,10" stroke="{PRIMARY}" stroke-width="6" stroke-linecap="round" fill="none"/>')
    parts.append(bottom_bar(-1, ["Home", "Search", "Folders", "Favourites", "Playlists", "Settings"]))
    return frame("".join(parts), "N Player detail",
                 "Detail screen: a video hero header that fades into the page, the title and "
                 "metadata, a resume chip, a Play/Resume button and round favourite, "
                 "add-to-playlist and hide actions.")


def screen_search():
    parts = [header("Search", "Find a file on this device")]
    parts.append(f'<rect x="{PAD}" y="210" width="{W-2*PAD}" height="112" rx="28" fill="{SURFACE_VARIANT}"/>')
    parts.append(f'<circle cx="{PAD+58}" cy="266" r="22" fill="none" stroke="{PRIMARY}" stroke-width="5"/>')
    parts.append(f'<path d="M{PAD+74},282 l24,24" stroke="{PRIMARY}" stroke-width="5" stroke-linecap="round"/>')
    parts.append(text(PAD + 116, 280, "trip", size=34, weight=500))
    parts.append(f'<rect x="{W-PAD-46}" y="248" width="4" height="40" fill="{PRIMARY}"/>')
    y = 400
    for i, (t, s, b) in enumerate([("Trip 2024", "1920x1080", "1:12:04"), ("Trip cutdown", "1280x720", "0:48"),
                                   ("Trip to the coast", "1920x1080", "22:07")]):
        parts.append(poster(PAD + i * (CARD_W + GAP), y, CARD_W, CARD_H_VIDEO, "video", t, s, b))
    y2 = y + CARD_H_VIDEO + 150
    for i, (t, s, b) in enumerate([("Trip (live)", "Aurora", "6:12"), ("Trip hop", "Kite", "3:38"),
                                   ("Road trip", "Mira", "4:44")]):
        parts.append(poster(PAD + i * (CARD_W + GAP), y2, CARD_W, CARD_H_AUDIO, "audio", t, s, b))
    parts.append(bottom_bar(1, ["Home", "Search", "Folders", "Favourites", "Playlists", "Settings"]))
    return frame("".join(parts), "N Player search",
                 "Search screen: a search field with the query 'trip' and a grid of matching "
                 "video and audio cards, with the Search destination selected in the bottom bar.")


def screen_folders():
    parts = [header("Folders", "Browse the device tree")]
    parts.append(text(PAD, 220, "All files", size=28, fill=ON_SURFACE_VARIANT))
    parts.append(f'<path d="M{PAD+130},204 l14,12 l-14,12" fill="none" stroke="{ON_SURFACE_VARIANT}" stroke-width="4" stroke-linecap="round"/>')
    parts.append(text(PAD + 166, 220, "Movies", size=28, fill=PRIMARY, weight=600))
    parts.append(section(320, "Folders", 4))
    y = 380
    for name, count in [("2024", 42), ("2023", 31), ("Downloads", 18), ("Clips", 7)]:
        parts.append(f'<rect x="{PAD}" y="{y}" width="{W-2*PAD}" height="118" rx="26" fill="{SURFACE_VARIANT}"/>')
        parts.append(f'<path d="M{PAD+26},{y+40} h34 l12,-12 h40 a8,8 0 0 1 8,8 v46 h-94 z" fill="none" stroke="{PRIMARY}" stroke-width="5" stroke-linejoin="round"/>')
        parts.append(text(PAD + 140, y + 60, name, size=32, weight=500))
        parts.append(text(PAD + 140, y + 98, "Movies/" + name, size=24, fill=ON_SURFACE_VARIANT))
        parts.append(text(W - PAD - 60, y + 70, str(count), size=28, fill=ON_SURFACE_VARIANT, anchor="end"))
        parts.append(f'<path d="M{W-PAD-34},{y+50} l14,12 l-14,12" fill="none" stroke="{ON_SURFACE_VARIANT}" stroke-width="4" stroke-linecap="round"/>')
        y += 138
    parts.append(section(y + 40, "Files", 12))
    y += 110
    for i, (t, s, b) in enumerate([("Beach.mp4", "1920x1080", "1:02"), ("Sunrise.mp4", "3840x2160", "0:38"),
                                   ("Drive.mp4", "1280x720", "9:14")]):
        parts.append(poster(PAD + i * (CARD_W + GAP), y, CARD_W, CARD_H_VIDEO, "video", t, s, b))
    parts.append(bottom_bar(2, ["Home", "Search", "Folders", "Favourites", "Playlists", "Settings"]))
    return frame("".join(parts), "N Player folders",
                 "Folders screen: breadcrumbs, a list of child folders with recursive item "
                 "counts, and a grid of the video files in the current folder, with the "
                 "Folders destination selected in the bottom bar.")


def screen_settings():
    parts = [header("Settings", "Appearance and privacy")]
    y = 250
    parts.append(text(PAD, y, "Appearance", size=32, weight=600))
    y += 56
    parts.append(text(PAD, y, "Theme", size=30))
    y += 40
    parts.append(text(PAD, y, "Choose light, dark, or follow the system setting", size=24, fill=ON_SURFACE_VARIANT))
    y += 46
    segs = ["System", "Light", "Dark"]
    seg_w = (W - 2 * PAD) // 3
    for i, s in enumerate(segs):
        sel = i == 0
        parts.append(f'<rect x="{PAD+i*seg_w}" y="{y}" width="{seg_w-8}" height="104" rx="26" fill="{"#2A3550" if sel else SURFACE_VARIANT}"/>')
        if sel:
            parts.append(f'<rect x="{PAD+i*seg_w}" y="{y}" width="{seg_w-8}" height="104" rx="26" fill="none" stroke="{PRIMARY}" stroke-width="3"/>')
        parts.append(text(PAD + i * seg_w + (seg_w - 8) // 2, y + 66, s, size=28,
                          fill=PRIMARY if sel else ON_SURFACE, anchor="middle", weight=600 if sel else 400))
    y += 150
    parts.append(toggle_row(y, "Match my wallpaper", "Use colours from your wallpaper", False))
    y += 150
    parts.append(f'<line x1="{PAD}" y1="{y}" x2="{W-PAD}" y2="{y}" stroke="{OUTLINE_VARIANT}" stroke-width="2"/>')
    y += 70
    parts.append(toggle_row(y, "Biometric lock", "Require biometric unlock to open the app", False))
    y += 150
    parts.append(f'<line x1="{PAD}" y1="{y}" x2="{W-PAD}" y2="{y}" stroke="{OUTLINE_VARIANT}" stroke-width="2"/>')
    y += 70
    parts.append(f'<rect x="{PAD}" y="{y-40}" width="{W-2*PAD}" height="120" rx="26" fill="{SURFACE_VARIANT}"/>')
    parts.append(text(PAD + 28, y, "Private folder", size=30))
    parts.append(text(PAD + 28, y + 40, "Items you have hidden from every other list", size=24, fill=ON_SURFACE_VARIANT))
    parts.append(f'<path d="M{W-PAD-40},{y-4} l16,14 l-16,14" fill="none" stroke="{ON_SURFACE_VARIANT}" stroke-width="4" stroke-linecap="round"/>')
    y += 140
    parts.append(f'<rect x="{PAD}" y="{y}" width="{W-2*PAD}" height="280" rx="36" fill="{SURFACE_VARIANT}"/>')
    parts.append(text(PAD + 36, y + 72, "N Player", size=34, weight=700))
    parts.append(text(PAD + 36, y + 124, "Plays the video and audio already on this device.", size=24, fill=ON_SURFACE_VARIANT))
    parts.append(text(PAD + 36, y + 164, "No account, no server, no network access.", size=24, fill=ON_SURFACE_VARIANT))
    parts.append(text(PAD + 36, y + 224, "Version 1.1.0 (3)", size=26, fill=PRIMARY, weight=600))
    parts.append(bottom_bar(5, ["Home", "Search", "Folders", "Favourites", "Playlists", "Settings"]))
    return frame("".join(parts), "N Player settings",
                 "Settings screen: a System/Light/Dark theme selector, a Match my wallpaper "
                 "toggle, a biometric lock toggle, a private folder row, and an About card "
                 "showing version 1.1.0, with the Settings destination selected.")


SCREENS = [
    ("phone-1-home", screen_home),
    ("phone-2-player", screen_player),
    ("phone-3-detail", screen_detail),
    ("phone-4-search", screen_search),
    ("phone-5-folders", screen_folders),
    ("phone-6-settings", screen_settings),
]


def feature_graphic():
    """1024x500 feature graphic for the store listing header."""
    fw, fh = 1024, 500
    parts = [
        f'<rect width="{fw}" height="{fh}" fill="url(#glow)" opacity="0.9"/>',
        f'<g transform="translate(96,150)">'
        f'<rect x="0" y="0" width="140" height="200" rx="26" fill="{SURFACE_VARIANT}"/>'
        f'<rect x="0" y="0" width="140" height="200" rx="26" fill="url(#art)"/>'
        f'<circle cx="70" cy="100" r="34" fill="#FFFFFF" opacity="0.9"/>'
        f'<path d="M58,84 l30,16 l-30,16 z" fill="{BG}"/>'
        f'</g>',
        f'<g transform="translate(272,110)">'
        f'<rect x="0" y="0" width="140" height="200" rx="26" fill="{SURFACE_VARIANT}"/>'
        f'<rect x="0" y="0" width="140" height="200" rx="26" fill="url(#audio)"/>'
        f'<circle cx="70" cy="100" r="30" fill="{PRIMARY}" opacity="0.18"/>'
        f'<path d="M62,78 v46 a10,10 0 1 1 -7,-10 v-34 h24 v10 h-17" fill="none" stroke="{ON_SURFACE_VARIANT}" stroke-width="5" stroke-linejoin="round"/>'
        f'</g>',
        text(468, 214, "N Player", size=64, weight=700),
        text(468, 272, "Play anything on your phone.", size=34, fill=ON_SURFACE_VARIANT),
        text(468, 322, "No account. No server. No network access.", size=30, fill=PRIMARY, weight=500),
        text(468, 386, "10 s skips  \u2022  20 / 40 min jumps  \u2022  private folder  \u2022  PiP", size=26, fill=ON_SURFACE_VARIANT),
    ]
    return frame("".join(parts), "N Player feature graphic",
                 "Feature graphic: the N Player name and tagline over two device cards, "
                 "with the no-account, no-server promise and the player features.", w=fw, h=fh)


def render(name, svg_text, w, h, out):
    tmp = pathlib.Path("/tmp/nplayer-store-svg")
    tmp.mkdir(parents=True, exist_ok=True)
    svg_path = tmp / f"{name}.svg"
    svg_path.write_text(svg_text, encoding="utf-8")
    png = out / f"{name}.png"
    subprocess.run(
        [
            "chromium", "--headless", "--no-sandbox", "--disable-gpu",
            "--hide-scrollbars", "--force-device-scale-factor=1",
            f"--window-size={w},{h}",
            f"--screenshot={png}",
            f"file://{svg_path}",
        ],
        check=True,
        capture_output=True,
    )
    print(f"{png.name}: {png.stat().st_size // 1024} KB")


def main():
    out = pathlib.Path(__file__).parent / "screenshots"
    out.mkdir(parents=True, exist_ok=True)

    for name, fn in SCREENS:
        render(name, fn(), W, H, out)

    render("feature-graphic", feature_graphic(), 1024, 500, out)


if __name__ == "__main__":
    main()
