package com.motsmeles.game.domain.usecases

import com.motsmeles.game.domain.engine.GameSession
import com.motsmeles.game.domain.engine.HintOutcome
import com.motsmeles.game.domain.engine.HintService
import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.engine.PraiseContext
import com.motsmeles.game.domain.engine.PraiseSelector
import com.motsmeles.game.domain.engine.RevealedHint
import com.motsmeles.game.domain.engine.StarRating
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.Level
import com.motsmeles.game.domain.model.LevelCatalog
import com.motsmeles.game.domain.ports.AdProvider
import com.motsmeles.game.domain.ports.ProgressRepository
import com.motsmeles.game.domain.ports.RewardedAdResult
import com.motsmeles.game.domain.ports.UnavailableReason
import com.motsmeles.game.domain.progress.GameSnapshot
import com.motsmeles.game.domain.progress.ProgressRules
import com.motsmeles.game.domain.progress.RandomSource

data class VictoryResult(
    val levelId: String,
    val chapterIndex: Int,
    val stars: Int,
    val elapsedMillis: Long,
    val praise: Praise,
    val chapterCompleted: Boolean,
    val giftClaimable: Boolean,
    val nextLevelId: String?,
    /** Niveaux terminés du chapitre avant / après cette victoire : alimente la barre de progression de fin de niveau. */
    val chapterCompletedBefore: Int,
    val chapterCompletedAfter: Int,
    val chapterTotal: Int,
)

sealed interface HintUseResult {
    data class Used(val hint: RevealedHint, val newBalance: Int) : HintUseResult
    data object NoBalance : HintUseResult
    data object LimitReached : HintUseResult
    data object NothingToReveal : HintUseResult
    /** La sauvegarde a échoué : aucun indice n'est consommé ni révélé. */
    data object Failed : HintUseResult
}

sealed interface RewardOutcome {
    data class Granted(val amount: Int) : RewardOutcome
    data object Dismissed : RewardOutcome
    data object Duplicate : RewardOutcome
    data class Unavailable(val reason: UnavailableReason) : RewardOutcome
}

/**
 * Cas d'utilisation de la partie. Chaque modification de progression passe par
 * [ProgressRepository.update] (atomique) ; rien n'est accordé ni retiré avant que l'action soit réussie.
 */
class GameUseCases(
    private val catalog: LevelCatalog,
    private val progress: ProgressRepository,
    private val ads: AdProvider,
    private val random: RandomSource,
) {
    suspend fun loadSnapshot(level: Level, language: Language): GameSnapshot? =
        progress.read().inProgress[ProgressRules.snapshotKey(level.id, language)]

    suspend fun saveSnapshot(session: GameSession) {
        if (session.isSolved) return
        saveSnapshot(session.level, session.language, session.snapshot())
    }

    /** Variante pour appeler depuis un autre fil que celui de la session : le cliché est pris avant. */
    suspend fun saveSnapshot(level: Level, language: Language, snapshot: GameSnapshot) {
        progress.update { ProgressRules.saveSnapshot(it, level.id, language, snapshot) }
    }

    suspend fun clearSnapshots(level: Level) {
        progress.update { ProgressRules.clearSnapshots(it, level.id) }
    }

    /** Victoire : vérifiée par le moteur, enregistrée de façon atomique et idempotente. */
    suspend fun completeLevel(session: GameSession): VictoryResult {
        check(session.isSolved) { "Le moteur ne considère pas le niveau comme résolu" }
        val level = session.level
        val elapsed = session.finish()
        val stars = StarRating.starsFor(elapsed)
        val before = progress.read()
        val alreadyDone = level.id in before.completed
        val praise = PraiseSelector.select(
            PraiseContext(
                stars = stars,
                hintsUsed = session.state.hintsUsed,
                wrongSelections = session.state.wrongSelections,
                elapsedMillis = elapsed,
                isFinalChallenge = level.isFinalChallenge,
                perfectStreakBefore = before.perfectStreak,
            ),
            previous = before.lastPraise,
            seed = level.id.hashCode().toLong() * 31 + before.completed.size,
        )
        val countBefore = ProgressRules.chapterCompletedCount(catalog, before, level.chapterIndex)
        val after = progress.update { ProgressRules.completeLevel(it, level.id, stars, elapsed, praise) }
        val stored = after.completed.getValue(level.id)
        return VictoryResult(
            levelId = level.id,
            chapterIndex = level.chapterIndex,
            stars = stored.stars,
            elapsedMillis = stored.elapsedMillis,
            praise = if (alreadyDone) (after.lastPraise ?: Praise.NICE) else praise,
            chapterCompleted = ProgressRules.isChapterComplete(catalog, after, level.chapterIndex),
            giftClaimable = ProgressRules.isGiftClaimable(catalog, after, level.chapterIndex),
            nextLevelId = catalog.levelAfter(level.id)?.id,
            chapterCompletedBefore = countBefore,
            chapterCompletedAfter = ProgressRules.chapterCompletedCount(catalog, after, level.chapterIndex),
            chapterTotal = catalog.chapters.firstOrNull { it.index == level.chapterIndex }?.levels?.size ?: Level.LEVELS_PER_CHAPTER,
        )
    }

    /** Ne retire l'indice qu'une fois le solde persisté ; ne révèle rien si la sauvegarde échoue. */
    suspend fun useHint(session: GameSession): HintUseResult {
        val balance = progress.read().hintBalance
        return when (val outcome = HintService.useHint(session.state, balance)) {
            is HintOutcome.Revealed -> {
                val after = try {
                    progress.update { ProgressRules.spendHint(it) ?: it }
                } catch (e: Exception) {
                    return HintUseResult.Failed
                }
                if (after.hintBalance >= balance) return HintUseResult.NoBalance
                session.applyHint(outcome)
                HintUseResult.Used(outcome.hint, after.hintBalance)
            }
            HintOutcome.NoBalance -> HintUseResult.NoBalance
            HintOutcome.LimitReached -> HintUseResult.LimitReached
            HintOutcome.NothingToReveal -> HintUseResult.NothingToReveal
        }
    }

    /** Trophée de chapitre (ex-cadeau) : 1 à 3 indices, une seule fois. Renvoie le gain, ou null si rien n'est réclamable. */
    suspend fun claimGift(chapterIndex: Int): Int? {
        var amount: Int? = null
        progress.update { state ->
            val claim = ProgressRules.claimGift(catalog, state, chapterIndex, random)
            amount = claim?.amount
            claim?.state ?: state
        }
        return amount
    }

    /** Publicité récompensée : la récompense (1 indice) n'est accordée qu'après confirmation du SDK. */
    suspend fun watchRewardedAd(): RewardOutcome {
        ads.rewardedUnavailableReason()?.let { return RewardOutcome.Unavailable(it) }
        return when (val result = ads.showRewarded()) {
            is RewardedAdResult.Rewarded -> {
                var granted = 0
                progress.update { state ->
                    val grant = ProgressRules.grantRewardedHints(state, result.rewardId, random)
                    granted = grant?.amount ?: 0
                    grant?.state ?: state
                }
                if (granted > 0) RewardOutcome.Granted(granted) else RewardOutcome.Duplicate
            }
            RewardedAdResult.Dismissed -> RewardOutcome.Dismissed
            is RewardedAdResult.Unavailable -> RewardOutcome.Unavailable(result.reason)
        }
    }
}
