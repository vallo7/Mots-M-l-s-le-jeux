package com.motsmeles.game.presentation.game

import androidx.annotation.AnyRes
import com.motsmeles.game.R
import com.motsmeles.game.domain.engine.GameSession
import com.motsmeles.game.domain.engine.MonotonicClock
import com.motsmeles.game.domain.engine.SessionEvent
import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.Level
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.ports.UnavailableReason
import com.motsmeles.game.domain.usecases.GameUseCases
import com.motsmeles.game.domain.usecases.HintUseResult
import com.motsmeles.game.domain.usecases.RewardOutcome
import com.motsmeles.game.domain.usecases.VictoryResult
import com.motsmeles.game.presentation.feedback.FeedbackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GameDialog { None, ConfirmQuit, RewardedOffer }

data class GameUiState(
    val level: Level,
    val language: Language,
    val ready: Boolean = false,
    val foundWordIndices: Set<Int> = emptySet(),
    val hintCells: List<Cell> = emptyList(),
    val selection: List<Cell> = emptyList(),
    val elapsedMillis: Long = 0,
    val hintBalance: Int = 0,
    val hintsLeftOnLevel: Int = 0,
    val solved: Boolean = false,
    val dialog: GameDialog = GameDialog.None,
    /** Incrémenté à chaque mauvaise sélection : déclenche la secousse de la grille. */
    val wrongToken: Int = 0,
    /** Dernier mot trouvé + compteur : déclenche l'effet « mot trouvé » (capsule verrouillée, vague de lettres, étincelles). */
    val foundToken: Int = 0,
    val lastFoundIndex: Int = -1,
    /** Incrémenté à chaque redémarrage : déclenche l'animation de mélange des lettres (aucune fenêtre de confirmation). */
    val restartToken: Int = 0,
    /** Incrémenté à chaque indice révélé : déclenche l'effet de révélation. */
    val hintToken: Int = 0,
)

/** Ce dont le contrôleur a besoin de l'application (navigation, messages, rafraîchissement de la progression). */
interface GameHost {
    fun onVictory(result: VictoryResult)
    fun onLeave()
    fun onProgressChanged()
    fun message(@AnyRes res: Int, quantity: Int? = null, plural: Boolean = false)
}

/**
 * Pilote une partie : gestes -> moteur -> sons/haptique -> sauvegarde. Un contrôleur par partie, avec sa propre portée
 * (un seul chronomètre d'affichage, annulé à la fermeture). Les écritures survivent à la fermeture de l'écran.
 */
class GameController(
    parentScope: CoroutineScope,
    val level: Level,
    val language: Language,
    private val useCases: GameUseCases,
    private val feedback: FeedbackController,
    private val clock: MonotonicClock,
    private val persistScope: CoroutineScope,
    private val readHintBalance: suspend () -> Int,
    private val host: GameHost,
) {
    private val scope = CoroutineScope(SupervisorJob(parentScope.coroutineContext[Job]) + Dispatchers.Main.immediate)
    private val session = GameSession(level, language, clock)
    private val _ui = MutableStateFlow(GameUiState(level, language, hintsLeftOnLevel = level.hints.maxPerLevel))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private var resumed = false
    private var closed = false
    private var hintBusy = false
    private var adBusy = false
    private var victoryFailed = false
    private var lastRestartAt = Long.MIN_VALUE / 2

    init {
        scope.launch {
            useCases.loadSnapshot(level, language)?.let { session.restore(it) }
            val balance = readHintBalance()
            syncFromSession()
            _ui.update { it.copy(ready = true, hintBalance = balance) }
            if (resumed && !session.isSolved) session.resume()
            if (session.isSolved) win() // sauvegarde interrompue juste après le dernier mot
        }
        scope.launch {
            while (isActive) {
                delay(TICK_MS)
                if (session.isRunning) _ui.update { it.copy(elapsedMillis = session.elapsedMillis()) }
            }
        }
    }

    private fun syncFromSession() {
        val state = session.state
        _ui.update {
            it.copy(
                foundWordIndices = state.foundWordIndices,
                hintCells = state.revealedHints.filter { h -> h.wordIndex !in state.foundWordIndices }.map { h -> h.cell },
                elapsedMillis = session.elapsedMillis(),
                solved = session.isSolved,
                hintsLeftOnLevel = (level.hints.maxPerLevel - state.hintsUsed).coerceAtLeast(0),
            )
        }
    }

    private fun canInteract(): Boolean {
        val s = _ui.value
        return s.ready && !s.solved && !session.isSolved && s.dialog == GameDialog.None && !closed
    }

    // ---- Tracé
    fun onSelectionStart(cell: Cell) {
        if (!canInteract()) return
        session.beginInteraction()
        feedback.play(Sound.SELECT_CELL)
        feedback.perform(HapticEffect.SELECTION)
        _ui.update { it.copy(selection = listOf(cell)) }
    }

    fun onSelectionChange(path: List<Cell>) {
        if (!canInteract()) return
        if (path.size > _ui.value.selection.size) {
            feedback.play(Sound.SELECT_CELL)
            feedback.perform(HapticEffect.SELECTION)
        }
        _ui.update { it.copy(selection = path) }
    }

    fun onSelectionEnd() {
        val path = _ui.value.selection
        _ui.update { it.copy(selection = emptyList()) }
        if (!canInteract() || path.size < 2) return
        when (val event = session.submit(path)) {
            is SessionEvent.Found -> {
                feedback.play(Sound.CORRECT)
                feedback.perform(HapticEffect.CONFIRM)
                syncFromSession()
                _ui.update { it.copy(foundToken = it.foundToken + 1, lastFoundIndex = event.wordIndex) }
                persistSnapshot()
                if (event.solved) win()
            }
            SessionEvent.Wrong -> {
                feedback.play(Sound.WRONG)
                feedback.perform(HapticEffect.ERROR)
                syncFromSession()
                _ui.update { it.copy(wrongToken = it.wrongToken + 1) }
            }
            else -> Unit
        }
    }

    // ---- Victoire : vérifiée par le moteur, enregistrée de façon atomique (avec reprises en cas d'échec disque)
    private fun win() {
        _ui.update { it.copy(solved = true) }
        scope.launch {
            delay(WIN_PAUSE_MS)
            var attempt = 0
            while (true) {
                try {
                    val result = withContext(NonCancellable) { useCases.completeLevel(session) }
                    host.onVictory(result)
                    return@launch
                } catch (e: Exception) {
                    attempt++
                    if (attempt >= MAX_SAVE_ATTEMPTS) {
                        victoryFailed = true
                        host.message(R.string.msg_error_generic)
                        return@launch
                    }
                    delay(RETRY_DELAY_MS)
                }
            }
        }
    }

    // ---- Indices
    fun onHintRequested() {
        if (!canInteract() || hintBusy) return
        hintBusy = true
        scope.launch {
            try {
                when (val result = useCases.useHint(session)) {
                    is HintUseResult.Used -> {
                        feedback.play(Sound.HINT)
                        feedback.perform(HapticEffect.CONFIRM)
                        syncFromSession()
                        _ui.update { it.copy(hintBalance = result.newBalance, hintToken = it.hintToken + 1) }
                        persistSnapshot()
                        host.onProgressChanged()
                    }
                    HintUseResult.NoBalance -> setDialog(GameDialog.RewardedOffer)
                    HintUseResult.LimitReached -> host.message(R.string.msg_hint_limit)
                    HintUseResult.NothingToReveal -> host.message(R.string.msg_hint_nothing)
                    HintUseResult.Failed -> host.message(R.string.msg_hint_failed)
                }
            } finally {
                hintBusy = false
            }
        }
    }

    fun onWatchAd() {
        if (adBusy) return
        setDialog(GameDialog.None)
        adBusy = true
        scope.launch {
            try {
                when (val outcome = useCases.watchRewardedAd()) {
                    is RewardOutcome.Granted -> {
                        feedback.play(Sound.PROGRESS_UP)
                        feedback.perform(HapticEffect.SUCCESS)
                        _ui.update { it.copy(hintBalance = readHintBalance()) }
                        host.message(R.plurals.msg_hints_earned, outcome.amount, true)
                        host.onProgressChanged()
                    }
                    RewardOutcome.Dismissed -> host.message(R.string.msg_ad_dismissed)
                    RewardOutcome.Duplicate -> host.message(R.string.msg_ad_duplicate)
                    is RewardOutcome.Unavailable -> host.message(
                        if (outcome.reason == UnavailableReason.OFFLINE) R.string.msg_ad_offline else R.string.msg_ad_unavailable,
                    )
                }
            } catch (e: Exception) {
                host.message(R.string.msg_error_generic)
            } finally {
                adBusy = false
            }
        }
    }

    // ---- Redémarrage, sortie, dialogues
    /**
     * Redémarrage immédiat, sans fenêtre de confirmation : la grille rejoue un mélange animé de ses lettres.
     * Un court verrou évite les doubles déclenchements pendant l'animation.
     */
    fun onRestartRequested() {
        val now = clock.nowMillis()
        if (!canInteract() || now - lastRestartAt < RESTART_LOCK_MS) return
        lastRestartAt = now
        session.restart()
        feedback.play(Sound.SHUFFLE)
        feedback.perform(HapticEffect.CONFIRM)
        syncFromSession()
        _ui.update { it.copy(selection = emptyList(), elapsedMillis = 0, restartToken = it.restartToken + 1) }
        persistScope.launch { runCatching { useCases.clearSnapshots(level) } }
    }

    /** Vrai pendant qu'un doigt trace sur la grille : aucune publicité ne doit s'afficher à ce moment. */
    fun isGridBeingTouched(): Boolean = _ui.value.selection.isNotEmpty()

    fun onBackRequested() {
        when {
            _ui.value.dialog != GameDialog.None -> setDialog(GameDialog.None)
            _ui.value.solved && !victoryFailed -> Unit
            session.hasStarted && !session.isSolved -> setDialog(GameDialog.ConfirmQuit)
            else -> leave()
        }
    }

    fun confirmQuit() {
        setDialog(GameDialog.None)
        leave()
    }

    fun dismissDialog() = setDialog(GameDialog.None)

    private fun leave() {
        persistSnapshot()
        host.onLeave()
    }

    private fun setDialog(dialog: GameDialog) {
        _ui.update { it.copy(dialog = dialog) }
        if (dialog == GameDialog.None) {
            if (resumed && !session.isSolved) session.resume()
        } else {
            session.pause()
        }
    }

    // ---- Cycle de vie : le chronomètre est suspendu en arrière-plan (appel, verrouillage, annonce plein écran…)
    fun onResume() {
        resumed = true
        if (_ui.value.ready && _ui.value.dialog == GameDialog.None && !session.isSolved) session.resume()
    }

    fun onPause() {
        resumed = false
        session.pause()
        persistSnapshot()
    }

    private fun persistSnapshot() {
        if (!session.hasStarted && session.state.revealedHints.isEmpty()) return
        val snapshot = session.snapshot() // pris sur le fil principal, écrit ailleurs
        persistScope.launch { runCatching { useCases.saveSnapshot(level, language, snapshot) } }
    }

    fun close() {
        closed = true
        scope.cancel()
    }

    private companion object {
        const val TICK_MS = 250L
        const val WIN_PAUSE_MS = 450L
        const val MAX_SAVE_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 800L
        /** Durée du verrou après un redémarrage (≈ durée de l'animation de mélange). */
        const val RESTART_LOCK_MS = 900L
    }
}
