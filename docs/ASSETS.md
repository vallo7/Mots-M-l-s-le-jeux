# Inventaire des assets

| Asset | Format | Emplacement | Origine / licence | Remplacement |
|---|---|---|---|---|
| Sons de praise (8) | mp3 mono 44,1 kHz, 0,9–1,9 s | `app/src/main/assets/audio/praise/` | Fournis par l'auteur du projet — **provenance et licence à confirmer par lui** | Même nom de fichier, ou éditer `praise_map.json` |
| Table de praise | JSON | `…/audio/praise/praise_map.json` | Projet | Voir README |
| Sons d'interface (12) | WAV 16 bits mono 32 kHz, 0,06–1,3 s | `app/src/main/assets/audio/ui/` | Générés par `scripts/generate_ui_sounds.py` (originaux) | Relancer le script ou remplacer le `.wav` (même nom) |
| Niveaux (2 chapitres) | JSON | `data/levels/` (embarqués comme assets `levels/`) | Contenu original | `docs/LEVEL_AUTHORING.md` |
| Icônes d'interface (glyphes teintables ×15 + icônes couleur ×9) | vecteur 64×64 | `app/src/main/res/drawable/ic_*.xml` | Dessinées pour le projet dans le style des références ; générées par `scripts/generate_vector_icons.py` (`--preview planche.png` pour un aperçu) | Éditer le script puis le relancer |
| Illustrations de chapitres (2) | vecteur 360×140 | `…/drawable/ill_chapter_meadow.xml`, `ill_chapter_dusk.xml` | Dessinées pour le projet | idem |
| Logo source / icône d'application | PNG 1024×1024, fond blanc | `assets/branding/app-logo.png` | **Image fournie par l'auteur** (droits à confirmer) | Voir ci-dessous |
| Icône d'application | PNG adaptatifs 108→432 px + monochrome | `app/src/main/res/mipmap-*/` + `mipmap-anydpi-v26/` | Générés par `scripts/generate_icons.py` | Voir ci-dessous |
| Icône Play Store | PNG 512×512 | `assets/branding/play-store-icon-512.png` | Généré | Relancer le script |
| Musique d'ambiance | OGG stéréo ~38 s, boucle sans coupure | `app/src/main/assets/audio/music/bgm_main.ogg` (+ 3 propositions dans `assets/audio/music_proposals/`) | Synthétisée par `scripts/generate_music_proposals.py` (ébauche originale) | `docs/MUSIC.md` |
| Tuiles, rubans, trophée, étoiles, filigrane, éléments de résultat | Code Compose (vectoriel) | `presentation/components/`, `screens/` | Projet | Éditer le composable |

## Remplacer le logo
1. Déposer le nouveau logo dans `assets/branding/app-logo.png` : carré, ≥ 1024×1024, **fond uni blanc** (le motif doit tenir dans le carré ; il est posé à 64 dp dans le calque adaptatif de 108 dp, réglage `ART_DP` dans `scripts/generate_icons.py`). Un logo vectoriel transparent (`app-logo.svg`, zone sûre 18→90 sur 108) est aussi accepté : il prend alors le pas sur le PNG.
2. Si le fond n'est pas blanc : ajuster `icon_background` dans `app/src/main/res/values/colors.xml` **et** `BACKGROUND` dans `scripts/generate_icons.py`.
3. `pip install pillow numpy && python3 scripts/generate_icons.py` → régénère les `mipmap-*` (premier plan + monochrome), les XML adaptatifs et `play-store-icon-512.png`.
4. Committer les fichiers générés.

## Icônes supplémentaires souhaitées (même style « Yeti » : aplats vifs, relief sous la forme)
Les icônes de la planche ont toutes été redessinées en vecteur ; il en manque quelques-unes **dans le même style** pour harmoniser le reste. Si vous en avez, les déposer dans **`assets/icons-reference/`** (un fichier par icône, PNG ≥ 256 px fond uni ou transparent, nom explicite) : ampoule d'indice · haut-parleur et note de musique · flèche « mélanger » · croix de fermeture · triangle de lecture · globe (langue) · cloche/vibration · calque « texte plus grand » · petites illustrations de chapitre (un décor par chapitre, ratio 360×140) · trophées de 3 niveaux (bronze, argent, or) · couronne du défi final.

## Remplacer / ajouter des sons de praise
Noms attendus : `nice.mp3 great.mp3 awesome.mp3 amazing.mp3 insane.mp3 unreal.mp3 divine.mp3 legendary.mp3` dans `app/src/main/assets/audio/praise/`. Le test `PraiseMapTest` et `check_repo.py` vérifient que tout fichier déclaré existe. Fichier manquant → son de réussite générique, sans erreur.
