#!/usr/bin/env python3
"""Contrôle statique des ressources Android (la CI s'en sert avant Gradle, faute de compilation locale possible).

- chaque R.string / R.plurals / R.drawable / R.mipmap / R.color / R.style utilisé dans le code existe ;
- chaque @string/@drawable/... du manifeste et des ressources existe ;
- strings.xml (EN, par défaut) et values-fr ont les mêmes clés traduisibles, les mêmes variantes de pluriel
  et les mêmes paramètres de formatage (%1$s, %1$d…) ;
- aucun texte visible écrit en dur dans les écrans (les chaînes vivent dans les ressources).
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
JAVA = ROOT / "app/src/main/java"
errors = []


def fail(msg):
    errors.append(msg)


def parse_strings(path):
    tree = ET.parse(path).getroot()
    strings, plurals, untranslatable = {}, {}, set()
    for el in tree:
        name = el.get("name")
        if el.tag == "string":
            strings[name] = "".join(el.itertext())
            if el.get("translatable") == "false":
                untranslatable.add(name)
        elif el.tag == "plurals":
            plurals[name] = {i.get("quantity"): "".join(i.itertext()) for i in el.findall("item")}
    return strings, plurals, untranslatable


def placeholders(text):
    return sorted(re.findall(r"%\d\$[sd]", text))


en_s, en_p, en_untr = parse_strings(RES / "values/strings.xml")
fr_s, fr_p, _ = parse_strings(RES / "values-fr/strings.xml")
ads_s, _, _ = parse_strings(RES / "values/ads_config.xml")
all_strings = {**en_s, **ads_s}

for name in sorted(set(en_s) - en_untr - set(fr_s)):
    fail(f"chaîne manquante en français : {name}")
for name in sorted(set(fr_s) - set(en_s)):
    fail(f"chaîne française sans équivalent par défaut : {name}")
for name in sorted((set(en_s) - en_untr) & set(fr_s)):
    if placeholders(en_s[name]) != placeholders(fr_s[name]):
        fail(f"paramètres de format différents entre EN et FR : {name}")
for name in sorted(set(en_p) ^ set(fr_p)):
    fail(f"pluriel présent dans une seule langue : {name}")
for name in sorted(set(en_p) & set(fr_p)):
    if set(en_p[name]) != set(fr_p[name]):
        fail(f"variantes de pluriel différentes : {name}")
    for q in en_p[name]:
        if q in fr_p[name] and placeholders(en_p[name][q]) != placeholders(fr_p[name][q]):
            fail(f"paramètres de pluriel différents : {name}/{q}")
for name in sorted(en_p):
    if not {"one", "other"} <= set(en_p[name]):
        fail(f"pluriel incomplet (one/other requis) : {name}")

drawables = {p.stem for d in RES.glob("drawable*") for p in d.iterdir() if p.is_file()}
mipmaps = {p.stem for d in RES.glob("mipmap*") for p in d.iterdir() if p.is_file()}
colors, styles = set(), set()
for xml in (RES / "values").glob("*.xml"):
    for el in ET.parse(xml).getroot():
        if el.tag == "color":
            colors.add(el.get("name"))
        if el.tag == "style":
            styles.add(el.get("name"))
known = {"string": set(all_strings), "plurals": set(en_p), "drawable": drawables, "mipmap": mipmaps, "color": colors, "style": styles}

for path in JAVA.rglob("*.kt"):
    text = path.read_text(encoding="utf-8")
    rel = path.relative_to(ROOT).as_posix()
    for kind, name in re.findall(r"\bR\.(string|plurals|drawable|mipmap|color|style)\.(\w+)", text):
        if name not in known[kind]:
            fail(f"{rel} : R.{kind}.{name} n'existe pas")
    # textes visibles en dur dans l'interface
    if "/presentation/" in rel:
        for pattern in (r'\bText\(\s*"', r'\btext\s*=\s*"', r'\bcontentDescription\s*=\s*"', r'\btitle\s*=\s*"'):
            for m in re.finditer(pattern, text):
                line = text[: m.start()].count("\n") + 1
                fail(f"{rel}:{line} : texte visible écrit en dur (utiliser une ressource)")

manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
sources = [manifest] + [p.read_text(encoding="utf-8") for p in RES.rglob("*.xml")]
for text in sources:
    for kind, name in re.findall(r"@(string|drawable|mipmap|color|style)/([\w.]+)", text):
        key = name.replace(".", "_") if kind == "style" else name
        if kind == "style":
            ok = name in styles
        else:
            ok = name in known[kind]
        if not ok:
            fail(f"référence @{kind}/{name} introuvable")

if errors:
    print("\n".join(f"ERREUR : {e}" for e in errors))
    sys.exit(1)
print(f"OK : {len(en_s)} chaînes, {len(en_p)} pluriels (EN/FR cohérents), {len(drawables)} drawables, références vérifiées.")
