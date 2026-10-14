package com.motsmeles.game.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.motsmeles.game.R
import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.progress.ProgressRules
import com.motsmeles.game.domain.ui.TimeFormat
import com.motsmeles.game.domain.usecases.VictoryResult
import com.motsmeles.game.presentation.AppState
import com.motsmeles.game.presentation.AppViewModel
import com.motsmeles.game.presentation.components.*
import com.motsmeles.game.presentation.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun praiseText(praise: Praise): Int = when (praise) {
    Praise.NICE -> R.string.praise_nice
    Praise.GREAT -> R.string.praise_great
    Praise.AWESOME -> R.string.praise_awesome
    Praise.AMAZING -> R.string.praise_amazing
    Praise.INSANE -> R.string.praise_insane
    Praise.UNREAL -> R.string.praise_unreal
    Praise.DIVINE -> R.string.praise_divine
    Praise.LEGENDARY -> R.string.praise_legendary
}

/**
 * Écran de victoire : les étoiles apparaissent une à une avec un rebond, le mot de félicitations rebondit (texte et voix),
 * puis le temps, l'accès au niveau suivant et, si le chapitre est terminé, le cadeau.
 */
@Composable
fun ResultScreen(result: VictoryResult, state: AppState, vm: AppViewModel) {
    val catalog = state.catalog ?: return
    val language = state.language
    val feedback = LocalFeedback.current
    val reduce = LocalReduceMotion.current
    val level = catalog.levelById(result.levelId)
    val giftClaimable = remember(catalog, state.progress) {
        ProgressRules.isGiftClaimable(catalog, state.progress, result.chapterIndex)
    }
    val starScales = remember(result.levelId) { List(3) { index -> Animatable(if (index < result.stars) 0f else 1f) } }
    val praiseScale = remember(result.levelId) { Animatable(0f) }

    LaunchedEffect(result.levelId) {
        feedback.play(Sound.VICTORY)
        feedback.perform(HapticEffect.SUCCESS)
        if (reduce) {
            starScales.forEach { it.snapTo(1f) }
            praiseScale.snapTo(1f)
            feedback.playPraise(result.praise, language)
            return@LaunchedEffect
        }
        delay(250)
        for (index in 0 until result.stars) {
            launch { starScales[index].animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow)) }
            feedback.play(Sound.SELECT_CELL)
            feedback.perform(HapticEffect.SELECTION)
            delay(240)
        }
        launch { praiseScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow)) }
        feedback.playPraise(result.praise, language)
    }

    val starsDescription = LocalContext.current.resources.getQuantityString(R.plurals.stars_count, result.stars, result.stars)
    WatermarkBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(WgSpacing.Lg)
                .testTag("result_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.result_title),
                style = MaterialTheme.typography.headlineLarge,
                color = WgColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            if (level != null) {
                Text(
                    stringResource(R.string.result_level_subtitle, level.chapterIndex, level.number, level.theme[language]),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WgColors.InkMuted,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(WgSpacing.Lg))
            Row(
                Modifier.semantics(mergeDescendants = true) { contentDescription = starsDescription },
                horizontalArrangement = Arrangement.spacedBy(WgSpacing.Sm),
            ) {
                for (index in 0 until 3) {
                    val earned = index < result.stars
                    Icon(
                        painterResource(if (earned) R.drawable.ic_star else R.drawable.ic_star_outline),
                        contentDescription = null,
                        modifier = Modifier
                            .size(76.dp)
                            .graphicsLayer {
                                val s = starScales[index].value
                                scaleX = s
                                scaleY = s
                                rotationZ = (1f - s) * -30f
                            },
                        tint = if (earned) WgColors.Sun else WgColors.InkMuted,
                    )
                }
            }
            Spacer(Modifier.height(WgSpacing.Md))
            Text(
                stringResource(praiseText(result.praise)),
                modifier = Modifier
                    .testTag("result_praise")
                    .graphicsLayer {
                        val s = praiseScale.value
                        scaleX = s
                        scaleY = s
                        rotationZ = (1f - s) * -12f
                    },
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WgColors.Plum,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(WgSpacing.Md))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WgSpacing.Sm)) {
                Text(stringResource(R.string.result_time), style = MaterialTheme.typography.titleMedium, color = WgColors.InkMuted)
                Text(TimeFormat.mmss(result.elapsedMillis), style = MaterialTheme.typography.headlineLarge, color = WgColors.Ink)
            }
            if (result.chapterCompleted) {
                Spacer(Modifier.height(WgSpacing.Sm))
                Text(stringResource(R.string.result_chapter_done), style = MaterialTheme.typography.titleLarge, color = WgColors.Ink, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.weight(1f))
            if (giftClaimable) {
                WgButton(
                    text = stringResource(R.string.result_gift_open),
                    onClick = { vm.claimGift(result.chapterIndex) },
                    modifier = Modifier.fillMaxWidth().testTag("result_gift"),
                    kind = ButtonKind.Secondary,
                    icon = R.drawable.ic_gift,
                )
                Spacer(Modifier.height(WgSpacing.Sm))
            }
            if (result.nextLevelId != null) {
                WgButton(
                    text = stringResource(R.string.result_next),
                    onClick = { vm.playNextAfter(result) },
                    modifier = Modifier.fillMaxWidth().testTag("result_next"),
                    enabled = !state.busy,
                    icon = R.drawable.ic_play,
                )
                Spacer(Modifier.height(WgSpacing.Sm))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WgSpacing.Sm)) {
                WgButton(
                    stringResource(R.string.result_home), vm::goHome, Modifier.weight(1f).testTag("result_home"),
                    kind = if (result.nextLevelId == null) ButtonKind.Primary else ButtonKind.Secondary,
                )
                WgButton(
                    stringResource(R.string.result_monument),
                    { vm.goHome(); vm.openMonument() },
                    Modifier.weight(1f).testTag("result_monument"),
                    kind = ButtonKind.Secondary,
                )
            }
        }
    }
}
