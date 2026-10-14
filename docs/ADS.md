# Publicités (Google Mobile Ads) — intégration de test

**Statut : intégration écrite (phase 6), non compilée ni testée sur appareil.** Les règles métier sont testées sur JVM (`ProgressRules.grantRewardedHints`, `InterstitialPolicy`, `GameUseCases.watchRewardedAd/startLevel`, 79 tests).

## Identifiants de test officiels Google (les seuls autorisés)
Configuration : `app/src/main/res/values/ads_config.xml` (et `AndroidManifest.xml` pour l'ID d'application).
- Éditeur de test : `ca-app-pub-3940256099942544`
- App ID : `ca-app-pub-3940256099942544~3347511713`
- Bannière `…/6300978111` · Interstitiel `…/1033173712` · Récompensée `…/5224354917`
La CI refuse tout identifiant `ca-app-pub-` d'un autre éditeur.

## Comportement
- **Consentement (UMP)** demandé au lancement ; le formulaire n'apparaît que si la région l'exige ; aucune annonce avant `canRequestAds`. Un réglage « Options de confidentialité des pubs » apparaît si la région l'exige.
- **Bannière** : zone réservée de 50 dp en bas de l'écran de jeu, sous le bouton Indice ; vide si indisponible ; ne recouvre jamais grille, mots, indice ni commandes système.
- **Récompensée** : proposée quand le solde d'indices est vide ; la récompense (1 ou 2) est tirée seulement après confirmation du SDK ; chaque diffusion a un identifiant unique mémorisé (32 derniers) → pas de double récompense, pas d'exploit de reprise ; fermée avant la fin → rien.
- **Interstitiel** : un démarrage de niveau neuf sur deux, avant l'ouverture de l'écran de jeu (D-033) ; jamais au premier rendu, pendant un tracé, pendant la victoire ; son échec ne bloque jamais.
- **Hors ligne / échec** : message discret (« hors ligne », « aucune vidéo disponible »), aucun indice retiré, aucune récompense fantôme, jeu intact.

## Avant production
1. Créer l'application et les blocs dans AdMob. 2. Remplacer les identifiants de `ads_config.xml` par les vrais (les fournir via un fichier non committé ou une variable de build, jamais dans Git). 3. Configurer le message UMP dans AdMob. 4. Tester sur appareil réel avec des appareils de test déclarés. 5. Compléter `PRIVACY.md` et la déclaration de données Play. 6. Retirer la contrainte CI « éditeur de test » pour la branche de release.
