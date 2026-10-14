package com.motsmeles.game.data

import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.model.Language
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Vérifie que les audios de praise fournis sont réellement déclarés et présents dans les assets. */
class PraiseMapTest {
    private val assetsDir: File = listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }
    private val mapFile = File(assetsDir, PraiseMap.FILE)
    private val map = PraiseMap.parse(mapFile.readText(Charsets.UTF_8))

    @Test fun `ladder in the map matches the Praise enum order`() {
        val ladder = JSONObject(mapFile.readText()).getJSONArray("ladder")
        assertEquals(Praise.entries.map { it.key }, (0 until ladder.length()).map { ladder.getString(it) })
    }

    @Test fun `every praise resolves to an existing non-empty file in both languages`() {
        Praise.entries.forEach { praise ->
            Language.entries.forEach { language ->
                val voice = map.resolve(praise, language)
                assertNotNull("${praise.key} sans voix ${language.code}", voice)
                val file = File(assetsDir, voice!!.assetPath)
                assertTrue("${file.path} manquant ou vide", file.isFile && file.length() > 0)
                assertTrue(voice.volume in 0.1f..1f)
            }
        }
    }

    @Test fun `file names follow the stable lowercase mp3 convention`() {
        Praise.entries.forEach { assertEquals("${it.key}.mp3", map.resolve(it, Language.EN)!!.assetPath.substringAfterLast('/')) }
    }

    @Test fun `praise is identical in french and english`() {
        Praise.entries.forEach { praise ->
            assertEquals(map.resolve(praise, Language.EN), map.resolve(praise, Language.FR))
        }
    }

    @Test fun `an unknown praise language entry falls back to no voice`() {
        val empty = PraiseMap.parse("""{"voices":{"en":{}}}""")
        Praise.entries.forEach { assertNull(empty.resolve(it, Language.FR)) }
    }

    @Test fun `every declared file exists`() {
        map.allAssetPaths().forEach { assertTrue("$it manquant", File(assetsDir, it).isFile) }
    }
}
