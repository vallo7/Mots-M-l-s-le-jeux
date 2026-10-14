# Système de design (v1.0)

Code : `app/src/main/java/com/motsmeles/game/presentation/theme/` et `.../components/`.

## Palette
| Jeton | Valeur | Usage |
|---|---|---|
| Cream | `#FFF8EE` | fond |
| Surface | `#FFFFFF` | cartes, grille |
| Ink | `#2B2A33` | texte principal (≈ 13:1 sur Cream) |
| InkMuted | `#6E6C7A` | texte secondaire (≈ 4,8:1) |
| Outline | `#E7DFD2` | traits, pistes |
| Coral | `#FF7A59` | action principale, niveau disponible |
| Teal | `#2BB3A3` | succès, mots trouvés, style 1 |
| Sun | `#FFC145` | étoiles, indices, style 2 |
| Plum | `#6C4AB6` | défi final (style 4), praise |
| Error | `#D64550` | erreurs |
Le texte posé sur un accent est Ink (≥ 5:1) sauf sur Plum (blanc). La couleur n'est jamais la seule information (icône + texte : cadenas, coche, étoiles, libellé de statut).

## Styles visuels de difficulté (4)
`STYLE_1` Teal · `STYLE_2` Sun · `STYLE_3` Coral tournent selon le chapitre (D-022) ; `FINAL` Plum est réservé au niveau 10 (couronne, bordure Plum, tuile en sommet du monument). Chaque chapitre a aussi son ambiance (`meadow`, `dusk` : fond et illustration).

## Formes, espacements, tailles
Rayons 8 / 16 / 24 dp, pilule · espacements 4 / 8 / 16 / 24 / 32 dp · cible ≥ 48 dp, bouton 56 dp, ligne de réglage ≥ 64 dp · typographie système (titres gras 32 / 22 / 18 sp, corps 16 sp), échelle de police plafonnée à 1,3 (+15 % avec « texte plus grand »).

## Composants
`Pressable` (base) · `WgButton` (Primary / Secondary / Ghost) · `WgIconButton` · `WgSwitchVisual` · `DifficultyBadge` · `StarsRow` · `ProgressSegments` · `GiftBadge` · `HintChip` · `ScreenTopBar` · `SheetOverlay` / `ConfirmSheet` · `MessageHost` · `WatermarkBackground` · `LetterGrid` · `AdBanner`.
États : repos · pressé (échelle 0,94, rebond contrôlé) · focus (contour 2 dp Ink) · sélectionné · désactivé (couleurs neutres) · erreur · chargement · confirmation — avec `contentDescription` et `stateDescription`.

## Motion
Interactions 100–300 ms (`WgMotion`). Pression : retour immédiat (échelle, son, haptique), action après ~90 ms, garde anti double-clic 350 ms (jamais un blocage long). Écrans, panneaux et messages **glissent** ; mots trouvés, étoiles, praise **rebondissent** ; la grille **se secoue** sur une erreur ; l'indice **pulse** ; le cadeau prêt **saute**. **Interdits : fondus, confettis, composants à fondu intégré** (contrôle CI). Décor : filigrane très peu contrasté (lettres à 5 %), dérive lente 18 s, tracé qui se dessine puis se retire sur 8 s ; tout est statique si les animations sont réduites (réglage système ou option).
