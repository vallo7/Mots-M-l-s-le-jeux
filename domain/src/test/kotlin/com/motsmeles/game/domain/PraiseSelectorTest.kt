package com.motsmeles.game.domain

import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.engine.PraiseContext
import com.motsmeles.game.domain.engine.PraiseSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PraiseSelectorTest {
    private val weak = PraiseContext(1, hintsUsed = 2, wrongSelections = 4, elapsedMillis = 700_000, isFinalChallenge = false, perfectStreakBefore = 0)
    private val perfect = PraiseContext(3, 0, 0, 40_000, isFinalChallenge = true, perfectStreakBefore = 5)
    private val average = PraiseContext(2, 1, 2, 300_000, false, 0)

    @Test fun `trivial performance never gets more than Nice`() {
        assertEquals(0, PraiseSelector.points(weak))
        (0L..50L).forEach { assertEquals(Praise.NICE, PraiseSelector.select(weak, null, it)) }
    }

    @Test fun `perfect final challenge reaches the top of the ladder`() {
        assertEquals(7, PraiseSelector.points(perfect))
        val picks = (0L..60L).map { PraiseSelector.select(perfect, null, it) }.toSet()
        assertTrue(picks.all { it == Praise.DIVINE || it == Praise.LEGENDARY })
        assertTrue(Praise.LEGENDARY in picks)
    }

    @Test fun `average performance stays in the low-middle range`() {
        val cap = PraiseSelector.points(average)
        (0L..60L).forEach { assertTrue(PraiseSelector.select(average, null, it).ordinal <= cap) }
    }

    @Test fun `selection is deterministic for a given seed`() {
        assertEquals(PraiseSelector.select(perfect, null, 42), PraiseSelector.select(perfect, null, 42))
    }

    @Test fun `previous praise is not repeated when an alternative exists`() {
        (0L..60L).forEach {
            assertNotEquals(Praise.LEGENDARY, PraiseSelector.select(perfect, Praise.LEGENDARY, it))
            assertNotEquals(Praise.DIVINE, PraiseSelector.select(perfect, Praise.DIVINE, it))
        }
    }

    @Test fun `file names follow stable keys`() {
        assertEquals(listOf("nice", "great", "awesome", "amazing", "insane", "unreal", "divine", "legendary"), Praise.entries.map { it.key })
    }
}
