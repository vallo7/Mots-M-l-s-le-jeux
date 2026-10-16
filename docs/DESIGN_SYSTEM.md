# Système de design (v2.0)

Code : `app/src/main/java/com/motsmeles/game/presentation/theme/`, `.../components/` et `.../fx/`.

## Palette (fond blanc, couleurs vives)
| Jeton | Valeur | Usage |
|---|---|---|
| Background | `#FFFFFF` | fond de tous les écrans |
| Ink / InkMuted | `#2A2750` / `#7B79A6` | texte principal / secondaire |
| Purple · PurpleDark · PurpleDeep | `#7B5CFF` · `#5B3EE0` · `#3F2AB0` | action secondaire, pilules de niveau, défi final, victoire |
| Green · GreenDark | `#5CD62F` · `#38A818` | action principale (jouer), progression |
| Blue · Yellow · Orange · Pink · Red · Teal | `#2EB8FF` · `#FFC81E` · `#FF8A1F` · `#FF3D8B` · `#FF4B55` · `#14D1B8` | accents, styles de difficulté, mots trouvés |
| Locked · LockedDark · LockedInk | `#D9D7EF` · `#BDBAE0` · `#9592C4` | éléments verrouillés |
Chaque couleur d'action a une **lèvre** foncée (variante `…Dark`) : c'est le relief 3D des boutons. Le texte sur une couleur d'action est blanc (Ink sur Yellow). La couleur n'est jamais la seule information (icône, texte, coche, cadenas).

## Styles de difficulté (4) et chapitres
`STYLE_1` Teal · `STYLE_2` Yellow · `STYLE_3` Pink tournent selon le chapitre ; `FINAL` Purple est réservé au niveau 10 (couronne, pilule dorée dans la liste). Ambiances de chapitre : `meadow` (vert, ciel jaune), `dusk` (violet).

## Formes, tailles, typographie
Rayons 10 / 18 / 28 dp, pilule · espacements 4 / 8 / 16 / 24 / 32 dp · **boutons plus petits** : bouton large 46 dp, bouton rond 38 dp, lèvre 4 dp — la **zone tactile reste de 48 dp** · compteur de temps 30 sp Black · titres Black (34 / 28 / 22 sp), libellés ExtraBold, texte Bold · échelle de police plafonnée à 1,3.

## Composants (`components/`)
`JuicyPressable` (base : écrasement à l'appui, rebond au relâchement, son, haptique, anti double-clic) · `BubbleFace` / `BubbleButton` / `WgButton` / `BubbleIconButton` / `RingButton` · `RibbonBanner` (rubans : rose, bleu, violet, jaune, vert, rouge) · `PopupHost` / `PopupScrim` / `PopupCard` / `ConfirmPopup` / `RewardedOfferPopup` · `ChunkyProgressBar` (barre continue) · `TrophyBadge` · `HintChip` · `StarsRow` / `StarFace` · `DifficultyBadge` · `WgSwitchVisual` · `MessageHost` · `WatermarkBackground` · `LetterGrid` (+ `fx/GridFx`) · `AdBanner`.
États : repos · pressé (écrasement + lèvre) · focus (contour 2 dp) · sélectionné · désactivé (lavande) · erreur · chargement · confirmation — avec descriptions accessibles.

## Motion (« bubble, juicy, fun »)
- **Ressorts** partout (`Motion.spring`, `spring()` Compose) : entrées en **pop** avec rebond (`popIn`), pression « gelée », points de pagination élastiques, compteurs qui rebondissent.
- **Grille** : serpent élastique qui suit le doigt (amortissement critique), pop des lettres, **vague + anneaux + étincelles** à chaque mot, secousse élastique à l'erreur, **mélange en trois étapes avec arcs et rotations** au redémarrage.
- **Écrans** : glissement avec décélération quintique et parallaxe (l'ancien écran recule d'un tiers). **Popups** : carte qui rebondit, voile qui s'ouvre en cercle, sortie en rétrécissant.
- **Victoire** : rayons qui tournent, étoiles qui retombent avec onde de choc, praise qui saute, barre de chapitre qui se remplit (courbe douce + reflet + rebond d'épaisseur).
- **Interdits (D-058)** : fondus et confettis. Les particules, anneaux et capsules **rétrécissent** au lieu de s'estomper ; les voiles et reflets ont une opacité **fixe**. Durées : interactions 100–300 ms, récompenses jusqu'à ~1,3 s. Tout devient statique avec « réduire les animations » (réglage système ou option).
