#!/usr/bin/env python3
"""Régénère les icônes Android à partir du logo source.

Deux modes, selon le fichier déposé dans assets/branding/ :

  1. app-logo.png  (par défaut, logo fourni : image carrée OPAQUE sur fond blanc, ≥ 1024 px)
     → le visuel est posé au centre du calque adaptatif (108 dp) à la taille ART_DP, dans la zone sûre, sur un fond
       blanc ; l'icône Play Store reprend l'image en plein cadre.
  2. app-logo.svg  (logo vectoriel sur fond transparent, viewBox carré, contenu dans la zone sûre 18..90 sur 108)
     → rendu tel quel comme calque de premier plan, sur la couleur @color/icon_background.

Sorties : app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png et ic_launcher_monochrome.png,
          app/src/main/res/mipmap-anydpi-v26/ic_launcher(.xml|_round.xml), assets/branding/play-store-icon-512.png.
Le fond adaptatif est la couleur @color/icon_background (app/src/main/res/values/colors.xml, blanc par défaut).
Icône monochrome (thèmes Android 13+) : les lettres restent pleines, les bandes colorées deviennent translucides,
de sorte que le mot reste lisible une fois teinté par le système.

    pip install pillow numpy   &&   python3 scripts/generate_icons.py
    (mode SVG : pip install cairosvg en plus)
"""
import io
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
BRAND = ROOT / "assets/branding"
RES = ROOT / "app/src/main/res"
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}   # facteur × 108 px
BACKGROUND = (0xFF, 0xFF, 0xFF)   # doit rester identique à icon_background dans colors.xml
CANVAS_DP = 108
# Taille (dp) du visuel dans le calque adaptatif. 64 dp : tout le motif reste visible sous un masque rond (zone ronde
# garantie ≈ 66 dp) et sous les masques carrés arrondis. Augmenter rogne les coins ; diminuer laisse plus de blanc.
ART_DP = 64

ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/icon_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />
</adaptive-icon>
"""


def load_png_logo():
    return Image.open(BRAND / "app-logo.png").convert("RGB")


def foreground_from_png(logo, canvas_px):
    """Visuel opaque centré sur un calque 108 dp blanc."""
    art_px = round(canvas_px * ART_DP / CANVAS_DP)
    canvas = Image.new("RGB", (canvas_px, canvas_px), BACKGROUND)
    offset = (canvas_px - art_px) // 2
    canvas.paste(logo.resize((art_px, art_px), Image.LANCZOS), (offset, offset))
    return canvas.convert("RGBA")


def monochrome_from_png(fg):
    """Silhouette teintable : lettres noires pleines, bandes colorées à ~45 % d'opacité, blanc transparent."""
    a = np.asarray(fg.convert("RGB")).astype(np.float32)
    lum = a.mean(axis=2)
    chroma = a.max(axis=2) - a.min(axis=2)
    letters = np.clip((170.0 - lum) / 90.0, 0.0, 1.0)            # noir -> 1, gris moyen -> 0
    colour = 0.45 * np.clip((chroma - 25.0) / 90.0, 0.0, 1.0)     # bandes colorées
    alpha = np.maximum(letters, colour * (1.0 - letters * 0.0))
    out = np.zeros(a.shape[:2] + (4,), dtype=np.uint8)
    out[..., 3] = np.round(alpha * 255).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def silhouette_from_alpha(img):
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    black = Image.new("RGBA", img.size, (0, 0, 0, 255))
    return Image.composite(black, out, img.getchannel("A"))


def main():
    svg, png = BRAND / "app-logo.svg", BRAND / "app-logo.png"
    use_svg = svg.exists()
    if not use_svg and not png.exists():
        sys.exit("Aucun logo : déposer assets/branding/app-logo.png (ou app-logo.svg)")
    if use_svg:
        import cairosvg
    logo = None if use_svg else load_png_logo()

    for name, factor in DENSITIES.items():
        px = round(CANVAS_DP * factor)
        folder = RES / f"mipmap-{name}"
        folder.mkdir(parents=True, exist_ok=True)
        if use_svg:
            data = cairosvg.svg2png(url=str(svg), output_width=px, output_height=px)
            fg = Image.open(io.BytesIO(data)).convert("RGBA")
            mono = silhouette_from_alpha(fg)
        else:
            fg = foreground_from_png(logo, px)
            mono = monochrome_from_png(fg)
        fg.save(folder / "ic_launcher_foreground.png", optimize=True)
        mono.save(folder / "ic_launcher_monochrome.png", optimize=True)

    anydpi = RES / "mipmap-anydpi-v26"
    anydpi.mkdir(parents=True, exist_ok=True)
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        (anydpi / name).write_text(ADAPTIVE, encoding="utf-8")

    if use_svg:
        big = Image.open(io.BytesIO(cairosvg.svg2png(url=str(svg), output_width=512, output_height=512))).convert("RGBA")
        store = Image.new("RGBA", (512, 512), BACKGROUND + (255,))
        store.alpha_composite(big)
        store = store.convert("RGB")
    else:
        store = logo.resize((512, 512), Image.LANCZOS)   # plein cadre : Google Play applique son propre masque
    store.save(BRAND / "play-store-icon-512.png", optimize=True)
    print("Icônes générées :", ", ".join(DENSITIES), "+ play-store-icon-512.png", "(mode SVG)" if use_svg else "(mode PNG)")


if __name__ == "__main__":
    main()
