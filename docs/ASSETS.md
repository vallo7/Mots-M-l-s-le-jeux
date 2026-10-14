# Inventaire des assets

| Asset | Format | Emplacement | Origine / licence | Remplacement |
|---|---|---|---|---|
| Sons de praise (8) | mp3 mono 44,1 kHz, 0,9–1,9 s | `app/src/main/assets/audio/praise/` | Fournis par l'auteur du projet — **provenance et licence à confirmer par lui** | Même nom de fichier, ou éditer `praise_map.json` |
| Table de praise | JSON | `…/audio/praise/praise_map.json` | Projet | Voir README |
| Sons d'interface (10) | WAV 16 bits mono 32 kHz, 0,06–1,3 s | `app/src/main/assets/audio/ui/` | Générés par `scripts/generate_ui_sounds.py` (originaux) | Relancer le script ou remplacer le `.wav` (même nom) |
| Niveaux (2 chapitres) | JSON | `data/levels/` (embarqués comme assets `levels/`) | Contenu original | `docs/LEVEL_AUTHORING.md` |
| Icônes d'interface (14) | vecteur 24×24, teintables | `app/src/main/res/drawable/ic_*.xml` | Formes dérivées des Material Icons (Apache 2.0) + monument/couronne dessinés pour le projet ; `scripts/generate_vector_icons.py` | Éditer le script puis le relancer |
| Illustrations de chapitres (2) | vecteur 360×140 | `…/drawable/ill_chapter_meadow.xml`, `ill_chapter_dusk.xml` | Dessinées pour le projet | idem |
| Logo source | SVG, fond transparent | `assets/branding/app-logo.svg` | Projet (provisoire) | Voir ci-dessous |
| Icône d'application | PNG adaptatifs 108→432 px + monochrome | `app/src/main/res/mipmap-*/` + `mipmap-anydpi-v26/` | Générés par `scripts/generate_icons.py` | Voir ci-dessous |
| Icône Play Store | PNG 512×512 | `assets/branding/play-store-icon-512.png` | Généré | Relancer le script |
| Tuiles, badges, cadeau, étoiles, watermark, éléments de résultat | Code Compose (vectoriel) | `presentation/components/`, `screens/` | Projet | Éditer le composable |

## Remplacer le logo
1. Déposer le logo dans `assets/branding/app-logo.svg` (carré, fond **transparent**, contenu dans la zone sûre 18→90 sur un viewBox de 108) **ou** `assets/branding/app-logo.png` (≥ 1024×1024, fond transparent).
2. Ajuster la couleur de fond de l'icône : `icon_background` dans `app/src/main/res/values/colors.xml` (et `BACKGROUND` dans `scripts/generate_icons.py` pour l'icône Play).
3. `pip install pillow cairosvg && python3 scripts/generate_icons.py` → régénère les `mipmap-*`, les XML adaptatifs et `play-store-icon-512.png`.
4. Committer les fichiers générés.

## Remplacer / ajouter des sons de praise
Noms attendus : `nice.mp3 great.mp3 awesome.mp3 amazing.mp3 insane.mp3 unreal.mp3 divine.mp3 legendary.mp3` dans `app/src/main/assets/audio/praise/`. Le test `PraiseMapTest` et `check_repo.py` vérifient que tout fichier déclaré existe. Fichier manquant → son de réussite générique, sans erreur.
