package com.motsmeles.game.data

import com.motsmeles.game.domain.content.LevelValidator
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Valide le contenu réellement livré dans data/levels (les mêmes fichiers que ceux embarqués dans l'APK). */
class LevelAssetsTest {
    private val dir: File = listOf(File("../data/levels"), File("data/levels")).first { it.isDirectory }
    private val catalog = LevelJsonParser.parseCatalog { name -> File(dir, name).readText(Charsets.UTF_8) }

    @Test fun `shipped catalog passes every validation rule`() {
        val issues = LevelValidator.validateCatalog(catalog)
        assertEquals(issues.joinToString("\n"), 0, issues.size)
    }

    @Test fun `two chapters of ten levels make twenty levels`() {
        assertEquals(2, catalog.chapters.size)
        assertTrue(catalog.chapters.all { it.levels.size == Level.LEVELS_PER_CHAPTER })
        assertEquals(20, catalog.levels.size)
    }

    @Test fun `3x3 grids only in chapter 1 levels 1 and 2`() {
        val threes = catalog.levels.filter { it.gridSize == 3 }.map { it.id }
        assertEquals(listOf("c1-l01", "c1-l02"), threes)
    }

    @Test fun `every level exists in both languages with the same word count`() {
        catalog.levels.forEach { level ->
            val counts = Language.entries.map { level.variant(it).words.size }.toSet()
            assertEquals("${level.id} : nombre de mots", 1, counts.size)
        }
    }

    @Test fun `4x4 grids only in chapter 1 and 12x12 only for final challenges`() {
        assertTrue(catalog.levels.filter { it.gridSize == 4 }.all { it.chapterIndex == 1 })
        assertTrue(catalog.levels.filter { it.gridSize == 12 }.all { it.number == Level.LEVELS_PER_CHAPTER })
        assertTrue(catalog.levels.none { it.gridSize == 5 })
        assertTrue(catalog.levels.all { it.gridSize in LevelValidator.ALLOWED_GRID_SIZES })
    }

    @Test fun `words get longer as levels go up`() {
        catalog.chapters.forEach { ch ->
            fun avgLen(n: Int) = ch.levels.first { it.number == n }.variant(Language.FR).words.map { it.letters.length }.average()
            assertTrue(avgLen(9) > avgLen(3))
            assertTrue(avgLen(10) > avgLen(9))
        }
    }

    @Test fun `final challenges are 12x12 and clearly harder than level 9`() {
        catalog.chapters.forEach { ch ->
            val ninth = ch.levels.first { it.number == 9 }
            val tenth = ch.levels.first { it.number == 10 }
            assertEquals(12, tenth.gridSize)
            assertTrue(tenth.difficulty.score >= ninth.difficulty.score * LevelValidator.FINAL_MIN_RATIO)
        }
    }

    @Test fun `unsupported schema is rejected`() {
        val e = runCatching { LevelJsonParser.parseCatalog { """{"schemaVersion": 99, "chapters": []}""" } }.exceptionOrNull()
        assertTrue(e is LevelJsonParser.LevelDataException)
    }
}
