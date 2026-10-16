package com.motsmeles.game.presentation.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.motsmeles.game.R
import com.motsmeles.game.domain.engine.GridGeometry
import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.presentation.fx.GridFx
import com.motsmeles.game.presentation.fx.FoundHighlight
import com.motsmeles.game.presentation.theme.LocalReduceMotion
import com.motsmeles.game.presentation.theme.WgColors
import kotlinx.coroutines.isActive

/**
 * Grille de lettres : point focal de l'écran de jeu. Le joueur trace avec le doigt une ligne droite (8 directions) ;
 * le tracé est aligné par [GridGeometry]. La grille ne décide jamais de la victoire : elle remonte le tracé au moteur.
 *
 * Tout est dessiné dans un seul Canvas piloté par [GridFx] : serpent élastique pendant le tracé, lettres qui « popent »,
 * vague + étincelles quand un mot est trouvé, secousse à l'erreur, mélange des lettres au redémarrage. Aucun fondu.
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
    wrongToken: Int,
    restartToken: Int,
    onSelectionStart: (Cell) -> Unit,
    onSelectionChange: (List<Cell>) -> Unit,
    onSelectionEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = grid.size
    val reduce = LocalReduceMotion.current
    val density = LocalDensity.current
    val description = stringResource(R.string.game_grid_description, n)
    val latestStart by rememberUpdatedState(onSelectionStart)
    val latestChange by rememberUpdatedState(onSelectionChange)
    val latestEnd by rememberUpdatedState(onSelectionEnd)

    val fx = remember(grid) { GridFx(n, reduce) }
    fx.reduceMotion = reduce
    remember(fx) { fx.primeHints(hintCells); true }

    // ---- Déclencheurs d'effets (les valeurs initiales ne rejouent jamais d'animation après une rotation d'écran)
    val initialWrong = remember(fx) { wrongToken }
    val initialRestart = remember(fx) { restartToken }
    var previousFound by remember(fx) { mutableStateOf(emptyList<FoundHighlight>()) }
    LaunchedEffect(fx, found) {
        if (found.isEmpty() && previousFound.isNotEmpty()) fx.onFoundCleared(previousFound)
        fx.onFoundChanged(found)
        previousFound = found
    }
    LaunchedEffect(fx, selection) { fx.onSelectionChanged(selection) }
    LaunchedEffect(fx, hintCells) { fx.onHintCells(hintCells) }
    LaunchedEffect(fx, wrongToken) { if (wrongToken != initialWrong) fx.onWrong() }
    LaunchedEffect(fx, restartToken) {
        if (restartToken != initialRestart) {
            fx.inputLockedUntil = SystemClock.uptimeMillis() + GridFx.SHUFFLE_TOTAL_MS
            fx.onRestart(restartToken)
        }
    }

    // ---- Secousse élastique de la carte à l'erreur (ressort sous-amorti : plusieurs oscillations qui s'éteignent)
    val shake = remember { Animatable(0f) }
    val shakePx = with(density) { 9.dp.toPx() }
    LaunchedEffect(wrongToken) {
        if (wrongToken != initialWrong && !reduce) {
            shake.snapTo(shakePx)
            shake.animateTo(0f, spring(dampingRatio = 0.2f, stiffness = 650f))
        }
    }

    // ---- Boucle d'images : tourne uniquement tant qu'un effet est actif (ou qu'un indice bat)
    val keepAlive = hintCells.isNotEmpty()
    LaunchedEffect(fx, fx.wake, keepAlive) {
        while (isActive) {
            val t = withFrameMillis { it }
            fx.tick(t)
            if (!keepAlive && !fx.hasWork(t)) break
        }
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = if (maxWidth < maxHeight) maxWidth else maxHeight
        val frame = 5.dp
        val inner = side - frame * 2
        val innerPx = with(density) { inner.toPx() }
        val cellPx = innerPx / n
        val fontPx = cellPx * 0.56f
        val textMeasurer = rememberTextMeasurer()
        val glyphs = remember(grid, fontPx) {
            val style = TextStyle(fontWeight = FontWeight.Black, fontSize = with(density) { fontPx.toSp() })
            grid.joinToString("").toSet().associateWith { textMeasurer.measure(it.toString(), style) }
        }
        val shape = RoundedCornerShape(26.dp)
        val lipColor = lerp(borderColor, Color.Black, 0.3f)
        Box(
            Modifier
                .testTag("letter_grid")
                .size(side)
                .graphicsLayer { translationX = shake.value }
                .drawBehind {
                    drawRoundRect(lipColor, Offset(0f, 6.dp.toPx()), size, CornerRadius(26.dp.toPx()))
                }
                .clip(shape)
                .background(WgColors.White)
                .border(frame, borderColor, shape)
                .semantics { contentDescription = description }
                .padding(frame),
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(n, enabled, cellPx, fx) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (SystemClock.uptimeMillis() < fx.inputLockedUntil) return@awaitEachGesture
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
                    }
                    .clearAndSetSemantics { },
            ) {
                with(fx) {
                    drawAll(
                        grid = grid,
                        found = found,
                        selection = selection,
                        selectionColor = selectionColor,
                        hintCells = hintCells,
                        glyphs = glyphs,
                        fontPx = fontPx,
                        ink = WgColors.Ink,
                        tileColor = WgColors.Track.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}
