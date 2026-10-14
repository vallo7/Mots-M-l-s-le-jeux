package com.motsmeles.game.domain.content

import com.motsmeles.game.domain.model.Level

/**
 * Score de difficulté éditorial : combine taille de grille, nombre et longueur des mots, palier de
 * directions et ambiguïté (proportion de lettres leurres). Formule identique dans
 * scripts/level_tools.py (vérifiée en CI).
 */
object DifficultyScore {
    fun compute(gridSize: Int, wordCount: Double, averageLength: Double, tier: Int, ambiguity: Double): Int =
        Math.round(gridSize * 2.0 + wordCount * 3.0 + averageLength * 1.5 + tier * 6.0 + ambiguity * 20.0).toInt()

    fun compute(level: Level): Int {
        val variants = level.variants.values
        val wordCount = variants.map { it.words.size }.average()
        val averageLength = variants.flatMap { it.words }.map { it.length }.average()
        return compute(level.gridSize, wordCount, averageLength, level.difficulty.tier, level.difficulty.ambiguity)
    }
}
