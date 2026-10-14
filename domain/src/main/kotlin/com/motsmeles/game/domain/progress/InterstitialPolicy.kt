package com.motsmeles.game.domain.progress

enum class LevelStartKind {
    /** Le joueur lance un niveau neuf (depuis l'accueil ou « niveau suivant »). Seul type compté. */
    FRESH,
    /** Reprise d'une partie commencée (retour d'écran, reprise d'appli, restauration). */
    RESUME,
    /** Redémarrage volontaire du niveau en cours. */
    RESTART,
}

data class InterstitialDecision(val state: ProgressState, val shouldOffer: Boolean)

/** Contexte UI dans lequel un interstitiel serait affiché. */
data class AdMoment(
    val isFirstRender: Boolean = false,
    val isGridBeingTouched: Boolean = false,
    val isResolvingWin: Boolean = false,
    val isSensitiveActionPending: Boolean = false,
)

/**
 * Un interstitiel est proposé un démarrage de niveau éligible sur deux (2e, 4e, 6e…).
 * Reprises et redémarrages ne sont jamais comptés. Le compteur est persistant.
 */
object InterstitialPolicy {
    const val EVERY_N_STARTS = 2

    /**
     * Compte un démarrage éligible : niveau neuf ([LevelStartKind.FRESH]) différent du dernier niveau
     * déjà compté. Revenir sur le même niveau (retour, reprise) n'est pas un nouveau démarrage.
     */
    fun onLevelStart(state: ProgressState, levelId: String, kind: LevelStartKind): InterstitialDecision {
        if (kind != LevelStartKind.FRESH || state.lastCountedStart == levelId) return InterstitialDecision(state, false)
        val count = state.eligibleLevelStarts + 1
        return InterstitialDecision(
            state.copy(eligibleLevelStarts = count, lastCountedStart = levelId),
            count % EVERY_N_STARTS == 0,
        )
    }

    fun canShowNow(moment: AdMoment): Boolean =
        !moment.isFirstRender && !moment.isGridBeingTouched && !moment.isResolvingWin && !moment.isSensitiveActionPending
}
