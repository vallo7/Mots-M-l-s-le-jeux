#!/usr/bin/env python3
"""Outil de production éditoriale des niveaux (exécuté par les auteurs, jamais par l'application).

Les listes de mots et les paramètres de difficulté ci-dessous sont le contenu éditorial. Le script
place les mots avec une graine fixe (résultat reproductible), remplit la grille et écrit
data/levels/*.json. L'application ne génère rien à l'exécution : elle lit ces fichiers.

    python3 scripts/generate_levels.py            # (ré)écrit data/levels
    python3 scripts/generate_levels.py --check    # échoue si les fichiers committés diffèrent
"""
import json
import os
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from level_tools import (  # noqa: E402
    DIRS, OPPOSITE, SCHEMA_VERSION, level_score, occurrences, style_for, tier_directions,
    to_letters, validate_chapters,
)

MAX_PLACE_TRIES = int(os.environ.get("MAX_PLACE_TRIES", "6000"))
OUT_DIR = Path(__file__).resolve().parent.parent / "data" / "levels"

FR_ALPHA = "EEEEEEEEEEEEAAAAAAAIIIIIIINNNNNNOOOOOORRRRRRSSSSSSTTTTTTUUUUUULLLLLDDDCCMMPPVFBGHQXYZJKW"
EN_ALPHA = "EEEEEEEEEEEEAAAAAAAAIIIIIIIOOOOOOONNNNNNRRRRRRTTTTTTSSSSSLLLLCCCDDDUUMMPPHHGGBFYWKVXZJQ"
ALPHA = {"fr": FR_ALPHA, "en": EN_ALPHA}


def L(number, size, tier, amb, hints, theme, fr, en, cross=0, cover=0.88):
    return dict(number=number, size=size, tier=tier, amb=amb, hints=hints, theme=theme,
                fr=fr, en=en, cross=cross, cover=cover)


CHAPTERS = [
    # Tailles : 3×3 (niv. 1-2) puis 4×4 (niv. 3-4) réservées au chapitre 1 ; ensuite 6, 7, 8, 10 ; 12×12 pour le défi final.
    dict(id="chapter-01", index=1, palette="meadow", theme=("Nature", "Nature"), levels=[
        L(1, 3, 1, 0.0, 2, ("Plage", "Beach"), ["MER", "ÎLE"], ["SEA", "SUN"], cross=0, cover=1.0),
        L(2, 3, 1, 0.1, 2, ("Jardin", "Garden"), ["PRÉ", "PIN", "EAU"], ["BEE", "BUG", "ELM"], cross=2, cover=1.0),
        L(3, 4, 1, 0.2, 3, ("Forêt", "Forest"), ["LOUP", "BOIS", "CERF", "PIN"], ["BEAR", "TREE", "LEAF", "OAK"], cross=0, cover=1.0),
        L(4, 4, 2, 0.3, 3, ("Montagne", "Mountain"), ["LAC", "SKI", "ROC", "PIC", "CIME"], ["ICE", "SKI", "ELK", "YAK", "PEAK"], cross=2, cover=1.0),
        L(5, 6, 2, 0.4, 3, ("Océan", "Ocean"), ["VAGUE", "CRABE", "CORAIL", "PLAGE", "THON"], ["WAVE", "CRAB", "SHELL", "CORAL", "WHALE"], cross=3, cover=0.95),
        L(6, 6, 2, 0.5, 3, ("Animaux", "Animals"), ["ZÈBRE", "PANDA", "TIGRE", "SINGE", "LAPIN", "RENARD"], ["ZEBRA", "PANDA", "TIGER", "MONKEY", "LLAMA", "EAGLE"], cross=5, cover=0.97),
        L(7, 7, 3, 0.55, 3, ("Fruits", "Fruits"), ["ANANAS", "MANGUE", "BANANE", "CERISE", "ORANGE", "FRAISE", "PÊCHE"], ["ORANGE", "BANANA", "CHERRY", "MANGO", "PEACH", "LEMON", "PAPAYA"], cross=5, cover=0.95),
        L(8, 8, 3, 0.6, 4, ("Météo", "Weather"), ["TONNERRE", "TEMPÊTE", "ÉCLAIR", "BRUINE", "AVERSE", "RAFALE", "VERGLAS", "MOUSSON"], ["BLIZZARD", "SUNSHINE", "THUNDER", "TORNADO", "DRIZZLE", "SHOWER", "BREEZE", "MONSOON"], cross=6, cover=0.95),
        L(9, 10, 3, 0.7, 4, ("Fleurs", "Flowers"), ["MARGUERITE", "COQUELICOT", "ORCHIDÉE", "HIBISCUS", "NARCISSE", "LAVANDE", "PIVOINE", "JASMIN", "TULIPE", "MUGUET"], ["SUNFLOWER", "DAFFODIL", "LAVENDER", "HIBISCUS", "MAGNOLIA", "JASMINE", "ORCHID", "VIOLET", "TULIP", "POPPY"], cross=8),
        L(10, 12, 4, 1.0, 5, ("Le grand air", "The great outdoors"),
          ["BIODIVERSITÉ", "ÉCOSYSTÈME", "VÉGÉTATION", "MONTAGNARD", "RANDONNÉE", "CAMPEMENT", "PROMENADE", "ESCALADE", "AVENTURE", "SENTIERS", "GLACIERS", "PANORAMA", "TORRENTS", "PRAIRIES"],
          ["BIODIVERSITY", "WILDERNESS", "RAINFOREST", "WATERFALL", "ADVENTURE", "ECOSYSTEM", "MOONLIGHT", "EVERGREEN", "TRAILHEAD", "CAMPFIRE", "MOUNTAIN", "WILDLIFE", "MEADOWS", "GLACIERS"], cross=10, cover=0.95),
    ]),
    dict(id="chapter-02", index=2, palette="dusk", theme=("Voyage", "Travel"), levels=[
        L(1, 6, 3, 0.3, 3, ("Transports", "Transport"), ["AVION", "TRAIN", "MÉTRO", "BATEAU", "CAMION", "FERRY"], ["PLANE", "TRAIN", "METRO", "TRUCK", "FERRY", "ROCKET"], cross=5, cover=0.97),
        L(2, 7, 3, 0.4, 3, ("Hôtel", "Hotel"), ["SUITE", "CLIENT", "BAGAGE", "BALCON", "PISCINE", "CHAMBRE", "SERVICE"], ["SUITE", "GUEST", "LOBBY", "TOWEL", "BALCONY", "PILLOW", "SHOWER"], cross=5, cover=0.95),
        L(3, 7, 3, 0.5, 3, ("Pays", "Countries"), ["BRÉSIL", "JAPON", "CANADA", "ESPAGNE", "ITALIE", "MEXIQUE", "ÉGYPTE"], ["BRAZIL", "JAPAN", "CANADA", "SPAIN", "ITALY", "MEXICO", "EGYPT"], cross=5, cover=0.95),
        L(4, 8, 3, 0.55, 3, ("Monuments", "Landmarks"), ["PYRAMIDE", "COLISÉE", "CHÂTEAU", "MOSQUÉE", "PAGODE", "PALAIS", "MUSÉE", "TEMPLE"], ["PYRAMID", "MONUMENT", "FORTRESS", "CASTLE", "TEMPLE", "MUSEUM", "PALACE", "STATUE"], cross=6, cover=0.95),
        L(5, 8, 4, 0.6, 4, ("Cuisine du monde", "World food"), ["COUSCOUS", "BURRITO", "RAVIOLI", "FALAFEL", "BAKLAVA", "PAELLA", "TAGINE", "FONDUE"], ["DUMPLING", "TORTILLA", "LASAGNA", "GOULASH", "BURRITO", "PAELLA", "NACHOS", "CURRY"], cross=6, cover=0.95),
        L(6, 8, 4, 0.7, 4, ("Aéroport", "Airport"), ["PASSAGER", "AÉRONEF", "TERMINAL", "BAGAGES", "TURBINE", "DOUANE", "HANGAR", "PILOTE"], ["BOARDING", "TERMINAL", "AIRLINE", "LUGGAGE", "CUSTOMS", "TAKEOFF", "HANGAR", "RUNWAY"], cross=7, cover=0.95),
        L(7, 10, 4, 0.7, 4, ("Mers et îles", "Seas and islands"), ["CATAMARAN", "PÉNINSULE", "ARCHIPEL", "NAUFRAGE", "MANGROVE", "PLONGÉE", "FALAISE", "LAGUNE"], ["CATAMARAN", "PENINSULA", "SHIPWRECK", "MANGROVE", "SEASHORE", "SNORKEL", "DOLPHIN", "LAGOON"], cross=6),
        L(8, 10, 4, 0.8, 4, ("Déserts", "Deserts"), ["DROMADAIRE", "CARAVANE", "SCORPION", "BÉDOUIN", "SAHARA", "MIRAGE", "CACTUS", "DUNES", "SABLE"], ["SANDSTORM", "SCORPION", "CARAVAN", "BEDOUIN", "MIRAGE", "CACTUS", "SAHARA", "CANYON", "OASIS"], cross=8),
        L(9, 10, 4, 0.85, 4, ("Capitales", "Capitals"), ["WASHINGTON", "BRUXELLES", "STOCKHOLM", "AMSTERDAM", "BUDAPEST", "LISBONNE", "VARSOVIE", "NAIROBI", "BANGKOK"], ["WASHINGTON", "BRUSSELS", "STOCKHOLM", "AMSTERDAM", "BUDAPEST", "LISBON", "WARSAW", "NAIROBI", "BANGKOK"], cross=9),
        L(10, 12, 4, 1.0, 5, ("Tour du monde", "Around the world"),
          ["EXPLORATEUR", "DESTINATION", "CARTOGRAPHIE", "DÉPAYSEMENT", "ITINÉRAIRE", "EXPÉDITION", "HÉMISPHÈRE", "AVENTURIER", "PANORAMIQUE", "PASSEPORT", "CONTINENT", "FRONTIÈRE", "LONGITUDE", "TRAVERSÉE", "VOYAGEUR"],
          ["EXPLORATION", "DESTINATION", "CARTOGRAPHY", "ADVENTURER", "EXPEDITION", "HEMISPHERE", "WANDERLUST", "ITINERARY", "CONTINENT", "LONGITUDE", "SIGHTSEEING", "PASSPORT", "FRONTIER", "TRAVELER", "LATITUDE"], cross=10, cover=0.97),
    ]),
]


def try_place(rng, n, words, dirs, min_cross, max_cover):
    grid = [[None] * n for _ in range(n)]
    axes = [[set() for _ in range(n)] for _ in range(n)]
    pos = [None] * len(words)
    cross = 0
    order = sorted(range(len(words)), key=lambda i: (-len(words[i]), i))
    for k, i in enumerate(order):
        w = words[i]
        cands = []
        for d in dirs:
            dr, dc = DIRS[d]
            ax = frozenset((d, OPPOSITE[d]))
            for r in range(n):
                for c in range(n):
                    er, ec = r + dr * (len(w) - 1), c + dc * (len(w) - 1)
                    if not (0 <= er < n and 0 <= ec < n):
                        continue
                    ov, ok = 0, True
                    for t in range(len(w)):
                        y, x = r + dr * t, c + dc * t
                        g = grid[y][x]
                        if g is None:
                            continue
                        if g != w[t] or ax in axes[y][x]:
                            ok = False
                            break
                        ov += 1
                    if ok:
                        cands.append((r, c, d, ov))
        if not cands:
            return None
        crossing = [x for x in cands if x[3] > 0]
        pool = crossing if (cross < min_cross and k > 0 and crossing) else cands
        r, c, d, ov = rng.choice(pool)
        dr, dc = DIRS[d]
        ax = frozenset((d, OPPOSITE[d]))
        for t in range(len(w)):
            y, x = r + dr * t, c + dc * t
            grid[y][x] = w[t]
            axes[y][x].add(ax)
        pos[i] = (r, c, d)
        cross += ov
    distinct = sum(1 for row in grid for ch in row if ch is not None)
    if cross < min_cross or distinct > max_cover * n * n:
        return None
    return grid, pos


def fill(rng, grid, letters, alpha, ambiguity):
    pool = "".join(letters)
    return ["".join(ch if ch is not None else (rng.choice(pool) if rng.random() < ambiguity else rng.choice(alpha))
                    for ch in row) for row in grid]


def build_variant(chapter, spec, lang):
    displays = spec[lang]
    letters = [to_letters(w) for w in displays]
    rng = random.Random(f"mots-meles/v2/{chapter['index']}/{spec['number']}/{lang}")
    dirs = tier_directions(spec["tier"])
    for _ in range(MAX_PLACE_TRIES):
        placed = try_place(rng, spec["size"], letters, dirs, spec["cross"], spec["cover"])
        if placed is None:
            continue
        base, pos = placed
        for _ in range(400):
            rows = fill(rng, base, letters, ALPHA[lang], spec["amb"])
            if all(len(occurrences(rows, w)) == 1 for w in letters):
                return {
                    "grid": rows,
                    "words": [{"display": displays[i], "letters": letters[i], "row": pos[i][0],
                               "col": pos[i][1], "direction": pos[i][2]} for i in range(len(displays))],
                }
    raise RuntimeError(f"Impossible de construire c{chapter['index']} l{spec['number']} {lang} : "
                       f"assouplir cover/cross ou changer les mots")


def build_chapter(ch):
    levels = []
    for spec in ch["levels"]:
        level = {
            "id": f"c{ch['index']}-l{spec['number']:02d}",
            "chapter": ch["index"],
            "number": spec["number"],
            "theme": {"fr": spec["theme"][0], "en": spec["theme"][1]},
            "visualStyle": style_for(ch["index"], spec["number"]),
            "gridSize": spec["size"],
            "difficulty": {"tier": spec["tier"], "ambiguity": spec["amb"], "score": 0},
            "hints": {"maxPerLevel": spec["hints"]},
            "variants": {lang: build_variant(ch, spec, lang) for lang in ("fr", "en")},
        }
        level["difficulty"]["score"] = level_score(level)
        levels.append(level)
    return {
        "schemaVersion": SCHEMA_VERSION,
        "id": ch["id"],
        "index": ch["index"],
        "paletteKey": ch["palette"],
        "theme": {"fr": ch["theme"][0], "en": ch["theme"][1]},
        "levels": [{k: v for k, v in lv.items() if k != "chapter"} for lv in levels],
    }, levels


def dump(obj):
    return json.dumps(obj, ensure_ascii=False, indent=1) + "\n"


def main():
    check = "--check" in sys.argv
    outputs = {}
    all_chapters = []
    for ch in CHAPTERS:
        data, levels = build_chapter(ch)
        outputs[f"{ch['id']}.json"] = dump(data)
        all_chapters.append({**data, "levels": levels})
    outputs["index.json"] = dump({"schemaVersion": SCHEMA_VERSION, "chapters": [f"{c['id']}.json" for c in CHAPTERS]})
    issues = validate_chapters(all_chapters)
    if issues:
        for lid, code, msg in issues:
            print(f"ERREUR [{lid or 'catalogue'}] {code} : {msg}")
        sys.exit(1)
    if check:
        stale = [n for n, text in outputs.items() if not (OUT_DIR / n).exists() or (OUT_DIR / n).read_text(encoding="utf-8") != text]
        if stale:
            print("Fichiers obsolètes (relancer scripts/generate_levels.py) :", ", ".join(stale))
            sys.exit(1)
        print("OK : data/levels correspond exactement au générateur.")
        return
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for name, text in outputs.items():
        (OUT_DIR / name).write_text(text, encoding="utf-8")
    for c in all_chapters:
        print(c["id"], [(lv["number"], lv["gridSize"], lv["difficulty"]["score"]) for lv in c["levels"]])


if __name__ == "__main__":
    main()
