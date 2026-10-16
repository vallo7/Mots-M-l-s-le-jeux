package com.motsmeles.game.data

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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Lecture des fichiers JSON du dossier data/levels (schéma versionné), via org.json uniquement. */
object LevelJsonParser {
    const val SUPPORTED_SCHEMA = 1

    class LevelDataException(message: String, cause: Throwable? = null) : Exception(message, cause)

    /** @param readFile lit un fichier relatif à data/levels (ex. « chapter-01.json »). */
    fun parseCatalog(readFile: (String) -> String): LevelCatalog {
        try {
            val index = JSONObject(readFile("index.json"))
            checkSchema(index, "index.json")
            val files = index.getJSONArray("chapters")
            val chapters = (0 until files.length()).map { parseChapter(JSONObject(readFile(files.getString(it))), files.getString(it)) }
            return LevelCatalog(chapters)
        } catch (e: JSONException) {
            throw LevelDataException("JSON de niveaux invalide : ${e.message}", e)
        }
    }

    private fun checkSchema(obj: JSONObject, name: String) {
        val v = obj.optInt("schemaVersion", -1)
        if (v != SUPPORTED_SCHEMA) throw LevelDataException("$name : schemaVersion $v non supporté (attendu $SUPPORTED_SCHEMA)")
    }

    private fun parseChapter(obj: JSONObject, name: String): Chapter {
        checkSchema(obj, name)
        val index = obj.getInt("index")
        val levels = obj.getJSONArray("levels")
        return Chapter(
            id = obj.getString("id"),
            index = index,
            theme = text(obj.getJSONObject("theme")),
            paletteKey = obj.getString("paletteKey"),
            levels = (0 until levels.length()).map { parseLevel(levels.getJSONObject(it), index) },
        )
    }

    private fun parseLevel(obj: JSONObject, chapterIndex: Int): Level {
        val diff = obj.getJSONObject("difficulty")
        val variants = obj.getJSONObject("variants")
        return Level(
            id = obj.getString("id"),
            chapterIndex = chapterIndex,
            number = obj.getInt("number"),
            theme = text(obj.getJSONObject("theme")),
            visualStyle = VisualStyle.valueOf(obj.getString("visualStyle")),
            gridSize = obj.getInt("gridSize"),
            difficulty = DifficultyProfile(diff.getInt("tier"), diff.getDouble("ambiguity"), diff.getInt("score")),
            hints = HintConfig(obj.getJSONObject("hints").getInt("maxPerLevel")),
            variants = Language.entries.associateWith { lang -> parseVariant(lang, variants.getJSONObject(lang.code)) },
        )
    }

    private fun parseVariant(language: Language, obj: JSONObject): LevelVariant {
        val grid = obj.getJSONArray("grid").strings()
        val words = obj.getJSONArray("words")
        return LevelVariant(
            language = language,
            grid = grid,
            words = (0 until words.length()).map {
                val w = words.getJSONObject(it)
                TargetWord(
                    display = w.getString("display"),
                    letters = w.getString("letters"),
                    start = Cell(w.getInt("row"), w.getInt("col")),
                    direction = Direction.valueOf(w.getString("direction")),
                )
            },
        )
    }

    private fun text(obj: JSONObject) = LocalizedText(fr = obj.getString("fr"), en = obj.getString("en"))
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
}
