# Mots Mêlés — jeu mobile de mots (Android, hors ligne)

Jeu de mots mêlés premium, hors ligne, bilingue FR/EN. Kotlin + Jetpack Compose. Cahier des charges : `roadmap_jeu_mobile_mots_pour_Claude.pdf`.

> **État : toutes les phases sont écrites ; aucune n'a été compilée avec Gradle** (l'environnement de travail n'accède pas aux dépôts Maven/Google). Le moteur, les règles, les niveaux, le calcul de mouvement et la persistance JSON ont été compilés et testés ici (90 tests de règles verts, exécutés avec un lanceur JUnit local) ; les écrans Compose (dont tout le moteur d'effets de la grille), les dépôts DataStore et l'intégration publicitaire attendent la **première exécution GitHub Actions**, qui est la vraie vérification : prévoir quelques erreurs de compilation à corriger depuis le journal. Détails honnêtes : `docs/RAPPORT_FINAL.md` et `docs/CHECKLIST.md`.

## Ce que fait le jeu
**Accueil** : carrousel de chapitres qu'on fait glisser (glissement fluide, carte centrale agrandie, parallaxe) ; toucher le chapitre en cours ouvre directement son niveau en cours · réglages (popup), solde d'indices, menu des niveaux · fond blanc + filigrane animé très discret.
**Jeu** : grille tracée au doigt avec un serpent élastique, lettres qui « popent », vague + étincelles quand un mot est trouvé, secousse sur une erreur · mots au-dessus, chronomètre agrandi, bouton Indice sous la grille à droite (badge **pub** sur l'ampoule quand il n'y a plus d'indice) · **Recommencer sans confirmation : les lettres se mélangent en animation** · bannière de test en zone réservée.
**Contenu** : 2 chapitres × 10 niveaux, grilles 3×3 (niv. 1-2 du chap. 1), 4×4 (chap. 1 seulement), 6×6, 7×7, 8×8, 10×10 et **12×12 pour le défi final** ; mots plus nombreux et plus longs à mesure qu'on avance.
**Règles** : étoiles selon le temps (< 1:00 = 3, < 3:00 = 2, sinon 1) · 2 indices offerts · **une pub récompensée = 1 indice** · trophée de fin de chapitre (1–3 indices) · **pub de test quand on revient dans l'appli** après l'avoir quittée (plus d'interstitiel au lancement d'une partie) · praise (Nice → Legendary) · sons, haptique, musique d'ambiance en boucle · animations sans fondu ni confettis · reprise de partie · paramètres (langue, son, musique, vibrations, accessibilité), crédits.
**Fin de niveau** : popup de victoire (ruban, étoiles qui rebondissent, praise, temps) avec la **barre de progression continue du chapitre** qui se remplit en douceur jusqu'au trophée.

## Prérequis
JDK 17, Python 3.12, Android SDK (plateforme 35). Le wrapper Gradle (8.9) est fourni.

## Commandes
```bash
./gradlew :domain:test                                             # règles pures (79 tests)
./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug # lint + tests + APK debug
# APK : app/build/outputs/apk/debug/app-debug.apk

python3 scripts/check_repo.py        # fichiers attendus, secrets, motion (fondus/confettis interdits)
python3 scripts/check_resources.py   # chaînes FR/EN, références R.*, textes en dur
python3 scripts/validate_levels.py   # règles de niveaux
python3 scripts/generate_levels.py --check
```

## Build GitHub Actions et téléchargement de l'APK
`.github/workflows/android.yml` (sur `push`, `pull_request`, `workflow_dispatch`) : contrôle du dépôt, ressources, niveaux, JDK 17, validation du wrapper, SDK Android, lint, tests unitaires, APK debug. Il échoue clairement si une étape échoue et n'exige aucun secret.
**Télécharger l'APK** : onglet *Actions* → l'exécution voulue → section *Artifacts* → **`word-game-debug-apk`**. En cas d'échec : artifact `word-game-build-reports`.
Tests sur émulateur (manuel) : workflow *Instrumented tests* (`workflow_dispatch`).

## Architecture et décisions
`:domain` (règles pures) + `:app` (data, plateforme, interface) : `docs/ARCHITECTURE.md`. Décisions/hypothèses : `docs/DECISIONS.md`. Niveaux : `docs/LEVEL_AUTHORING.md`. Design : `docs/DESIGN_SYSTEM.md`. Publicités : `docs/ADS.md`. Assets : `docs/ASSETS.md`. QA : `docs/QA.md`.

## Sons
- **Praise** : `app/src/main/assets/audio/praise/` — `nice.mp3 great.mp3 awesome.mp3 amazing.mp3 insane.mp3 unreal.mp3 divine.mp3 legendary.mp3`. Correspondance et volumes dans `praise_map.json` (même voix pour `fr` et `en`). Remplacer un son = garder le nom. Fichier manquant ou langue sans entrée = son de réussite générique, sans erreur.
- **Interface** : `app/src/main/assets/audio/ui/*.wav` (12 sons, dont `shuffle.wav` pour le mélange des lettres et `swipe.wav` pour le carrousel), générés par `python3 scripts/generate_ui_sounds.py`.
- **Musique d'ambiance** : `app/src/main/assets/audio/music/bgm_main.ogg` (boucle sans coupure, volume bas, mise en pause en arrière-plan, option *Musique* dans les paramètres). Trois propositions dans `assets/audio/music_proposals/` ; changer de morceau : `docs/MUSIC.md`.

## Logo et icône
Le logo actuel est l'image fournie (grille de mots mêlés) : `assets/branding/app-logo.png` (1024 px, fond blanc). Pour le remplacer : déposer le nouveau PNG carré (≥ 1024 px, fond uni) au même nom puis `pip install pillow numpy && python3 scripts/generate_icons.py` (icônes adaptatives, version monochrome Android 13+ et icône Play Store). Détails : `docs/ASSETS.md`.

## Changer la langue
Paramètres (engrenage en haut à gauche de l'accueil) → ligne **Langue** : **FR | EN** (le bouton a été retiré de l'accueil). Le choix est persistant et appliqué immédiatement ; tant qu'il n'est pas fait, l'application suit la langue du téléphone. Les textes sont dans `app/src/main/res/values/` (EN) et `values-fr/`.

## Publicités
Identifiants de **test** Google uniquement (`app/src/main/res/values/ads_config.xml`) : bannière, récompensée (1 indice) et annonce d'ouverture au retour dans l'appli. Procédure avant production : `docs/ADS.md`.

## Signature release (plus tard)
Le workflow debug ne demande aucun secret. Pour une release : keystore **hors du dépôt**, mots de passe et alias dans GitHub Secrets, injectés au build. Jamais de clé privée dans Git (`*.jks`, `*.keystore` ignorés et bloqués par la CI).

## Dépannage
- *« fichier attendu manquant » / « son manquant » alors que les fichiers sont sur GitHub* : le run testé date d'avant leur ajout. **« Re-run jobs » rejoue l'ancien commit** : lancer un nouveau run (*Actions* → *Android CI* → *Run workflow* sur `main`, ou pousser un nouveau commit) et vérifier que le hash du commit en haut du run est le plus récent. Le premier pas du workflow affiche le commit testé et la liste des sons présents.
- *Fichiers `.gitkeep` partout* : inoffensifs ; la CI les supprime avant le build.
- *Échec de compilation Kotlin/Compose au premier run* : attendu (voir l'état ci-dessus) ; copier l'erreur du journal pour correction.
- *SDK manquant* : installer la plateforme Android 35 et accepter les licences (`sdkmanager --licenses`).
- *`gradlew: Permission denied`* : `chmod +x gradlew`.
- *`LevelAssetsTest` ne trouve pas les niveaux* : lancer Gradle depuis la racine du dépôt.
- *Niveaux modifiés mais CI rouge* : `python3 scripts/generate_levels.py` puis committer `data/levels/`.
