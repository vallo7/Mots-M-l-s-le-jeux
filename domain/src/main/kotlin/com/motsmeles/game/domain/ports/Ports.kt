package com.motsmeles.game.domain.ports

import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.LevelCatalog
import com.motsmeles.game.domain.progress.ProgressState
import com.motsmeles.game.domain.settings.AppSettings

/** Contenu des niveaux (assets embarqués, aucun réseau). */
interface LevelRepository {
    suspend fun loadCatalog(): LevelCatalog
}

/** Progression persistante. [update] est atomique : lecture-transformation-écriture. */
interface ProgressRepository {
    suspend fun read(): ProgressState
    suspend fun update(transform: (ProgressState) -> ProgressState): ProgressState
}

/** Réglages persistants. [update] est atomique. */
interface SettingsRepository {
    suspend fun read(): AppSettings
    suspend fun update(transform: (AppSettings) -> AppSettings): AppSettings
}

sealed interface RewardedAdResult {
    /** Récompense confirmée par le SDK. [rewardId] est unique par diffusion. */
    data class Rewarded(val rewardId: String) : RewardedAdResult
    data object Dismissed : RewardedAdResult
    data class Unavailable(val reason: UnavailableReason) : RewardedAdResult
}

enum class UnavailableReason { OFFLINE, NO_FILL, NOT_LOADED, ERROR }

/** Fournisseur de publicités (Google Mobile Ads aujourd'hui, remplaçable). Ne bloque jamais le jeu. */
interface AdProvider {
    /** null = une publicité récompensée est prête ; sinon la raison pour laquelle elle ne l'est pas. */
    suspend fun rewardedUnavailableReason(): UnavailableReason?
    suspend fun showRewarded(): RewardedAdResult
    /** @return true si un interstitiel a effectivement été affiché. */
    suspend fun showInterstitialIfReady(): Boolean
}

enum class Sound { CLICK, SELECT_CELL, CORRECT, WRONG, HINT, GIFT, PROGRESS_UP, VICTORY, MENU_CHANGE, SUCCESS_GENERIC }

interface AudioManager {
    fun play(sound: Sound)
    /** Joue le praise dans [language] ; repli sur [Sound.SUCCESS_GENERIC] si le fichier manque. */
    fun playPraise(praise: Praise, language: Language)
    fun setSoundEffectsEnabled(enabled: Boolean)
    fun setMusicEnabled(enabled: Boolean)
    fun release()
}

enum class HapticEffect { SELECTION, CONFIRM, ERROR, SUCCESS }

interface HapticsManager {
    fun perform(effect: HapticEffect)
    fun setEnabled(enabled: Boolean)
}
