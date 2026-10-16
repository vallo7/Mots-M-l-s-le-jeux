package com.motsmeles.game.domain

import com.motsmeles.game.domain.engine.GameEngine
import com.motsmeles.game.domain.engine.HintOutcome
import com.motsmeles.game.domain.engine.HintService
import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.domain.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HintServiceTest {
    private val game = GameEngine.newGame(Fixtures.level(maxHints = 2), Language.EN)

    @Test fun `hint reveals first cell of first unfound word and costs one`() {
        val r = HintService.useHint(game, balance = 2) as HintOutcome.Revealed
        assertEquals(0, r.hint.wordIndex)
        assertEquals(Cell(0, 0), r.hint.cell)
        assertEquals(1, r.newBalance)
        assertEquals(1, r.state.hintsUsed)
    }

    @Test fun `no balance means no hint and no state change`() {
        assertEquals(HintOutcome.NoBalance, HintService.useHint(game, balance = 0))
    }

    @Test fun `already hinted words are skipped then nothing left`() {
        val first = HintService.useHint(game, 3) as HintOutcome.Revealed
        val second = HintService.useHint(first.state, first.newBalance) as HintOutcome.Revealed
        assertEquals(1, second.hint.wordIndex)
        assertEquals(HintOutcome.LimitReached, HintService.useHint(second.state, second.newBalance))
    }

    @Test fun `found words are never hinted`() {
        val found = GameEngine.select(game, Fixtures.SUN).state
        val r = HintService.useHint(found, 1) as HintOutcome.Revealed
        assertEquals(1, r.hint.wordIndex)
    }

    @Test fun `nothing to reveal when solved or all remaining words are already hinted`() {
        val solved = GameEngine.select(GameEngine.select(game, Fixtures.SUN).state, Fixtures.SKY).state
        assertEquals(HintOutcome.NothingToReveal, HintService.useHint(solved, 5))
        val roomy = GameEngine.newGame(Fixtures.level(maxHints = 5), Language.EN)
        val a = HintService.useHint(roomy, 5) as HintOutcome.Revealed
        val b = HintService.useHint(a.state, a.newBalance) as HintOutcome.Revealed
        assertEquals(HintOutcome.NothingToReveal, HintService.useHint(b.state, b.newBalance))
    }

    @Test fun `limit per level is enforced before balance`() {
        val capped = GameEngine.newGame(Fixtures.level(maxHints = 1), Language.EN)
        val a = HintService.useHint(capped, 5) as HintOutcome.Revealed
        assertTrue(HintService.useHint(a.state, a.newBalance) is HintOutcome.LimitReached)
    }
}
