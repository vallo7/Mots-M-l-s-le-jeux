# Checklist de réalisation

Légende : ✅ fait et vérifié ici · 🟡 écrit, **non vérifié par un build Android** (Gradle/Compose/SDK indisponibles dans l'environnement de travail) · ⬜ à faire

## Phases
| Phase | État | Détail |
|---|---|---|
| 0 Cadrage | ✅ | `DECISIONS.md` (45 décisions) ; aucune image de référence n'existe (D-001) |
| 1 Fondations | 🟡 | Gradle 2 modules, thème, chargement réel (4 étapes), lancement blanc, workflow CI — jamais exécuté |
| 2 Moteur | ✅ | 79 tests JVM verts (session, tracé, indices, étoiles, praise, progression, cadeau, publicité, interstitiel, cas d'utilisation) |
| 3 Interface complète | 🟡 | Accueil, jeu, monument, paramètres, crédits, résultat, navigation, retour, immersif, persistance DataStore — écrits, non compilés |
| 4 Identité et sensations | 🟡 | Sons d'interface générés et contrôlés ; icônes/illustrations dessinées et vues ; audio/haptique compilés contre `android.jar` ; composants animés non compilés |
| 5 Contenu | ✅ | 20 niveaux FR+EN validés (Kotlin + Python), reproductibles |
| 6 Monétisation de test | 🟡 | Règles testées ✅ ; intégration Google Mobile Ads + UMP écrite, non compilée |
| 7 QA et optimisation | 🟡 | Contrôles automatiques ✅ ; tests sur appareils/émulateurs ⬜ (`docs/QA.md`) |
| 8 Livraison | 🟡 | Dépôt complet livré ; clone propre, CI verte et APK publié **non vérifiés** |

## Critères d'acceptation
- 🟡 Compile depuis un clone propre via GitHub Actions
- 🟡 Lancement blanc puis chargement réel ≥ 2 s une seule fois (logique testée ✅, rendu non vérifié)
- 🟡 Accueil sur le niveau en cours ; boutons aux emplacements demandés
- 🟡 Mots au-dessus de la grille ; Indice sous la grille à droite ; bannière en zone réservée
- ✅ 20 niveaux cohérents, résolubles, bilingues ; 3×3 uniquement c1 niveaux 1-2
- ✅ Étoiles : seuils exacts, y compris 10:00
- ✅ Victoire verrouille le niveau (règle) ; 🟡 côté interface
- ✅ 2 indices initiaux une fois ; récompensée 1–2 après confirmation sans doublon ; cadeau 1–3 une fois par chapitre (règles + cas d'utilisation) ; 🟡 SDK/UI
- ✅ Interstitiel 1 sur 2 ; reprises/redémarrages/retours non comptés ; 🟡 SDK
- 🟡 Sons, haptique, boutons animés, praise, watermark intégrés et désactivables (code écrit, non exécuté)
- ✅ Aucun fondu ni confetti, composants à fondu interdits (contrôle CI)
- 🟡 Persistance après fermeture forcée : sérialisation, migration et corruption testées ✅ ; DataStore non exécuté
- 🟡 Retour Android, immersif, hors ligne : logique testée ✅ ; rendu non vérifié
- ✅ Aucun secret ni identifiant publicitaire de production (contrôle CI)
- ✅ README : build local/CI, APK, logo, audios, langue

## Vérifications exécutées (environnement de travail)
- `:domain` : 79 tests JUnit → OK (kotlinc 2.0.21)
- `:app` code pur JVM : 33 tests → OK (niveaux livrés, table de praise, sons d'interface, persistance JSON, séquence de démarrage, retours)
- `scripts/check_repo.py`, `validate_levels.py`, `generate_levels.py --check`, `check_resources.py` → OK
- Compilation sans erreur de `:domain` et du code `:app` limité au framework (`android.jar` API 34)
- Non exécuté : Gradle, lint, tests `:app` sous Gradle, rendu Compose, émulateur, appareils, SDK publicitaire
