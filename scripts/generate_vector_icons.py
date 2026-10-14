#!/usr/bin/env python3
"""Écrit les icônes vectorielles de l'interface (res/drawable/ic_*.xml) et les illustrations de chapitres (ill_*.xml).

Les chemins sont des formes simples dessinées pour le projet (grille 24x24). Teintables : fond noir, la couleur est
appliquée à l'exécution. `python3 scripts/generate_vector_icons.py --preview /tmp/sheet.png` produit une planche contact.
"""
import sys
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable"

ICONS = {
    "ic_back": "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z",
    "ic_close": "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
    "ic_check": "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z",
    "ic_play": "M8,5v14l11,-7z",
    "ic_restart": "M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z",
    "ic_settings": "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z",
    "ic_monument": "M12,2l4,5H8z M7,8h10v4H7z M4,13h16v4H4z M2,18h20v3H2z",
    "ic_hint": "M9,21c0,0.55 0.45,1 1,1h4c0.55,0 1,-0.45 1,-1v-1H9v1zM12,2C8.14,2 5,5.14 5,9c0,2.38 1.19,4.47 3,5.74V17c0,0.55 0.45,1 1,1h6c0.55,0 1,-0.45 1,-1v-2.26c1.81,-1.27 3,-3.36 3,-5.74 0,-3.86 -3.14,-7 -7,-7z",
    "ic_gift": "M20,6h-2.18c0.11,-0.31 0.18,-0.65 0.18,-1 0,-1.66 -1.34,-3 -3,-3 -1.05,0 -1.96,0.54 -2.5,1.35l-0.5,0.67 -0.5,-0.68C10.96,2.54 10.05,2 9,2 7.34,2 6,3.34 6,5c0,0.35 0.07,0.69 0.18,1H4c-1.11,0 -1.99,0.89 -1.99,2L2,19c0,1.11 0.89,2 2,2h16c1.11,0 2,-0.89 2,-2V8c0,-1.11 -0.89,-2 -2,-2zM15,4c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zM9,4c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zM20,19L4,19v-2h16v2zM20,14L4,14L4,8h5.08L7,10.83 8.62,12 11,8.76l1,-1.36 1,1.36L15.38,12 17,10.83 14.92,8L20,8v6z",
    "ic_star": "M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z",
    "ic_star_outline": "M22,9.24l-7.19,-0.62L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21 12,17.27 18.18,21l-1.63,-7.03L22,9.24zM12,15.4l-3.76,2.27 1,-4.28 -3.32,-2.88 4.38,-0.38L12,6.1l1.71,4.04 4.38,0.38 -3.32,2.88 1,4.28L12,15.4z",
    "ic_lock": "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10c0,-1.1 -0.9,-2 -2,-2zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z",
    "ic_crown": "M5,16L3,5l5.5,5L12,4l3.5,6L21,5l-2,11H5zM19,19c0,0.6 -0.4,1 -1,1H6c-0.6,0 -1,-0.4 -1,-1v-1h14v1z",
    "ic_info": "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-6h2v6zM13,9h-2V7h2v2z",
}

# Illustrations de chapitres (viewport 360x140), couleurs de la palette du jeu.
ILLUSTRATIONS = {
    "ill_chapter_meadow": [
        ("#FFE9C7", "M0,0h360v140h-360z"),
        ("#FFC145", "M286,46m-26,0a26,26 0 1,1 52,0a26,26 0 1,1 -52,0"),
        ("#9ADBC4", "M0,96C50,60 110,64 170,88C230,112 290,78 360,70L360,140L0,140z"),
        ("#2BB3A3", "M0,116C60,92 130,96 200,112C260,126 320,104 360,100L360,140L0,140z"),
        ("#1E8F82", "M56,112l10,-30l10,30z M82,114l8,-24l8,24z M276,108l10,-32l10,32z"),
    ],
    "ill_chapter_dusk": [
        ("#E4DAFA", "M0,0h360v140h-360z"),
        ("#FFC145", "M76,52m-22,0a22,22 0 1,1 44,0a22,22 0 1,1 -44,0"),
        ("#B7A2EA", "M0,104L64,66L112,96L186,40L252,98L310,72L360,100L360,140L0,140z"),
        ("#6C4AB6", "M0,124L70,92L130,118L206,78L270,120L330,98L360,112L360,140L0,140z"),
        ("#FFFFFF", "M186,40l-14,18l8,-3l6,5l6,-5l8,3z"),
    ],
}


def icon_xml(path):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp"\n    android:height="24dp"\n    android:viewportWidth="24"\n    android:viewportHeight="24">\n'
            f'    <path\n        android:fillColor="#FF000000"\n        android:pathData="{path}" />\n</vector>\n')


def illustration_xml(paths):
    body = "".join(f'    <path\n        android:fillColor="{c}"\n        android:pathData="{d}" />\n' for c, d in paths)
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="360dp"\n    android:height="140dp"\n    android:viewportWidth="360"\n    android:viewportHeight="140">\n'
            f'{body}</vector>\n')


def preview(target):
    import io
    import cairosvg
    from PIL import Image
    tiles = []
    for name, d in ICONS.items():
        svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="96" height="96"><rect width="24" height="24" fill="#FFF8EE"/><path d="{d}" fill="#2B2A33"/></svg>'
        tiles.append(Image.open(io.BytesIO(cairosvg.svg2png(bytestring=svg.encode()))).convert("RGB"))
    for name, paths in ILLUSTRATIONS.items():
        body = "".join(f'<path d="{d}" fill="{c}"/>' for c, d in paths)
        svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 360 140" width="360" height="140">{body}</svg>'
        tiles.append(Image.open(io.BytesIO(cairosvg.svg2png(bytestring=svg.encode()))).convert("RGB"))
    icons, ills = tiles[: len(ICONS)], tiles[len(ICONS):]
    cols = 8
    rows = (len(icons) + cols - 1) // cols
    sheet = Image.new("RGB", (max(cols * 96, 720), rows * 96 + 150), "white")
    for i, im in enumerate(icons):
        sheet.paste(im, ((i % cols) * 96, (i // cols) * 96))
    for i, im in enumerate(ills):
        sheet.paste(im, (i * 360, rows * 96 + 5))
    sheet.save(target)


if __name__ == "__main__":
    if "--preview" in sys.argv:
        preview(sys.argv[sys.argv.index("--preview") + 1])
    else:
        RES.mkdir(parents=True, exist_ok=True)
        for name, d in ICONS.items():
            (RES / f"{name}.xml").write_text(icon_xml(d), encoding="utf-8")
        for name, paths in ILLUSTRATIONS.items():
            (RES / f"{name}.xml").write_text(illustration_xml(paths), encoding="utf-8")
        print(f"{len(ICONS)} icônes + {len(ILLUSTRATIONS)} illustrations écrites dans {RES}")
