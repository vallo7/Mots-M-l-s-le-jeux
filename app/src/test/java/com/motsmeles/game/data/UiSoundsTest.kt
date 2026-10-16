package com.motsmeles.game.data

import com.motsmeles.game.domain.ports.Sound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

/** Vérifie que chaque son d'interface déclaré existe réellement, est un WAV valide et reste modéré. */
class UiSoundsTest {
    private val assetsDir: File = listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }

    @Test fun `every ui sound has an existing wav file`() {
        Sound.entries.forEach { sound ->
            val f = File(assetsDir, UiSounds.assetPath(sound))
            assertTrue("${f.path} manquant", f.isFile && f.length() > 1000)
            RandomAccessFile(f, "r").use { raf ->
                val header = ByteArray(12).also { raf.readFully(it) }
                assertEquals("RIFF", String(header, 0, 4))
                assertEquals("WAVE", String(header, 8, 4))
            }
        }
    }

    @Test fun `volumes are moderate and the file names are unique`() {
        val names = Sound.entries.map { UiSounds.fileFor(it) }
        assertEquals(names.size, names.toSet().size)
        Sound.entries.forEach { assertTrue(UiSounds.volumeFor(it) in 0.3f..0.95f) }
    }

    @Test fun `ui sounds stay short enough to be preloaded in memory`() {
        Sound.entries.forEach {
            val bytes = File(assetsDir, UiSounds.assetPath(it)).length()
            assertTrue("${it.name} trop lourd", bytes < 200_000)
        }
    }
}
