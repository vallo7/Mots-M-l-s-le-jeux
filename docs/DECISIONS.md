# Décisions et hypothèses (Phase 0)

Chaque entrée : la décision, la raison, et ce qu'il faut faire si elle ne convient pas.
Statut : **[À CONFIRMER]** = hypothèse faite faute d'information, facile à changer.

## Références visuelles

**D-001 — Aucune image de référence n'existe (confirmé par l'utilisateur).** Le cahier des charges parle d'images jointes ; aucune n'était dans le contexte ni dans le dépôt. Le système visuel est donc celui décrit par écrit (minimaliste, chaleureux, sans fondu ni confettis). Aucune fidélité à des références n'est revendiquée. La disposition suit strictement les emplacements écrits dans le cahier des charges.

## Mécanique de jeu

**D-002 — Mécanique : mots mêlés (word search) par tracé du doigt. [À CONFIRMER]**
Le texte évoque « mot croisé », mais décrit une liste de mots à trouver au-dessus d'une grille de lettres, des positions et orientations par mot, des « croisements » et des tailles 3×3 à 10×10 : c'est la structure d'un mots mêlés. Choix : le joueur fait glisser son doigt sur des cases alignées ; au relâchement, le moteur compare la sélection au placement des mots cibles. Sens normal ou inverse acceptés. Si les images montrent autre chose (ex. mots à saisir dans une grille croisée), seule la couche `GameEngine.select` et le format des niveaux changent.

**D-003 — Résolution vérifiée par le moteur.** `GameState.isSolved` est calculé par le domaine à partir des mots trouvés ; l'interface ne fait que l'afficher. Une sélection est valide si son ensemble de cases égale celui d'un mot cible (l'unicité de chaque mot dans la grille est garantie par le validateur).

**D-004 — Directions par palier de difficulté.** Palier 1 : → ↓. Palier 2 : + ↘. Palier 3 : + ← ↑. Palier 4 : les 8 directions. Les tracés du joueur peuvent aller dans n'importe quelle direction quel que soit le palier.

**D-005 — Indice = révèle la première case d'un mot non trouvé.** Un mot n'est indicé qu'une fois. Plafond par niveau configurable (`hints.maxPerLevel`). Le solde n'est décrémenté que si l'indice a pu être exécuté : `HintService` calcule le résultat sans effet, l'appelant persiste le nouveau solde puis applique la révélation.

## Praise et audio

**D-006 — Huit niveaux de praise, dans cet ordre (confirmé par l'utilisateur).** `nice, great, awesome, amazing, insane, unreal, divine, legendary` (mp3 fournis). Le brief écrivait « Awsome » : la clé technique est `awesome`. Les fichiers gardent leurs noms (`.mp3`, prioritaires sur l'exemple « .ogg » du brief). Dossier : `app/src/main/assets/audio/praise/`, correspondance : `praise_map.json`.

**D-007 — Le praise est identique en français et en anglais (confirmé par l'utilisateur).** Mêmes voix (`fr` recopie `en` dans `praise_map.json`) et même texte (« Nice!… Legendary! », identique dans `values/` et `values-fr/`). Si une langue n'a pas d'entrée ou si un fichier manque, le son de réussite générique est joué sans erreur. Pour des voix françaises plus tard : ajouter les fichiers et éditer le bloc `fr`.

**D-008 — Égalisation par volume, sans toucher aux fichiers.** Niveaux moyens mesurés entre −22,6 et −11,9 dB. Le champ `volume` du map atténue les plus forts (de 1.0 à 0.35). Un volume ne peut qu'atténuer ; modifiable dans le JSON.

## Démarrage, écrans

**D-009 — Lancement blanc.** Thème `Theme.WordGame.Starting` basé sur l'API SplashScreen (`androidx.core:core-splashscreen`) : fond blanc, icône transparente, compatible Android 12+ et antérieures.

**D-010 — Chargement : 2 s minimum, une seule fois au démarrage à froid.** `StartupSequence` : la barre reflète les étapes réellement exécutées (lecture, validation des niveaux) ; si elles finissent avant 2 s, la barre est pleine et on attend le temps restant. Pas de délai lors des navigations suivantes. Quand l'audio sera préchargé (phase 4), ce sera une étape réelle de plus.

**D-011 — Motion sans fondu.** Interdits : `fadeIn/fadeOut/Crossfade`, confettis. Transitions par glissement, échelle, rebond. Garde-fou automatique dans `scripts/check_repo.py` (CI). Animations réduites : si l'échelle d'animation système vaut 0, les animations décoratives sont désactivées.

**D-012 — Orientation portrait uniquement, thème clair uniquement (v1).** Réduit les cas de recréation d'activité et garde l'identité visuelle maîtrisée. À revoir si besoin.

**D-013 — Écran provisoire de phase 1.** Après le chargement, l'APK de la phase 1 affiche un écran de confirmation (niveaux lus et validés). Il est signalé comme provisoire dans `CHECKLIST.md` et remplacé par l'accueil en phase 3. Aucun élément factice ne doit subsister dans la livraison finale.

## Architecture et stack

**D-014 — Kotlin + Compose, deux modules.** `:domain` (JVM pur, aucune dépendance Android : moteur, règles, validation, ports) et `:app` (Android : data, platform, presentation). Injection de dépendances manuelle (`AppContainer`) : pas de génération de code, moins de risques de build.

**D-015 — Persistance : DataStore typé + sérialisation JSON versionnée (phase 3), pas Room.** La progression est un petit document ; Room (et KSP) ajouteraient de la complexité sans bénéfice. `ProgressState.schemaVersion` + migration à l'ouverture ; `ProgressRepository.update` est atomique (lecture-transformation-écriture).

**D-016 — Versions figées, stables et compatibles :** AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, Compose BOM 2024.10.01, minSdk 26, targetSdk/compileSdk 35, JDK 17. Choisies pour leur compatibilité éprouvée, pas pour être les plus récentes ; une montée de version se fait en un seul fichier (`gradle/libs.versions.toml`).

**D-017 — minSdk 26.** Icônes adaptatives sans variantes PNG, API SplashScreen/Haptics simples, > 95 % des appareils actifs.

**D-018 — JSON des niveaux lus via `org.json`** (fourni par Android) : zéro dépendance et zéro plugin de compilation supplémentaires.

## Contenu

**D-019 — Une grille par langue.** Chaque niveau a une variante `fr` et une variante `en` (mots et grille propres à la langue, même thème, même nombre de mots, mêmes dimensions). Le progrès est par niveau, indépendant de la langue. Changer de langue en cours de niveau repart d'une grille neuve pour cette langue.

**D-020 — Niveaux éditorialisés, jamais générés à l'exécution.** Les listes de mots sont écrites à la main dans `scripts/generate_levels.py`, qui les place avec une graine fixe et écrit `data/levels/*.json` (reproductible, contrôlé par `--check` en CI). L'application ne lit que ces fichiers.

**D-021 — Difficulté = score calculé.** `2×taille + 3×nombre de mots + 1,5×longueur moyenne + 6×palier + 20×ambiguïté` (arrondi). Le score stocké est revérifié (Kotlin et Python). Règles : score strictement croissant dans un chapitre ; taille jamais décroissante, hausse d'un cran maximum (3→4→5→6) hors défi final ; défi final (niveau 10) en 10×10, score ≥ 1,3 × celui du niveau 9 (réel : ×1,5 et ×1,34) ; 3×3 réservé aux niveaux 1 et 2 du chapitre 1.

**D-022 — Quatre styles visuels.** `STYLE_1..3` tournent selon le chapitre : bande = (niveau−1)/3, style = (bande + chapitre − 1) mod 3 ; `FINAL` est réservé au niveau 10.

## Progression, étoiles, indices

**D-023 — Étoiles.** < 3:00 = 3 ; de 3:00 à < 10:00 = 2 ; ≥ 10:00 = 1 (10:00 pile = 1 étoile). Horloge monotone injectée (`SystemClock.elapsedRealtime`). Le chronomètre démarre à la première interaction avec la grille et se suspend quand l'appli passe en arrière-plan.

**D-024 — Un niveau gagné est verrouillé.** Seul le premier niveau non terminé est « disponible », les suivants « verrouillés », les terminés « terminés » : non rejouables. Terminer un niveau déjà terminé ne change rien (idempotent).

**D-025 — Praise.** Points de performance 0–7 (étoiles, sans indice, sans erreur, temps < 90 s, défi final, série de 3 étoiles) ; le praise est tiré (graine déterministe) parmi les deux plus hauts niveaux accessibles, sans répéter le précédent. Une performance banale ne donne jamais mieux que « Nice ».

**D-026 — Indices.** 2 gratuits au premier lancement (une seule fois, drapeau persistant). Publicité récompensée : 1 ou 2, tirés seulement après confirmation du SDK, avec identifiant de diffusion mémorisé (32 derniers) contre les doubles récompenses. Cadeau : un par chapitre, réclamable une seule fois après le niveau 10, 1 à 3 indices.

**D-027 — Interstitiel.** « Lancer une partie » = démarrage d'un niveau neuf (depuis l'accueil ou « niveau suivant »). Compteur persistant ; l'interstitiel est proposé au 2e, 4e, 6e… démarrage. Reprises et redémarrages ne sont jamais comptés. Jamais au premier rendu d'un écran, pendant un tracé, pendant la résolution d'une victoire ni après une action sensible ; jamais bloquant. Le premier démarrage n'affiche rien.

## Limites de cette livraison

**D-028 — Rien n'a pu être compilé avec Gradle/Android ici.** L'environnement de travail n'a pas accès aux dépôts Maven/Google. Ont été compilés et testés avec `kotlinc` + JUnit (et `android.jar` API 34 pour le code qui n'utilise que le framework) : tout `:domain`, les parseurs JSON, la persistance JSON, la table de praise, la séquence de démarrage, le contrôleur de retours, l'audio `SoundPool`, l'haptique et la connectivité. **Non compilés** : tout le code Compose (écrans, composants), `AppViewModel`/`GameController` (coroutines), les dépôts DataStore, l'intégration Google Mobile Ads/UMP, le build Gradle. La première exécution GitHub Actions est la vraie vérification : il faut s'attendre à quelques erreurs de compilation à corriger à partir du journal.

## Phases 3 à 8 — décisions complémentaires

**D-029 — Accueil.** L'accueil met en avant le niveau en cours (chapitre, thème, difficulté, grille) avec un bouton « Jouer » unique : aucun écran intermédiaire. Boutons : Paramètres et langue FR|EN en haut à gauche, Monument en haut à droite. La progression vers le cadeau du chapitre est une barre de 10 segments menant à l'icône cadeau.

**D-030 — Navigation à état, sans bibliothèque.** Pile d'écrans dans `AppViewModel` ; transitions par glissement (sens avant/arrière). Retour : depuis le jeu → confirmation si une partie est commencée, puis accueil ; depuis les menus → écran précédent ; depuis l'accueil → comportement Android standard (sortie). Après une victoire, Retour mène à l'accueil.

**D-031 — Reprise de partie.** La partie en cours (mots trouvés, indices, erreurs, temps écoulé) est sauvegardée à chaque mot trouvé, à chaque indice, à la mise en arrière-plan et en quittant ; elle est supprimée à la victoire. La confirmation de sortie informe que la progression est sauvegardée. Un redémarrage volontaire remet la grille et le chronomètre à zéro (après confirmation si la partie est commencée).

**D-032 — Victoire atomique.** `completeLevel` écrit en une seule transaction : niveau terminé, étoiles, temps, praise, série, suppression de la sauvegarde de partie. Si l'application est tuée entre le dernier mot et l'écriture, la sauvegarde contient tous les mots : à la réouverture la victoire est enregistrée automatiquement. Un niveau gagné est verrouillé et ne peut pas être rejoué.

**D-033 — Interstitiel.** Compté au tap « Jouer » sur un niveau neuf (pas au retour sur le même niveau, pas lors d'un redémarrage) et affiché AVANT l'ouverture de l'écran de jeu, jamais au premier rendu ni pendant un tracé. Le premier démarrage n'affiche rien ; ensuite 1 sur 2.

**D-034 — Persistance.** DataStore typé avec sérialisation JSON versionnée (`schemaVersion`, chaîne de migrations, refus d'écraser une sauvegarde d'une version plus récente) ; copie de secours `.bak` du dernier état valide, restaurée si le fichier est corrompu (le fichier corrompu est conservé en `.corrupt`). Les écritures de sauvegarde de partie passent par un fil unique pour conserver l'ordre.

**D-035 — Langue dans l'application.** Choix FR/EN persistant, appliqué immédiatement (contexte localisé fourni à Compose). Tant que le joueur n'a pas choisi, l'application suit la langue du téléphone. Les chaînes sont dans `res/values` (EN) et `res/values-fr`, vérifiées en CI (parité, paramètres, pluriels).

**D-036 — Paramètres proposés.** Langue, effets sonores, retour haptique, réduire les animations, texte plus grand, crédits et mentions légales, options de confidentialité publicitaire (si la région l'exige), restauration des valeurs par défaut (avec confirmation). Pas de réglage de musique : aucun fichier de musique n'est livré (le brief dit « si présente »).

**D-037 — Audio.** Sons d'interface générés pour le jeu (`scripts/generate_ui_sounds.py`, WAV 16 bits mono 32 kHz, enveloppes adoucies, pic à −4 dBFS) et préchargés dans un `SoundPool` (6 flux max) pendant le chargement ; libération en fermant l'activité, pause en arrière-plan. WAV plutôt qu'OGG : aucun risque de codec, fichiers de 1 à 80 ko.

**D-038 — Haptique.** Quatre intensités (sélection légère, validation moyenne, erreur très courte, succès marqué) via `Vibrator` ; actif seulement si l'option du jeu ET le réglage Android « retour tactile » le permettent.

**D-039 — Immersif.** Barres système masquées pendant la partie (sticky : un balayage depuis le bord les fait réapparaître), restaurées en quittant l'écran de jeu. Les gestes système et le bouton Retour restent disponibles.

**D-040 — Publicités.** Google Mobile Ads avec les identifiants de test officiels uniquement ; consentement UMP au lancement ; bannière dans une zone réservée de 50 dp sous le bouton Indice ; récompensée proposée quand le solde est vide (1 ou 2 indices tirés après confirmation, identifiant de diffusion unique) ; hors ligne ou sans annonce : message discret, aucun indice retiré, aucune récompense fantôme.

**D-041 — Composants sans fondu.** `AlertDialog`, `Dialog`, `Snackbar`, `ModalBottomSheet`, `DropdownMenu` sont interdits (fondus intégrés) : panneaux modaux maison qui glissent (`SheetOverlay`), messages qui glissent (`MessageHost`), interrupteur maison (`WgSwitchVisual`). Contrôle automatique en CI.

**D-042 — Icônes.** Icônes d'interface vectorielles (`scripts/generate_vector_icons.py`) : formes dérivées des Material Icons de Google (Apache 2.0, créditées dans l'application et `LICENSES.md`) et deux formes dessinées pour le projet (monument, couronne). Logo et icône d'application : `assets/branding/app-logo.svg` → `scripts/generate_icons.py`.

**D-043 — Accessibilité.** Cibles ≥ 48 dp, descriptions de contenu et états (`stateDescription`) sur tous les contrôles, statuts des niveaux dits en texte (pas par la couleur seule), texte agrandissable (échelle système plafonnée à 1,3 + option « texte plus grand »), animations réduites (réglage système ou option). **Limite connue :** le tracé de mots au doigt n'a pas d'alternative pour lecteur d'écran (voir `docs/QA.md`).

**D-044 — Tests instrumentés.** Un parcours de fumée (`SmokeTest`) est fourni avec un workflow manuel sur émulateur (`instrumented.yml`) ; il n'a jamais été exécuté. Risque connu : les animations infinies du décor peuvent ralentir la détection d'inactivité de Compose Test.

**D-045 — Réduction de la portée du « 3×3 ».** Inchangé : 3×3 uniquement aux niveaux 1 et 2 du chapitre 1 (validé).
