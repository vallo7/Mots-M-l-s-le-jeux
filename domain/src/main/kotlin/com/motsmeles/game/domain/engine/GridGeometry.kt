package com.motsmeles.game.domain.engine

import com.motsmeles.game.domain.model.Cell
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/** Géométrie du tracé : position tactile -> case, et alignement du tracé sur l'une des 8 directions. */
object GridGeometry {
    /** Case sous le point (x, y) ; les points hors grille sont ramenés sur la case de bord la plus proche. */
    fun cellAt(x: Float, y: Float, cellSize: Float, size: Int): Cell {
        if (cellSize <= 0f || size <= 0) return Cell(0, 0)
        val col = Math.floor((x / cellSize).toDouble()).toInt().coerceIn(0, size - 1)
        val row = Math.floor((y / cellSize).toDouble()).toInt().coerceIn(0, size - 1)
        return Cell(row, col)
    }

    /** Tracé aligné de [start] vers [current] : horizontal, vertical ou diagonal, toujours dans la grille. */
    fun snapPath(start: Cell, current: Cell, size: Int): List<Cell> {
        val dr = current.row - start.row
        val dc = current.col - start.col
        if (dr == 0 && dc == 0) return listOf(start)
        val adr = abs(dr)
        val adc = abs(dc)
        val stepR: Int
        val stepC: Int
        val length: Int
        when {
            adr >= 2 * adc -> { stepR = sign(dr.toFloat()).toInt(); stepC = 0; length = adr }
            adc >= 2 * adr -> { stepR = 0; stepC = sign(dc.toFloat()).toInt(); length = adc }
            else -> {
                stepR = sign(dr.toFloat()).toInt()
                stepC = sign(dc.toFloat()).toInt()
                val roomR = if (stepR > 0) size - 1 - start.row else start.row
                val roomC = if (stepC > 0) size - 1 - start.col else start.col
                length = min(max(adr, adc), min(roomR, roomC))
            }
        }
        return (0..length).map { Cell(start.row + stepR * it, start.col + stepC * it) }
    }
}
