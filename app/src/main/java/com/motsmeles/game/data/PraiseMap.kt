package com.motsmeles.game.data

import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.model.Language
import org.json.JSONObject

data class PraiseVoice(val assetPath: String, val volume: Float)

/**
 * Lecture de assets/audio/praise/praise_map.json. [resolve] renvoie null quand aucun fichier n'est
 * déclaré pour la langue : l'appelant joue alors le son de réussite générique (D-006).
 */
class PraiseMap private constructor(private val voices: Map<Language, Map<Praise, PraiseVoice>>) {
    fun resolve(praise: Praise, language: Language): PraiseVoice? = voices[language]?.get(praise)

    /** Chemins d'assets attendus, utiles au préchargement et aux vérifications. */
    fun allAssetPaths(): Set<String> = voices.values.flatMap { it.values }.map { it.assetPath }.toSet()

    companion object {
        const val DIRECTORY = "audio/praise"
        const val FILE = "$DIRECTORY/praise_map.json"

        /** Table vide : toute voix manquante retombe sur le son de réussite générique. */
        fun empty(): PraiseMap = PraiseMap(emptyMap())

        fun parse(json: String): PraiseMap {
            val root = JSONObject(json)
            val voices = root.getJSONObject("voices")
            val result = Language.entries.associateWith { language ->
                val block = voices.optJSONObject(language.code)
                val entries = mutableMapOf<Praise, PraiseVoice>()
                val names = block?.names()
                if (block != null && names != null) {
                    for (i in 0 until names.length()) {
                        val key = names.getString(i)
                        val praise = Praise.fromKey(key) ?: continue
                        val entry = block.getJSONObject(key)
                        entries[praise] = PraiseVoice(
                            assetPath = "$DIRECTORY/${entry.getString("file")}",
                            volume = entry.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f),
                        )
                    }
                }
                entries.toMap()
            }
            return PraiseMap(result)
        }
    }
}
