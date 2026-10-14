package com.motsmeles.game.domain

import com.motsmeles.game.domain.content.LevelValidator
import com.motsmeles.game.domain.content.TextNormalizer
import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.domain.model.Direction
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.TargetWord
import com.motsmeles.game.domain.model.VisualStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelValidatorTest {
    private fun codes(level: com.motsmeles.game.domain.model.Level) = LevelValidator.validateLevel(level).map { it.code }

    @Test fun `reference level is valid`() {
        assertEquals(emptyList<String>(), LevelValidator.validateLevel(Fixtures.level()).map { it.toString() })
    }

    @Test fun `normalizer strips accents and non letters`() {
        assertEquals("ILE", TextNormalizer.toGridLetters("Île"))
        assertEquals("ETE", TextNormalizer.toGridLetters("été"))
        assertEquals("ARCENCIEL", TextNormalizer.toGridLetters("arc-en-ciel"))
    }

    @Test fun `grid mismatch is detected`() {
        val bad = Fixtures.enVariant.copy(grid = listOf("SXN", "KQZ", "YWV"))
        assertTrue("GRID_MISMATCH" in codes(Fixtures.level(variants = mapOf(Language.EN to bad, Language.FR to Fixtures.frVariant))))
    }

    @Test fun `word leaving the grid is detected`() {
        val bad = Fixtures.enVariant.copy(words = listOf(TargetWord("SUN", "SUN", Cell(0, 1), Direction.E), Fixtures.enVariant.words[1]))
        assertTrue("OUT_OF_BOUNDS" in codes(Fixtures.level(variants = mapOf(Language.EN to bad, Language.FR to Fixtures.frVariant))))
    }

    @Test fun `direction forbidden by tier is detected`() {
        val reversed = Fixtures.enVariant.copy(
            grid = listOf("NUS", "KQZ", "YWV"),
            words = listOf(TargetWord("SUN", "SUN", Cell(0, 2), Direction.W), Fixtures.enVariant.words[1].copy(start = Cell(0, 0))),
        )
        assertTrue("DIRECTION" in codes(Fixtures.level(variants = mapOf(Language.EN to reversed, Language.FR to Fixtures.frVariant))))
    }

    @Test fun `word appearing twice is ambiguous`() {
        val twice = Fixtures.enVariant.copy(grid = listOf("SUN", "KQZ", "YWV").let { listOf(it[0], it[1], "SUN").let { g -> g } })
        // SUN en ligne 0 et en ligne 2 ; SKY colonne 0 devient S K S -> on remplace par un mot cohérent
        val fixed = twice.copy(grid = listOf("SUN", "KQZ", "SUN"), words = listOf(twice.words[0], TargetWord("SK", "SK", Cell(0, 0), Direction.S)))
        assertTrue("AMBIGUOUS_WORD" in codes(Fixtures.level(variants = mapOf(Language.EN to fixed, Language.FR to Fixtures.frVariant))))
    }

    @Test fun `word contained in another is detected`() {
        val grid = listOf("SUNS", "KQZW", "YWVX", "ABCD")
        val v = Fixtures.enVariant.copy(
            grid = grid,
            words = listOf(TargetWord("SUNS", "SUNS", Cell(0, 0), Direction.E), TargetWord("UN", "UN", Cell(0, 1), Direction.E)),
        )
        val level = Fixtures.level(gridSize = 4, number = 3, variants = mapOf(Language.EN to v, Language.FR to Fixtures.frVariant))
        assertTrue("WORD_CONTAINED" in codes(level))
    }

    @Test fun `3x3 grid is refused outside chapter 1 levels 1 and 2`() {
        assertTrue("GRID_3X3" in codes(Fixtures.level(id = "c2-l01", chapter = 2, number = 1)))
        assertTrue("GRID_3X3" in codes(Fixtures.level(id = "c1-l03", chapter = 1, number = 3)))
        assertTrue("GRID_3X3" !in codes(Fixtures.level(id = "c1-l02", chapter = 1, number = 2)))
    }

    @Test fun `wrong score and wrong style are detected`() {
        assertTrue("SCORE" in codes(Fixtures.level(score = 999)))
        assertTrue("STYLE_RULE" in codes(Fixtures.level(style = VisualStyle.STYLE_3)))
    }

    @Test fun `missing language variant is detected`() {
        assertTrue("VARIANT_MISSING" in codes(Fixtures.level(variants = mapOf(Language.EN to Fixtures.enVariant))))
    }

    @Test fun `final level must be 10x10 with final style`() {
        assertTrue("FINAL_SIZE" in codes(Fixtures.level(id = "c1-l10", number = 10)))
        assertTrue("STYLE_FINAL" in codes(Fixtures.level(id = "c1-l10", number = 10, style = VisualStyle.STYLE_1)))
        assertTrue("STYLE_FINAL" in codes(Fixtures.level(id = "c1-l02", number = 2, style = VisualStyle.FINAL)))
    }

    @Test fun `style rotation depends on chapter and band`() {
        assertEquals(VisualStyle.STYLE_1, VisualStyle.forLevel(1, 1))
        assertEquals(VisualStyle.STYLE_2, VisualStyle.forLevel(1, 4))
        assertEquals(VisualStyle.STYLE_3, VisualStyle.forLevel(1, 9))
        assertEquals(VisualStyle.STYLE_2, VisualStyle.forLevel(2, 1))
        assertEquals(VisualStyle.FINAL, VisualStyle.forLevel(2, 10))
    }

    @Test fun `catalog checks ordering of difficulty and chapter shape`() {
        val issues = LevelValidator.validateCatalog(Fixtures.stubCatalog(1)).map { it.code }
        assertTrue("DIFFICULTY_ORDER" in issues) // niveaux factices identiques
    }
}
