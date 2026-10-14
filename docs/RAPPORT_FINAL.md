# Rapport final

## Lien du workflow et artifact
Workflow : `.github/workflows/android.yml` (onglet *Actions* du dépôt une fois poussé). Artifact APK debug : **`word-game-debug-apk`**. **Je n'ai pas pu exécuter ce workflow : aucun APK n'a été produit ni publié par moi.**

## Fonctionnalités livrées (code écrit)
Accueil (niveau en cours, Jouer, langue FR|EN, paramètres, monument, progression vers le cadeau) · écran de jeu (retour, chapitre/niveau, redémarrer, chronomètre, mots au-dessus, grille tracée au doigt, indice à droite, bannière réservée) · monument des chapitres (tour de 10 paliers, statuts, étoiles, thèmes, défi final distinct, cadeau) · paramètres (langue, sons, haptique, animations réduites, texte plus grand, crédits, confidentialité pubs, restauration) · résultat (étoiles, temps, praise dynamique texte + voix, niveau suivant, cadeau) · chargement réel ≥ 2 s une fois + lancement blanc · 20 niveaux FR/EN · étoiles, indices, cadeau, récompensée, interstitiel 1/2 · sons d'interface + praise, haptique 4 intensités · immersif sticky, retour Android, reprise de partie, persistance atomique avec migration et copie de secours · CI, contrôles automatiques, documentation.

## Tests exécutés (dans mon environnement) et résultats
| Quoi | Résultat |
|---|---|
| `:domain` — 79 tests JUnit (kotlinc 2.0.21) | ✅ OK |
| `:app` code pur JVM — 33 tests (niveaux livrés, praise, sons, persistance, démarrage, retours) | ✅ OK |
| `validate_levels.py`, `generate_levels.py --check`, `check_repo.py`, `check_resources.py` | ✅ OK |
| Compilation de `:domain` et du code `:app` limité au framework (`android.jar` API 34) | ✅ sans erreur |
| Rendu des icônes et du logo (images vérifiées à l'œil) | ✅ |

## Ce qui n'a PAS été vérifié
- Gradle, AGP, lint, tests sous Gradle, **compilation de tout le code Compose**, de `AppViewModel`/`GameController` (coroutines), de DataStore et de Google Mobile Ads/UMP. Il y a probablement des erreurs de compilation à corriger au premier run.
- Aucun rendu à l'écran, aucun appareil, aucun émulateur ; fluidité, tailles d'écran, encoche, accessibilité non mesurées ; `SmokeTest` jamais lancé.
- Aucune annonce réelle ou de test n'a été diffusée.

## Écarts, hypothèses et limites
- Aucune image de référence n'existe : mécanique « mots mêlés par tracé » choisie à partir du texte (D-002).
- Pas de musique (aucun fichier) : option absente (D-036).
- Praise identique en FR et EN, texte compris (D-007, confirmé).
- Provenance/licence des 8 sons de praise à confirmer par l'auteur du projet.
- Pas d'alternative lecteur d'écran pour le tracé ; portrait et thème clair uniquement.
- Versions de dépendances volontairement figées (D-016), pas les plus récentes.

## Remplacer les assets
Logo, sons, niveaux, identifiants publicitaires : `docs/ASSETS.md`, `docs/LEVEL_AUTHORING.md`, `docs/ADS.md`, README.

## Prochaine étape recommandée
Pousser le dépôt, lancer le workflow, m'envoyer le journal du premier échec ; boucler jusqu'au vert, puis dérouler `docs/QA.md` sur un vrai téléphone.
