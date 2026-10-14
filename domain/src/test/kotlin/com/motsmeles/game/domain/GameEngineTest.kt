package com.motsmeles.game.domain

import com.motsmeles.game.domain.engine.GameEngine
import com.motsmeles.game.domain.engine.SelectionResult
import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.domain.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    private val start = GameEngine.newGame(Fixtures.level(), Language.EN)

    @Test fun `selecting a target word finds it`() {
        val r = GameEngine.select(start, Fixtures.SUN)
        assertTrue(r is SelectionResult.Found)
        assertEquals(setOf(0), r.state.foundWordIndices)
        assertFalse(r.state.isSolved)
    }

    @Test fun `reverse drag also finds the word`() {
        val r = GameEngine.select(start, Fixtures.SUN.reversed())
        assertTrue(r is SelectionResult.Found)
    }

    @Test fun `partial selection is wrong and counted`() {
        val r = GameEngine.select(start, Fixtures.SUN.take(2))
        assertTrue(r is SelectionResult.Wrong)
        assertEquals(1, r.state.wrongSelections)
        assertTrue(r.state.foundWordIndices.isEmpty())
    }

    @Test fun `non straight path is wrong`() {
        val r = GameEngine.select(start, listOf(Cell(0, 0), Cell(0, 1), Cell(1, 1)))
        assertTrue(r is SelectionResult.Wrong)
    }

    @Test fun `out of bounds path is wrong`() {
        val r = GameEngine.select(start, listOf(Cell(0, 2), Cell(0, 3)))
        assertTrue(r is SelectionResult.Wrong)
    }

    @Test fun `single tap is ignored and not an error`() {
        val r = GameEngine.select(start, listOf(Cell(1, 1)))
        assertTrue(r is SelectionResult.Ignored)
        assertEquals(0, r.state.wrongSelections)
    }

    @Test fun `already found word does not count as error`() {
        val once = GameEngine.select(start, Fixtures.SUN).state
        val again = GameEngine.select(once, Fixtures.SUN)
        assertTrue(again is SelectionResult.AlreadyFound)
        assertEquals(0, again.state.wrongSelections)
    }

    @Test fun `level is solved only when every word is found`() {
        val s1 = GameEngine.select(start, Fixtures.SUN).state
        assertFalse(s1.isSolved)
        val s2 = GameEngine.select(s1, Fixtures.SKY).state
        assertTrue(s2.isSolved)
    }

    @Test fun `french variant uses its own grid`() {
        val fr = GameEngine.newGame(Fixtures.level(), Language.FR)
        val r = GameEngine.select(fr, listOf(Cell(2, 0), Cell(2, 1), Cell(2, 2)))
        assertTrue(r is SelectionResult.Found)
        assertEquals(1, (r as SelectionResult.Found).wordIndex)
    }
}
