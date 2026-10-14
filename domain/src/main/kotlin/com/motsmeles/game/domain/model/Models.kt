package com.motsmeles.game.domain.model

/** Langues supportées. Le contenu (grilles, mots, thèmes) existe dans chaque langue. */
enum class Language(val code: String) {
    FR("fr"),
    EN("en");

    companion object {
        fun fromCode(code: String?): Language =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}

data class Cell(val row: Int, val col: Int)

/** Les 8 directions de lecture possibles dans une grille de mots mêlés. */
enum class Direction(val dRow: Int, val dCol: Int) {
    E(0, 1), S(1, 0), SE(1, 1), W(0, -1), N(-1, 0), SW(1, -1), NW(-1, -1), NE(-1, 1);

    companion object {
        /** Direction du pas unitaire de [from] vers [to], ou null si ce n'est pas un pas voisin. */
        fun stepBetween(from: Cell, to: Cell): Direction? {
            val dr = to.row - from.row
            val dc = to.col - from.col
            return entries.firstOrNull { it.dRow == dr && it.dCol == dc }
        }

        /** Directions autorisées pour *placer* les mots selon le palier de difficulté (1..4). */
        fun forTier(tier: Int): Set<Direction> = when {
            tier <= 1 -> setOf(E, S)
            tier == 2 -> setOf(E, S, SE)
            tier == 3 -> setOf(E, S, SE, W, N)
            else -> entries.toSet()
        }
    }
}

data class LocalizedText(val fr: String, val en: String) {
    operator fun get(language: Language): String = if (language == Language.FR) fr else en
}

/**
 * Mot à trouver. [display] est le texte affiché (accents autorisés), [letters] la suite de
 * lettres A-Z présente dans la grille, [start] la première case et [direction] le sens de pose.
 */
data class TargetWord(
    val display: String,
    val letters: String,
    val start: Cell,
    val direction: Direction,
) {
    val length: Int get() = letters.length
    val cells: List<Cell>
        get() = List(letters.length) { i ->
            Cell(start.row + direction.dRow * i, start.col + direction.dCol * i)
        }
    val end: Cell get() = cells.last()
}

data class LevelVariant(
    val language: Language,
    val grid: List<String>,
    val words: List<TargetWord>,
) {
    fun inBounds(cell: Cell): Boolean =
        cell.row in grid.indices && cell.col in 0 until grid[cell.row].length

    fun letterAt(cell: Cell): Char = grid[cell.row][cell.col]
}

/** Profil de difficulté éditorial. Le score est vérifié par [com.motsmeles.game.domain.content.DifficultyScore]. */
data class DifficultyProfile(val tier: Int, val ambiguity: Double, val score: Int) {
    val allowedDirections: Set<Direction> get() = Direction.forTier(tier)
}

data class HintConfig(val maxPerLevel: Int)

/** 4 styles visuels : trois varient selon le chapitre, [FINAL] est réservé au niveau 10. */
enum class VisualStyle {
    STYLE_1, STYLE_2, STYLE_3, FINAL;

    companion object {
        fun forLevel(chapterIndex: Int, number: Int): VisualStyle {
            if (number == Level.FINAL_NUMBER) return FINAL
            val band = (number - 1) / 3
            return when ((band + chapterIndex - 1) % 3) {
                0 -> STYLE_1
                1 -> STYLE_2
                else -> STYLE_3
            }
        }
    }
}

data class Level(
    val id: String,
    val chapterIndex: Int,
    val number: Int,
    val theme: LocalizedText,
    val visualStyle: VisualStyle,
    val gridSize: Int,
    val difficulty: DifficultyProfile,
    val hints: HintConfig,
    val variants: Map<Language, LevelVariant>,
) {
    val isFinalChallenge: Boolean get() = number == FINAL_NUMBER

    fun variant(language: Language): LevelVariant = variants.getValue(language)

    companion object {
        const val LEVELS_PER_CHAPTER = 10
        const val FINAL_NUMBER = 10
    }
}

data class Chapter(
    val id: String,
    val index: Int,
    val theme: LocalizedText,
    val paletteKey: String,
    val levels: List<Level>,
)

data class LevelCatalog(val chapters: List<Chapter>) {
    val levels: List<Level> = chapters.sortedBy { it.index }.flatMap { chapter -> chapter.levels.sortedBy { it.number } }

    fun levelById(id: String): Level? = levels.firstOrNull { it.id == id }
    fun chapterOf(level: Level): Chapter? = chapters.firstOrNull { it.index == level.chapterIndex }
    fun levelAfter(id: String): Level? {
        val i = levels.indexOfFirst { it.id == id }
        return if (i in 0 until levels.lastIndex) levels[i + 1] else null
    }
}
