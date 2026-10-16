# Écrire et valider des niveaux

## Format (`data/levels/chapter-NN.json`, `schemaVersion: 1`)
```json
{ "schemaVersion": 1, "id": "chapter-01", "index": 1, "paletteKey": "meadow",
  "theme": { "fr": "Nature", "en": "Nature" },
  "levels": [ {
    "id": "c1-l01", "number": 1,
    "theme": { "fr": "Plage", "en": "Beach" },
    "visualStyle": "STYLE_1", "gridSize": 3,   // 3, 4, 6, 7, 8, 10 ou 12
    "difficulty": { "tier": 1, "ambiguity": 0.0, "score": 23 },
    "hints": { "maxPerLevel": 2 },
    "variants": {
      "fr": { "grid": ["EIM","VLE","EER"],
              "words": [ { "display": "MER", "letters": "MER", "row": 0, "col": 2, "direction": "W" } ] },
      "en": { … }
    } } ] }
```
`grid` : lignes de lettres A-Z (la « grille solution ») ; `words` : mot affiché (accents permis), lettres sans accent, case de départ et direction (`E S SE W N SW NW NE`). `index.json` liste les fichiers de chapitres.

## Règles validées (Kotlin `LevelValidator` + Python `scripts/level_tools.py`)
Grille carrée de la bonne taille, lettres A-Z · chaque mot correspond à la grille, reste dedans, utilise une direction autorisée par le palier · aucun mot contenu dans un autre (sens normal ou inverse) · **chaque mot n'apparaît qu'une fois** dans la grille, dans les 8 directions · niveau résoluble (rejoué par le vrai moteur) · FR et EN présents avec le même nombre de mots · score recalculé identique · style conforme à la règle (D-022) · 10 niveaux par chapitre, score strictement croissant, tailles **3/4/6/7/8/10/12**, 3×3 seulement c1 niveaux 1-2, 4×4 seulement au chapitre 1, 12×12 seulement au niveau 10, défi final en 12×12 et ≥ 1,25× le niveau 9 (D-046, D-059).

## Ajouter un niveau ou un chapitre
1. Éditer les listes dans `scripts/generate_levels.py` (`CHAPTERS`) : thème, taille, palier, ambiguïté, mots FR et EN. Un chapitre = 10 niveaux.
2. `python3 scripts/generate_levels.py` (place les mots, remplit, calcule les scores, valide, écrit `data/levels`).
3. `python3 scripts/validate_levels.py` puis `./gradlew :app:testDebugUnitTest` (le test `LevelAssetsTest` valide aussi le contenu côté Kotlin).
4. Ajouter le palier de couleurs du chapitre côté interface (clé `paletteKey`) quand l'écran existera (phase 3).
La CI échoue si les JSON diffèrent du générateur (`--check`) ou si une règle est violée.
