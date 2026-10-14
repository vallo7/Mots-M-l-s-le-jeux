package com.motsmeles.game.presentation.feedback

import com.motsmeles.game.domain.engine.MonotonicClock
import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.ports.AudioManager
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.HapticsManager
import com.motsmeles.game.domain.ports.Sound

/**
 * Point d'entrée unique des retours sonores et haptiques de l'interface, avec une garde anti double-clic
 * courte (jamais un blocage long des boutons).
 */
class FeedbackController(
    private val audio: AudioManager,
    private val haptics: HapticsManager,
    private val clock: MonotonicClock,
) {
    private var lastClickAt = Long.MIN_VALUE / 2

    /** Vrai si un clic peut être traité ; refuse les clics répétés pendant [CLICK_LOCK_MS]. */
    fun tryAcquireClick(): Boolean {
        val now = clock.nowMillis()
        if (now - lastClickAt < CLICK_LOCK_MS) return false
        lastClickAt = now
        return true
    }

    fun play(sound: Sound) = audio.play(sound)
    fun playPraise(praise: Praise, language: Language) = audio.playPraise(praise, language)
    fun perform(effect: HapticEffect) = haptics.perform(effect)

    companion object {
        const val CLICK_LOCK_MS = 350L
    }
}
