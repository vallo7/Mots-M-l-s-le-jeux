package com.motsmeles.game.presentation.startup

import com.motsmeles.game.domain.content.ValidationIssue
import com.motsmeles.game.domain.engine.MonotonicClock
import com.motsmeles.game.domain.model.LevelCatalog
import com.motsmeles.game.domain.ports.LevelRepository

class StartupException(message: String, val issues: List<ValidationIssue> = emptyList(), cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Séquence de démarrage à froid. Chaque étape est réellement exécutée : lecture des niveaux, validation
 * des grilles, lecture de la progression et des réglages, préchargement des sons. La durée minimale de
 * 2 s est appliquée une seule fois, après la dernière étape, jamais lors des navigations suivantes (D-010).
 */
class StartupSequence(
    private val repository: LevelRepository,
    private val clock: MonotonicClock,
    private val validate: suspend (LevelCatalog) -> List<ValidationIssue>,
    private val loadProfile: suspend () -> Unit,
    private val preloadAudio: suspend () -> Unit,
    private val waitFor: suspend (Long) -> Unit,
    private val minimumMillis: Long = MIN_DURATION_MS,
) {
    enum class Step { READING_LEVELS, CHECKING_GRIDS, LOADING_PROGRESS, LOADING_AUDIO, READY }

    suspend fun run(onProgress: (progress: Float, step: Step) -> Unit): LevelCatalog {
        val startedAt = clock.nowMillis()
        onProgress(0f, Step.READING_LEVELS)
        val catalog = try {
            repository.loadCatalog()
        } catch (e: Exception) {
            throw StartupException("Lecture des niveaux impossible : ${e.message}", cause = e)
        }
        onProgress(0.25f, Step.CHECKING_GRIDS)
        val issues = validate(catalog)
        if (issues.isNotEmpty()) throw StartupException("${issues.size} problème(s) dans les niveaux", issues)
        onProgress(0.5f, Step.LOADING_PROGRESS)
        try {
            loadProfile()
        } catch (e: Exception) {
            throw StartupException("Lecture de la progression impossible : ${e.message}", cause = e)
        }
        onProgress(0.75f, Step.LOADING_AUDIO)
        // Un son qui ne charge pas ne doit jamais empêcher de jouer.
        try {
            preloadAudio()
        } catch (e: Exception) {
            // jeu jouable sans son
        }
        onProgress(1f, Step.READY)
        val remaining = minimumMillis - (clock.nowMillis() - startedAt)
        if (remaining > 0) waitFor(remaining)
        return catalog
    }

    companion object {
        const val MIN_DURATION_MS = 2_000L
    }
}
