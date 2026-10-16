# Plan de QA

## Automatique (CI à chaque push)
`check_repo.py` (fichiers, secrets, motion) · aides locales : `check_kotlin_imports.py` et `check_kotlin_calls.py` (imports et signatures, quand Gradle est indisponible) · `check_resources.py` (FR/EN, références, textes en dur) · `validate_levels.py` + `generate_levels.py --check` · `:domain:test` (79) · `:app:testDebugUnitTest` (33) · `:app:lintDebug` · `:app:assembleDebug`.
Manuel sur émulateur : workflow « Instrumented tests » (`SmokeTest` : démarrage → accueil, Jouer → écran de jeu, monument).

## Matrice manuelle à dérouler sur appareil (⬜ non exécutée)
| Cas | Attendu |
|---|---|
| Petit écran 360×640, densité 2× | Rien ne se chevauche ; grille ≥ ~220 dp ; 10 mots lisibles (4 colonnes) ; bannière visible |
| Grand écran 412×915 et encoche | Contenu dans les zones sûres ; grille centrée |
| Android 8.0 (API 26) et 15 (API 35) | Lancement blanc, chargement, accueil ; icônes adaptatives |
| Échelle de police système 1,3 + « Texte plus grand » | Textes lisibles, pas de coupure bloquante |
| Animations supprimées (système) et option « Réduire les animations » | Filigrane, rebonds et vagues statiques ; tout reste jouable |
| Mode avion | Jeu complet ; « Obtenir des indices » → message hors ligne ; aucun indice retiré ni gagné |
| Fermeture forcée en pleine partie, puis relance | Accueil propose « Reprendre » ; mots trouvés et temps restaurés |
| Fermeture forcée juste après le dernier mot | À la réouverture du niveau, victoire enregistrée automatiquement |
| App en arrière-plan 1 min pendant le chrono | Le temps ne continue pas de courir |
| Appel entrant / écran verrouillé | Chrono suspendu, reprise propre |
| Geste Retour : jeu / menus / accueil | Confirmation / écran précédent / sortie Android |
| Barres système en jeu | Masquées ; balayage depuis le bord les affiche brièvement ; restaurées hors jeu |
| Changement de langue FR↔EN | Immédiat, persistant ; praise inchangé (D-007) |
| Sons/haptique désactivés | Jeu jouable, aucun son ni vibration |
| Victoire niveau 10 puis trophée | Trophée réclamable une seule fois ; « +N indices » |
| Quitter l'appli > 5 s puis revenir (≥ 60 s après la dernière) | Annonce de test d'ouverture ; indisponible/échec = on joue quand même ; aucune annonce au lancement d'une partie |
| Toucher Recommencer | Aucune fenêtre ; les lettres se mélangent puis se reposent (≈ 1 s) ; la saisie reprend après |
| Faire glisser le carrousel d'accueil | Glissement fluide, point actif élastique ; toucher le chapitre en cours ouvre son niveau |
| Vidéo récompensée fermée avant la fin | Aucun indice ; message |
| Rotation | Verrouillée en portrait |

## Limites connues
- Pas d'alternative lecteur d'écran pour le tracé de la grille (le reste est annoncé : états, mots, scores).
- Pas de thème sombre, pas de paysage (D-012).
- Aucune mesure de performance réelle (fluidité) n'a pu être faite ; la grille est dessinée par un seul `Canvas`, les lettres ne se recomposent pas pendant le tracé.
