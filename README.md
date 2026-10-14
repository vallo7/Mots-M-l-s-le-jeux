# Mots Mêlés — jeu mobile de mots (Android, hors ligne)

Jeu de mots mêlés premium, hors ligne, bilingue FR/EN. Kotlin + Jetpack Compose. Cahier des charges : `roadmap_jeu_mobile_mots_pour_Claude.pdf`.

> **État : toutes les phases sont écrites ; aucune n'a été compilée avec Gradle** (l'environnement de travail n'accède pas aux dépôts Maven/Google). Le moteur, les règles, les niveaux, la persistance JSON, la séquence de démarrage, l'audio et l'haptique ont été compilés et testés ici (112 tests verts) ; les écrans Compose, les dépôts DataStore et l'intégration publicitaire attendent la **première exécution GitHub Actions**, qui est la vraie vérification : prévoir quelques erreurs de compilation à corriger depuis le journal. Détails honnêtes : `docs/RAPPORT_FINAL.md` et `docs/CHECKLIST.md`.

## Ce que fait le jeu
Accueil sur le niveau en cours (« Jouer » en un tap) · grille de lettres à tracer au doigt, mots à trouver au-dessus, indice sous la grille à droite, bannière de test en zone réservée · 2 chapitres × 10 niveaux (grilles 3×3 à 10×10, défi final au niveau 10) · étoiles selon le temps (< 3:00 = 3, < 10:00 = 2, sinon 1) · praise (Nice → Legendary, même voix en FR et EN) · 2 indices offerts, vidéo récompensée (1–2), cadeau par chapitre (1–3) · interstitiel de test 1 démarrage sur 2 · sons, haptique, animations sans fondu ni confettis · reprise de partie · paramètres, crédits.

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
- **Interface** : `app/src/main/assets/audio/ui/*.wav`, générés par `python3 scripts/generate_ui_sounds.py`.

## Logo et icône
Déposer le logo dans `assets/branding/app-logo.svg` (carré, fond transparent, zone sûre 18→90 sur 108) ou `app-logo.png` (≥ 1024 px), puis `pip install pillow cairosvg && python3 scripts/generate_icons.py`. Détails : `docs/ASSETS.md`.

## Changer la langue
Bouton **FR | EN** en haut à gauche de l'accueil (ou Paramètres). Le choix est persistant et appliqué immédiatement ; tant qu'il n'est pas fait, l'application suit la langue du téléphone. Les textes sont dans `app/src/main/res/values/` (EN) et `values-fr/`.

## Publicités
Identifiants de **test** Google uniquement (`app/src/main/res/values/ads_config.xml`). Procédure avant production : `docs/ADS.md`.

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
