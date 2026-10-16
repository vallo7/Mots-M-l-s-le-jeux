package com.motsmeles.game.domain.engine

import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.domain.model.Direction
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.Level
import com.motsmeles.game.domain.model.LevelVariant

data class RevealedHint(val wordIndex: Int, val cell: Cell)

/** État immuable d'une partie. La résolution est *calculée* ici, jamais déduite de l'UI. */
data class GameState(
    val level: Level,
    val language: Language,
    val foundWordIndices: Set<Int> = emptySet(),
    val revealedHints: List<RevealedHint> = emptyList(),
    val wrongSelections: Int = 0,
) {
    val variant: LevelVariant get() = level.variant(language)
    val hintsUsed: Int get() = revealedHints.size
    val isSolved: Boolean get() = variant.words.indices.all { it in foundWordIndices }
    val foundCells: Set<Cell> get() = foundWordIndices.flatMap { variant.words[it].cells }.toSet()
}

sealed interface SelectionResult {
    val state: GameState

    data class Found(val wordIndex: Int, override val state: GameState) : SelectionResult
    data class AlreadyFound(val wordIndex: Int, override val state: GameState) : SelectionResult
    data class Wrong(override val state: GameState) : SelectionResult
    /** Tap simple ou sélection trop courte : ni erreur, ni succès. */
    data class Ignored(override val state: GameState) : SelectionResult
}

object GameEngine {
    fun newGame(level: Level, language: Language): GameState = GameState(level, language)

    /**
     * Évalue le tracé du joueur ([path], dans l'ordre du doigt). Le sens importe peu : on compare
     * l'ensemble de cases au placement des mots cibles (unicité garantie par le validateur).
     */
    fun select(state: GameState, path: List<Cell>): SelectionResult {
        if (path.size < 2) return SelectionResult.Ignored(state)
        if (!isStraightLine(state.variant, path)) {
            return SelectionResult.Wrong(state.copy(wrongSelections = state.wrongSelections + 1))
        }
        val selected = path.toSet()
        val index = state.variant.words.indexOfFirst { it.cells.toSet() == selected }
        return when {
            index < 0 -> SelectionResult.Wrong(state.copy(wrongSelections = state.wrongSelections + 1))
            index in state.foundWordIndices -> SelectionResult.AlreadyFound(index, state)
            else -> SelectionResult.Found(index, state.copy(foundWordIndices = state.foundWordIndices + index))
        }
    }

    fun isStraightLine(variant: LevelVariant, path: List<Cell>): Boolean {
        if (path.size < 2 || path.any { !variant.inBounds(it) }) return false
        val direction = Direction.stepBetween(path[0], path[1]) ?: return false
        return (1 until path.size).all { Direction.stepBetween(path[it - 1], path[it]) == direction }
    }
}
