# Journal des modifications

## v3 — 16 octobre 2026 : refonte visuelle « juicy », nouvelles règles, nouvelle icône
**Interface et animations**
- Nouveau système de design : couleurs vives, fond blanc, polices plus grasses, boutons « bulle » 3D plus petits (zone tactile de 48 dp conservée), effet gelée à l'appui. Icônes redessinées d'après les références (trophée à la place du cadeau, étoile, ampoule, couronne, horloge, badge pub…), rubans de popup d'après la planche fournie. Voir `docs/DESIGN_SYSTEM.md`.
- Accueil : carrousel de chapitres (glisse, parallaxe, point actif élastique) ; toucher le chapitre en cours ouvre le niveau en cours. Bouton FR/EN retiré de l'accueil (déplacé dans les paramètres).
- Grille : rendu Canvas unique (`fx/GridFx.kt`) — serpent élastique, pop des lettres, vague + anneaux + étincelles à chaque mot, secousse élastique à l'erreur, **mélange animé des lettres au redémarrage (popup supprimé)**.
- Popups (paramètres, crédits, quitter, pub récompensée) : ruban en titre, entrée en rebond, voile qui s'ouvre en cercle. Écran des niveaux en pilules (maquette). Victoire : rayons, étoiles qui rebondissent avec onde de choc, **barre de progression continue du chapitre** animée.
- Ampoule d'indice : badge « pub » quand le solde est à zéro. Compteur de temps agrandi. Filigrane plus gros et plus discret. Chargement : tuiles qui sautent, barre continue.
**Règles et contenu**
- Étoiles : < 1:00 = 3, < 3:00 = 2, ≥ 3:00 = 1. Pub récompensée = 1 indice. Interstitiel au lancement supprimé ; annonce de test au retour dans l'appli (`ReturnAdPolicy`).
- Grilles 3/4/6/7/8/10 + 12×12 pour le défi final ; 20 niveaux régénérés avec des mots plus longs et plus nombreux (ratio final ≥ 1,25 : D-046).
**Audio** : musique d'ambiance en boucle (option dédiée), sons `shuffle` et `swipe`, 3 propositions de morceaux (`docs/MUSIC.md`).
**Icône d'application** : remplacée par l'image fournie (mode PNG de `generate_icons.py`, calque monochrome dédié).
**Qualité** : 90 tests de règles exécutés (dont les nouveaux : étoiles, politique de pub de retour, validateur, mouvement). **Non compilé avec Gradle** (environnement sans Maven/Google) : première exécution GitHub Actions à vérifier.

## v2 — 14 octobre 2026 : correctif de la CI (premier run)
**Problème observé** : l'étape « Check expected files, secrets and motion rules » échouait en signalant `praise_map.json`, les 8 mp3 de praise et les 10 wav d'interface comme absents.
**Diagnostic** : l'archive livrée contient bien ces 19 fichiers et `check_repo.py` passe en local ; l'arborescence de `main` sur GitHub (captures) les contient aussi. Le commit testé par ce run ne les avait donc pas : run lancé avant leur ajout, ou « Re-run jobs » d'un ancien run (qui rejoue l'ancien commit).
**Modifications**
- `.github/workflows/android.yml` : nouvelle première étape qui affiche le commit testé, le nombre de fichiers suivis et le contenu des dossiers de sons ; nouvelle étape qui supprime les `.gitkeep` avant le build (ils sont inoffensifs mais inutiles dans `res/`).
- `scripts/check_repo.py` : message d'aide explicite quand des fichiers audio manquent (commit testé, comment relancer) ; `docs/CHANGELOG.md` ajouté aux fichiers attendus.
- `scripts/check_kotlin_calls.py` (nouveau) : vérifie que les appels aux fonctions du projet ne contiennent ni argument nommé inconnu ni trop d'arguments (aide quand Gradle est indisponible). Résultat sur tout le code : aucune incohérence.
- `README.md` : section Dépannage (Re-run vs nouveau run, `.gitkeep`).
- `docs/QA.md` : mention des deux scripts d'aide.
- `assets/illustrations/README.md` (nouveau) remplace un `.gitkeep` ; dossier vide `res/xml` supprimé.
- `docs/CHANGELOG.md` (nouveau).
**Inchangé** : tout le code Kotlin, les ressources, les niveaux et les sons.

## v1 — 13 octobre 2026
Première livraison complète (phases 0 à 8 écrites ; voir `docs/RAPPORT_FINAL.md`).
