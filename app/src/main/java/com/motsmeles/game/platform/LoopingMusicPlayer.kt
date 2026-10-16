package com.motsmeles.game.platform

import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.motsmeles.game.BuildConfig
import java.io.IOException

/**
 * Musique d'ambiance en boucle sans coupure : deux [MediaPlayer] chaînés (`setNextMediaPlayer`) se relaient,
 * ce qui supprime le petit silence que laisse `isLooping` au point de bouclage.
 *
 * Aucun fichier = aucune musique, sans erreur : le jeu reste jouable. Fichier attendu : [ASSET_PATH]
 * (voir docs/MUSIC.md pour changer de morceau). À utiliser depuis le thread principal.
 */
class LoopingMusicPlayer(private val assets: AssetManager) {
    private var current: MediaPlayer? = null
    private var next: MediaPlayer? = null
    private var enabled = true
    private var paused = false
    private var broken = false

    val hasTrack: Boolean
        get() = try {
            assets.openFd(ASSET_PATH).close()
            true
        } catch (e: IOException) {
            false
        }

    fun setEnabled(value: Boolean) {
        enabled = value
        refresh()
    }

    /** Appli en arrière-plan : la musique se met en pause (jamais de son hors de l'écran). */
    fun onBackground() {
        paused = true
        current?.let { runCatching { if (it.isPlaying) it.pause() } }
    }

    fun onForeground() {
        paused = false
        refresh()
    }

    fun release() {
        runCatching { current?.release() }
        runCatching { next?.release() }
        current = null
        next = null
    }

    private fun refresh() {
        when {
            !enabled || broken -> release()
            paused -> Unit
            current == null -> startChain()
            else -> runCatching { if (current?.isPlaying == false) current?.start() }
        }
    }

    private fun startChain() {
        try {
            val first = build() ?: return
            val second = build()
            current = first
            next = second
            chain(first, second)
            first.start()
        } catch (e: Exception) {
            fail(e)
        }
    }

    private fun build(): MediaPlayer? = try {
        val player = MediaPlayer()
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        assets.openFd(ASSET_PATH).use { afd -> player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length) }
        player.setVolume(VOLUME, VOLUME)
        player.prepare()
        player
    } catch (e: IOException) {
        if (BuildConfig.DEBUG) Log.w(TAG, "Musique absente ou illisible : $ASSET_PATH")
        broken = true
        null
    }

    private fun chain(player: MediaPlayer, follower: MediaPlayer?) {
        follower?.let { player.setNextMediaPlayer(it) }
        player.setOnCompletionListener { finished ->
            try {
                finished.release()
                current = next
                next = build()
                val now = current ?: return@setOnCompletionListener
                chain(now, next)
                if (paused || !enabled) runCatching { now.pause() }
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    private fun fail(e: Exception) {
        if (BuildConfig.DEBUG) Log.w(TAG, "Musique désactivée : ${e.message}")
        broken = true
        release()
    }

    companion object {
        const val ASSET_PATH = "audio/music/bgm_main.ogg"
        /** Volume bas : la musique reste un fond, les effets et praises restent audibles. */
        private const val VOLUME = 0.32f
        private const val TAG = "Music"
    }
}
