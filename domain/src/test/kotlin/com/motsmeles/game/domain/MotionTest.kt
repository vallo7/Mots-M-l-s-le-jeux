package com.motsmeles.game.domain

import com.motsmeles.game.domain.ui.Motion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test fun `spring starts at zero and settles on one`() {
        assertEquals(0f, Motion.spring(0f), 1e-6f)
        assertEquals(1f, Motion.spring(2f), 0.01f)
    }

    @Test fun `a bouncy spring overshoots one then comes back`() {
        val peak = (1..200).maxOf { Motion.spring(it / 100f, frequency = 16f, damping = 0.4f) }
        assertTrue("dépassement attendu, obtenu $peak", peak > 1.05f)
        assertEquals(1f, Motion.spring(Motion.springSettleSeconds(16f, 0.4f) * 1.5f, 16f, 0.4f), 0.02f)
    }

    @Test fun `a critically damped spring never overshoots`() {
        val peak = (1..300).maxOf { Motion.spring(it / 100f, frequency = 16f, damping = 1f) }
        assertTrue(peak <= 1.0001f)
    }

    @Test fun `settle time is finite and shrinks with frequency`() {
        assertTrue(Motion.springSettleSeconds(12f, 0.5f) > Motion.springSettleSeconds(24f, 0.5f))
        assertTrue(Motion.springSettleSeconds() in 0.1f..3f)
    }

    @Test fun `easings hit their endpoints`() {
        assertEquals(0f, Motion.easeOutBack(0f), 1e-5f)
        assertEquals(1f, Motion.easeOutBack(1f), 1e-5f)
        assertEquals(0f, Motion.easeInOutCubic(0f), 1e-6f)
        assertEquals(1f, Motion.easeInOutCubic(1f), 1e-6f)
        assertEquals(1f, Motion.easeOutCubic(1f), 1e-6f)
        assertTrue(Motion.easeOutBack(0.8f) > 1f)
    }

    @Test fun `bump is a 0 to 1 to 0 pulse`() {
        assertEquals(0f, Motion.bump(0f), 1e-5f)
        assertEquals(1f, Motion.bump(0.5f), 1e-5f)
        assertEquals(0f, Motion.bump(1f), 1e-5f)
    }

    @Test fun `quadratic arc passes through its endpoints`() {
        assertEquals(10f, Motion.quad(10f, 99f, 50f, 0f), 1e-5f)
        assertEquals(50f, Motion.quad(10f, 99f, 50f, 1f), 1e-5f)
    }
}
