#!/usr/bin/env python3
"""Génère les icônes vectorielles de l'interface (res/drawable/ic_*.xml) et les illustrations de chapitres (ill_*.xml).

Deux familles, toutes dessinées pour le projet (redessinées d'après le style des références « Yeti Rush » :
aplats francs, léger relief « lèvre » sous chaque forme) :

  * GLYPHES teintables  — noir plein, la couleur est appliquée à l'exécution (tint) : retour, fermer, jouer, etc.
  * ICÔNES COULEUR       — plusieurs tons figés : trophée, étoile, ampoule, badge pub, couronne, horloge…

Aucun fichier raster : tout est vectoriel (net à toutes les densités, APK léger).

    python3 scripts/generate_vector_icons.py                      # écrit app/src/main/res/drawable/*.xml
    python3 scripts/generate_vector_icons.py --preview sheet.png  # planche contact (aperçu sans Android)
"""
import math
import sys
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable"
K = 0.5522847498  # approximation d'un quart de cercle par une Bézier cubique


# ----------------------------------------------------------------------------- primitives de chemins (M/L/C/Z absolus)
def f(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return s if s not in ("-0", "") else "0"


def pts(points, close=True):
    d = "M" + " L".join(f"{f(x)},{f(y)}" for x, y in points)
    return d + ("Z" if close else "")


def circle(cx, cy, r):
    k = r * K
    return (f"M{f(cx + r)},{f(cy)}C{f(cx + r)},{f(cy + k)} {f(cx + k)},{f(cy + r)} {f(cx)},{f(cy + r)}"
            f"C{f(cx - k)},{f(cy + r)} {f(cx - r)},{f(cy + k)} {f(cx - r)},{f(cy)}"
            f"C{f(cx - r)},{f(cy - k)} {f(cx - k)},{f(cy - r)} {f(cx)},{f(cy - r)}"
            f"C{f(cx + k)},{f(cy - r)} {f(cx + r)},{f(cy - k)} {f(cx + r)},{f(cy)}Z")


def rrect(x, y, w, h, r):
    r = min(r, w / 2, h / 2)
    k = r * K
    return (f"M{f(x + r)},{f(y)}L{f(x + w - r)},{f(y)}C{f(x + w - r + k)},{f(y)} {f(x + w)},{f(y + r - k)} {f(x + w)},{f(y + r)}"
            f"L{f(x + w)},{f(y + h - r)}C{f(x + w)},{f(y + h - r + k)} {f(x + w - r + k)},{f(y + h)} {f(x + w - r)},{f(y + h)}"
            f"L{f(x + r)},{f(y + h)}C{f(x + r - k)},{f(y + h)} {f(x)},{f(y + h - r + k)} {f(x)},{f(y + h - r)}"
            f"L{f(x)},{f(y + r)}C{f(x)},{f(y + r - k)} {f(x + r - k)},{f(y)} {f(x + r)},{f(y)}Z")


def star_pts(cx, cy, outer, inner, n=5, rot=-90.0):
    out = []
    for i in range(n * 2):
        a = math.radians(rot + i * 180.0 / n)
        r = outer if i % 2 == 0 else inner
        out.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return out


def arc_pts(cx, cy, r, a0, a1, steps=28):
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / steps)),
             cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / steps))) for i in range(steps + 1)]


def arrow_head(cx, cy, r, angle, tip=10, half=10, back=4):
    """Pointe de flèche posée au bout d'un arc, orientée selon la tangente (sens des angles croissants)."""
    a = math.radians(angle)
    px, py = cx + r * math.cos(a), cy + r * math.sin(a)
    dx, dy = -math.sin(a), math.cos(a)
    nx, ny = -dy, dx
    return [(px + dx * tip, py + dy * tip), (px + nx * half - dx * back, py + ny * half - dy * back),
            (px - nx * half - dx * back, py - ny * half - dy * back)]


def gear_pts(cx, cy, r_out, r_in, teeth=8, tooth_frac=0.42):
    out = []
    step = 360.0 / teeth
    for i in range(teeth):
        a = i * step
        half_o = step * tooth_frac / 2
        half_i = step * 0.5 - half_o * 0.55
        for ang, rad in ((a - half_i, r_in), (a - half_o, r_out), (a + half_o, r_out), (a + half_i, r_in)):
            out.append((cx + rad * math.cos(math.radians(ang)), cy + rad * math.sin(math.radians(ang))))
    return out


# ----------------------------------------------------------------------------- modèle de forme
def fill(path, color="#FF000000", evenodd=False):
    return dict(kind="fill", path=path, color=color, evenodd=evenodd)


def stroke(path, width, color="#FF000000", cap="round", join="round"):
    return dict(kind="stroke", path=path, width=width, color=color, cap=cap, join=join)


# ----------------------------------------------------------------------------- GLYPHES teintables (viewport 64)
def line(p, q):
    return pts([p, q], close=False)


GLYPHS = {
    "ic_back": [stroke(pts([(40, 12), (20, 32), (40, 52)], close=False), 10)],
    "ic_close": [stroke(line((17, 17), (47, 47)), 10), stroke(line((47, 17), (17, 47)), 10)],
    "ic_check": [stroke(pts([(13, 34), (26, 47), (51, 18)], close=False), 11)],
    "ic_play": [fill(pts([(20, 11), (20, 53), (55, 32)])), stroke(pts([(20, 11), (20, 53), (55, 32)]), 8)],
    "ic_restart": [
        stroke(pts(arc_pts(32, 33, 18, 35, 318), close=False), 9),
        fill(pts(arrow_head(32, 33, 18, 318, tip=11, half=11, back=5))),
    ],
    "ic_settings": [fill(
        pts(gear_pts(32, 32, 28, 22, teeth=8)) + circle(32, 32, 10), evenodd=True)],
    "ic_levels": [fill(rrect(8, 9, 48, 10, 5)), fill(rrect(8, 22, 48, 10, 5)),
                  fill(rrect(8, 35, 48, 10, 5)), fill(rrect(8, 48, 48, 10, 5))],
    "ic_home": [
        stroke(pts([(7, 33), (32, 11), (57, 33)], close=False), 9),
        fill(rrect(14, 31, 36, 25, 6) + rrect(27, 41, 10, 15, 3), evenodd=True),
    ],
    "ic_lock": [
        stroke("M21,30L21,22C21,8 43,8 43,22L43,30", 8),
        fill(rrect(10, 28, 44, 30, 8) + circle(32, 42, 5), evenodd=True),
    ],
    "ic_info": [fill(circle(32, 32, 27) + circle(32, 19, 4) + rrect(29, 28, 6, 19, 3), evenodd=True)],
    "ic_sound": [
        fill(pts([(8, 25), (20, 25), (33, 12), (33, 52), (20, 39), (8, 39)])),
        stroke(pts(arc_pts(34, 32, 13, -50, 50), close=False), 5.5),
        stroke(pts(arc_pts(34, 32, 22, -50, 50), close=False), 5.5),
    ],
    "ic_music": [
        fill(circle(19, 47, 8.5)), fill(circle(43, 41, 8.5)),
        fill(rrect(24, 14, 6, 34, 3)), fill(rrect(48, 8, 6, 34, 3)),
        fill(pts([(26, 14), (54, 8), (54, 19), (26, 25)])),
    ],
    "ic_vibrate": [
        fill(rrect(21, 8, 22, 48, 6) + rrect(26, 14, 12, 32, 2), evenodd=True),
        stroke(line((11, 22), (11, 42)), 5.5), stroke(line((53, 22), (53, 42)), 5.5),
        stroke(line((3, 28), (3, 36)), 5.5), stroke(line((61, 28), (61, 36)), 5.5),
    ],
    "ic_globe": [
        fill(circle(32, 32, 28) + circle(32, 32, 22), evenodd=True),
        stroke(line((6, 32), (58, 32)), 4.5),
        stroke(pts([(32 + 12 * math.cos(math.radians(a)), 32 + 28 * math.sin(math.radians(a)))
                    for a in range(0, 361, 12)]), 4.5),
    ],
    "ic_text": [
        stroke(pts([(5, 50), (21, 12), (37, 50)], close=False), 7), stroke(line((12, 37), (30, 37)), 6),
        stroke(pts([(40, 50), (49, 29), (58, 50)], close=False), 5.5), stroke(line((43.5, 43), (54.5, 43)), 4.5),
    ],
    "ic_motion": [
        fill(pts(star_pts(26, 36, 24, 6.5, n=4))),
        fill(pts(star_pts(49, 14, 11, 3.4, n=4))),
        fill(pts(star_pts(50, 48, 8, 2.6, n=4))),
    ],
    "ic_star_empty": [
        fill(pts(star_pts(32, 35, 28, 14)), "#FF000000"),
        stroke(pts(star_pts(32, 35, 24, 12)), 6, "#FF000000"),
    ],
}

# ----------------------------------------------------------------------------- ICÔNES COULEUR (viewport 64)
GOLD, GOLD_D, GOLD_L = "#FFD21F", "#F5A000", "#FFF0A0"
ORANGE_D = "#E27A00"

COLOR_ICONS = {
    # Trophée : remplace le cadeau (poignées, coupe, tige, socle ; relief « lèvre » à droite et en bas).
    "ic_trophy": [
        fill(pts(arc_pts(12, 21, 10.5, 90, 270) + arc_pts(12, 21, 5, 270, 90)), GOLD_D),
        fill(pts(arc_pts(52, 21, 10.5, -90, 90) + arc_pts(52, 21, 5, 90, -90)), GOLD_D),
        fill("M11,5L53,5C53,28 48,42 32,43C16,42 11,28 11,5Z", GOLD_D),
        fill("M11,4L53,4C53,26 47,39 32,40C17,39 11,26 11,4Z", GOLD),
        fill("M16,8L25,8C25,24 28,32 34,37C22,35 16,25 16,8Z", GOLD_L),
        fill(pts(star_pts(38, 21, 8.5, 3.8)), "#FFFFFF"),
        fill(rrect(26, 39, 12, 11, 2), GOLD_D),
        fill(rrect(16, 49, 32, 11, 4.5), ORANGE_D),
        fill(rrect(16, 47, 32, 10, 4.5), GOLD),
    ],
    # Étoile de notation : aplat doré, lèvre orangée dessous, reflet clair.
    "ic_star": [
        fill(pts(star_pts(32, 38, 28, 14)), GOLD_D), stroke(pts(star_pts(32, 38, 22, 11)), 10, GOLD_D),
        fill(pts(star_pts(32, 35, 28, 14)), GOLD), stroke(pts(star_pts(32, 35, 22, 11)), 10, GOLD),
        fill(pts([(20, 21), (26, 13), (28, 22)]), GOLD_L),
    ],
    "ic_star_off": [
        fill(pts(star_pts(32, 38, 28, 14)), "#B9B2E6"), stroke(pts(star_pts(32, 38, 22, 11)), 10, "#B9B2E6"),
        fill(pts(star_pts(32, 35, 28, 14)), "#D7D2F5"), stroke(pts(star_pts(32, 35, 22, 11)), 10, "#D7D2F5"),
    ],
    # Ampoule (idée) : globe doré, culot gris-bleu, reflet.
    "ic_bulb": [
        fill(circle(32, 27, 20), GOLD_D),
        fill(pts([(22, 36), (42, 36), (40, 44), (24, 44)]), GOLD_D),
        fill(circle(32, 25, 20), GOLD),
        fill(pts([(23, 33), (41, 33), (39, 43), (25, 43)]), GOLD),
        fill("M20,19C21,13 26,9 31,9C26,12 24,16 24,21Z", GOLD_L),
        fill(rrect(23, 45, 18, 7, 3.5), "#6F6AA8"),
        fill(rrect(23, 43, 18, 7, 3.5), "#9C97D6"),
        fill(rrect(26, 52, 12, 6, 3), "#6F6AA8"),
    ],
    # Badge « pub » : pastille verte + triangle de lecture (affiché sur l'ampoule quand il n'y a plus d'indice).
    "ic_ad_badge": [
        fill(rrect(4, 10, 56, 48, 16), "#1FA83A"),
        fill(rrect(4, 6, 56, 48, 16), "#3FDC5C"),
        fill(pts([(25, 19), (25, 41), (44, 30)]), "#FFFFFF"),
        stroke(pts([(25, 19), (25, 41), (44, 30)]), 4, "#FFFFFF"),
    ],
    "ic_crown": [
        fill(pts([(6, 20), (20, 34), (32, 12), (44, 34), (58, 20), (53, 47), (11, 47)]), GOLD_D),
        fill(pts([(6, 17), (20, 31), (32, 9), (44, 31), (58, 17), (53, 43), (11, 43)]), GOLD),
        fill(rrect(11, 44, 42, 9, 4), GOLD_D),
        fill(circle(32, 9, 4.5), "#FF4B7D"), fill(circle(6, 17, 4), "#FF4B7D"), fill(circle(58, 17, 4), "#FF4B7D"),
    ],
    "ic_clock": [
        fill(circle(32, 35, 27), "#E27A00"),
        fill(circle(32, 32, 27), GOLD),
        fill(circle(32, 32, 20), "#FFFFFF"),
        stroke(pts([(32, 32), (32, 18)], close=False), 5, "#5A4FCF"),
        stroke(pts([(32, 32), (42, 38)], close=False), 5, "#5A4FCF"),
    ],
    "ic_check_green": [
        stroke(pts([(13, 36), (26, 49), (52, 20)], close=False), 14, "#2F9A12"),
        stroke(pts([(13, 32), (26, 45), (52, 16)], close=False), 14, "#6BD62E"),
    ],
}

# ----------------------------------------------------------------------------- ILLUSTRATIONS de chapitre (viewport 360x140)
def tri(x, y, w, h):
    return pts([(x, y), (x + w / 2, y - h), (x + w, y)])


ILLUSTRATIONS = {
    # Prairie : soleil, collines, arbres ronds, nuages — verts et jaunes vifs.
    "ill_chapter_meadow": [
        fill("M0,0L360,0L360,140L0,140Z", "#FFE45C"),
        fill(circle(268, 52, 34), "#FFF6A8"), fill(circle(268, 52, 24), "#FFC01F"),
        fill(rrect(36, 30, 62, 14, 7), "#FFFFFF"), fill(rrect(52, 20, 34, 14, 7), "#FFFFFF"),
        fill(rrect(150, 44, 54, 12, 6), "#FFFFFF"),
        fill("M0,96C56,62 118,66 178,90C238,114 298,80 360,72L360,140L0,140z", "#7BE05A"),
        fill("M0,118C64,94 136,98 204,114C262,128 322,106 360,102L360,140L0,140z", "#2DBE4A"),
        fill(circle(70, 92, 15), "#169A3A"), fill(rrect(67, 98, 6, 18, 2), "#7A4A21"),
        fill(circle(104, 100, 11), "#169A3A"), fill(rrect(101, 104, 5, 14, 2), "#7A4A21"),
        fill(circle(296, 98, 16), "#169A3A"), fill(rrect(293, 104, 6, 18, 2), "#7A4A21"),
    ],
    # Crépuscule : lune, montagnes violettes à deux faces, étoiles à quatre branches.
    "ill_chapter_dusk": [
        fill("M0,0L360,0L360,140L0,140Z", "#9B7BFF"),
        fill(circle(74, 48, 28), "#FFF3B8"), fill(circle(84, 42, 24), "#9B7BFF"),
        fill(pts(star_pts(160, 30, 9, 3, n=4)), "#FFFFFF"),
        fill(pts(star_pts(236, 22, 7, 2.4, n=4)), "#FFFFFF"),
        fill(pts(star_pts(318, 38, 10, 3.2, n=4)), "#FFE45C"),
        fill("M0,112L66,62L120,100L196,34L270,104L318,74L360,100L360,140L0,140z", "#6F4DE0"),
        fill("M196,34L270,104L222,104L204,70z", "#5A3BC4"),
        fill("M66,62L120,100L82,100L72,80z", "#5A3BC4"),
        fill("M196,34L182,54L190,50L196,58L204,50L210,54z", "#FFFFFF"),
        fill("M0,126L72,98L134,122L210,88L276,124L336,104L360,114L360,140L0,140z", "#4A2FB0"),
    ],
}


# ----------------------------------------------------------------------------- écriture XML
def shapes_xml(shapes, viewport, size_dp):
    body = []
    for s in shapes:
        if s["kind"] == "fill":
            fill_type = '\n        android:fillType="evenOdd"' if s["evenodd"] else ""
            body.append(f'    <path\n        android:fillColor="{s["color"]}"{fill_type}\n        android:pathData="{s["path"]}" />\n')
        else:
            body.append(
                f'    <path\n        android:strokeColor="{s["color"]}"\n        android:strokeWidth="{f(s["width"])}"\n'
                f'        android:strokeLineCap="{s["cap"]}"\n        android:strokeLineJoin="{s["join"]}"\n'
                f'        android:pathData="{s["path"]}" />\n')
    w, h = viewport
    sw, sh = size_dp
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{sw}dp"\n    android:height="{sh}dp"\n    android:viewportWidth="{w}"\n    android:viewportHeight="{h}">\n'
            + "".join(body) + '</vector>\n')


# ----------------------------------------------------------------------------- aperçu (rasterisation PIL, sans Android)
def parse_subpaths(d):
    """Aplatit un chemin M/L/C/Z absolu en listes de points."""
    toks = []
    cur = ""
    for ch in d:
        if ch.isalpha():
            if cur.strip():
                toks.append(cur.strip())
            cur = ch
        else:
            cur += ch
    if cur.strip():
        toks.append(cur.strip())
    subs, sub, pos = [], [], (0.0, 0.0)
    for t in toks:
        cmd, args = t[0], t[1:].replace(",", " ").split()
        nums = [float(a) for a in args]
        if cmd == "M":
            if sub:
                subs.append(sub)
            sub = [(nums[0], nums[1])]
            pos = sub[0]
            for i in range(2, len(nums), 2):
                sub.append((nums[i], nums[i + 1]))
                pos = sub[-1]
        elif cmd == "L":
            for i in range(0, len(nums), 2):
                pos = (nums[i], nums[i + 1])
                sub.append(pos)
        elif cmd == "C":
            for i in range(0, len(nums), 6):
                p0, p1, p2, p3 = pos, (nums[i], nums[i + 1]), (nums[i + 2], nums[i + 3]), (nums[i + 4], nums[i + 5])
                for s in range(1, 17):
                    u = s / 16
                    a, b, c, e = (1 - u) ** 3, 3 * (1 - u) ** 2 * u, 3 * (1 - u) * u ** 2, u ** 3
                    sub.append((a * p0[0] + b * p1[0] + c * p2[0] + e * p3[0], a * p0[1] + b * p1[1] + c * p2[1] + e * p3[1]))
                pos = p3
        elif cmd in "Zz":
            if sub:
                subs.append(sub)
            sub = []
        else:
            raise ValueError(f"commande de chemin non gérée dans l'aperçu : {cmd}")
    if sub:
        subs.append(sub)
    return subs


def hex_rgba(c):
    c = c.lstrip("#")
    if len(c) == 8:
        return tuple(int(c[i:i + 2], 16) for i in (2, 4, 6)) + (int(c[0:2], 16),)
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def render(shapes, viewport, px, bg, tint=None):
    from PIL import Image, ImageChops, ImageDraw
    ss = 4
    w, h = int(viewport[0] * px / viewport[1]), px
    W, H = w * ss, h * ss
    sx, sy = W / viewport[0], H / viewport[1]
    img = Image.new("RGBA", (W, H), bg)
    for s in shapes:
        color = hex_rgba(tint if (tint and s["color"] == "#FF000000") else s["color"])
        layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        if s["kind"] == "fill":
            mask = Image.new("L", (W, H), 0)
            for sub in parse_subpaths(s["path"]):
                m = Image.new("L", (W, H), 0)
                ImageDraw.Draw(m).polygon([(x * sx, y * sy) for x, y in sub], fill=255)
                mask = ImageChops.logical_xor(mask.convert("1"), m.convert("1")).convert("L") if s["evenodd"] \
                    else ImageChops.lighter(mask, m)
            layer.paste(color, (0, 0), mask)
        else:
            mask = Image.new("L", (W, H), 0)
            d = ImageDraw.Draw(mask)
            lw = s["width"] * sx
            for sub in parse_subpaths(s["path"]):
                pp = [(x * sx, y * sy) for x, y in sub]
                if len(pp) > 1:
                    d.line(pp + ([pp[0]] if s["path"].rstrip().endswith("Z") else []), fill=255, width=int(lw))
                for x, y in pp:
                    d.ellipse([x - lw / 2, y - lw / 2, x + lw / 2, y + lw / 2], fill=255)
            layer.paste(color, (0, 0), mask)
        img = Image.alpha_composite(img, layer)
    return img.resize((w, h), Image.LANCZOS)


def preview(target):
    from PIL import Image, ImageDraw
    cell = 110
    names = list(GLYPHS) + list(COLOR_ICONS)
    cols = 8
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell + 2 * 160), (255, 255, 255, 255))
    draw = ImageDraw.Draw(sheet)
    for i, name in enumerate(names):
        shapes = GLYPHS.get(name) or COLOR_ICONS[name]
        bg = (255, 255, 255, 255)
        tint = "#FF2A2750" if name in GLYPHS else None
        img = render(shapes, (64, 64), 84, bg, tint)
        sheet.paste(img, ((i % cols) * cell + 13, (i // cols) * cell + 6))
        draw.text(((i % cols) * cell + 4, (i // cols) * cell + 94), name[3:], fill=(120, 120, 150, 255))
    for i, (name, shapes) in enumerate(ILLUSTRATIONS.items()):
        img = render(shapes, (360, 140), 140, (255, 255, 255, 255))
        sheet.paste(img, (6 + i * 372, rows * cell + 6))
    # Boutons « bulles » simulés : glyphe blanc sur pastille colorée.
    x0 = 6
    for name, col in (("ic_levels", "#7B5CFF"), ("ic_settings", "#2EB8FF"), ("ic_home", "#7B5CFF"), ("ic_restart", "#FF8A1F")):
        circ = Image.new("RGBA", (84, 84), (255, 255, 255, 0))
        cd = ImageDraw.Draw(circ)
        cd.ellipse([2, 6, 82, 82], fill=hex_rgba("#3D2AA0"))
        cd.ellipse([2, 0, 82, 76], fill=hex_rgba(col))
        glyph = render(GLYPHS[name], (64, 64), 44, (0, 0, 0, 0), "#FFFFFFFF")
        circ.alpha_composite(glyph, (20, 14))
        sheet.alpha_composite(circ, (x0, rows * cell + 160 + 10))
        x0 += 96
    sheet.convert("RGB").save(target)


if __name__ == "__main__":
    if "--preview" in sys.argv:
        preview(sys.argv[sys.argv.index("--preview") + 1])
    else:
        RES.mkdir(parents=True, exist_ok=True)
        for name, shapes in GLYPHS.items():
            (RES / f"{name}.xml").write_text(shapes_xml(shapes, (64, 64), (24, 24)), encoding="utf-8")
        for name, shapes in COLOR_ICONS.items():
            (RES / f"{name}.xml").write_text(shapes_xml(shapes, (64, 64), (32, 32)), encoding="utf-8")
        for name, shapes in ILLUSTRATIONS.items():
            (RES / f"{name}.xml").write_text(shapes_xml(shapes, (360, 140), (360, 140)), encoding="utf-8")
        print(f"{len(GLYPHS)} glyphes + {len(COLOR_ICONS)} icônes couleur + {len(ILLUSTRATIONS)} illustrations écrits dans {RES}")
