package com.motsmeles.game.presentation.startup

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.motsmeles.game.R
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgMotion
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSize
import com.motsmeles.game.presentation.theme.WgSpacing
import com.motsmeles.game.presentation.theme.rememberReduceMotion
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/** Chargement réel : la barre reflète les étapes exécutées ; animation d'échelle uniquement (pas de fondu). */
@Composable
fun LoadingScreen(progress: Float, @StringRes stepLabel: Int, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(WgMotion.StandardMs, easing = FastOutSlowInEasing),
        label = "loading-progress",
    )
    Column(
        modifier = modifier
            .testTag("loading_screen")
            .fillMaxSize()
            .background(WgColors.Cream)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(WgSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LetterTiles()
        Spacer(Modifier.height(WgSpacing.Xl))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = WgColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(WgSpacing.Lg))
        ProgressTrack(animated)
        Spacer(Modifier.height(WgSpacing.Md))
        Text(
            text = stringResource(stepLabel),
            style = MaterialTheme.typography.bodyLarge,
            color = WgColors.InkMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProgressTrack(progress: Float) {
    val description = stringResource(R.string.loading_progress_description)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .height(10.dp)
            .clip(RoundedCornerShape(WgRadius.Pill))
            .background(WgColors.Outline)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
            },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(WgRadius.Pill))
                .background(WgColors.Coral),
        )
    }
}

/** Grille 3×3 de lettres dont les tuiles « respirent » en vague (échelle). Statique si animations réduites. */
@Composable
private fun LetterTiles() {
    val reduceMotion = rememberReduceMotion()
    val letters = stringResource(R.string.loading_tile_letters)
    val palette = listOf(WgColors.Sun, WgColors.Coral, WgColors.Teal)
    val phase: State<Float> = rememberInfiniteTransition(label = "tiles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "tile-phase",
    )
    Column(
        modifier = Modifier.clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(WgSpacing.Sm),
    ) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(WgSpacing.Sm)) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    Box(
                        modifier = Modifier
                            .size(WgSize.Tile)
                            .graphicsLayer {
                                if (!reduceMotion) {
                                    val t = ((phase.value - index * 0.09f) % 1f + 1f) % 1f
                                    val scale = 1f + 0.16f * max(0f, sin(t * 2f * PI.toFloat()))
                                    scaleX = scale
                                    scaleY = scale
                                }
                            }
                            .clip(RoundedCornerShape(WgRadius.Md))
                            .background(palette[(row + col) % palette.size]),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = letters.getOrNull(index)?.toString().orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            color = WgColors.Ink,
                        )
                    }
                }
            }
        }
    }
}
