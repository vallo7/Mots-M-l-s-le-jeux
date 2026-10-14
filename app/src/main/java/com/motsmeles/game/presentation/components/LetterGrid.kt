package com.motsmeles.game.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motsmeles.game.R
import com.motsmeles.game.domain.engine.GridGeometry
import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.presentation.theme.LocalReduceMotion
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgRadius

data class FoundHighlight(val cells: List<Cell>, val color: Color)

/**
 * Grille de lettres : point focal de l'écran de jeu. Le joueur trace avec le doigt une ligne droite (8 directions) ;
 * le tracé est aligné par [GridGeometry]. La grille ne décide jamais de la victoire : elle remonte le tracé au moteur.
 */
@Composable
fun LetterGrid(
    grid: List<String>,
    found: List<FoundHighlight>,
    hintCells: List<Cell>,
    selection: List<Cell>,
    selectionColor: Color,
    borderColor: Color,
    enabled: Boolean,
    shakeToken: Int,
    onSelectionStart: (Cell) -> Unit,
    onSelectionChange: (List<Cell>) -> Unit,
    onSelectionEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = grid.size
    val reduce = LocalReduceMotion.current
    val description = stringResource(R.string.game_grid_description, n)
    val latestStart by rememberUpdatedState(onSelectionStart)
    val latestChange by rememberUpdatedState(onSelectionChange)
    val latestEnd by rememberUpdatedState(onSelectionEnd)
    val pulse = rememberInfiniteTransition(label = "hint-pulse").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "hint-scale",
    )
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeToken) {
        if (shakeToken > 0 && !reduce) {
            shake.snapTo(0f)
            for (target in listOf(-9f, 9f, -6f, 6f, 0f)) shake.animateTo(target, tween(45))
        }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = if (maxWidth < maxHeight) maxWidth else maxHeight
        val density = LocalDensity.current
        val sidePx = with(density) { side.toPx() }
        val cellPx = sidePx / n
        val shape = RoundedCornerShape(WgRadius.Md)
        Box(
            Modifier
                .testTag("letter_grid")
                .size(side)
                .graphicsLayer { translationX = shake.value }
                .clip(shape)
                .background(WgColors.Surface)
                .border(3.dp, borderColor, shape)
                .semantics { contentDescription = description }
                .pointerInput(n, enabled, cellPx) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val start = GridGeometry.cellAt(down.position.x, down.position.y, cellPx, n)
                        latestStart(start)
                        var path = listOf(start)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                latestEnd()
                                return@awaitEachGesture
                            }
                            val cell = GridGeometry.cellAt(change.position.x, change.position.y, cellPx, n)
                            val next = GridGeometry.snapPath(start, cell, n)
                            if (next != path) {
                                path = next
                                latestChange(path)
                            }
                            change.consume()
                        }
                        latestEnd()
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cs = size.width / n
                found.forEach { drawCapsule(it.cells, it.color.copy(alpha = 0.55f), cs) }
                if (selection.isNotEmpty()) drawCapsule(selection, selectionColor.copy(alpha = 0.6f), cs)
                hintCells.forEach { cell ->
                    val scale = if (reduce) 1f else pulse.value
                    drawCircle(
                        color = WgColors.Teal,
                        radius = cs * 0.46f * scale,
                        center = Offset((cell.col + 0.5f) * cs, (cell.row + 0.5f) * cs),
                        style = Stroke(width = cs * 0.09f),
                    )
                }
            }
            val letterSize = with(density) { (cellPx * 0.5f).toSp() }
            Column(Modifier.fillMaxSize().clearAndSetSemantics { }) {
                for (r in 0 until n) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0 until n) {
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = grid[r][c].toString(),
                                    fontSize = letterSize,
                                    fontWeight = FontWeight.Bold,
                                    color = WgColors.Ink,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCapsule(cells: List<Cell>, color: Color, cellSize: Float) {
    val first = cells.first()
    val last = cells.last()
    drawLine(
        color = color,
        start = Offset((first.col + 0.5f) * cellSize, (first.row + 0.5f) * cellSize),
        end = Offset((last.col + 0.5f) * cellSize, (last.row + 0.5f) * cellSize),
        strokeWidth = cellSize * 0.78f,
        cap = StrokeCap.Round,
    )
}
