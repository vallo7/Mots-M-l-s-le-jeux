package com.motsmeles.game.domain.progress

import com.motsmeles.game.domain.engine.Praise

data class LevelResult(val stars: Int, val elapsedMillis: Long)

/** Partie en cours sauvegardée (reprise après interruption). Les cases d'indice sont déduites des mots indicés. */
data class GameSnapshot(
    val foundWordIndices: Set<Int>,
    val hintedWordIndices: List<Int>,
    val wrongSelections: Int,
    val elapsedMillis: Long,
)

enum class LevelStatus { LOCKED, AVAILABLE, COMPLETED }

/**
 * Progression persistante (sérialisée par la couche data, avec migration de schéma).
 * Tout changement passe par [ProgressRules] : fonctions pures, idempotentes quand c'est nécessaire.
 */
data class ProgressState(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val initialHintsGranted: Boolean = false,
    val hintBalance: Int = 0,
    val completed: Map<String, LevelResult> = emptyMap(),
    /** Indices des chapitres dont le cadeau a été réclamé. */
    val claimedGiftChapters: Set<Int> = emptySet(),
    /** Identifiants des dernières récompenses publicitaires déjà accordées (anti-doublon). */
    val grantedRewardIds: List<String> = emptyList(),
    val lastPraise: Praise? = null,
    /** Niveaux consécutifs terminés avec 3 étoiles. */
    val perfectStreak: Int = 0,
    /** Parties en cours, clé « idNiveau:langue ». Supprimées à la victoire. */
    val inProgress: Map<String, GameSnapshot> = emptyMap(),
) {
    companion object {
        const val CURRENT_SCHEMA = 1
    }
}
