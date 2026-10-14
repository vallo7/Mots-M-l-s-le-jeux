# Journal des modifications

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
