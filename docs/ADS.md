# Publicités (Google Mobile Ads) — intégration de test

**Statut : intégration écrite (phase 6), non compilée ni testée sur appareil.** Les règles métier sont testées sur JVM (`ProgressRules.grantRewardedHints`, `ReturnAdPolicy`, `GameUseCases.watchRewardedAd`).

## Identifiants de test officiels Google (les seuls autorisés)
Configuration : `app/src/main/res/values/ads_config.xml` (et `AndroidManifest.xml` pour l'ID d'application).
- Éditeur de test : `ca-app-pub-3940256099942544`
- App ID : `ca-app-pub-3940256099942544~3347511713`
- Bannière `…/6300978111` · Annonce d'ouverture (retour dans l'appli) `…/9257395921` · Récompensée `…/5224354917`
La CI refuse tout identifiant `ca-app-pub-` d'un autre éditeur.

## Comportement
- **Consentement (UMP)** demandé au lancement ; le formulaire n'apparaît que si la région l'exige ; aucune annonce avant `canRequestAds`. Un réglage « Options de confidentialité des pubs » apparaît si la région l'exige.
- **Bannière** : zone réservée de 50 dp en bas de l'écran de jeu, sous le bouton Indice ; vide si indisponible ; ne recouvre jamais grille, mots, indice ni commandes système.
- **Récompensée** : proposée quand le solde d'indices est vide ; la récompense (**1 indice**) n'est accordée qu'après confirmation du SDK ; chaque diffusion a un identifiant unique mémorisé (32 derniers) → pas de double récompense, pas d'exploit de reprise ; fermée avant la fin → rien.
- **Annonce de retour** (D-049) : affichée quand le joueur revient dans l'appli après l'avoir quittée sans la fermer (`ReturnAdPolicy` : absence ≥ 5 s, ≥ 60 s depuis la précédente, jamais au démarrage à froid ni au retour d'une autre pub plein écran, jamais pendant un tracé, la victoire ou une action en cours). L'annonce est préchargée, expire après 4 h, et son échec ne bloque jamais le jeu. **Plus aucun interstitiel au lancement d'une partie.**
- **Hors ligne / échec** : message discret (« hors ligne », « aucune vidéo disponible »), aucun indice retiré, aucune récompense fantôme, jeu intact.

## Avant production
1. Créer l'application et les blocs dans AdMob. 2. Remplacer les identifiants de `ads_config.xml` par les vrais (les fournir via un fichier non committé ou une variable de build, jamais dans Git). 3. Configurer le message UMP dans AdMob. 4. Tester sur appareil réel avec des appareils de test déclarés. 5. Compléter `PRIVACY.md` et la déclaration de données Play. 6. Retirer la contrainte CI « éditeur de test » pour la branche de release.
