# Architecture

```
┌────────────────────────────── :app (Android) ──────────────────────────────┐
│ presentation/  AppViewModel (navigation, progression, réglages)             │
│                GameController (une partie), écrans Compose, composants,     │
│                thème et jetons, FeedbackController                          │
│ data/          LevelJsonParser, ProgressJson/SettingsJson (versionnés),     │
│                DataStore*Repository, AssetLevelRepository, PraiseMap, UiSounds│
│ platform/      SoundPoolAudioManager, VibratorHapticsManager, ImmersiveMode, │
│                GoogleAdProvider, ConsentManager, ConnectivityChecker         │
└───────────────▲──────────────────────────────────────────────────────────────┘
                │ dépend de
┌───────────────┴──────────────── :domain (JVM pur) ───────────────────────────┐
│ model/     Level, Chapter, LevelVariant, TargetWord, Direction, Cell…         │
│ engine/    GameEngine, GameSession, GameTimer, GridGeometry, StarRating,      │
│            HintService, PraiseSelector                                        │
│ progress/  ProgressState, ProgressRules, InterstitialPolicy                   │
│ usecases/  GameUseCases (démarrage, victoire, indice, cadeau, récompensée)    │
│ content/   LevelValidator, DifficultyScore, TextNormalizer                    │
│ ui/        MonumentBuilder, HomeModelBuilder, DifficultyBand, TimeFormat      │
│ settings/  AppSettings                                                        │
│ ports/     LevelRepository, ProgressRepository, SettingsRepository,           │
│            AdProvider, AudioManager, HapticsManager                           │
└───────────────────────────────────────────────────────────────────────────────┘
```
`:domain` n'importe rien d'Android ni de coroutines : toute la logique se teste sur JVM. Les ports sont implémentés dans `:app` ; remplacer un fournisseur (autre SDK publicitaire, autre moteur audio) ne touche pas au jeu.

## Flux
- **Démarrage** : `StartupSequence` → niveaux (lecture, validation) → progression + 2 indices offerts (une fois) + réglages → préchargement des sons → ≥ 2 s au total (une seule fois).
- **Partie** : doigt → `LetterGrid` → `GridGeometry.snapPath` → `GameController` → `GameSession.submit` → `GameEngine.select` (cases du tracé = cases d'un mot cible) → sons/haptique → sauvegarde de partie.
- **Victoire** : `GameSession.isSolved` (moteur) → `GameUseCases.completeLevel` → `StarRating` + `PraiseSelector` → `ProgressRepository.update` (atomique, idempotent) → écran de résultat.
- **Indice** : `HintService` calcule sans effet → solde persisté → seulement alors révélation ; échec d'écriture = rien ne change.
- **Récompensée** : SDK confirme → `ProgressRules.grantRewardedHints(rewardId)` (aléa 1–2 tiré à ce moment, identifiant déjà vu = refusé).
- **Interstitiel** : tap « Jouer » sur niveau neuf → `InterstitialPolicy` → annonce affichée avant l'ouverture du jeu.

## Principes
États UI immuables (`StateFlow`) · aucune E/S sur le thread UI (`Dispatchers.IO`/`Default`) · horloge, aléa et délais injectés · un contrôleur par partie avec sa portée (un seul chronomètre d'affichage, annulé à la fermeture) · écritures de sauvegarde sérialisées sur un fil unique · contenu séparé du code · textes dans `res/values*`.
