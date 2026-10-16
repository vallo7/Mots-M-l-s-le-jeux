# Musique d'ambiance

## Ce qui est livré
- `app/src/main/assets/audio/music/bgm_main.ogg` : boucle jouée en continu, sans coupure (deux `MediaPlayer` chaînés), **volume 0,32**, mise en pause quand l'appli passe en arrière-plan. Option **Musique** dans les paramètres (activée par défaut, persistante, appliquée tout de suite). Fichier absent ou illisible = pas de musique, sans erreur.
- 3 propositions originales dans `assets/audio/music_proposals/` (hors APK), toutes en boucle de 16 mesures, générées par `python3 scripts/generate_music_proposals.py` :

| | Titre | Tempo / tonalité | Ambiance | Instruments |
|---|---|---|---|---|
| **A** (installée) | Sunny Marimba | 100 BPM · do majeur (C – Am – F – G) | joyeux, énergique doux, « petit gain » à chaque mot | marimba en arpèges, pad chaud, shaker, tom doux |
| **B** | Lo-fi Puzzle | 78 BPM · Dm9 – G13 – Cmaj9 – Am7 | concentré, cosy, idéal pour les grilles longues | piano électrique, basse, batterie feutrée |
| **C** | Music Box Dream | 84 BPM · fa majeur (Fmaj7 – Dm7 – B♭maj7 – C) | calme, onirique, très peu fatigant | boîte à musique, cordes planantes |

> **Honnêteté** : ces morceaux sont des **ébauches synthétisées par code**. Leur niveau, leur équilibre spectral et le raccord de boucle ont été mesurés (crête ≈ -2 dBFS, aucun saut à la jonction), mais **je n'ai pas pu les écouter** : à vous de juger au casque et sur haut-parleur de téléphone. Pour une version finale, un morceau composé ou sous licence sera plus riche.

## Changer de morceau
1. Écouter les fichiers de `assets/audio/music_proposals/`.
2. Copier l'élu sur `app/src/main/assets/audio/music/bgm_main.ogg` (ou `python3 scripts/generate_music_proposals.py --install B`). OGG Vorbis stéréo, 30–60 s, 96–128 kbit/s (≈ 500 Ko), **un nombre entier de mesures** et une fin qui retombe sur le début.
3. Pour un autre nom de fichier, changer `ASSET_PATH` dans `platform/LoopingMusicPlayer.kt`.

## Cahier des charges d'un bon morceau de fond pour ce jeu
- **Tempo 75–105 BPM**, tonalité majeure ou modale douce, pas de grosse montée ni de rupture : le joueur se concentre sur une grille.
- Mélodie **simple et peu mémorable** (pentatonique), instruments **ronds** (marimba, piano électrique, boîte à musique, guitare nylon) ; éviter les aigus agressifs et les caisses claires fortes.
- Boucle **continue** : pas de silence ni d'intro/outro ; durée 30–90 s pour qu'on ne la repère pas trop vite.
- Niveau **-18 à -14 LUFS** environ ; le jeu la joue à 32 % pour laisser les effets et les voix de praise passer devant.
- Variantes possibles plus tard : une ambiance par chapitre (clé `paletteKey`) et une version plus tendue pour les défis finaux.

## Où trouver un morceau sous licence (à vérifier à chaque téléchargement)
- Banques gratuites avec licence d'usage commercial : *Pixabay Music*, *OpenGameArt* (filtrer la licence : CC0 ou CC-BY), *FreePD* (domaine public), *Incompetech* (CC-BY, **attribution obligatoire** — à ajouter aux crédits).
- Banques payantes ou par abonnement : *Epidemic Sound*, *Artlist*, *Uppbeat*, *AudioJungle* (licence « jeu mobile » à lire).
- Mots-clés de recherche utiles : *casual puzzle game loop*, *cozy marimba loop*, *lo-fi chill loop*, *music box lullaby loop*, *light ambient game background*, *seamless loop*.
- Toujours vérifier : droit de **redistribuer dans une application**, **boucle sans coupure**, et noter la source dans `LICENSES.md`.
