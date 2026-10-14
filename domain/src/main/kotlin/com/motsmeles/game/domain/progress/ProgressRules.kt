package com.motsmeles.game.domain.progress

import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.Level
import com.motsmeles.game.domain.model.LevelCatalog

/** Source d'aléa injectable (tests déterministes). Intervalle [from, untilExclusive). */
fun interface RandomSource {
    fun nextInt(from: Int, untilExclusive: Int): Int
}

data class GiftClaim(val state: ProgressState, val amount: Int)
data class RewardGrant(val state: ProgressState, val amount: Int)

object ProgressRules {
    const val INITIAL_FREE_HINTS = 2
    const val REWARDED_MIN = 1
    const val REWARDED_MAX = 2
    const val GIFT_MIN = 1
    const val GIFT_MAX = 3
    const val REMEMBERED_REWARD_IDS = 32

    /** Premier lancement : exactement 2 indices gratuits, une seule fois. */
    fun initialize(state: ProgressState): ProgressState =
        if (state.initialHintsGranted) state
        else state.copy(initialHintsGranted = true, hintBalance = state.hintBalance + INITIAL_FREE_HINTS)

    /** Un niveau gagné est verrouillé : rappeler cette fonction ne change rien (idempotent). */
    fun completeLevel(
        state: ProgressState,
        levelId: String,
        stars: Int,
        elapsedMillis: Long,
        praise: Praise?,
    ): ProgressState {
        if (levelId in state.completed) return state
        return state.copy(
            completed = state.completed + (levelId to LevelResult(stars.coerceIn(1, 3), elapsedMillis.coerceAtLeast(0))),
            perfectStreak = if (stars >= 3) state.perfectStreak + 1 else 0,
            lastPraise = praise ?: state.lastPraise,
            inProgress = state.inProgress.filterKeys { !it.startsWith("$levelId:") },
        )
    }

    fun snapshotKey(levelId: String, language: Language): String = "$levelId:${language.code}"

    /** Sauvegarde la partie en cours ; sans effet sur un niveau déjà terminé. */
    fun saveSnapshot(state: ProgressState, levelId: String, language: Language, snapshot: GameSnapshot): ProgressState =
        if (levelId in state.completed) state
        else state.copy(inProgress = state.inProgress + (snapshotKey(levelId, language) to snapshot))

    fun clearSnapshots(state: ProgressState, levelId: String): ProgressState =
        state.copy(inProgress = state.inProgress.filterKeys { !it.startsWith("$levelId:") })

    /** Premier niveau non terminé dans l'ordre du catalogue, ou null si tout est terminé. */
    fun currentLevel(catalog: LevelCatalog, state: ProgressState): Level? =
        catalog.levels.firstOrNull { it.id !in state.completed }

    fun statusOf(catalog: LevelCatalog, state: ProgressState, levelId: String): LevelStatus = when {
        levelId in state.completed -> LevelStatus.COMPLETED
        currentLevel(catalog, state)?.id == levelId -> LevelStatus.AVAILABLE
        else -> LevelStatus.LOCKED
    }

    fun chapterCompletedCount(catalog: LevelCatalog, state: ProgressState, chapterIndex: Int): Int =
        catalog.levels.count { it.chapterIndex == chapterIndex && it.id in state.completed }

    fun isChapterComplete(catalog: LevelCatalog, state: ProgressState, chapterIndex: Int): Boolean {
        val total = catalog.levels.count { it.chapterIndex == chapterIndex }
        return total > 0 && chapterCompletedCount(catalog, state, chapterIndex) == total
    }

    fun isGiftClaimable(catalog: LevelCatalog, state: ProgressState, chapterIndex: Int): Boolean =
        chapterIndex !in state.claimedGiftChapters && isChapterComplete(catalog, state, chapterIndex)

    /** Cadeau de chapitre : 1 à 3 indices, une seule fois par chapitre (idempotent). */
    fun claimGift(catalog: LevelCatalog, state: ProgressState, chapterIndex: Int, random: RandomSource): GiftClaim? {
        if (!isGiftClaimable(catalog, state, chapterIndex)) return null
        val amount = random.nextInt(GIFT_MIN, GIFT_MAX + 1)
        return GiftClaim(
            state.copy(
                hintBalance = state.hintBalance + amount,
                claimedGiftChapters = state.claimedGiftChapters + chapterIndex,
            ),
            amount,
        )
    }

    /** Retire un indice. À appeler seulement quand l'indice a pu être exécuté. */
    fun spendHint(state: ProgressState): ProgressState? =
        if (state.hintBalance > 0) state.copy(hintBalance = state.hintBalance - 1) else null

    /**
     * Récompense publicitaire confirmée par le SDK. [rewardId] identifie *cette* diffusion :
     * un rappel multiple ou une reprise d'appli avec le même identifiant n'accorde rien de plus.
     * L'aléa (1 à 2) n'est tiré qu'ici, après confirmation.
     */
    fun grantRewardedHints(state: ProgressState, rewardId: String, random: RandomSource): RewardGrant? {
        if (rewardId.isBlank() || rewardId in state.grantedRewardIds) return null
        val amount = random.nextInt(REWARDED_MIN, REWARDED_MAX + 1)
        return RewardGrant(
            state.copy(
                hintBalance = state.hintBalance + amount,
                grantedRewardIds = (state.grantedRewardIds + rewardId).takeLast(REMEMBERED_REWARD_IDS),
            ),
            amount,
        )
    }
}
