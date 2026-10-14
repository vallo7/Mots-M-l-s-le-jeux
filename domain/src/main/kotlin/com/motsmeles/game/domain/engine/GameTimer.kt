package com.motsmeles.game.domain.engine

/** Horloge monotone injectée (SystemClock.elapsedRealtime côté Android), jamais l'heure civile. */
fun interface MonotonicClock {
    fun nowMillis(): Long
}

/**
 * Chronomètre de partie. Démarre à la première interaction avec la grille ([startIfNeeded]),
 * se suspend en arrière-plan ([pause]) et reprend ([resume]) uniquement si la partie a démarré.
 */
class GameTimer(private val clock: MonotonicClock) {
    private var accumulatedMillis = 0L
    private var runningSince: Long? = null
    private var started = false
    private var stopped = false

    val hasStarted: Boolean get() = started
    val isRunning: Boolean get() = runningSince != null

    fun startIfNeeded() {
        if (started || stopped) return
        started = true
        runningSince = clock.nowMillis()
    }

    fun pause() {
        val since = runningSince ?: return
        accumulatedMillis += (clock.nowMillis() - since).coerceAtLeast(0)
        runningSince = null
    }

    fun resume() {
        if (!started || stopped || runningSince != null) return
        runningSince = clock.nowMillis()
    }

    /** Fige le temps final (victoire). Idempotent. */
    fun stop(): Long {
        pause()
        stopped = true
        return accumulatedMillis
    }

    /** Restaure un temps déjà écoulé (reprise d'une partie) : le chronomètre reste en pause jusqu'à [resume]. */
    fun restoreElapsed(millis: Long) {
        accumulatedMillis = millis.coerceAtLeast(0)
        runningSince = null
        started = true
        stopped = false
    }

    fun reset() {
        accumulatedMillis = 0L
        runningSince = null
        started = false
        stopped = false
    }

    fun elapsedMillis(): Long {
        val since = runningSince ?: return accumulatedMillis
        return accumulatedMillis + (clock.nowMillis() - since).coerceAtLeast(0)
    }
}
