package com.motsmeles.game

import com.motsmeles.game.domain.engine.MonotonicClock
import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.ports.AudioManager
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.HapticsManager
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.presentation.feedback.FeedbackController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackControllerTest {
    private class Clock(var now: Long = 10_000) : MonotonicClock { override fun nowMillis() = now }
    private class Audio : AudioManager {
        val played = mutableListOf<Any>()
        override fun play(sound: Sound) { played += sound }
        override fun playPraise(praise: Praise, language: Language) { played += praise }
        override fun setSoundEffectsEnabled(enabled: Boolean) = Unit
        override fun setMusicEnabled(enabled: Boolean) = Unit
        override fun release() = Unit
    }
    private class Haptics : HapticsManager {
        val performed = mutableListOf<HapticEffect>()
        override fun perform(effect: HapticEffect) { performed += effect }
        override fun setEnabled(enabled: Boolean) = Unit
    }

    @Test fun `repeated clicks inside the lock window are refused then accepted again`() {
        val clock = Clock()
        val f = FeedbackController(Audio(), Haptics(), clock)
        assertTrue(f.tryAcquireClick())
        clock.now += 100
        assertFalse(f.tryAcquireClick())
        clock.now += FeedbackController.CLICK_LOCK_MS
        assertTrue(f.tryAcquireClick())
    }

    @Test fun `the first click is always accepted`() {
        assertTrue(FeedbackController(Audio(), Haptics(), Clock(0)).tryAcquireClick())
    }

    @Test fun `sounds and haptics are forwarded`() {
        val a = Audio(); val h = Haptics()
        val f = FeedbackController(a, h, Clock())
        f.play(Sound.CLICK); f.playPraise(Praise.LEGENDARY, Language.FR); f.perform(HapticEffect.SUCCESS)
        assertEquals(listOf<Any>(Sound.CLICK, Praise.LEGENDARY), a.played)
        assertEquals(listOf(HapticEffect.SUCCESS), h.performed)
    }
}
