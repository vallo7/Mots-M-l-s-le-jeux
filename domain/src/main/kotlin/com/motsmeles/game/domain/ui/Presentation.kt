package com.motsmeles.game.domain.ui

import com.motsmeles.game.domain.model.Chapter
import com.motsmeles.game.domain.model.Level
import com.motsmeles.game.domain.model.LevelCatalog
import com.motsmeles.game.domain.model.LocalizedText
import com.motsmeles.game.domain.model.VisualStyle
import com.motsmeles.game.domain.progress.LevelStatus
import com.motsmeles.game.domain.progress.ProgressRules
import com.motsmeles.game.domain.progress.ProgressState
import com.motsmeles.game.domain.model.Language
import java.util.Locale

/** Badge de difficulté affiché sur les niveaux. */
enum class DifficultyBand {
    EASY, MEDIUM, HARD, FINAL;

    companion object {
        fun of(number: Int): DifficultyBand = when {
            number >= Level.FINAL_NUMBER -> FINAL
            number >= 7 -> HARD
            number >= 4 -> MEDIUM
            else -> EASY
        }
    }
}

object TimeFormat {
    /** 83 s -> « 1:23 ». Indépendant de la langue du téléphone. */
    fun mmss(millis: Long): String {
        val total = millis.coerceAtLeast(0) / 1000
        return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60)
    }
}

data class MonumentLevel(
    val levelId: String,
    val number: Int,
    val theme: LocalizedText,
    val gridSize: Int,
    val style: VisualStyle,
    val band: DifficultyBand,
    val status: LevelStatus,
    val stars: Int,
    val isFinal: Boolean,
)

data class MonumentChapter(
    val index: Int,
    val theme: LocalizedText,
    val paletteKey: String,
    val levels: List<MonumentLevel>,
    val completedCount: Int,
    val giftClaimed: Boolean,
    val giftClaimable: Boolean,
)

object MonumentBuilder {
    fun build(catalog: LevelCatalog, progress: ProgressState): List<MonumentChapter> =
        catalog.chapters.sortedBy { it.index }.map { chapter ->
            MonumentChapter(
                index = chapter.index,
                theme = chapter.theme,
                paletteKey = chapter.paletteKey,
                levels = chapter.levels.sortedBy { it.number }.map { level ->
                    MonumentLevel(
                        levelId = level.id,
                        number = level.number,
                        theme = level.theme,
                        gridSize = level.gridSize,
                        style = level.visualStyle,
                        band = DifficultyBand.of(level.number),
                        status = ProgressRules.statusOf(catalog, progress, level.id),
                        stars = progress.completed[level.id]?.stars ?: 0,
                        isFinal = level.isFinalChallenge,
                    )
                },
                completedCount = ProgressRules.chapterCompletedCount(catalog, progress, chapter.index),
                giftClaimed = chapter.index in progress.claimedGiftChapters,
                giftClaimable = ProgressRules.isGiftClaimable(catalog, progress, chapter.index),
            )
        }
}

/** Ce que l'accueil affiche : niveau en cours, progression vers le cadeau, solde d'indices. */
data class HomeModel(
    val currentLevel: Level?,
    val currentChapter: Chapter?,
    /** Chapitre dont la progression/le cadeau est mis en avant. */
    val giftChapter: Chapter?,
    val giftChapterCompleted: Int,
    val giftChapterTotal: Int,
    val giftClaimable: Boolean,
    val hintBalance: Int,
    val allCompleted: Boolean,
    val hasSnapshot: Boolean,
)

object HomeModelBuilder {
    fun build(catalog: LevelCatalog, progress: ProgressState, language: Language): HomeModel {
        val current = ProgressRules.currentLevel(catalog, progress)
        val currentChapter = current?.let { catalog.chapterOf(it) }
        val claimable = catalog.chapters.sortedBy { it.index }.firstOrNull { ProgressRules.isGiftClaimable(catalog, progress, it.index) }
        val giftChapter = claimable ?: currentChapter ?: catalog.chapters.maxByOrNull { it.index }
        return HomeModel(
            currentLevel = current,
            currentChapter = currentChapter,
            giftChapter = giftChapter,
            giftChapterCompleted = giftChapter?.let { ProgressRules.chapterCompletedCount(catalog, progress, it.index) } ?: 0,
            giftChapterTotal = giftChapter?.levels?.size ?: 0,
            giftClaimable = claimable != null,
            hintBalance = progress.hintBalance,
            allCompleted = current == null,
            hasSnapshot = current != null && ProgressRules.snapshotKey(current.id, language) in progress.inProgress,
        )
    }
}
