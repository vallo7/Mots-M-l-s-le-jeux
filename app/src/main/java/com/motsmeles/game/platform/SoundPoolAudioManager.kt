package com.motsmeles.game.platform

import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.motsmeles.game.BuildConfig
import com.motsmeles.game.data.PraiseMap
import com.motsmeles.game.data.UiSounds
import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.ports.AudioManager
import com.motsmeles.game.domain.ports.Sound
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Sons courts préchargés dans un [SoundPool] (faible latence, flux limités pour éviter la saturation).
 * Un fichier absent n'est pas une erreur : le praise retombe sur le son de réussite générique.
 */
class SoundPoolAudioManager(
    private val assets: AssetManager,
    private val praiseMapProvider: () -> PraiseMap,
) : AudioManager {
    private class Item(val path: String, val volume: Float)

    private var pool: SoundPool? = null
    private val sampleByPath = HashMap<String, Int>()
    private val volumeByPath = HashMap<String, Float>()
    @Volatile private var soundEnabled = true
    private var praiseMap: PraiseMap = PraiseMap.empty()

    val isLoaded: Boolean get() = pool != null

    /** Précharge tous les sons ; [onProgress] reçoit 0..1. Idempotent. À appeler hors du thread principal. */
    suspend fun preload(onProgress: (Float) -> Unit = {}) {
        if (pool != null) return
        praiseMap = runCatching(praiseMapProvider).getOrDefault(PraiseMap.empty())
        val items = LinkedHashMap<String, Item>()
        Sound.entries.forEach { items[UiSounds.assetPath(it)] = Item(UiSounds.assetPath(it), UiSounds.volumeFor(it)) }
        Language.entries.forEach { lang ->
            Praise.entries.forEach { p ->
                praiseMap.resolve(p, lang)?.let { items[it.assetPath] = Item(it.assetPath, it.volume) }
            }
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sp = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attributes).build()
        val loadable = items.values.filter { assetExists(it.path) }
        if (BuildConfig.DEBUG) {
            items.values.filter { it !in loadable }.forEach { Log.w(TAG, "Son manquant : ${it.path}") }
        }
        val done = AtomicInteger(0)
        suspendCoroutine { continuation ->
            val resumed = AtomicBoolean(false)
            val finish = { if (resumed.compareAndSet(false, true)) continuation.resume(Unit) }
            if (loadable.isEmpty()) {
                finish()
                return@suspendCoroutine
            }
            sp.setOnLoadCompleteListener { _, _, _ ->
                val n = done.incrementAndGet()
                onProgress(n.toFloat() / loadable.size)
                if (n >= loadable.size) finish()
            }
            loadable.forEach { item ->
                try {
                    assets.openFd(item.path).use { afd -> sampleByPath[item.path] = sp.load(afd, 1) }
                    volumeByPath[item.path] = item.volume
                } catch (e: IOException) {
                    done.incrementAndGet()
                }
            }
            // Garde-fou : ne jamais bloquer le démarrage si un chargement n'aboutit pas.
            Handler(Looper.getMainLooper()).postDelayed({ finish() }, LOAD_TIMEOUT_MS)
        }
        pool = sp
    }

    private fun assetExists(path: String): Boolean = try {
        assets.openFd(path).close()
        true
    } catch (e: IOException) {
        false
    }

    override fun play(sound: Sound) {
        if (!soundEnabled) return
        playPath(UiSounds.assetPath(sound))
    }

    override fun playPraise(praise: Praise, language: Language) {
        if (!soundEnabled) return
        val voice = praiseMap.resolve(praise, language)
        if (voice != null && sampleByPath.containsKey(voice.assetPath)) playPath(voice.assetPath)
        else play(Sound.SUCCESS_GENERIC)
    }

    private fun playPath(path: String) {
        val id = sampleByPath[path] ?: return
        val volume = (volumeByPath[path] ?: 1f).coerceIn(0f, 1f)
        pool?.play(id, volume, volume, 1, 0, 1f)
    }

    override fun setSoundEffectsEnabled(enabled: Boolean) {
        soundEnabled = enabled
        if (!enabled) pool?.autoPause()
    }

    /** Pas de musique dans cette version : aucun fichier n'est livré, l'option n'est donc pas proposée. */
    override fun setMusicEnabled(enabled: Boolean) = Unit

    fun onAppStopped() { pool?.autoPause() }
    fun onAppStarted() { pool?.autoResume() }

    override fun release() {
        pool?.release()
        pool = null
        sampleByPath.clear()
        volumeByPath.clear()
    }

    private companion object {
        const val TAG = "Audio"
        const val LOAD_TIMEOUT_MS = 4_000L
    }
}
