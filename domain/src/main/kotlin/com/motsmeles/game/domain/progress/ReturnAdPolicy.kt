package com.motsmeles.game.domain.progress

/** Contexte UI dans lequel une publicité plein écran serait affichée. */
data class AdMoment(
    val isFirstRender: Boolean = false,
    val isGridBeingTouched: Boolean = false,
    val isResolvingWin: Boolean = false,
    val isSensitiveActionPending: Boolean = false,
)

/**
 * Publicité de retour dans l'application : proposée quand le joueur revient dans le jeu après l'avoir quitté
 * sans le fermer (l'appli était en arrière-plan). Aucune publicité au lancement à froid, ni au démarrage d'un niveau.
 *
 * Garde-fous (jamais de publicité involontaire ni répétitive) :
 *  - absence d'au moins [MIN_AWAY_MS] (un simple coup d'œil à une notification n'en déclenche pas) ;
 *  - au moins [COOLDOWN_MS] depuis la dernière publicité de retour ;
 *  - jamais sans interaction préalable : le retour doit suivre une session réellement démarrée ;
 *  - jamais quand le retour vient d'une publicité plein écran (récompensée ou de retour) ;
 *  - jamais pendant une manipulation de grille, une victoire en cours de résolution ou une action sensible.
 * Les durées sont mesurées par une horloge monotone, jamais par l'heure civile du téléphone.
 */
object ReturnAdPolicy {
    const val MIN_AWAY_MS = 5_000L
    const val COOLDOWN_MS = 60_000L

    fun shouldShow(
        awayMillis: Long,
        sinceLastReturnAdMillis: Long?,
        cameBackFromAd: Boolean,
        sessionStarted: Boolean,
        moment: AdMoment,
    ): Boolean =
        sessionStarted &&
            !cameBackFromAd &&
            awayMillis >= MIN_AWAY_MS &&
            (sinceLastReturnAdMillis == null || sinceLastReturnAdMillis >= COOLDOWN_MS) &&
            canShowNow(moment)

    fun canShowNow(moment: AdMoment): Boolean =
        !moment.isFirstRender && !moment.isGridBeingTouched && !moment.isResolvingWin && !moment.isSensitiveActionPending
}
