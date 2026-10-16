package com.motsmeles.game.domain.engine

import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.Level
import com.motsmeles.game.domain.progress.GameSnapshot

sealed interface SessionEvent {
    data class Found(val wordIndex: Int, val solved: Boolean) : SessionEvent
    data class AlreadyFound(val wordIndex: Int) : SessionEvent
    data object Wrong : SessionEvent
    data object Ignored : SessionEvent
}

/**
 * Partie en cours : moteur de règles + chronomètre. Sans Android, sans coroutines.
 * Le chronomètre démarre à la première interaction avec la grille.
 */
class GameSession(val level: Level, val language: Language, clock: MonotonicClock) {
    private val timer = GameTimer(clock)

    var state: GameState = GameEngine.newGame(level, language)
        private set

    val isSolved: Boolean get() = state.isSolved
    val hasStarted: Boolean get() = timer.hasStarted
    val isRunning: Boolean get() = timer.isRunning
    fun elapsedMillis(): Long = timer.elapsedMillis()

    fun beginInteraction() {
        if (!isSolved) timer.startIfNeeded()
    }

    fun submit(path: List<Cell>): SessionEvent {
        if (isSolved) return SessionEvent.Ignored
        beginInteraction()
        val result = GameEngine.select(state, path)
        state = result.state
        return when (result) {
            is SelectionResult.Found -> SessionEvent.Found(result.wordIndex, state.isSolved)
            is SelectionResult.AlreadyFound -> SessionEvent.AlreadyFound(result.wordIndex)
            is SelectionResult.Wrong -> SessionEvent.Wrong
            is SelectionResult.Ignored -> SessionEvent.Ignored
        }
    }

    fun applyHint(outcome: HintOutcome.Revealed) {
        state = outcome.state
    }

    fun pause() = timer.pause()

    fun resume() {
        if (!isSolved) timer.resume()
    }

    /** Fige le chronomètre et renvoie le temps final. */
    fun finish(): Long = timer.stop()

    fun restart() {
        state = GameEngine.newGame(level, language)
        timer.reset()
    }

    fun snapshot(): GameSnapshot = GameSnapshot(
        foundWordIndices = state.foundWordIndices,
        hintedWordIndices = state.revealedHints.map { it.wordIndex },
        wrongSelections = state.wrongSelections,
        elapsedMillis = timer.elapsedMillis(),
    )

    /** Restaure une sauvegarde ; ignore les indices invalides (sauvegarde d'une autre version). */
    fun restore(snapshot: GameSnapshot) {
        val words = state.variant.words
        val valid = { i: Int -> i in words.indices }
        state = GameState(
            level = level,
            language = language,
            foundWordIndices = snapshot.foundWordIndices.filter(valid).toSet(),
            revealedHints = snapshot.hintedWordIndices.filter(valid).distinct().map { RevealedHint(it, words[it].start) },
            wrongSelections = snapshot.wrongSelections.coerceAtLeast(0),
        )
        timer.restoreElapsed(snapshot.elapsedMillis)
    }
}
