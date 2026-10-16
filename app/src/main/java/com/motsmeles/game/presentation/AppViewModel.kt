package com.motsmeles.game.presentation

import android.util.Log
import androidx.annotation.AnyRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.motsmeles.game.AppContainer
import com.motsmeles.game.BuildConfig
import com.motsmeles.game.R
import com.motsmeles.game.domain.content.LevelValidator
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.Level
import com.motsmeles.game.domain.model.LevelCatalog
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.progress.AdMoment
import com.motsmeles.game.domain.progress.LevelStatus
import com.motsmeles.game.domain.progress.ReturnAdPolicy
import com.motsmeles.game.domain.progress.ProgressRules
import com.motsmeles.game.domain.settings.AppSettings
import com.motsmeles.game.domain.ui.ChapterStatus
import com.motsmeles.game.domain.usecases.GameUseCases
import com.motsmeles.game.domain.usecases.VictoryResult
import com.motsmeles.game.presentation.feedback.FeedbackController
import com.motsmeles.game.presentation.game.GameController
import com.motsmeles.game.presentation.game.GameHost
import com.motsmeles.game.presentation.startup.StartupException
import com.motsmeles.game.presentation.startup.StartupSequence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Source de vérité de l'application : navigation, progression, réglages, lancement des parties.
 * Les règles de jeu vivent dans `:domain` ; ce ViewModel ne fait que les orchestrer.
 */
class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    val feedback = FeedbackController(container.audio, container.haptics, container.clock)
    val bannerUnitId: String get() = container.bannerUnitId
    val privacyOptionsRequired: Boolean get() = container.consent.privacyOptionsRequired

    var activeGame: GameController? = null
        private set

    private var useCases: GameUseCases? = null
    private var messageSeq = 0L
    private var gameToken = 0L

    // Publicité de retour dans l'appli (horloge monotone : jamais l'heure civile du téléphone).
    private var stoppedAtMillis: Long? = null
    private var lastReturnAdAtMillis: Long? = null
    private var stoppedWhileAdShowing = false
    private var sessionStarted = false

    init {
        startup()
    }

    // ---- Démarrage à froid
    private fun startup() {
        val sequence = StartupSequence(
            repository = container.levelRepository,
            clock = container.clock,
            validate = { catalog -> withContext(Dispatchers.Default) { LevelValidator.validateCatalog(catalog) } },
            loadProfile = {
                container.progressRepository.update { ProgressRules.initialize(it) } // 2 indices offerts, une seule fois
                container.settingsRepository.read()
            },
            preloadAudio = { withContext(Dispatchers.IO) { container.audioEngine.preload() } },
            waitFor = { millis -> delay(millis) },
        )
        viewModelScope.launch {
            try {
                val catalog = sequence.run { progress, step ->
                    _state.update { it.copy(phase = Phase.Loading(progress, step.label())) }
                }
                finishStartup(catalog)
            } catch (e: StartupException) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, e.message ?: "startup failed", e)
                    e.issues.forEach { Log.e(TAG, it.toString()) }
                }
                _state.update { it.copy(phase = Phase.Failed) }
            }
        }
    }

    private fun StartupSequence.Step.label(): Int = when (this) {
        StartupSequence.Step.READING_LEVELS -> R.string.loading_step_read
        StartupSequence.Step.CHECKING_GRIDS -> R.string.loading_step_check
        StartupSequence.Step.LOADING_PROGRESS -> R.string.loading_step_progress
        StartupSequence.Step.LOADING_AUDIO -> R.string.loading_step_audio
        StartupSequence.Step.READY -> R.string.loading_step_ready
    }

    private suspend fun finishStartup(catalog: LevelCatalog) {
        val progress = container.progressRepository.read()
        val settings = container.settingsRepository.read()
        applySettings(settings)
        useCases = GameUseCases(catalog, container.progressRepository, container.ads, container.random)
        _state.update {
            it.copy(
                phase = Phase.Ready,
                catalog = catalog,
                progress = progress,
                settings = settings,
                language = settings.resolveLanguage(Locale.getDefault().language),
                stack = listOf(Screen.Home),
            )
        }
        sessionStarted = true
    }

    private fun applySettings(settings: AppSettings) {
        container.audio.setSoundEffectsEnabled(settings.soundEnabled)
        container.audio.setMusicEnabled(settings.musicEnabled)
        container.haptics.setEnabled(settings.hapticsEnabled)
    }

    private fun refreshProgress() {
        viewModelScope.launch {
            val progress = container.progressRepository.read()
            _state.update { it.copy(progress = progress) }
        }
    }

    // ---- Réglages
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val settings = container.settingsRepository.update(transform)
            applySettings(settings)
            _state.update { it.copy(settings = settings, language = settings.resolveLanguage(Locale.getDefault().language)) }
        }
    }

    fun toggleLanguage() {
        val next = if (_state.value.language == Language.FR) Language.EN else Language.FR
        updateSettings { it.copy(language = next) }
    }

    fun restoreDefaultSettings() = updateSettings { AppSettings.DEFAULT }

    // ---- Navigation
    private fun push(screen: Screen) = _state.update { it.copy(stack = it.stack + screen, direction = NavDirection.Forward) }

    private fun pop() = _state.update {
        if (it.stack.size > 1) it.copy(stack = it.stack.dropLast(1), direction = NavDirection.Back) else it
    }

    fun openSettings() = push(Screen.Settings)
    fun openMonument() = push(Screen.Monument)
    fun openCredits() = push(Screen.Credits)

    fun goHome() {
        closeGame()
        refreshProgress()
        _state.update { it.copy(stack = listOf(Screen.Home), direction = NavDirection.Back) }
    }

    /** Bouton Retour (physique, gestuel ou à l'écran) : ferme le menu courant ; le jeu gère sa propre confirmation. */
    fun onBack() {
        when (_state.value.stack.last()) {
            is Screen.Game -> activeGame?.onBackRequested()
            is Screen.Result -> goHome()
            Screen.Home -> Unit
            else -> pop()
        }
    }

    // ---- Lancement des parties
    fun playCurrent() {
        val catalog = _state.value.catalog ?: return
        ProgressRules.currentLevel(catalog, _state.value.progress)?.let { launchLevel(it) }
    }

    /**
     * Tap sur un chapitre du carrousel d'accueil : le chapitre en cours ouvre son niveau en cours ;
     * un chapitre terminé ou verrouillé répond par un court message (jamais de niveau gagné rejoué).
     */
    fun openChapter(chapterIndex: Int) {
        val home = _state.value.home() ?: return
        val card = home.chapters.firstOrNull { it.index == chapterIndex } ?: return
        when (card.status) {
            ChapterStatus.CURRENT -> card.currentLevel?.let { launchLevel(it) }
            ChapterStatus.COMPLETED -> showMessage(R.string.msg_chapter_completed)
            ChapterStatus.LOCKED -> showMessage(R.string.msg_chapter_locked)
        }
    }

    /** Tap sur un niveau du monument : seul le niveau disponible se lance. */
    fun openLevel(levelId: String) {
        val catalog = _state.value.catalog ?: return
        val level = catalog.levelById(levelId) ?: return
        when (ProgressRules.statusOf(catalog, _state.value.progress, levelId)) {
            LevelStatus.AVAILABLE -> launchLevel(level)
            LevelStatus.COMPLETED -> showMessage(R.string.msg_level_completed)
            LevelStatus.LOCKED -> showMessage(R.string.msg_level_locked)
        }
    }

    fun playNextAfter(result: VictoryResult) {
        val catalog = _state.value.catalog ?: return
        val next = result.nextLevelId?.let { catalog.levelById(it) }
        if (next != null) launchLevel(next) else goHome()
    }

    private fun launchLevel(level: Level) {
        val cases = useCases ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val progress = container.progressRepository.read()
                closeGame()
                val token = ++gameToken
                activeGame = GameController(
                    parentScope = viewModelScope,
                    level = level,
                    language = _state.value.language,
                    useCases = cases,
                    feedback = feedback,
                    clock = container.clock,
                    persistScope = container.persistScope,
                    readHintBalance = { container.progressRepository.read().hintBalance },
                    host = gameHost,
                )
                _state.update {
                    it.copy(
                        progress = progress,
                        stack = listOf(Screen.Home, Screen.Game(level.id, token)),
                        direction = NavDirection.Forward,
                    )
                }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private val gameHost = object : GameHost {
        override fun onVictory(result: VictoryResult) {
            closeGame()
            refreshProgress()
            _state.update { it.copy(stack = listOf(Screen.Home, Screen.Result(result)), direction = NavDirection.Forward) }
        }

        override fun onLeave() = goHome()
        override fun onProgressChanged() = refreshProgress()
        override fun message(@AnyRes res: Int, quantity: Int?, plural: Boolean) = showMessage(res, quantity, plural)
    }

    private fun closeGame() {
        activeGame?.close()
        activeGame = null
    }

    // ---- Cadeau de chapitre
    fun claimGift(chapterIndex: Int) {
        val cases = useCases ?: return
        viewModelScope.launch {
            val amount = runCatching { cases.claimGift(chapterIndex) }.getOrNull()
            if (amount != null) {
                container.audio.play(Sound.GIFT)
                container.haptics.perform(HapticEffect.SUCCESS)
                showMessage(R.plurals.msg_gift_earned, amount, plural = true)
            }
            refreshProgress()
        }
    }

    // ---- Messages
    fun showMessage(@AnyRes res: Int, quantity: Int? = null, plural: Boolean = false) {
        val message = UiMessage(++messageSeq, res, quantity, plural)
        _state.update { it.copy(message = message) }
        viewModelScope.launch {
            delay(MESSAGE_MS)
            _state.update { if (it.message?.id == message.id) it.copy(message = null) else it }
        }
    }

    // ---- Publicités / consentement / cycle de vie
    fun onConsentResult(canRequestAds: Boolean) {
        _state.update { it.copy(adsEnabled = canRequestAds) }
        if (canRequestAds) container.adsStarter.start()
    }

    fun openPrivacyOptions() {
        container.activityHolder.current?.let { container.consent.showPrivacyOptions(it) { } }
    }

    fun onAppStarted() {
        container.audioEngine.onAppStarted()
        val stoppedAt = stoppedAtMillis ?: return
        stoppedAtMillis = null
        val now = container.clock.nowMillis()
        val state = _state.value
        val moment = AdMoment(
            isFirstRender = state.phase != Phase.Ready,
            isGridBeingTouched = activeGame?.isGridBeingTouched() == true,
            isResolvingWin = state.stack.lastOrNull() is Screen.Result,
            isSensitiveActionPending = state.busy,
        )
        val show = state.adsEnabled && ReturnAdPolicy.shouldShow(
            awayMillis = now - stoppedAt,
            sinceLastReturnAdMillis = lastReturnAdAtMillis?.let { now - it },
            cameBackFromAd = stoppedWhileAdShowing,
            sessionStarted = sessionStarted,
            moment = moment,
        )
        stoppedWhileAdShowing = false
        if (!show) return
        viewModelScope.launch {
            // Une publicité qui échoue, n'est pas prête ou est hors ligne ne bloque jamais le jeu.
            val shown = runCatching { container.ads.showReturnAdIfReady() }.getOrDefault(false)
            if (shown) lastReturnAdAtMillis = container.clock.nowMillis()
        }
    }

    fun onAppStopped() {
        container.audioEngine.onAppStopped()
        stoppedAtMillis = container.clock.nowMillis()
        stoppedWhileAdShowing = container.ads.isFullScreenAdShowing()
    }

    override fun onCleared() {
        closeGame()
        super.onCleared()
    }

    companion object {
        private const val TAG = "App"
        private const val MESSAGE_MS = 2_300L
        fun factory(container: AppContainer) = viewModelFactory { initializer { AppViewModel(container) } }
    }
}
