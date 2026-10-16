package com.motsmeles.game.domain

import com.motsmeles.game.domain.content.DifficultyScore
import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.domain.model.Chapter
import com.motsmeles.game.domain.model.DifficultyProfile
import com.motsmeles.game.domain.model.Direction
import com.motsmeles.game.domain.model.HintConfig
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.Level
import com.motsmeles.game.domain.model.LevelCatalog
import com.motsmeles.game.domain.model.LevelVariant
import com.motsmeles.game.domain.model.LocalizedText
import com.motsmeles.game.domain.model.TargetWord
import com.motsmeles.game.domain.model.VisualStyle

object Fixtures {
    val enVariant = LevelVariant(
        Language.EN,
        listOf("SUN", "KQZ", "YWV"),
        listOf(
            TargetWord("SUN", "SUN", Cell(0, 0), Direction.E),
            TargetWord("SKY", "SKY", Cell(0, 0), Direction.S),
        ),
    )
    val frVariant = LevelVariant(
        Language.FR,
        listOf("MER", "TOK", "ILE"),
        listOf(
            TargetWord("MER", "MER", Cell(0, 0), Direction.E),
            TargetWord("Île", "ILE", Cell(2, 0), Direction.E),
        ),
    )

    /** Niveau 3×3 valide (chapitre 1, niveau 1). */
    fun level(
        id: String = "c1-l01",
        chapter: Int = 1,
        number: Int = 1,
        gridSize: Int = 3,
        tier: Int = 1,
        ambiguity: Double = 0.0,
        maxHints: Int = 3,
        variants: Map<Language, LevelVariant> = mapOf(Language.EN to enVariant, Language.FR to frVariant),
        style: VisualStyle = VisualStyle.forLevel(chapter, number),
        score: Int? = null,
    ): Level {
        val draft = Level(
            id, chapter, number, LocalizedText("Plage", "Beach"), style, gridSize,
            DifficultyProfile(tier, ambiguity, 0), HintConfig(maxHints), variants,
        )
        val computed = score ?: DifficultyScore.compute(draft)
        return draft.copy(difficulty = draft.difficulty.copy(score = computed))
    }

    /** Catalogue factice (niveaux sans contenu valide) pour tester la progression. */
    fun stubCatalog(chapters: Int = 2): LevelCatalog = LevelCatalog(
        (1..chapters).map { c ->
            Chapter(
                "chapter-0$c", c, LocalizedText("Chapitre $c", "Chapter $c"), "palette$c",
                (1..10).map { n -> level(id = "c$c-l%02d".format(n), chapter = c, number = n) },
            )
        },
    )

    val SUN = listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2))
    val SKY = listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0))
}
