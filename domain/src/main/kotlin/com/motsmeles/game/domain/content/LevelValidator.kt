package com.motsmeles.game.domain.content

import com.motsmeles.game.domain.engine.GameEngine
import com.motsmeles.game.domain.engine.SelectionResult
import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.domain.model.Direction
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.Level
import com.motsmeles.game.domain.model.LevelCatalog
import com.motsmeles.game.domain.model.LevelVariant
import com.motsmeles.game.domain.model.VisualStyle

data class ValidationIssue(val levelId: String?, val code: String, val message: String) {
    override fun toString(): String = "[${levelId ?: "catalogue"}] $code : $message"
}

/** Règles de validation du contenu, exécutées au démarrage, en test et (miroir Python) en CI. */
object LevelValidator {
    val ALLOWED_GRID_SIZES = setOf(3, 4, 5, 6, 10)
    const val FINAL_MIN_RATIO = 1.3
    private val SIZE_STEPS = listOf(3, 4, 5, 6)

    fun validateCatalog(catalog: LevelCatalog): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        if (catalog.chapters.isEmpty()) issues += ValidationIssue(null, "NO_CHAPTER", "aucun chapitre")
        val ids = catalog.levels.map { it.id }
        ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.forEach {
            issues += ValidationIssue(it, "DUPLICATE_ID", "identifiant de niveau en double")
        }
        val sortedChapters = catalog.chapters.sortedBy { it.index }
        sortedChapters.forEachIndexed { i, chapter ->
            if (chapter.index != i + 1) issues += ValidationIssue(null, "CHAPTER_INDEX", "chapitre ${chapter.id} : index ${chapter.index}, attendu ${i + 1}")
            if (chapter.theme.fr.isBlank() || chapter.theme.en.isBlank()) issues += ValidationIssue(null, "CHAPTER_THEME", "chapitre ${chapter.id} : thème manquant")
            val levels = chapter.levels.sortedBy { it.number }
            if (levels.map { it.number } != (1..Level.LEVELS_PER_CHAPTER).toList()) {
                issues += ValidationIssue(null, "CHAPTER_LEVELS", "chapitre ${chapter.id} : les niveaux doivent être numérotés 1..${Level.LEVELS_PER_CHAPTER}")
            }
            levels.forEach { if (it.chapterIndex != chapter.index) issues += ValidationIssue(it.id, "CHAPTER_REF", "chapterIndex ${it.chapterIndex} ≠ ${chapter.index}") }
            for (k in 1 until levels.size) {
                val prev = levels[k - 1]
                val cur = levels[k]
                if (cur.difficulty.score <= prev.difficulty.score) {
                    issues += ValidationIssue(cur.id, "DIFFICULTY_ORDER", "score ${cur.difficulty.score} ≤ score du niveau précédent ${prev.difficulty.score}")
                }
                if (cur.gridSize < prev.gridSize) {
                    issues += ValidationIssue(cur.id, "GRID_SHRINK", "la taille de grille ne doit pas diminuer")
                }
                if (!cur.isFinalChallenge) {
                    val a = SIZE_STEPS.indexOf(prev.gridSize)
                    val b = SIZE_STEPS.indexOf(cur.gridSize)
                    if (a >= 0 && b >= 0 && b - a > 1) issues += ValidationIssue(cur.id, "GRID_JUMP", "hausse de taille trop brutale (${prev.gridSize}→${cur.gridSize})")
                }
            }
            val ninth = levels.firstOrNull { it.number == Level.FINAL_NUMBER - 1 }
            val tenth = levels.firstOrNull { it.number == Level.FINAL_NUMBER }
            if (ninth != null && tenth != null && tenth.difficulty.score < ninth.difficulty.score * FINAL_MIN_RATIO) {
                issues += ValidationIssue(tenth.id, "FINAL_NOT_HARD_ENOUGH", "le défi final doit valoir ≥ ${FINAL_MIN_RATIO}× le niveau 9 (${tenth.difficulty.score} vs ${ninth.difficulty.score})")
            }
            levels.forEach { issues += validateLevel(it) }
        }
        return issues
    }

    fun validateLevel(level: Level): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        fun add(code: String, message: String) { issues += ValidationIssue(level.id, code, message) }

        if (level.id.isBlank()) add("ID", "identifiant vide")
        if (level.chapterIndex < 1) add("CHAPTER", "chapitre invalide")
        if (level.number !in 1..Level.LEVELS_PER_CHAPTER) add("NUMBER", "numéro hors 1..${Level.LEVELS_PER_CHAPTER}")
        if (level.theme.fr.isBlank() || level.theme.en.isBlank()) add("THEME", "thème manquant dans une langue")
        if (level.gridSize !in ALLOWED_GRID_SIZES) add("GRID_SIZE", "taille ${level.gridSize} non autorisée")
        if (level.gridSize == 3 && !(level.chapterIndex == 1 && level.number <= 2)) add("GRID_3X3", "3×3 réservé aux niveaux 1 et 2 du chapitre 1")
        if (level.isFinalChallenge) {
            if (level.visualStyle != VisualStyle.FINAL) add("STYLE_FINAL", "le niveau 10 doit utiliser le style FINAL")
            if (level.gridSize != 10) add("FINAL_SIZE", "le défi final doit être en 10×10")
        } else if (level.visualStyle == VisualStyle.FINAL) {
            add("STYLE_FINAL", "le style FINAL est réservé au niveau 10")
        }
        if (level.number in 1..Level.LEVELS_PER_CHAPTER && level.visualStyle != VisualStyle.forLevel(level.chapterIndex, level.number)) {
            add("STYLE_RULE", "style ${level.visualStyle} ≠ ${VisualStyle.forLevel(level.chapterIndex, level.number)} attendu")
        }
        if (level.difficulty.tier !in 1..4) add("TIER", "palier ${level.difficulty.tier} hors 1..4")
        if (level.difficulty.ambiguity !in 0.0..1.0) add("AMBIGUITY", "ambiguïté hors 0..1")
        if (level.hints.maxPerLevel < 1) add("HINTS", "maxPerLevel doit être ≥ 1")
        for (language in Language.entries) {
            if (level.variants[language] == null) add("VARIANT_MISSING", "variante ${language.code} absente")
        }
        if (level.variants.values.map { it.words.size }.distinct().size > 1) add("WORD_COUNT", "nombre de mots différent entre langues")
        if (issues.none { it.code == "VARIANT_MISSING" } && level.variants.isNotEmpty()) {
            val expected = DifficultyScore.compute(level)
            if (expected != level.difficulty.score) add("SCORE", "score ${level.difficulty.score} ≠ calculé $expected")
        }
        level.variants.values.forEach { issues += validateVariant(level, it) }
        return issues
    }

    private fun validateVariant(level: Level, variant: LevelVariant): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val tag = variant.language.code
        fun add(code: String, message: String) { issues += ValidationIssue(level.id, code, "[$tag] $message") }

        val n = level.gridSize
        if (variant.grid.size != n || variant.grid.any { it.length != n }) {
            add("GRID_SHAPE", "la grille doit être ${n}×$n")
            return issues
        }
        if (variant.grid.any { row -> row.any { it !in 'A'..'Z' } }) add("GRID_CHARS", "la grille ne doit contenir que A-Z")
        if (variant.words.isEmpty()) add("NO_WORDS", "aucun mot cible")

        val lettersSeen = mutableSetOf<String>()
        variant.words.forEachIndexed { i, w ->
            if (w.letters != TextNormalizer.toGridLetters(w.display)) add("WORD_LETTERS", "mot #$i « ${w.display} » : letters « ${w.letters} » incohérent")
            if (w.length < 2) add("WORD_LENGTH", "mot #$i trop court")
            if (!lettersSeen.add(w.letters)) add("WORD_DUPLICATE", "mot en double « ${w.letters} »")
            if (w.direction !in level.difficulty.allowedDirections) add("DIRECTION", "mot « ${w.letters} » : direction ${w.direction} interdite au palier ${level.difficulty.tier}")
            val cells = w.cells
            if (cells.any { !variant.inBounds(it) }) {
                add("OUT_OF_BOUNDS", "mot « ${w.letters} » sort de la grille")
            } else if (cells.indices.any { variant.letterAt(cells[it]) != w.letters[it] }) {
                add("GRID_MISMATCH", "mot « ${w.letters} » ne correspond pas aux lettres de la grille")
            }
        }
        // Un mot ne doit pas contenir un autre mot (sens normal ou inverse) : sinon ambiguïté.
        for (a in variant.words) for (b in variant.words) {
            if (a !== b && (a.letters.contains(b.letters) || a.letters.contains(b.letters.reversed()))) {
                add("WORD_CONTAINED", "« ${b.letters} » est contenu dans « ${a.letters} »")
            }
        }
        if (issues.any { it.code in setOf("OUT_OF_BOUNDS", "GRID_MISMATCH", "GRID_CHARS") }) return issues

        // Unicité : chaque mot n'apparaît qu'une fois dans la grille, dans les 8 directions.
        variant.words.forEach { w ->
            val found = occurrences(variant, w.letters)
            if (found.size != 1) add("AMBIGUOUS_WORD", "« ${w.letters} » apparaît ${found.size} fois dans la grille")
        }
        // Résolubilité : on rejoue la solution avec le vrai moteur.
        if (issues.isEmpty()) {
            var state = GameEngine.newGame(level, variant.language)
            variant.words.forEachIndexed { i, w ->
                val result = GameEngine.select(state, w.cells)
                if (result !is SelectionResult.Found || result.wordIndex != i) add("UNSOLVABLE", "le moteur n'accepte pas le mot « ${w.letters} »")
                state = result.state
            }
            if (!state.isSolved) add("UNSOLVABLE", "niveau non résolu après avoir joué tous les mots")
        }
        return issues
    }

    /** Ensembles de cases où [letters] apparaît (dédoublonnés : un palindrome ne compte qu'une fois). */
    fun occurrences(variant: LevelVariant, letters: String): Set<Set<Cell>> {
        val result = mutableSetOf<Set<Cell>>()
        val n = variant.grid.size
        for (r in 0 until n) for (c in 0 until n) for (d in Direction.entries) {
            val endR = r + d.dRow * (letters.length - 1)
            val endC = c + d.dCol * (letters.length - 1)
            if (endR !in 0 until n || endC !in 0 until n) continue
            val cells = List(letters.length) { Cell(r + d.dRow * it, c + d.dCol * it) }
            if (cells.indices.all { variant.letterAt(cells[it]) == letters[it] }) result += cells.toSet()
        }
        return result
    }
}
