package com.motsmeles.game.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motsmeles.game.R
import com.motsmeles.game.presentation.theme.LocalReduceMotion
import com.motsmeles.game.presentation.theme.WgColors

/**
 * Fond blanc avec filigrane animé en boucle : une très grande grille de lettres presque invisible qui dérive
 * lentement, et un tracé qui se dessine puis se retire. Plus gros et plus discret qu'avant. Statique si animations réduites.
 */
@Composable
fun WatermarkBackground(
    modifier: Modifier = Modifier,
    background: Color = WgColors.Background,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(background)) {
        if (LocalReduceMotion.current) {
            WatermarkGrid(drift = { 0.5f }, sweep = { 1f })
        } else {
            val transition = rememberInfiniteTransition(label = "watermark")
            val drift = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing), RepeatMode.Reverse),
                label = "drift",
            )
            val sweep = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    keyframes {
                        durationMillis = 9_000
                        0f at 0 using LinearEasing
                        1f at 3_400 using FastOutSlowInEasing
                        1f at 6_800
                        0f at 9_000 using FastOutSlowInEasing
                    },
                ),
                label = "sweep",
            )
            WatermarkGrid(drift = { drift.value }, sweep = { sweep.value })
        }
        content()
    }
}

@Composable
private fun WatermarkGrid(drift: () -> Float, sweep: () -> Float) {
    val letters = stringResource(R.string.loading_tile_letters)
    Box(Modifier.fillMaxSize().clearAndSetSemantics { }) {
        Box(
            Modifier
                .align(Alignment.Center)
                .requiredSize(900.dp)
                .graphicsLayer {
                    val d = drift()
                    translationX = (d - 0.5f) * 110.dp.toPx()
                    translationY = (0.5f - d) * 80.dp.toPx()
                    rotationZ = -10f
                },
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(150.dp)
                    .graphicsLayer {
                        scaleX = sweep()
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .clip(RoundedCornerShape(75.dp))
                    .background(WgColors.Purple.copy(alpha = 0.045f)),
            )
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                for (r in 0 until 5) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (c in 0 until 5) {
                            Text(
                                text = letters[(r * 5 + c) % letters.length].toString(),
                                fontSize = 150.sp,
                                fontWeight = FontWeight.Black,
                                color = WgColors.Purple.copy(alpha = 0.035f),
                            )
                        }
                    }
                }
            }
        }
    }
}
