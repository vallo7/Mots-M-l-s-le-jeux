package com.motsmeles.game.domain

import com.motsmeles.game.domain.engine.GameTimer
import com.motsmeles.game.domain.engine.MonotonicClock
import com.motsmeles.game.domain.engine.StarRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerAndStarsTest {
    private class FakeClock(var now: Long = 1_000L) : MonotonicClock {
        override fun nowMillis(): Long = now
    }

    @Test fun `timer does not run before first interaction`() {
        val clock = FakeClock()
        val timer = GameTimer(clock)
        clock.now += 5_000
        assertEquals(0, timer.elapsedMillis())
        assertFalse(timer.hasStarted)
    }

    @Test fun `timer counts from first interaction and ignores pauses`() {
        val clock = FakeClock()
        val timer = GameTimer(clock)
        timer.startIfNeeded()
        clock.now += 2_000
        timer.pause()
        clock.now += 60_000 // application en arrière-plan
        assertEquals(2_000, timer.elapsedMillis())
        timer.resume()
        clock.now += 1_500
        assertEquals(3_500, timer.elapsedMillis())
    }

    @Test fun `resume without start does nothing and startIfNeeded is idempotent`() {
        val clock = FakeClock()
        val timer = GameTimer(clock)
        timer.resume()
        assertFalse(timer.isRunning)
        timer.startIfNeeded()
        clock.now += 1_000
        timer.startIfNeeded()
        clock.now += 1_000
        assertEquals(2_000, timer.elapsedMillis())
    }

    @Test fun `stop freezes the time`() {
        val clock = FakeClock()
        val timer = GameTimer(clock)
        timer.startIfNeeded()
        clock.now += 4_000
        assertEquals(4_000, timer.stop())
        clock.now += 10_000
        timer.resume()
        assertEquals(4_000, timer.elapsedMillis())
    }

    @Test fun `reset restores the initial state`() {
        val clock = FakeClock()
        val timer = GameTimer(clock)
        timer.startIfNeeded()
        clock.now += 4_000
        timer.reset()
        assertEquals(0, timer.elapsedMillis())
        assertFalse(timer.hasStarted)
        timer.startIfNeeded()
        assertTrue(timer.isRunning)
    }

    @Test fun `star thresholds are exact including 10 minutes`() {
        assertEquals(3, StarRating.starsFor(0))
        assertEquals(3, StarRating.starsFor(179_999))
        assertEquals(2, StarRating.starsFor(180_000))
        assertEquals(2, StarRating.starsFor(599_999))
        assertEquals(1, StarRating.starsFor(600_000))
        assertEquals(1, StarRating.starsFor(3_600_000))
    }
}
