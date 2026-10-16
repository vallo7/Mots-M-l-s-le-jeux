package com.motsmeles.game.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motsmeles.game.R
import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.progress.ProgressRules
import com.motsmeles.game.domain.ui.TimeFormat
import com.motsmeles.game.domain.usecases.VictoryResult
import com.motsmeles.game.presentation.AppState
import com.motsmeles.game.presentation.AppViewModel
import com.motsmeles.game.presentation.components.ButtonKind
import com.motsmeles.game.presentation.components.ChunkyProgressBar
import com.motsmeles.game.presentation.components.ColorIcon
import com.motsmeles.game.presentation.components.RibbonBanner
import com.motsmeles.game.presentation.components.Ribbons
import com.motsmeles.game.presentation.components.RingButton
import com.motsmeles.game.presentation.components.StarFace
import com.motsmeles.game.presentation.components.TrophyBadge
import com.motsmeles.game.presentation.components.WatermarkBackground
import com.motsmeles.game.presentation.components.WgButton
import com.motsmeles.game.presentation.components.popIn
import com.motsmeles.game.presentation.theme.LocalFeedback
import com.motsmeles.game.presentation.theme.LocalReduceMotion
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

private const val STAR_FIRST_DELAY_MS = 420
private const val STAR_STEP_MS = 330

/**
 * Écran de victoire (maquette violette) : ruban « Victoire », rayons qui tournent derrière trois étoiles qui retombent
 * en rebondissant avec une onde de choc, mot de félicitations qui saute (texte + voix), temps, puis barre de progression
 * CONTINUE du chapitre qui se remplit en douceur jusqu'au trophée. Boutons ronds cerclés de blanc : accueil, niveaux, suivant.
 * Sans effet de fondu ni de particules de papier.
 */
@Composable
fun ResultScreen(result: VictoryResult, state: AppState, vm: AppViewModel) {
    val catalog = state.catalog ?: return
    val language = state.language
    val feedback = LocalFeedback.current
    val reduce = LocalReduceMotion.current
    val level = catalog.levelById(result.levelId)
    val trophyReady = remember(catalog, state.progress) { ProgressRules.isGiftClaimable(catalog, state.progress, result.chapterIndex) }
    val praiseScale = remember(result.levelId) { Animatable(if (reduce) 1f else 0f) }
    val barDelay = STAR_FIRST_DELAY_MS + 3 * STAR_STEP_MS + 650
    val progressChanged = result.chapterCompletedAfter != result.chapterCompletedBefore

    LaunchedEffect(result.levelId) {
        feedback.play(Sound.VICTORY)
        feedback.perform(HapticEffect.SUCCESS)
        if (reduce) {
            feedback.playPraise(result.praise, language)
            return@LaunchedEffect
        }
        delay(STAR_FIRST_DELAY_MS.toLong())
        for (index in 0 until result.stars) {
            feedback.play(Sound.SELECT_CELL)
            feedback.perform(HapticEffect.SELECTION)
            delay(STAR_STEP_MS.toLong())
        }
        launch { praiseScale.animateTo(1f, spring(dampingRatio = 0.34f, stiffness = 260f)) }
        feedback.playPraise(result.praise, language)
        if (progressChanged) {
            delay((barDelay - STAR_FIRST_DELAY_MS - result.stars * STAR_STEP_MS).coerceAtLeast(0).toLong())
            feedback.play(Sound.PROGRESS_UP)
            feedback.perform(HapticEffect.CONFIRM)
        }
    }

    val starsDescription = LocalContext.current.resources.getQuantityString(R.plurals.stars_count, result.stars, result.stars)
    WatermarkBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 22.dp)
                .testTag("result_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().popIn(key = result.levelId)) {
                val shape = RoundedCornerShape(32.dp)
                Column(
                    Modifier
                        .padding(top = 30.dp)
                        .fillMaxWidth()
                        .shadow(16.dp, shape)
                        .background(WgColors.PurpleDeep, shape)
                        .padding(bottom = 6.dp)
                        .background(WgColors.Purple, shape)
                        .padding(start = 18.dp, end = 18.dp, top = 52.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (level != null) {
                        Text(
                            stringResource(R.string.result_level_subtitle, level.chapterIndex, level.number, level.theme[language]),
                            style = MaterialTheme.typography.bodyLarge,
                            color = WgColors.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.semantics(mergeDescendants = true) { contentDescription = starsDescription }, contentAlignment = Alignment.Center) {
                        RotatingRays(Modifier.size(300.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            for (index in 0 until 3) {
                                VictoryStar(
                                    earned = index < result.stars,
                                    size = if (index == 1) 96.dp else 76.dp,
                                    delayMs = STAR_FIRST_DELAY_MS + index * STAR_STEP_MS,
                                    modifier = Modifier.padding(top = if (index == 1) 0.dp else 14.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(praiseText(result.praise)),
                        modifier = Modifier
                            .testTag("result_praise")
                            .graphicsLayer {
                                val s = praiseScale.value
                                scaleX = s
                                scaleY = s
                                rotationZ = (1f - s) * -14f
                            },
                        style = TextStyle(
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black,
                            shadow = Shadow(WgColors.PurpleDeep, Offset(0f, 8f), 0f),
                        ),
                        color = WgColors.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    TimePill(TimeFormat.mmss(result.elapsedMillis))
                    Spacer(Modifier.height(16.dp))
                    ChapterProgress(result, trophyReady, barDelay)
                    if (trophyReady) {
                        Spacer(Modifier.height(14.dp))
                        WgButton(
                            text = stringResource(R.string.result_gift_open),
                            onClick = { vm.claimGift(result.chapterIndex) },
                            modifier = Modifier.fillMaxWidth().testTag("result_gift"),
                            kind = ButtonKind.Primary,
                            sound = Sound.GIFT,
                            haptic = HapticEffect.SUCCESS,
                        )
                    }
                }
                RibbonBanner(
                    stringResource(R.string.result_title),
                    Ribbons.Pink,
                    Modifier.align(Alignment.TopCenter).fillMaxWidth(0.9f).semantics { heading() },
                    height = 62.dp,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RingButton(
                    R.drawable.ic_home, stringResource(R.string.result_home), vm::goHome,
                    Modifier.testTag("result_home").popIn(delayMs = barDelay - 200, key = "home"),
                    color = WgColors.Purple, dark = WgColors.PurpleDark, size = 60.dp, sound = Sound.MENU_CHANGE,
                )
                RingButton(
                    R.drawable.ic_levels, stringResource(R.string.cd_monument), vm::openMonument,
                    Modifier.testTag("result_levels").popIn(delayMs = barDelay - 120, key = "levels"),
                    color = WgColors.Purple, dark = WgColors.PurpleDark, size = 60.dp, sound = Sound.MENU_CHANGE,
                )
                if (result.nextLevelId != null) {
                    RingButton(
                        R.drawable.ic_play, stringResource(R.string.result_next), { vm.playNextAfter(result) },
                        Modifier.testTag("result_next").popIn(delayMs = barDelay - 40, key = "next"),
                        color = WgColors.Blue, dark = WgColors.BlueDark, size = 76.dp,
                    )
                }
            }
        }
    }
}

/** Étoile de victoire : tombe de haut en grossissant avec un fort rebond, onde de choc blanche à l'impact, visage souriant. */
@Composable
private fun VictoryStar(earned: Boolean, size: Dp, delayMs: Int, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(if (earned && !reduce) 0f else 1f) }
    val ring = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (earned && !reduce) {
            delay(delayMs.toLong())
            launch { ring.animateTo(1f, tween(560, easing = FastOutSlowInEasing)) }
            scale.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 280f))
        }
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (earned) {
            Canvas(Modifier.fillMaxSize()) {
                val p = ring.value
                if (p > 0.001f && p < 0.999f) {
                    drawCircle(Color.White, radius = this.size.minDimension * (0.35f + 0.85f * p), style = Stroke(width = 12.dp.toPx() * (1f - p)))
                }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = scale.value
                    scaleX = s
                    scaleY = s
                    rotationZ = (1f - s) * -45f
                },
        ) {
            ColorIcon(if (earned) R.drawable.ic_star else R.drawable.ic_star_off, Modifier.fillMaxSize())
            if (earned) StarFace()
        }
    }
}

/** Rayons de lumière qui tournent lentement derrière les étoiles (violet clair, opacité fixe : aucun fondu). */
@Composable
private fun RotatingRays(modifier: Modifier) {
    val reduce = LocalReduceMotion.current
    val angle = if (reduce) null else rememberInfiniteTransition(label = "rays").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(36_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rays-angle",
    )
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        val rays = 12
        rotate(angle?.value ?: 0f, c) {
            for (i in 0 until rays) {
                val a0 = (i * 360f / rays) * PI.toFloat() / 180f
                val a1 = ((i + 0.5f) * 360f / rays) * PI.toFloat() / 180f
                val path = Path().apply {
                    moveTo(c.x, c.y)
                    lineTo(c.x + cos(a0) * r, c.y + sin(a0) * r)
                    lineTo(c.x + cos(a1) * r, c.y + sin(a1) * r)
                    close()
                }
                drawPath(path, Color.White.copy(alpha = 0.13f))
            }
        }
    }
}

@Composable
private fun TimePill(time: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(WgRadius.Pill))
            .background(WgColors.White)
            .padding(start = 8.dp, end = 18.dp, top = 3.dp, bottom = 3.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ColorIcon(R.drawable.ic_clock, Modifier.size(34.dp))
        Text(
            time,
            style = TextStyle(fontWeight = FontWeight.Black, fontSize = 30.sp, fontFeatureSettings = "tnum"),
            color = WgColors.Ink,
        )
    }
}

/**
 * Progression du chapitre : barre CONTINUE qui part de l'avancement d'avant la victoire et se remplit de façon fluide
 * jusqu'à l'avancement actuel, avec reflet qui balaie, rebond d'épaisseur à l'arrivée, vers le trophée qui saute si prêt.
 */
@Composable
private fun ChapterProgress(result: VictoryResult, trophyReady: Boolean, startDelayMs: Int) {
    val total = result.chapterTotal.coerceAtLeast(1)
    val before = result.chapterCompletedBefore.toFloat() / total
    val after = result.chapterCompletedAfter.toFloat() / total
    val description = stringResource(R.string.home_gift_description, result.chapterCompletedAfter, total)
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description }.testTag("result_chapter_progress"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.result_chapter_progress, result.chapterIndex),
            style = MaterialTheme.typography.titleSmall,
            color = WgColors.White,
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChunkyProgressBar(
                fraction = after,
                modifier = Modifier.weight(1f),
                color = WgColors.Green,
                dark = WgColors.GreenDark,
                height = 24.dp,
                animateFrom = before,
                animationMs = 1300,
                startDelayMs = startDelayMs,
            )
            TrophyBadge(ready = trophyReady, size = 52.dp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (result.chapterCompleted) stringResource(R.string.result_chapter_done)
            else stringResource(R.string.home_gift_progress, result.chapterCompletedAfter, total),
            style = MaterialTheme.typography.labelLarge,
            color = WgColors.White,
            textAlign = TextAlign.Center,
        )
    }
}
