package com.motsmeles.game.data

import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.progress.GameSnapshot
import com.motsmeles.game.domain.progress.LevelResult
import com.motsmeles.game.domain.progress.ProgressState
import com.motsmeles.game.domain.settings.AppSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistenceJsonTest {
    private val rich = ProgressState(
        initialHintsGranted = true,
        hintBalance = 3,
        completed = mapOf("c1-l01" to LevelResult(3, 91_000), "c1-l02" to LevelResult(1, 700_000)),
        claimedGiftChapters = setOf(1),
        grantedRewardIds = listOf("a", "b"),
        lastPraise = Praise.AMAZING,
        perfectStreak = 2,
        inProgress = mapOf("c1-l03:fr" to GameSnapshot(setOf(0, 2), listOf(1), 4, 33_000)),
    )

    @Test fun `progress survives a round trip`() {
        assertEquals(rich, ProgressJson.decode(ProgressJson.encode(rich)))
    }

    @Test fun `empty default state round trips`() {
        assertEquals(ProgressState(), ProgressJson.decode(ProgressJson.encode(ProgressState())))
    }

    @Test fun `garbage and missing schema are rejected`() {
        assertTrue(runCatching { ProgressJson.decode("not json") }.exceptionOrNull() is PersistedFormatException)
        assertTrue(runCatching { ProgressJson.decode("{}") }.exceptionOrNull() is PersistedFormatException)
        assertTrue(runCatching { ProgressJson.decode("") }.exceptionOrNull() is PersistedFormatException)
    }

    @Test fun `a save from a newer version is refused instead of overwritten`() {
        val newer = JSONObject(ProgressJson.encode(rich)).put("schemaVersion", 99).toString()
        assertTrue(runCatching { ProgressJson.decode(newer) }.exceptionOrNull() is PersistedFormatException)
    }

    @Test fun `older saves are migrated step by step`() {
        val old = """{"schemaVersion":1,"hintBalance":4,"initialHintsGranted":true}"""
        val migrations = mapOf<Int, (JSONObject) -> JSONObject>(1 to { it.put("hintBalance", it.getInt("hintBalance") + 10) })
        val state = ProgressJson.decode(old, currentVersion = 2, migrations = migrations)
        assertEquals(14, state.hintBalance)
        assertTrue(runCatching { ProgressJson.decode(old, currentVersion = 3, migrations = migrations) }.isFailure)
    }

    @Test fun `missing optional fields fall back to defaults and values are clamped`() {
        val s = ProgressJson.decode("""{"schemaVersion":1,"hintBalance":-5,"completed":{"x":{"stars":9,"elapsedMillis":-1}}}""")
        assertEquals(0, s.hintBalance)
        assertEquals(LevelResult(3, 0), s.completed["x"])
        assertNull(s.lastPraise)
    }

    @Test fun `a save written before the interstitial removal still loads`() {
        val legacy = """{"schemaVersion":1,"hintBalance":2,"eligibleLevelStarts":7,"lastCountedStart":"c1-l03"}"""
        assertEquals(2, ProgressJson.decode(legacy).hintBalance)
    }

    @Test fun `settings saved before the music option default to music on`() {
        val legacy = """{"schemaVersion":1,"sound":false,"haptics":true,"reduceMotion":false,"largeText":false}"""
        val s = SettingsJson.decode(legacy)
        assertEquals(false, s.soundEnabled)
        assertEquals(true, s.musicEnabled)
    }

    @Test fun `settings round trip including language null`() {
        val custom = AppSettings(Language.FR, soundEnabled = false, musicEnabled = false, hapticsEnabled = false, reduceMotion = true, largeText = true)
        assertEquals(custom, SettingsJson.decode(SettingsJson.encode(custom)))
        assertEquals(AppSettings.DEFAULT, SettingsJson.decode(SettingsJson.encode(AppSettings.DEFAULT)))
        assertNull(SettingsJson.decode(SettingsJson.encode(AppSettings.DEFAULT)).language)
    }

    @Test fun `bad settings are rejected`() {
        assertTrue(runCatching { SettingsJson.decode("{}") }.exceptionOrNull() is PersistedFormatException)
        assertTrue(runCatching { SettingsJson.decode("[") }.exceptionOrNull() is PersistedFormatException)
    }
}
