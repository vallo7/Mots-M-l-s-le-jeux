package com.motsmeles.game.presentation.startup

import androidx.annotation.StringRes
import androidx.compose.animation.core.CubicBezierEasing
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
import com.motsmeles.game.presentation.components.ChunkyProgressBar
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgMotion
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSize
import com.motsmeles.game.presentation.theme.WgSpacing
import com.motsmeles.game.presentation.theme.rememberReduceMotion
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * Chargement réel : la barre reflète les étapes exécutées (progression déterministe). Les tuiles rebondissent en vague
 * (écrasement + étirement), la barre est continue avec un reflet qui balaie ; aucun fondu.
 */
@Composable
fun LoadingScreen(progress: Float, @StringRes stepLabel: Int, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(WgMotion.SlowMs, easing = CubicBezierEasing(0.25f, 0.9f, 0.3f, 1f)),
        label = "loading-progress",
    )
    val description = stringResource(R.string.loading_progress_description)
    Column(
        modifier = modifier
            .testTag("loading_screen")
            .fillMaxSize()
            .background(WgColors.Background)
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
        ChunkyProgressBar(
            fraction = animated,
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .semantics {
                    contentDescription = description
                    progressBarRangeInfo = ProgressBarRangeInfo(animated.coerceIn(0f, 1f), 0f..1f)
                },
            color = WgColors.Purple,
            dark = WgColors.PurpleDark,
            height = 16.dp,
        )
        Spacer(Modifier.height(WgSpacing.Md))
        Text(
            text = stringResource(stepLabel),
            style = MaterialTheme.typography.bodyLarge,
            color = WgColors.InkMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/** Grille 3×3 de tuiles vives qui sautent en vague : écrasement au sol, étirement en l'air. Statique si animations réduites. */
@Composable
private fun LetterTiles() {
    val reduceMotion = rememberReduceMotion()
    val letters = stringResource(R.string.loading_tile_letters)
    val palette = listOf(
        WgColors.Yellow to WgColors.YellowDark,
        WgColors.Pink to WgColors.PinkDark,
        WgColors.Blue to WgColors.BlueDark,
        WgColors.Green to WgColors.GreenDark,
        WgColors.Purple to WgColors.PurpleDark,
    )
    val phase: State<Float> = rememberInfiniteTransition(label = "tiles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
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
                    val (face, lip) = palette[(row * 2 + col) % palette.size]
                    Box(
                        modifier = Modifier
                            .size(WgSize.Tile)
                            .graphicsLayer {
                                if (!reduceMotion) {
                                    // Saut : montée avec étirement, retombée avec écrasement.
                                    val t = ((phase.value - index * 0.07f) % 1f + 1f) % 1f
                                    val jump = max(0f, sin(t * PI.toFloat() * 1.6f)).let { if (t < 0.625f) it else 0f }
                                    val landing = if (t in 0.625f..0.8f) sin((t - 0.625f) / 0.175f * PI.toFloat()) else 0f
                                    translationY = -jump * 16.dp.toPx()
                                    scaleY = 1f + 0.12f * jump - 0.16f * landing
                                    scaleX = 1f - 0.07f * jump + 0.14f * landing
                                }
                            },
                    ) {
                        Box(Modifier.fillMaxSize().padding(top = 4.dp).background(lip, RoundedCornerShape(WgRadius.Md)))
                        Box(
                            Modifier.fillMaxSize().padding(bottom = 4.dp).background(face, RoundedCornerShape(WgRadius.Md)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = letters.getOrNull(index)?.toString().orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (face == WgColors.Yellow) WgColors.Ink else WgColors.White,
                            )
                        }
                    }
                }
            }
        }
    }
}
