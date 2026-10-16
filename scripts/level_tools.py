"""Outils partagés pour les niveaux : normalisation, score, validation.

Miroir Python des règles de `domain/.../content/LevelValidator.kt` : il permet à la CI de valider
`data/levels/*.json` sans Android. Toute règle modifiée ici doit l'être aussi côté Kotlin.
"""
import json
import math
import unicodedata
from pathlib import Path

DIRS = {
    "E": (0, 1), "S": (1, 0), "SE": (1, 1), "W": (0, -1),
    "N": (-1, 0), "SW": (1, -1), "NW": (-1, -1), "NE": (-1, 1),
}
OPPOSITE = {"E": "W", "W": "E", "S": "N", "N": "S", "SE": "NW", "NW": "SE", "SW": "NE", "NE": "SW"}
TIER_DIRS = {
    1: ["E", "S"],
    2: ["E", "S", "SE"],
    3: ["E", "S", "SE", "W", "N"],
    4: list(DIRS),
}
LANGS = ("fr", "en")
ALLOWED_SIZES = {3, 4, 6, 7, 8, 10, 12}
SIZE_STEPS = [3, 4, 6, 7, 8, 10]  # 12 est réservé au défi final (hors règle de saut)
LEVELS_PER_CHAPTER = 10
FINAL_MIN_RATIO = 1.25
SCHEMA_VERSION = 1


def tier_directions(tier):
    return TIER_DIRS[min(max(tier, 1), 4)]


def to_letters(display):
    decomposed = unicodedata.normalize("NFD", display).upper()
    return "".join(ch for ch in decomposed if "A" <= ch <= "Z")


def style_for(chapter, number):
    if number == LEVELS_PER_CHAPTER:
        return "FINAL"
    band = (number - 1) // 3
    return ["STYLE_1", "STYLE_2", "STYLE_3"][(band + chapter - 1) % 3]


def jround(x):
    """Équivalent de Math.round (arrondi au supérieur sur .5)."""
    return int(math.floor(x + 0.5))


def difficulty_score(grid_size, word_count, avg_len, tier, ambiguity):
    return jround(grid_size * 2.0 + word_count * 3.0 + avg_len * 1.5 + tier * 6.0 + ambiguity * 20.0)


def level_score(level):
    variants = [level["variants"][lang] for lang in LANGS]
    word_count = sum(len(v["words"]) for v in variants) / len(variants)
    lengths = [len(w["letters"]) for v in variants for w in v["words"]]
    avg = sum(lengths) / len(lengths)
    d = level["difficulty"]
    return difficulty_score(level["gridSize"], word_count, avg, d["tier"], d["ambiguity"])


def word_cells(word):
    dr, dc = DIRS[word["direction"]]
    return [(word["row"] + dr * i, word["col"] + dc * i) for i in range(len(word["letters"]))]


def occurrences(grid, letters):
    n = len(grid)
    found = set()
    for r in range(n):
        for c in range(n):
            for dr, dc in DIRS.values():
                er, ec = r + dr * (len(letters) - 1), c + dc * (len(letters) - 1)
                if not (0 <= er < n and 0 <= ec < n):
                    continue
                cells = [(r + dr * i, c + dc * i) for i in range(len(letters))]
                if all(grid[y][x] == letters[i] for i, (y, x) in enumerate(cells)):
                    found.add(frozenset(cells))
    return found


def validate_variant(level, lang, variant):
    issues = []
    lid = level["id"]

    def add(code, msg):
        issues.append((lid, code, f"[{lang}] {msg}"))

    n = level["gridSize"]
    grid = variant["grid"]
    if len(grid) != n or any(len(row) != n for row in grid):
        add("GRID_SHAPE", f"la grille doit être {n}x{n}")
        return issues
    if any(ch < "A" or ch > "Z" for row in grid for ch in row):
        add("GRID_CHARS", "la grille ne doit contenir que A-Z")
    words = variant["words"]
    if not words:
        add("NO_WORDS", "aucun mot cible")
    allowed = set(tier_directions(level["difficulty"]["tier"]))
    seen = set()
    for i, w in enumerate(words):
        if w["letters"] != to_letters(w["display"]):
            add("WORD_LETTERS", f"mot #{i} {w['display']!r} : letters incohérent")
        if len(w["letters"]) < 2:
            add("WORD_LENGTH", f"mot #{i} trop court")
        if w["letters"] in seen:
            add("WORD_DUPLICATE", f"mot en double {w['letters']}")
        seen.add(w["letters"])
        if w["direction"] not in allowed:
            add("DIRECTION", f"{w['letters']} : direction {w['direction']} interdite au palier {level['difficulty']['tier']}")
        cells = word_cells(w)
        if any(not (0 <= y < n and 0 <= x < n) for y, x in cells):
            add("OUT_OF_BOUNDS", f"{w['letters']} sort de la grille")
        elif any(grid[y][x] != w["letters"][i2] for i2, (y, x) in enumerate(cells)):
            add("GRID_MISMATCH", f"{w['letters']} ne correspond pas à la grille")
    for a in words:
        for b in words:
            if a is not b and (b["letters"] in a["letters"] or b["letters"][::-1] in a["letters"]):
                add("WORD_CONTAINED", f"{b['letters']} est contenu dans {a['letters']}")
    if any(code in ("OUT_OF_BOUNDS", "GRID_MISMATCH", "GRID_CHARS") for _, code, _ in issues):
        return issues
    for w in words:
        count = len(occurrences(grid, w["letters"]))
        if count != 1:
            add("AMBIGUOUS_WORD", f"{w['letters']} apparaît {count} fois dans la grille")
    if not issues:
        placed = {frozenset(word_cells(w)) for w in words}
        if len(placed) != len(words):
            add("UNSOLVABLE", "deux mots occupent les mêmes cases")
    return issues


def validate_level(level):
    issues = []
    lid = level["id"]

    def add(code, msg):
        issues.append((lid, code, msg))

    chapter, number, size = level["chapter"], level["number"], level["gridSize"]
    if not lid:
        add("ID", "identifiant vide")
    if not 1 <= number <= LEVELS_PER_CHAPTER:
        add("NUMBER", "numéro hors 1..10")
    if not all(level["theme"].get(lang) for lang in LANGS):
        add("THEME", "thème manquant dans une langue")
    if size not in ALLOWED_SIZES:
        add("GRID_SIZE", f"taille {size} non autorisée")
    if size == 3 and not (chapter == 1 and number <= 2):
        add("GRID_3X3", "3x3 réservé aux niveaux 1 et 2 du chapitre 1")
    if size == 4 and chapter != 1:
        add("GRID_4X4", "4x4 réservé au chapitre 1")
    if size == 12 and number != LEVELS_PER_CHAPTER:
        add("GRID_12X12", "12x12 réservé au défi final (niveau 10)")
    final = number == LEVELS_PER_CHAPTER
    if final:
        if level["visualStyle"] != "FINAL":
            add("STYLE_FINAL", "le niveau 10 doit utiliser le style FINAL")
        if size != 12:
            add("FINAL_SIZE", "le défi final doit être en 12x12")
    elif level["visualStyle"] == "FINAL":
        add("STYLE_FINAL", "le style FINAL est réservé au niveau 10")
    if 1 <= number <= LEVELS_PER_CHAPTER and level["visualStyle"] != style_for(chapter, number):
        add("STYLE_RULE", f"style {level['visualStyle']} attendu {style_for(chapter, number)}")
    d = level["difficulty"]
    if not 1 <= d["tier"] <= 4:
        add("TIER", "palier hors 1..4")
    if not 0.0 <= d["ambiguity"] <= 1.0:
        add("AMBIGUITY", "ambiguïté hors 0..1")
    if level["hints"]["maxPerLevel"] < 1:
        add("HINTS", "maxPerLevel doit être >= 1")
    missing = [lang for lang in LANGS if lang not in level["variants"]]
    for lang in missing:
        add("VARIANT_MISSING", f"variante {lang} absente")
    if missing:
        return issues
    counts = {len(level["variants"][lang]["words"]) for lang in LANGS}
    if len(counts) > 1:
        add("WORD_COUNT", "nombre de mots différent entre langues")
    expected = level_score(level)
    if expected != d["score"]:
        add("SCORE", f"score {d['score']} != calculé {expected}")
    for lang in LANGS:
        issues += validate_variant(level, lang, level["variants"][lang])
    return issues


def validate_chapters(chapters):
    issues = []
    ids = [lv["id"] for ch in chapters for lv in ch["levels"]]
    for dup in sorted({i for i in ids if ids.count(i) > 1}):
        issues.append((dup, "DUPLICATE_ID", "identifiant de niveau en double"))
    for i, chapter in enumerate(sorted(chapters, key=lambda c: c["index"])):
        if chapter["index"] != i + 1:
            issues.append((None, "CHAPTER_INDEX", f"{chapter['id']} : index {chapter['index']} attendu {i + 1}"))
        if not all(chapter["theme"].get(lang) for lang in LANGS):
            issues.append((None, "CHAPTER_THEME", f"{chapter['id']} : thème manquant"))
        levels = sorted(chapter["levels"], key=lambda lv: lv["number"])
        if [lv["number"] for lv in levels] != list(range(1, LEVELS_PER_CHAPTER + 1)):
            issues.append((None, "CHAPTER_LEVELS", f"{chapter['id']} : niveaux 1..10 attendus"))
        for lv in levels:
            if lv["chapter"] != chapter["index"]:
                issues.append((lv["id"], "CHAPTER_REF", "chapter != index du chapitre"))
        for prev, cur in zip(levels, levels[1:]):
            if cur["difficulty"]["score"] <= prev["difficulty"]["score"]:
                issues.append((cur["id"], "DIFFICULTY_ORDER", f"score {cur['difficulty']['score']} <= précédent {prev['difficulty']['score']}"))
            if cur["gridSize"] < prev["gridSize"]:
                issues.append((cur["id"], "GRID_SHRINK", "la taille de grille ne doit pas diminuer"))
            if cur["number"] != LEVELS_PER_CHAPTER and prev["gridSize"] in SIZE_STEPS and cur["gridSize"] in SIZE_STEPS:
                if SIZE_STEPS.index(cur["gridSize"]) - SIZE_STEPS.index(prev["gridSize"]) > 1:
                    issues.append((cur["id"], "GRID_JUMP", "hausse de taille trop brutale"))
        if len(levels) >= LEVELS_PER_CHAPTER:
            ninth, tenth = levels[8], levels[9]
            if tenth["difficulty"]["score"] < ninth["difficulty"]["score"] * FINAL_MIN_RATIO:
                issues.append((tenth["id"], "FINAL_NOT_HARD_ENOUGH", f"{tenth['difficulty']['score']} < {FINAL_MIN_RATIO} x {ninth['difficulty']['score']}"))
        for lv in levels:
            issues += validate_level(lv)
    return issues


def load_chapters(levels_dir):
    levels_dir = Path(levels_dir)
    index = json.loads((levels_dir / "index.json").read_text(encoding="utf-8"))
    chapters = []
    for name in index["chapters"]:
        chapter = json.loads((levels_dir / name).read_text(encoding="utf-8"))
        for lv in chapter["levels"]:
            lv.setdefault("chapter", chapter["index"])
        chapters.append(chapter)
    return index, chapters
