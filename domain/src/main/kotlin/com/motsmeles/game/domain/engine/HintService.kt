package com.motsmeles.game.domain.engine

sealed interface HintOutcome {
    /** Indice exécutable : l'appelant persiste [newBalance] puis applique [state]. */
    data class Revealed(val hint: RevealedHint, val state: GameState, val newBalance: Int) : HintOutcome
    data object NoBalance : HintOutcome
    data object LimitReached : HintOutcome
    data object NothingToReveal : HintOutcome
}

/**
 * Un indice révèle la première case d'un mot non trouvé et pas déjà indicé.
 * Le solde n'est décrémenté que dans un résultat [HintOutcome.Revealed].
 */
object HintService {
    fun useHint(state: GameState, balance: Int): HintOutcome {
        if (state.isSolved) return HintOutcome.NothingToReveal
        if (state.hintsUsed >= state.level.hints.maxPerLevel) return HintOutcome.LimitReached
        if (balance <= 0) return HintOutcome.NoBalance
        val hinted = state.revealedHints.map { it.wordIndex }.toSet()
        val words = state.variant.words
        val index = words.indices.firstOrNull { it !in state.foundWordIndices && it !in hinted }
            ?: return HintOutcome.NothingToReveal
        val hint = RevealedHint(index, words[index].start)
        return HintOutcome.Revealed(hint, state.copy(revealedHints = state.revealedHints + hint), balance - 1)
    }
}
