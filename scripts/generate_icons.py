#!/usr/bin/env python3
"""Régénère les icônes Android à partir du logo source.

Entrée  : assets/branding/app-logo.svg (viewBox carré, fond transparent, contenu dans la zone sûre 18..90 sur 108)
          ou, à défaut, assets/branding/app-logo.png (1024x1024 minimum, fond transparent).
Sorties : app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png et ic_launcher_monochrome.png,
          app/src/main/res/mipmap-anydpi-v26/ic_launcher(.xml|_round.xml), assets/branding/play-store-icon-512.png.
Le fond adaptatif est la couleur @color/icon_background (app/src/main/res/values/colors.xml).
    pip install pillow cairosvg   &&   python3 scripts/generate_icons.py
"""
import io
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
BRAND = ROOT / "assets/branding"
RES = ROOT / "app/src/main/res"
DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
BACKGROUND = (0xFF, 0x7A, 0x59)

ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/icon_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />
</adaptive-icon>
"""


def load_logo(size):
    svg, png = BRAND / "app-logo.svg", BRAND / "app-logo.png"
    if svg.exists():
        import cairosvg
        data = cairosvg.svg2png(url=str(svg), output_width=size, output_height=size)
        return Image.open(io.BytesIO(data)).convert("RGBA")
    if png.exists():
        return Image.open(png).convert("RGBA").resize((size, size), Image.LANCZOS)
    sys.exit("Aucun logo : déposer assets/branding/app-logo.svg ou app-logo.png")


def silhouette(img):
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.putalpha(img.getchannel("A"))
    black = Image.new("RGBA", img.size, (0, 0, 0, 255))
    return Image.composite(black, out, img.getchannel("A"))


def main():
    big = load_logo(1024)
    for name, px in DENSITIES.items():
        folder = RES / f"mipmap-{name}"
        folder.mkdir(parents=True, exist_ok=True)
        fg = big.resize((px, px), Image.LANCZOS)
        fg.save(folder / "ic_launcher_foreground.png", optimize=True)
        silhouette(fg).save(folder / "ic_launcher_monochrome.png", optimize=True)
    anydpi = RES / "mipmap-anydpi-v26"
    anydpi.mkdir(parents=True, exist_ok=True)
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        (anydpi / name).write_text(ADAPTIVE, encoding="utf-8")
    store = Image.new("RGBA", (512, 512), BACKGROUND + (255,))
    store.alpha_composite(big.resize((512, 512), Image.LANCZOS))
    store.convert("RGB").save(BRAND / "play-store-icon-512.png", optimize=True)
    print("Icônes générées :", ", ".join(DENSITIES), "+ play-store-icon-512.png")


if __name__ == "__main__":
    main()
