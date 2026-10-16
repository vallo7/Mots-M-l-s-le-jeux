package com.motsmeles.game.data

import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.progress.GameSnapshot
import com.motsmeles.game.domain.progress.LevelResult
import com.motsmeles.game.domain.progress.ProgressState
import com.motsmeles.game.domain.settings.AppSettings
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class PersistedFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Sérialisation versionnée de la progression. Une version plus ancienne passe par [MIGRATIONS]. */
object ProgressJson {
    /** Migrations : clé = version source, valeur = transformation vers la version suivante. Vide : v1 est la première. */
    val MIGRATIONS: Map<Int, (JSONObject) -> JSONObject> = emptyMap()

    fun encode(state: ProgressState): String {
        val completed = JSONObject()
        state.completed.forEach { (id, r) -> completed.put(id, JSONObject().put("stars", r.stars).put("elapsedMillis", r.elapsedMillis)) }
        val progress = JSONObject()
        state.inProgress.forEach { (key, s) ->
            progress.put(
                key,
                JSONObject()
                    .put("found", JSONArray(s.foundWordIndices.sorted()))
                    .put("hinted", JSONArray(s.hintedWordIndices))
                    .put("wrong", s.wrongSelections)
                    .put("elapsedMillis", s.elapsedMillis),
            )
        }
        val json = JSONObject()
            .put("schemaVersion", ProgressState.CURRENT_SCHEMA)
            .put("initialHintsGranted", state.initialHintsGranted)
            .put("hintBalance", state.hintBalance)
            .put("completed", completed)
            .put("claimedGiftChapters", JSONArray(state.claimedGiftChapters.sorted()))
            .put("grantedRewardIds", JSONArray(state.grantedRewardIds))
            .put("perfectStreak", state.perfectStreak)
            .put("inProgress", progress)
        state.lastPraise?.let { json.put("lastPraise", it.key) }
        return json.toString()
    }

    fun decode(
        text: String,
        currentVersion: Int = ProgressState.CURRENT_SCHEMA,
        migrations: Map<Int, (JSONObject) -> JSONObject> = MIGRATIONS,
    ): ProgressState {
        try {
            var json = JSONObject(text)
            if (!json.has("schemaVersion")) throw PersistedFormatException("schemaVersion absent")
            var version = json.getInt("schemaVersion")
            if (version > currentVersion) throw PersistedFormatException("sauvegarde d'une version plus récente ($version)")
            while (version < currentVersion) {
                val step = migrations[version] ?: throw PersistedFormatException("migration $version -> ${version + 1} absente")
                json = step(json).put("schemaVersion", version + 1)
                version++
            }
            return parse(json)
        } catch (e: JSONException) {
            throw PersistedFormatException("JSON de progression invalide", e)
        }
    }

    private fun parse(json: JSONObject): ProgressState {
        val completed = json.optJSONObject("completed")?.let { obj ->
            obj.names()?.let { names ->
                (0 until names.length()).associate { i ->
                    val id = names.getString(i)
                    val r = obj.getJSONObject(id)
                    id to LevelResult(r.getInt("stars").coerceIn(1, 3), r.getLong("elapsedMillis").coerceAtLeast(0))
                }
            }
        } ?: emptyMap()
        val inProgress = json.optJSONObject("inProgress")?.let { obj ->
            obj.names()?.let { names ->
                (0 until names.length()).associate { i ->
                    val key = names.getString(i)
                    val s = obj.getJSONObject(key)
                    key to GameSnapshot(
                        foundWordIndices = s.getJSONArray("found").ints().toSet(),
                        hintedWordIndices = s.getJSONArray("hinted").ints(),
                        wrongSelections = s.optInt("wrong", 0),
                        elapsedMillis = s.optLong("elapsedMillis", 0),
                    )
                }
            }
        } ?: emptyMap()
        return ProgressState(
            schemaVersion = ProgressState.CURRENT_SCHEMA,
            initialHintsGranted = json.optBoolean("initialHintsGranted", false),
            hintBalance = json.optInt("hintBalance", 0).coerceAtLeast(0),
            completed = completed,
            claimedGiftChapters = json.optJSONArray("claimedGiftChapters")?.ints()?.toSet() ?: emptySet(),
            grantedRewardIds = json.optJSONArray("grantedRewardIds")?.strings() ?: emptyList(),
            lastPraise = Praise.fromKey(json.optString("lastPraise", "")),
            perfectStreak = json.optInt("perfectStreak", 0),
            inProgress = inProgress,
        )
    }

    private fun JSONArray.ints(): List<Int> = (0 until length()).map { getInt(it) }
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
}

/** Sérialisation des réglages. */
object SettingsJson {
    const val SCHEMA = 1

    fun encode(s: AppSettings): String {
        val json = JSONObject()
            .put("schemaVersion", SCHEMA)
            .put("sound", s.soundEnabled)
            .put("music", s.musicEnabled)
            .put("haptics", s.hapticsEnabled)
            .put("reduceMotion", s.reduceMotion)
            .put("largeText", s.largeText)
        s.language?.let { json.put("language", it.code) }
        return json.toString()
    }

    fun decode(text: String): AppSettings {
        try {
            val json = JSONObject(text)
            val version = json.optInt("schemaVersion", -1)
            if (version != SCHEMA) throw PersistedFormatException("réglages : schemaVersion $version non supporté")
            val code = json.optString("language", "")
            return AppSettings(
                language = if (code.isEmpty()) null else Language.fromCode(code),
                soundEnabled = json.optBoolean("sound", true),
                musicEnabled = json.optBoolean("music", true),
                hapticsEnabled = json.optBoolean("haptics", true),
                reduceMotion = json.optBoolean("reduceMotion", false),
                largeText = json.optBoolean("largeText", false),
            )
        } catch (e: JSONException) {
            throw PersistedFormatException("JSON de réglages invalide", e)
        }
    }
}
